package no.ion.hybridmodules;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.*;

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
            Objects.requireNonNull(hybridModuleName);
            this.hybridModuleName = hybridModuleName;
            Objects.requireNonNull(paths);
            this.hybridModulePath = paths;
        }

        /**
         * Require a particular version of the hybrid module.
         *
         * <p>If there are multiple hybrid modules with the given name in path, {@code setVersion} must be called
         * to pick the wanted version. If empty, the hybrid module without version is picked.
         */
        public void setVersion(Optional<ModuleDescriptor.Version> version) {
            Objects.requireNonNull(version);
            this.version = version;
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

    /** Load class from the hybrid module. */
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return root.getClassLoader().loadInternalClass(name);
    }

    /** Load exported class from the hybrid module. */
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
