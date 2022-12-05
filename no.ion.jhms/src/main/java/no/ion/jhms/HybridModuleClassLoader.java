package no.ion.jhms;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static no.ion.jhms.PackageUtil.getPackageName;

/** Class loader responsible for loading classes from a modular JAR. */
public class HybridModuleClassLoader extends ClassLoader {
    /**
     * The JVM apparently needed to resolve jdk.internal.reflect.SerializationConstructorAccessorImpl
     * with a HybridModuleClassLoader.  Not entirely sure why: it was around the invocation on an interface.
     * Likely triggered by internals of JVM.  But jdk.internal.reflect is not an (unqualified) exported package.
     * Delegating to application class loader resolves the issue.
     *
     * <p>Consider always letting the parent class loader load a class before the hybrid module.</p>.
     *
     * <p>This serves the same purpose as OSGi's org.osgi.framework.bootdelegation.</p>
     */
    private static final Set<String> IMPLICITLY_EXPORTED_CLASSES = Set.of("jdk.internal.reflect.SerializationConstructorAccessorImpl",
                                                                          "jdk.internal.reflect.ConstructorAccessorImpl");

    static {
        if (!ClassLoader.registerAsParallelCapable())
            throw new InternalError();
    }

    private final HybridModuleJar jar;
    private final HybridModule hybridModule;

    /** The set of packages defined by this module. */
    private final Set<String> packages = new TreeSet<>();

    /** The packages exported by this module. */
    private final Map<String, Set<String>> exports = new TreeMap<>();

    /** All packages visible to internal code including transient dependencies of the required hybrid modules. */
    private final TreeMap<String, HybridModule> hybridModulesByPackage;

    private final TreeMap<String, PlatformModule> platformModulesByPackage;

    HybridModuleClassLoader(HybridModuleJar jar,
                            HybridModule hybridModule,
                            Set<String> packages,
                            TreeMap<String, HybridModule> hybridModulesByPackage,
                            TreeMap<String, PlatformModule> platformModulesByPackage,
                            Map<String, Set<String>> exports) {
        super(jar.hybridModuleId().toString(),
                // The platform class loader should observe classes exactly 1:1 with the ModuleFinder.ofSystem()
                // used to find modules not provided by the application, see HybridModuleFinder. It's not
                // known whether this is in fact the case.
                ClassLoader.getPlatformClassLoader());
        this.jar = jar;
        this.hybridModule = hybridModule;
        this.hybridModulesByPackage = hybridModulesByPackage;
        this.platformModulesByPackage = platformModulesByPackage;

        this.packages.addAll(packages);
        this.exports.putAll(exports);
    }

    TreeMap<String, HybridModule> hybridModulesByPackage() {
        return new TreeMap<>(hybridModulesByPackage);
    }

    TreeMap<String, PlatformModule> platformModulesByPackage() {
        return new TreeMap<>(platformModulesByPackage);
    }

    /**
     * Load class in a package exported by THIS hybrid module.
     *
     * <p>The intended use-case for this method is when a some code outside of the module wants to get an
     * exported class defined by this hybrid module. Can be used to bootstrap hybrid modules execution.
     *
     * <p>DO NOT use {@link #loadClass(String)} for this use-case since it assumes the call is from within the module
     * and have access to all readable hybrid modules.
     */
    Class<?> loadExportedClass(String name) throws ClassNotFoundException {
        String packageName = getPackageName(name);
        if (!exports.containsKey(packageName)) {
            throw new ClassNotFoundException(name);
        }

        synchronized (getClassLoadingLock(name)) {
            // The documentation says this will cache only if this class was the initiating class loader,
            // but a test verified my suspicion that it also caches if this class is the defining class loader.
            Class<?> klass = findLoadedClass(name);
            if (klass == null) {
                klass = defineClassInJar(name);
                if (klass == null) {
                    throw new ClassNotFoundException(name);
                }
            }

            return klass;
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = loadClassUnlocked(name);

            if (resolve) {
                resolveClass(c);
            }

            return c;
        }
    }

    @Override
    public Enumeration<URL> findResources(String absoluteName) {
        throw new UnsupportedOperationException(HybridModuleClassLoader.class.getSimpleName() + ".findResources(String): " + absoluteName);
    }

    @Override
    public InputStream getResourceAsStream(String absoluteName) {
        Optional<String> packageName = PackageUtil.getPackageNameFromAbsoluteNameOfResource(absoluteName);

        if (packageName.isPresent()) {
            PlatformModule platformModule = platformModulesByPackage.get(packageName.get());
            if (platformModule != null) {
                return platformModule.getResourceAsStream(absoluteName);
            }
        }

        return packageName
                .map(name -> hybridModulesByPackage.getOrDefault(name, hybridModule))
                .orElse(hybridModule)
                // This works as intended even if 'getClassLoader() == this'.
                .getClassLoader()
                .jar
                .getResourceAsStream(absoluteName)
                .orElse(null);
    }

    @Override
    public URL getResource(String name) {
        try {
            return new URL("jhms", null, 0, "/" + hybridModule.id().toString() + "/" + name, new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL url) {
                    return new URLConnection(url) {
                        @Override public InputStream getInputStream() { return getResourceAsStream(name); }
                        @Override public void connect() {}

                    };
                }
            });
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Class<?> loadClassUnlocked(String name) throws ClassNotFoundException {
        // If the class has already been loaded.
        Class<?> c = findLoadedClass(name);
        if (c != null) {
            return c;
        }

        // If the class is in a readable platform module package
        String packageName = getPackageName(name);
        PlatformModule platformModule = platformModulesByPackage.get(packageName);
        if (platformModule != null || IMPLICITLY_EXPORTED_CLASSES.contains(name)) {
            return getParent().loadClass(name);
        }

        // If the class is in a readable hybrid module package
        HybridModule hybridModule = hybridModulesByPackage.get(packageName);
        if (hybridModule != null) {
            if (hybridModule.getClassLoader() == this) {
                return defineClassInJar(name);
            } else {
                return hybridModule.getClassLoader().loadExportedClass(name);
            }
        }

        throw new ClassNotFoundException(name);
    }

    private Class<?> defineClassInJar(String name) throws ClassNotFoundException {
        byte[] bytes = jar.getClassBytes(name);
        if (bytes == null) {
            throw new ClassNotFoundException(name);
        }

        return defineClass(name, bytes, 0, bytes.length);
    }
}
