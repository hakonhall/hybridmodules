package no.ion.jhms;

import java.lang.module.ModuleDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;

import static java.util.Objects.requireNonNull;

public class HybridModuleContainer implements AutoCloseable {
    private final HybridModuleFinder finder;
    private final HybridModule root;

    public static class ResolveParams {
        final String hybridModuleName;
        final Path[] hybridModulePath;

        Optional<ModuleDescriptor.Version> version = null;

        /**
         * @param hybridModuleName  The name of the root hybrid module to resolve.
         * @param paths             The hybrid module path specifying where to look for hybrid modules.
         */
        public ResolveParams(String hybridModuleName, Path... paths) {
            this.hybridModuleName = requireNonNull(hybridModuleName);
            this.hybridModulePath = requireNonNull(paths);
        }

        /**
         * Require a particular version of the hybrid module.
         *
         * <p>If there are multiple hybrid modules with the given name in path, {@code setVersion} must be called
         * to pick the wanted version. If empty, the hybrid module without version is picked.
         */
        public void setVersion(Optional<ModuleDescriptor.Version> version) {
            this.version = requireNonNull(version);
        }
    }

    public static HybridModuleContainer resolve(ResolveParams params) {
        HybridModuleFinder finder = HybridModuleFinder.of(params.hybridModulePath);
        try {
            HybridModuleResolver resolver = new HybridModuleResolver(finder);
            HybridModule module = resolver.resolve(params.hybridModuleName, params.version);
            HybridModuleContainer container = new HybridModuleContainer(finder, module);
            finder = null; // finder object is owned by container
            return container;
        } finally {
            if (finder != null) {
                finder.close();
            }
        }
    }

    private HybridModuleContainer(HybridModuleFinder finder, HybridModule root) {
        this.finder = finder;
        this.root = root;
    }

    /** Get the main class of the root hybrid module. */
    public Optional<String> getMainClass() {
        return root.getMainClass();
    }

    /** Invoke the main method of the root hybrid module. It must be in an exported package. */
    public void main(String... args) throws InvocationTargetException {
        main(null, args);
    }

    /** Invoke a main method in the root hybrid module. */
    public void main(String mainClassName, String... args) throws InvocationTargetException {
        if (mainClassName == null) {
            mainClassName = getMainClass()
                    .orElseThrow(() -> new IllegalArgumentException("The root hybrid module " +
                            root.getHybridModuleId() + " does not have a main class defined in the module descriptor"));
        }

        Class<?> mainClass;
        try {
            mainClass = loadExportedClass(mainClassName);
        } catch (ClassNotFoundException e) {
            NoClassDefFoundError error = new NoClassDefFoundError(mainClassName +
                    " not found in an exported package of hybrid module " + root.getHybridModuleId());
            error.initCause(e);
            throw error;
        }

        int mainClassModifiers = mainClass.getModifiers();
        // Note: We're allowing the main method to be defined in an abstract class or on an interface.
        if (!Modifier.isPublic(mainClassModifiers)) {
            throw new IllegalAccessError("The main class " + mainClassName + " in hybrid module " +
                    root.getHybridModuleId() + " is not public");
        }

        Method method;
        try {
            method = mainClass.getDeclaredMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("There is no main(String[]) method in class " + mainClassName +
                    " in hybrid module " + root.getHybridModuleId());
        }

        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers) || method.getReturnType() != void.class) {
            throw new IllegalArgumentException("The main(String[]) method in class " + mainClassName +
                    " in hybrid module " + root.getHybridModuleId() + " is not public static void");
        }

        try {
            method.invoke(null, (Object) args);
        } catch (IllegalAccessException e) {
            // This should never happen as we have successfully loaded a public and exported type.
            IllegalAccessError error = new IllegalAccessError("The public static void main(String[]) method in " + mainClassName +
                    " in hybrid module " + root.getHybridModuleId());
            error.initCause(e);
            throw error;
        }
    }

    /** Load class from the hybrid module. */
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return root.getClassLoader().loadInternalClass(name);
    }

    /** Load exported Class from the hybrid module. */
    public Class<?> loadExportedClass(String name) throws ClassNotFoundException {
        return root.getClassLoader().loadExportedClass(name);
    }

    @Override
    public void close() {
        finder.close();
    }

    public String getDependencyGraphDescription() {
        StringBuilder builder = new StringBuilder();
        Set<String> processedHybridModules = new HashSet<>();
        appendDependencyDescription(builder, processedHybridModules, root);
        return builder.toString();
    }

    private void appendDependencyDescription(StringBuilder builder,
                                             Set<String> processedHybridModules,
                                             HybridModule hybridModule) {
        if (processedHybridModules.contains(hybridModule.toString())) {
            return;
        }

        HybridModuleId id = hybridModule.getHybridModuleId();
        builder.append(id.toString()).append('\n');

        builder.append("  exported packages").append('\n');
        Map<String, HybridModule> exportedPackages = hybridModule.getExportedPackages();

        TreeMap<String, List<String>> hybridModuleNameByPackage = new TreeMap<>();
        for (var entry : exportedPackages.entrySet()) {
            hybridModuleNameByPackage.computeIfAbsent(entry.getValue().toString(), v -> new ArrayList<>()).add(entry.getKey());
        }

        for (var key : hybridModuleNameByPackage.navigableKeySet()) {
            builder.append("    hybrid module ").append(key).append('\n');
            hybridModuleNameByPackage.get(key)
                    .forEach(packageName -> builder.append("      ").append(packageName).append('\n'));
        }

        processedHybridModules.add(hybridModule.toString());


        Map<String, HybridModule> reads = hybridModule.getReads();

        reads.values().forEach(hm -> appendDependencyDescription(builder, processedHybridModules, hm));
    }
}
