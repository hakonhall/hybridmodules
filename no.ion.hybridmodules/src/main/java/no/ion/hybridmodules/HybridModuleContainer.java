package no.ion.hybridmodules;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.*;

public class HybridModuleContainer implements AutoCloseable {
    private final HybridModuleFinder finder;
    private final HybridModule hybridModule;

    public static class ResolveParams {
        final String moduleName;
        final Path[] hybridModulePath;

        Optional<ModuleDescriptor.Version> version = null;

        /**
         * @param hybridModuleName  The name of the hybrid module to resolve.
         * @param paths             The hybrid module path specifying which paths to look for JARs.
         */
        public ResolveParams(String hybridModuleName, Path... paths) {
            Objects.requireNonNull(hybridModuleName);
            this.moduleName = hybridModuleName;
            Objects.requireNonNull(paths);
            this.hybridModulePath = paths;
        }

        /**
         * Require a particular version of the hybrid module. If version is empty, the hybrid module must be
         * without a version.
         */
        public void setVersion(Optional<ModuleDescriptor.Version> version) {
            Objects.requireNonNull(version);
            this.version = version;
        }
    }

    public static HybridModuleContainer resolve(ResolveParams params) {
        HybridModuleFinder finder = HybridModuleFinder.of(params.hybridModulePath);
        try {
            HybridModule module = new HybridModuleResolver(finder).resolve(params.moduleName, params.version);
            HybridModuleContainer container = new HybridModuleContainer(finder, module);
            finder = null;
            return container;
        } finally {
            if (finder != null) {
                finder.close();
            }
        }
    }

    private HybridModuleContainer(HybridModuleFinder finder, HybridModule hybridModule) {
        this.finder = finder;
        this.hybridModule = hybridModule;
    }

    /** Load class from hybrid module. */
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return hybridModule.getClassLoader().loadInternalClass(name);
    }

    /** Load exported class from hybrid module. */
    public Class<?> loadExportedClass(String name) throws ClassNotFoundException {
        return hybridModule.getClassLoader().loadExportedClass(name);
    }

    @Override
    public void close() {
        finder.close();
    }

    public String getDependencyGraphDescription() {
        StringBuilder builder = new StringBuilder();
        Set<String> processedHybridModules = new HashSet<>();
        appendDependencyDescription(builder, processedHybridModules, hybridModule);
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
