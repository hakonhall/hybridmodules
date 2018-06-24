package no.ion.hybridmodules;

import java.lang.module.ModuleDescriptor;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static no.ion.hybridmodules.PackageUtil.getPackageName;

/** Class loader responsible for loading classes from a modular JAR. */
class HybridModuleClassLoader extends ClassLoader {
    // A StackWalker can be used by multiple threads and is thread-safe.
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private final Jar jar;

    /** The set of packages defined by this module. */
    private final Set<String> packages = new HashSet<>();

    /** The packages exported by this module. */
    private final Set<String> exportedPackages = new HashSet<>();

    /** All packages visible to internal code including transient dependencies of the required hybrid modules. */
    private final Map<String, HybridModule> readsByPackage;

    HybridModuleClassLoader(Jar jar, Map<String, HybridModule> hybridModulesByPackage) {
        super(jar.moduleId().toString(),
                // The platform class loader should observe classes exactly 1:1 with the ModuleFinder.ofSystem()
                // used to find modules not provided by the application, see HybridModuleClassLoader. It's not
                // known whether this is in fact the case.
                ClassLoader.getPlatformClassLoader());
        this.jar = jar;
        this.readsByPackage = hybridModulesByPackage;

        ModuleDescriptor descriptor = jar.descriptor();
        if (!descriptor.isOpen()) {
            // todo: support open module
            throw new InvalidHybridModuleException("Module at " + jar.uri() + " is not open");
        }

        if (!descriptor.opens().isEmpty()) {
            // todo: support open packages
            throw new InvalidHybridModuleException("Module at " + jar.uri() + " has open packages, which is not yet supported");
        }

        if (descriptor.isAutomatic()) {
            // todo: support automatic modules?
            throw new InvalidHybridModuleException("Module at " + jar.uri() + " is an automatic module, which is not supoprted");
        }

        for (var exports : descriptor.exports()) {
            if (exports.isQualified()) {
                // todo: support qualified exports
                throw new InvalidHybridModuleException("Module at " + jar.uri() + " has qualified exports, which is not yet supported");
            }

            exportedPackages.add(exports.source());
        }

        packages.addAll(descriptor.packages());

        for (var requires : descriptor.requires()) {
            Optional<ModuleDescriptor.Version> version = requires.compiledVersion();
            if (!version.isPresent()) {
                throw new InvalidHybridModuleException("Module at " + jar.uri() + " requires a module " + requires.name() +
                        " which is missing the version it was compiled at");
            }

            // todo:
            requires.modifiers();
        }
    }

    /**
     * Load class in a package exported by this hybrid module.
     *
     * <p>The intended use-case for this method is when a some code outside of the module wants to get an
     * exported class defined by this hybrid module. Can be used to bootstrap hybrid modules execution.
     *
     * <p>DO NOT use {@link #loadClass(String)} for this use-case since it assumes the call is from within the module
     * and have access to all readable hybrid modules.
     */
    Class<?> loadExportedClass(String name) throws ClassNotFoundException {
        // todo: support qualified exports and opens
        // Class<?> callerClass = STACK_WALKER.getCallerClass();
        // todo: need to get caller module, somehow. probably access class loader assuming HybridModuleClassLoader

        String packageName = getPackageName(name);
        if (!exportedPackages.contains(packageName)) {
            throw new ClassNotFoundException(name);
        }

        synchronized (getClassLoadingLock(name)) {
            // The documentation says this will cache only if this class was the initiating class loader,
            // but I bet it also caches if the defining class loader.
            // todo: double-check cache
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

    /**
     * Load class from hybrid module excluding required modules. Used for bootstrapping hybrid module execution,
     * e.g. finding main().
     */
    Class<?> loadInternalClass(String name) throws ClassNotFoundException {
        String packageName = getPackageName(name);
        if (!packages.contains(packageName)) {
            throw new ClassNotFoundException(name);
        }

        return loadClass(name);
    }

    /**
     * Bottom-half of loading a class as the initiating class loader (the top-half is {@link #loadClass}).
     *
     * <p>This method is only (must only be) called when a class in the module tries to load a class,
     * i.e. as the initiating class loader, which means the class is either in the parent,
     * in the module, or in a required module (by delegation).
     *
     * <p>To delegate loading to another {@code HybridModuleClassLoader}, {@link #loadExportedClass} is (must be) used.
     *
     * <p>{@code ClassLoader} recommends overriding {@link #loadClass(String)} instead of
     * {@link #loadClass(Module, String)}.
     *
     * @param name class name
     * @return Class instance
     * @throws ClassNotFoundException if package is not exported (or opened)
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String packageName = getPackageName(name);
        HybridModule hybridModule = readsByPackage.get(packageName);
        if (hybridModule == null) {
            throw new ClassNotFoundException(name);
        }

        if (hybridModule.getClassLoader() == this) {
            Class<?> klass = defineClassInJar(name);
            if (klass == null) {
                throw new ClassNotFoundException(name);
            }
            return klass;
        } else {
            return hybridModule.getClassLoader().loadExportedClass(name);
        }
    }

    private Class<?> defineClassInJar(String name) {
        byte[] bytes = jar.getClassBytes(name);
        if (bytes == null) {
            return null;
        }

        return defineClass(name, bytes, 0, bytes.length);
    }

    /**
     * Called from another modules's {@link #findClass} when it needs to delegate loading (via {@code requires}).
     */
    private Class<?> delegatedLoadingOfExportedClass(String name) throws ClassNotFoundException {
        // todo
        return null;
    }
}
