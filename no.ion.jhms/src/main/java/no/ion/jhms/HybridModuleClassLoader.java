package no.ion.jhms;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.*;

import static no.ion.jhms.PackageUtil.getPackageName;
import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;

/** Class loader responsible for loading classes from a modular JAR. */
public class HybridModuleClassLoader extends ClassLoader {
    private static final StackWalker stackWalker = StackWalker.getInstance(RETAIN_CLASS_REFERENCE);

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
    public Enumeration<URL> getResources(String name) throws IOException {
        return super.getResources(name);
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

        // While testing with the JUnit 5 console launcher it was found that
        // java.lang.reflect.AnnotatedElement.isAnnotationPresent() requires jdk.internal.reflect.ConstructorAccessorImpl
        // in java.base to be loaded by the invoking class loader.  But its package is not exported.
        //
        // In JPMS all (types in) packages of all modules are visible, and all types can be loaded successfully.
        // It is only if such types are accessed, e.g. a constructor being invoked, that JPMS enforces its accessibility
        // restrictions.
        //
        // In JHMS a non-exported package is not even visible, which would cause a ClassNotFoundException to be thrown.
        //
        // OSGi also noted irregularities w.r.t. class loading of system classes.  From its Core 7 specification:
        // "Certain Java virtual machines, also Oracle's VMs, appear to make the erroneous assumption that the
        // delegation to the parent class loader always occurs."  OSGi specifies a org.osgi.framework.bootdelegation
        // system property to force delegation to the "system class loader" (AFAIK the parent class loader of the
        // OSGi framework).  OSGi always delegates for java.* packages.
        //
        // Note: The canonical way to use reflection on a class C in a hybrid module M is to use M's class loader.
        //
        // In JHMS delegation is always to parent first (system class loader, not application class loader that
        // includes the class path).  An alternative is to have a system property of packages to delegate for,
        // like org.osgi.framework.bootdelegation.

        try {
            return getParent().loadClass(name);
        } catch (ClassNotFoundException e) {
            // nothing
        }

        // If the class is in a readable platform module package
        String packageName = getPackageName(name);
        /*PlatformModule platformModule = platformModulesByPackage.get(packageName);
        if (platformModule != null) {
            return getParent().loadClass(name);
        }*/

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
