package no.ion.jhms;

import java.util.*;

import static no.ion.jhms.PackageUtil.getPackageName;

/** Class loader responsible for loading classes from a modular JAR. */
public class HybridModuleClassLoader extends ClassLoader {
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

    private Class<?> loadClassUnlocked(String name) throws ClassNotFoundException {
        // If the class has already been loaded.
        Class<?> c = findLoadedClass(name);
        if (c != null) {
            return c;
        }

        // If the class is in a readable platform module package
        String packageName = getPackageName(name);
        PlatformModule platformModule = platformModulesByPackage.get(packageName);
        if (platformModule != null) {
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
