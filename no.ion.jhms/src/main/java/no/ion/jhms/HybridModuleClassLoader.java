package no.ion.jhms;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import static no.ion.jhms.PackageUtil.getPackageName;

/** Class loader responsible for loading classes from a modular JAR. */
public class HybridModuleClassLoader extends ClassLoader {

    static {
        if (!ClassLoader.registerAsParallelCapable())
            throw new InternalError();
    }

    private final HybridModuleJar jar;
    private final HybridModule hybridModule;

    /** The packages exported by this module. */
    private final Map<String, Set<String>> exports = new TreeMap<>();

    /** All packages visible to internal code including transient dependencies of the required hybrid modules. */
    private final TreeMap<String, HybridModule> hybridModulesByPackage;

    private final TreeMap<String, PlatformModule> platformModulesByPackage;

    HybridModuleClassLoader(HybridModuleJar jar,
                            HybridModule hybridModule,
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
        try {
            return Collections.enumeration(List.of(getResource(absoluteName)));
        } catch (UncheckedIOException e) {}
        return Collections.enumeration(List.of());
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
        if (platformModulesByPackage.containsKey(packageName)) {
            return getParent().loadClass(name);
        }

        // Some special classes must be loaded by the platform class loader
        if (tryLoadingWithPlatformClassLoaderHack(name)) {
            try {
                return getParent().loadClass(name);
            } catch (ClassNotFoundException ignored) {}
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

        throw new ClassNotFoundException(name + ": its package is not exported by any module read by hybrid module " + this.hybridModule.id());
    }

    /**
     * It has been observed that various JDK classes in packages OUTSIDE those exported by 'requires'
     * at compile time, needs to be resolved at run time.  This method returns true for such class names
     * (and others unfortunately).  If the loading fails, the failure should be ignored to allow the
     * hybrid module and its dependencies to load it.
     *
     * <p>Example 1: The JVM apparently needed to resolve jdk.internal.reflect.SerializationConstructorAccessorImpl
     * with a HybridModuleClassLoader.  Not entirely sure why: it was around the invocation on an interface.
     * Likely triggered by internals of JVM.  But jdk.internal.reflect is not an (unqualified) exported package.
     * Delegating to application class loader resolves the issue.</p>
     *
     * <p>Example 2: jdk.internal.reflect.ConstructorAccessorImpl is another case.</p>
     *
     * <p>Example 3: Jimfs tries to add a file system provider and loops over the JRE's provides.  The JRE apparently
     * tries to load all file system providers with the caller's class loader (which seems like a bug),
     * and an exception is thrown because jdk.internal.jrtfs.JrtFileSystemProvider isn't even in an exported
     * package, and its module may not be in the read modules even.  Passing loading through to the
     * platform class loader allows the class to be loaded.</p>
     *
     * <p>Therefore, I have found all implementations of module services in OpenJDK 17.  All 123
     * match com.sun.*, sun.*, or jdk.* except one special case: org.jcp.xml.dsig.internal.dom.XMLDSigRI.</p>
     *
     * <p>This serves the same purpose as OSGi's org.osgi.framework.bootdelegation.</p>
     */
    private boolean tryLoadingWithPlatformClassLoaderHack(String className) {
        return className.startsWith("com.sun.") ||
               className.startsWith("sun.") ||
               className.startsWith("jdk.") ||
               className.equals("org.jcp.xml.dsig.internal.dom.XMLDSigRI");
    }

    private Class<?> defineClassInJar(String name) throws ClassNotFoundException {
        byte[] bytes = jar.getClassBytes(name);
        if (bytes == null) {
            throw new ClassNotFoundException(name);
        }

        return defineClass(name, bytes, 0, bytes.length);
    }
}
