package no.ion.jhms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

class HybridModule2 {
    private final HybridModuleId2 id;
    private final HybridModularJar jar;
    private final Set<HybridModule2> directHybridModuleDependencies;
    private final Set<PlatformModule> directPlatformModulesDependencies;
    private final Set<HybridModule2> directTransitiveHybridModules;
    private final Set<PlatformModule> directTransitivePlatformModules;
    private final Map<String, Set<String>> directExports;

    // Lookup table mapping package name to module
    private final Map<String, HybridModule2> readableHybridModulesByPackage = new HashMap<>();
    private final Map<String, PlatformModule> readablePlatformModulesByPackage = new HashMap<>();

    private HybridModule2(HybridModuleId2 id,
                          HybridModularJar jar,
                          Set<HybridModule2> directHybridModuleDependencies,
                          Set<PlatformModule> directPlatformModulesDependencies,
                          Set<HybridModule2> directTransitiveHybridModules,
                          Set<PlatformModule> directTransitivePlatformModules,
                          Map<String, Set<String>> directExports) {
        this.id = id;
        this.jar = jar;
        this.directHybridModuleDependencies = directHybridModuleDependencies;
        this.directPlatformModulesDependencies = directPlatformModulesDependencies;
        this.directTransitiveHybridModules = directTransitiveHybridModules;
        this.directTransitivePlatformModules = directTransitivePlatformModules;
        this.directExports = directExports;

        initialize();
    }

    HybridModuleId2 id() { return id; }
    HybridModuleDescriptor descriptor() { return jar.descriptor(); }
    Set<HybridModule2> getDirectHybridModuleDependencies() { return directHybridModuleDependencies; }
    Set<PlatformModule> getDirectPlatformModulesDependencies() { return directPlatformModulesDependencies; }
    Set<HybridModule2> getDirectTransitiveHybridModules() { return directTransitiveHybridModules; }
    Set<PlatformModule> getDirectTransitivePlatformModules() { return directTransitivePlatformModules; }

    @Override
    public boolean equals(Object other) {
        // Normally, equality would be determined by this.id. However if we ever support instantiating more
        // than one run time hybrid module for a given hybrid module name and version, we would determine
        // equality by reference anyways.
        return other == this;
    }

    @Override
    public int hashCode() {
        // See equals()
        return super.hashCode();
    }

    static class Builder {
        private final HybridModuleId2 id;
        private final HybridModularJar jar;

        // All direct dependencies (requires)
        private final Set<HybridModule2> directHybridModuleDependencies = new HashSet<>();
        private final Set<PlatformModule> directPlatformModulesDependencies = new HashSet<>();

        // The direct transitive modules of this hybrid module
        private final Set<HybridModule2> directTransitiveHybridModules = new HashSet<>();
        private final Set<PlatformModule> directTransitivePlatformModules = new HashSet<>();

        private final TreeMap<String, Set<String>> directExports = new TreeMap<>();

        private final Set<String> packages = new TreeSet<>();

        Builder(HybridModuleId2 id, HybridModularJar jar) {
            this.id = id;
            this.jar = jar;
        }

        void addHybridModuleRequires(HybridModule2 hybridModule, boolean transitive) {
            directHybridModuleDependencies.add(hybridModule);
            if (transitive) directTransitiveHybridModules.add(hybridModule);
        }

        void addPlatformModuleRequires(PlatformModule platformModule, boolean transitive) {
            directPlatformModulesDependencies.add(platformModule);
            if (transitive) directTransitivePlatformModules.add(platformModule);
        }

        void addExports(String packageName, Set<String> targetModuleNames) {
            directExports.put(packageName, targetModuleNames);
        }

        void addPackages(Set<String> packages) {
            this.packages.addAll(packages);
        }

        HybridModule2 build() {
            return new HybridModule2(
                    id,
                    jar,
                    directHybridModuleDependencies,
                    directPlatformModulesDependencies,
                    directTransitiveHybridModules,
                    directTransitivePlatformModules,
                    directExports);
        }
    }

