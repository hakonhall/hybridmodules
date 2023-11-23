package no.ion.jhms;

import java.lang.module.ResolutionException;
import java.util.*;
import java.util.stream.Collectors;

class HybridModule extends BaseModule {
    private final HybridModuleId id;
    private final HybridModuleJar jar;
    private final List<PlatformModule> platformReads;
    private final List<PlatformModule> platformReadClosure;
    private final List<HybridModule> hybridReads;
    private final List<HybridModule> hybridReadClosure;
    private final HashMap<String, Boolean> transitiveByRequires;

    private HybridModuleClassLoader classLoader;

    private HybridModule(HybridModuleJar jar,
                         Set<String> packages,
                         Map<String, PlatformModule> platformReads,
                         Map<String, PlatformModule> platformReadClosure,
                         Map<HybridModuleId, HybridModule> hybridReads,
                         Map<HybridModuleId, HybridModule> hybridReadClosure,
                         Map<String, Set<String>> exports,
                         HashMap<String, Boolean> transitiveByRequires) {
        super(jar.hybridModuleId().name(), packages, exports);
        this.id = jar.hybridModuleId();
        this.jar = jar;
        this.platformReads = platformReads.values().stream().sorted(Comparator.comparing(PlatformModule::name)).collect(Collectors.toCollection(ArrayList::new));
        this.platformReadClosure = platformReadClosure.values().stream().sorted(Comparator.comparing(PlatformModule::name)).collect(Collectors.toCollection(ArrayList::new));
        hybridReads.put(jar.hybridModuleId(), this);
        hybridReadClosure.put(jar.hybridModuleId(), this);
        this.hybridReads = hybridReads.values().stream().sorted(Comparator.comparing(HybridModule::id)).collect(Collectors.toCollection(ArrayList::new));
        this.hybridReadClosure = hybridReadClosure.values().stream().sorted(Comparator.comparing(HybridModule::id)).collect(Collectors.toCollection(ArrayList::new));
        this.transitiveByRequires = transitiveByRequires;

    }

    HybridModuleId id() { return id; }
    List<PlatformModule> platformReads() { return platformReads; }
    List<PlatformModule> platformReadClosure() { return platformReadClosure; }
    List<HybridModule> hybridReads() { return hybridReads; }
    List<HybridModule> hybridReadClosure() { return hybridReadClosure; }

    Optional<String> getMainClass() { return jar.descriptor().mainClass(); }

    HybridModuleClassLoader getClassLoader() { return classLoader; }

    void fillModuleGraph(ModuleGraph graph) {
        graph.markAsRootHybridModule(id);
        fillModuleGraph2(graph);
    }

    @Override
    public boolean equals(Object other) {
        // Normally, equality would be determined by this.id. However if we ever support instantiating more
        // than one hybrid module for a given hybrid module name and version, we would determine
        // equality by reference anyways.
        return other == this;
    }

    @Override
    public int hashCode() {
        // See equals()
        return super.hashCode();
    }

    static class Builder {
        private final HybridModuleJar jar;
        private final Set<String> packages = new HashSet<>();
        private final Set<String> requiresNames = new HashSet<>();
        private final Map<String, PlatformModule> platformReads = new HashMap<>();
        private final Map<String, PlatformModule> platformReadClosure = new HashMap<>();
        private final Map<HybridModuleId, HybridModule> hybridReads = new HashMap<>();
        private final Map<HybridModuleId, HybridModule> hybridReadClosure = new HashMap<>();
        private final Map<String, Set<String>> exports = new HashMap<>();
        private final HashMap<String, Boolean> transitiveByRequires = new HashMap<>();

        Builder(HybridModuleJar jar) {
            this.jar = jar;
        }

        void setPackages(Set<String> packages) {
            this.packages.addAll(packages);
        }

        void addHybridModuleRequires(HybridModule hybridModule, boolean transitive) {
            if (!requiresNames.add(hybridModule.id().name())) {
                // JLS 11, 7.7.1 Dependences
                throw new ResolutionException("Hybrid module " + jar.hybridModuleId() + " requires " + hybridModule.id().name() + " twice");
            }

            hybridModule.hybridReadClosure().forEach(hm -> hybridReads.putIfAbsent(hm.id, hm));
            hybridModule.platformReadClosure().forEach(pm -> platformReads.putIfAbsent(pm.name(), pm));
            transitiveByRequires.put(hybridModule.id().name(), transitive);

            if (transitive) {
                hybridModule.hybridReadClosure().forEach(hm -> hybridReadClosure.putIfAbsent(hm.id, hm));
                hybridModule.platformReadClosure().forEach(pm -> platformReadClosure.putIfAbsent(pm.name(), pm));
            }
        }

        void addPlatformModuleRequires(PlatformModule platformModule, boolean transitive) {
            if (!requiresNames.add(platformModule.name())) {
                // JLS 11, 7.7.1 Dependences
                throw new ResolutionException("Hybrid module " + jar.hybridModuleId() + " requires " + platformModule.name() + " twice");
            }

            platformModule.readClosure().forEach(pm -> platformReads.putIfAbsent(pm.name(), pm));
            transitiveByRequires.put(platformModule.name(), transitive);

            if (transitive) {
                platformModule.readClosure().forEach(pm -> platformReadClosure.putIfAbsent(pm.name(), pm));
            }
        }

