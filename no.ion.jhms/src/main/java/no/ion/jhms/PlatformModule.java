package no.ion.jhms;

import java.lang.module.ResolutionException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

class PlatformModule extends BaseModule {
    private final String name;
    private final Set<PlatformModule> reads;
    private final Set<PlatformModule> readClosure;

    private volatile Set<String> packagesVisibleToHybridModules = null;

    private PlatformModule(String moduleName,
                           Set<String> packages,
                           HashSet<PlatformModule> reads,
                           HashSet<PlatformModule> readClosure,
                           Map<String, Set<String>> exports) {
        super(moduleName, packages, exports);
        this.name = moduleName;
        this.reads = reads;
        this.readClosure = readClosure;

        reads.add(this);
        readClosure.add(this);
    }

    String name() { return name; }

    Set<PlatformModule> reads() { return reads; }
    Set<PlatformModule> readClosure() { return readClosure; }

    @Override
    Set<String> packagesVisibleTo(BaseModule module) {
        // Assume all hybrid modules have the same visibility of platform modules.
        // This allows us to cache the result of the calculation.

        Set<String> packages = packagesVisibleToHybridModules;
        if (packages == null) {
            packages = super.packagesVisibleTo(module);
            packagesVisibleToHybridModules = packages;
        }

        return packages;
    }

    void fillModuleGraph(ModuleGraph graph) {
        if (graph.containsPlatformModule(name) || !graph.params().platformModuleIncluded(name)) {
            return;
        }

        if (graph.params().includeExports()) {
            graph.addPlatformModule(name, unqualifiedExports());
        } else {
            graph.addPlatformModule(name);
        }

        reads.forEach(readPlatformModule -> {
            if (graph.platformModuleInUniverse(readPlatformModule.name)) {
                readPlatformModule.fillModuleGraph(graph);

                if (graph.params().includeSelf() || !name.equals(readPlatformModule.name)) {
                    List<String> readEdgePackages;
                    if (graph.params().includeExports()) {
                        if (readPlatformModule.name.equals(name)) {
                            readEdgePackages = unexportedPackages();
                        } else {
                            readEdgePackages = readPlatformModule.qualifiedExportsTo(this);
                        }
                    } else {
                        readEdgePackages = List.of();
                    }
                    graph.addReadEdge(name, readPlatformModule.name, readEdgePackages);
                }
            }
        });
    }

    @Override
    public boolean equals(Object other) {
        // See HybridModule::equals
        return other == this;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    static class Builder {
        private final String moduleName;
        private final Set<String> packages = new HashSet<>();
        private final Set<String> requiresNames = new HashSet<>();
        private final HashSet<PlatformModule> reads = new HashSet<>();
        private final HashSet<PlatformModule> readClosure = new HashSet<>();
        private final TreeMap<String, Set<String>> exports = new TreeMap<>();

        Builder(String moduleName) {
            this.moduleName = moduleName;
        }

        void setPackages(Set<String> packages) { this.packages.addAll(packages); }

        void addRequires(PlatformModule platformModule, boolean transitive) {
            if (!requiresNames.add(platformModule.name())) {
                // JLS 11, 7.7.1 Dependences
                throw new ResolutionException("Platform module " + moduleName + " requires " + platformModule.name() + " twice");
            }

            reads.addAll(platformModule.readClosure());

            if (transitive) {
                readClosure.addAll(platformModule.readClosure());
            }
        }

        void addExports(String packageName, Set<String> friends) {
            if (exports.put(packageName, friends) != null) {
                throw new ResolutionException("Platform module " + moduleName + " exports " + packageName + " twice");
            }
        }

        PlatformModule build() {
            return new PlatformModule(moduleName, packages, reads, readClosure, exports);
        }
    }
}