    private void initialize() {
        ReadabilityResolver resolver = new ReadabilityResolver(this);
   }

    /** Returns all packages that are exported (transitively) by {@code this} to {@code id}. */
    private Map<String, HybridModule2> getTransitiveHybridPackagesExportedTo(HybridModuleId2 id) {
        var exportedPackages = new HashMap<String, HybridModule2>();

        for (var entry : transitiveExports.entrySet()) {
            Set<String> moduleNamesExportedTo = entry.getValue();
            if (moduleNamesExportedTo.size() > 0 && !moduleNamesExportedTo.contains(id.name())) continue;

            String packageName = entry.getKey();
            HybridModule2 hybridModule = readableHybridModulesByPackage.get(packageName);
            if (hybridModule == null) {
                throw new IllegalStateException("Hybrid module " + this.id + " exports package " +
                        packageName + " to " + id + ", either directly or transitively, " +
                        "but do not know which hybrid module that exports it");
            }

            exportedPackages.put(packageName, hybridModule);
        }

        return exportedPackages;
    }






    /** Returns all packages that are exported (transitively) by {@code this} to {@code id}. */
    private TreeMap<String, PlatformModule> getAllPlatformPackagesExportedTo(HybridModuleId2 id) {
        var exportedPackages = new TreeMap<String, PlatformModule>();

        for (var entry : exports.entrySet()) {
            Set<String> moduleNamesExportedTo = entry.getValue();
            if (moduleNamesExportedTo.size() > 0 && !moduleNamesExportedTo.contains(id.name())) continue;

            String packageName = entry.getKey();
            PlatformModule platformModule = readablePlatformModulesByPackage.get(packageName);
            if (platformModule == null) {
                throw new IllegalStateException("Hybrid module " + this.id + " exports package " +
                        packageName + " to " + id + ", transitively, " +
                        "but do not know which platform module that exports it");
            }

            exportedPackages.put(packageName, platformModule);
        }

        return exportedPackages;
    }

    private Set<HybridModule2> getReadableHybridModules() {
        HashMap<String, HybridModule2> readableHybridModulesByName = new HashMap<>();

    }

    private void addHybridModuleDependency(HybridModule2 hybridModule) {
        HybridModule2 existingHybridModuleDependencyWithSameName =
                hybridModuleDependenciesByName.get(hybridModule.id().name());
        if (existingHybridModuleDependencyWithSameName != null) {
            if (hybridModule.id().version().equals(existingHybridModuleDependencyWithSameName.id().version())) {
                return; // already added
            } else {
                throw new InvalidHybridModuleException("Hybrid module " + id + " reads " + hybridModule.id.name() +
                        " both at version " + existingHybridModuleDependencyWithSameName.id.version() + " and " +
                        hybridModule.id.version());
            }
        }

        hybridModuleDependenciesByName.put(hybridModule.id().name(), existingHybridModuleDependencyWithSameName);

        for (HybridModule2 hybridModuleDependency : hybridModule.getDirectHybridModuleDependencies()) {
            addHybridModuleDependency(hybridModuleDependency);
        }

        for (PlatformModule platformModuleDependency : hybridModule.getDirectPlatformModulesDependencies()) {
            addPlatformModuleRequires(platformModuleDependency);
        }
    }

    private void addPlatformModule(PlatformModule platformModule, boolean transitive) {
        if (!directPlatformModulesDependencies.add(platformModule)) {
            // JLS 11, 7.7.1 Dependences
            throw new InvalidHybridModuleException("Hybrid module " + id + " requires " + platformModule.name() + " twice");
        }

        if (platformModulesDependencies.add(platformModule)) {
            platformModulesDependencies.addAll(platformModule.getAllTransitivePlatformDependencies());
        } else {
            // transitive dependencies have presumably been added already
        }

        if (transitive) {
            if (directTransitivePlatformModules.add(platformModule)) {
                directTransitivePlatformModules.addAll(platformModule.getAllTransitivePlatformDependencies());
            } else {
                // transitive dependencies have presumably been added already
            }
        }
    }



}