        void addExports(String packageName, Set<String> friends) {
            exports.put(packageName, friends);
        }

        HybridModule build() {
            HybridModule module = new HybridModule(
                    jar,
                    packages,
                    platformReads,
                    platformReadClosure,
                    hybridReads,
                    hybridReadClosure,
                    exports,
                    transitiveByRequires);

            // The hybrid module has a reference to the class loader, and vice versa, which complicates construction.

            TreeMap<String, PlatformModule> platformModuleByPackage = new TreeMap<>();
            for (var platformModule : platformReads.values()) {
                for (var packageName : platformModule.packagesVisibleTo(module)) {
                    PlatformModule previousOwner = platformModuleByPackage.put(packageName, platformModule);

                    if (previousOwner != null) {
                        throw new InvalidHybridModuleException("Package " + packageName + " visible to hybrid module " +
                                module.id() + " is exported from two different readable modules (" +
                                previousOwner.name() + " and " + platformModule.name() + ")");
                    }
                }
            }

            TreeMap<String, HybridModule> hybridModuleByPackage = new TreeMap<>();
            for (var hybridModule : hybridReads.values()) {
                for (var packageName : hybridModule.packagesVisibleTo(module)) {
                    PlatformModule previousPlatformOwner = platformModuleByPackage.get(packageName);
                    if (previousPlatformOwner != null) {
                        throw new InvalidHybridModuleException("Package " + packageName + " visible to hybrid module " +
                                module.id() + " is exported from two different readable modules (" +
                                previousPlatformOwner.name() + " and " + module.id() + ")");
                    }

                    HybridModule previousOwner = hybridModuleByPackage.put(packageName, hybridModule);
                    if (previousOwner != null) {
                        throw new InvalidHybridModuleException("Package " + packageName + " visible to hybrid module " +
                                module.id() + " is exported from two different readable modules (" +
                                previousOwner.id() + " and " + hybridModule.id() + ")");
                    }
                }
            }

            HybridModuleClassLoader classLoader = new HybridModuleClassLoader(jar,
                                                                              module,
                                                                              hybridModuleByPackage,
                                                                              platformModuleByPackage,
                                                                              exports);

            module.setHybridModuleClassLoader(classLoader);

            return module;
        }
    }

    private void setHybridModuleClassLoader(HybridModuleClassLoader classLoader) { this.classLoader = classLoader; }

    private void fillModuleGraph2(ModuleGraph graph) {
        if (graph.containsHybridModule(id) || !graph.hybridModuleInUniverse(id)) {
            return;
        }

        if (graph.params().includeExports()) {
            graph.addHybridModule(id, unqualifiedExports());
        } else {
            graph.addHybridModule(id);
        }

        hybridReads.forEach(readHybridModule -> {
            if (graph.hybridModuleInUniverse(readHybridModule.id)) {
                readHybridModule.fillModuleGraph2(graph);

                if (graph.params().includeSelf() || !id.equals(readHybridModule.id)) {
                    List<String> readEdgePackages;
                    if (graph.params().includeExports()) {
                        if (id.equals(readHybridModule.id)) {
                            // Add all unexported packages as implicit qualified exports on the read edge.
                            readEdgePackages = unexportedPackages();
                        } else {
                            // The read edge only contains those packages that readHybridModule exports qualified to this module.
                            readEdgePackages = readHybridModule.qualifiedExportsTo(this);
                        }
                    } else {
                        readEdgePackages = List.of();
                    }

                    Boolean transitive = transitiveByRequires.get(readHybridModule.id().name());
                    ModuleGraph.ReadEdge.Type type =
                            transitive == null ?
                                    ModuleGraph.ReadEdge.Type.IMPLICIT :
                                    transitive ?
                                            ModuleGraph.ReadEdge.Type.REQUIRES_TRANSITIVE :
                                            ModuleGraph.ReadEdge.Type.REQUIRES;

                    graph.addReadEdge(id, readHybridModule.id, readEdgePackages, type);
                }
            }
        });

        platformReads.forEach(readPlatformModule -> {
            if (graph.platformModuleInUniverse(readPlatformModule.name())) {
                readPlatformModule.fillModuleGraph(graph);

                List<String> qualifiedExports = graph.params().includeExports() ?
                        readPlatformModule.qualifiedExportsTo(this) :
                        List.of();

                Boolean transitive = transitiveByRequires.get(readPlatformModule.name());
                ModuleGraph.ReadEdge.Type type =
                        transitive == null ?
                                ModuleGraph.ReadEdge.Type.IMPLICIT :
                                transitive ?
                                        ModuleGraph.ReadEdge.Type.REQUIRES_TRANSITIVE :
                                        ModuleGraph.ReadEdge.Type.REQUIRES;

                graph.addReadEdge(id, readPlatformModule.name(), qualifiedExports, type);
            }
        });
    }
}
