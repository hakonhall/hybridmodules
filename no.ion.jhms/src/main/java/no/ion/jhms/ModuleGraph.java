package no.ion.jhms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ModuleGraph {
    private final Params params;
    private final Set<String> rootHybridModules = new HashSet<>();
    private final TreeMap<String, HybridModuleNode> hybridModules = new TreeMap<>();
    private final TreeMap<String, PlatformModuleNode> platformModules = new TreeMap<>();
    private final TreeMap<String, List<ReadEdge>> readEdges = new TreeMap<>();

    private Set<HybridModuleId> hybridModuleUniverse = null;
    private Set<String> platformModuleUniverse = null;

    /**
     * Parameters affecting the content of the module graph, however more info may be set than guaranteed.
     *
     * <p>The nodes of the module graph are hybrid modules (module name and version), and platform modules (names).
     * The edges are the read edges.
     */
    public static class Params {
        private final Set<String> moduleBlacklist = new HashSet<>();

        private boolean includeSelf = false;
        private boolean includeExports = false;
        private boolean excludeUnreadable = false;
        private boolean excludePlatformModules = false;

        /** Exclude the given module from the module graph (name@version for hybrid module, name for platform module). */
        public void excludeModule(String module) { moduleBlacklist.add(module); }

        /** Whether to exclude those modules from the graph that are not readable by the roots. */
        public void excludeUnreadable(boolean excludeUnreadable) { this.excludeUnreadable = excludeUnreadable; }

        /** Whether to exclude all platform modules from the graph, and all edges to/from these. */
        public void excludePlatformModules(boolean excludePlatformModules) { this.excludePlatformModules = excludePlatformModules; }

        /**
         * All modules 1. reads themselves, and 2. (effectively) export all of its own packages to itself.
         * This method can be used to exclude those edges.
         */
        public void includeSelf(boolean includeSelf) { this.includeSelf = includeSelf; }

        /**
         * Whether to include information on exported packages (= package visibility).
         *
         * <p>The unqualified exports from a module is shown on the node. Qualified exports shows on the read edge.
         * All unexported packages are shown on the read edge to self (with {@link #includeSelf(boolean) includeSelf(true)}).
         */
        public void includeExports(boolean includeExports) { this.includeExports = includeExports; }

        boolean includeSelf() { return includeSelf; }

        boolean moduleExcluded(String name) { return moduleBlacklist.contains(name); }
        Set<String> modulesExcluded() { return Set.copyOf(moduleBlacklist); }

        boolean excludePlatformModules() { return excludePlatformModules; }
        boolean includeExports() { return includeExports; }
        boolean excludeUnreadable() { return excludeUnreadable; }
    }

    public static class HybridModuleNode implements Comparable<HybridModuleNode> {
        private final HybridModuleId id;
        private final List<String> unqualifiedExports;

        private HybridModuleNode(HybridModuleId id, List<String> unqualifiedExports) {
            this.id = id;
            this.unqualifiedExports = new ArrayList<>(unqualifiedExports);
            this.unqualifiedExports.sort(Comparator.naturalOrder());
        }

        public String id() { return id2String(id); }
        public List<String> unqualifiedExports() { return unqualifiedExports; }

        @Override
        public int compareTo(HybridModuleNode that) { return this.id.compareTo(that.id); }
    }

    public static class PlatformModuleNode implements Comparable<PlatformModuleNode> {
        private final String name;
        private final List<String> unqualifiedExports;

        private PlatformModuleNode(String name, List<String> unqualifiedExports) {
            this.name = name;
            this.unqualifiedExports = new ArrayList<>(unqualifiedExports);
            this.unqualifiedExports.sort(Comparator.naturalOrder());
        }

        public String name() { return name; }
        public List<String> unqualifiedExports() { return unqualifiedExports; }

        @Override
        public int compareTo(PlatformModuleNode that) { return this.name.compareTo(that.name); }
    }

    public static class ReadEdge {
        private final String fromModule;
        private final String toModule;
        private final List<String> exports;

        public ReadEdge(String fromModule, String toModule, List<String> exports) {
            this.fromModule = fromModule;
            this.toModule = toModule;
            this.exports = new ArrayList<>(exports);
            this.exports.sort(Comparator.naturalOrder());
        }

        public String fromModule() { return fromModule; }
        public String toModule() { return toModule; }
        public List<String> exports() { return exports; }
    }

    ModuleGraph() { this(new Params()); }
    ModuleGraph(Params params) { this.params = params; }

    public Set<String> rootHybridModules() { return rootHybridModules; }
    public List<HybridModuleNode> hybridModules() { return new ArrayList<>(hybridModules.values()); }
    public List<PlatformModuleNode> platformModules() { return new ArrayList<>(platformModules.values()); }
    public TreeMap<String, List<ReadEdge>> readEdges() {
        return new TreeMap<>(readEdges.entrySet().stream().collect(Collectors.toMap(
                entry -> entry.getKey(),
                entry -> entry.getValue().stream()
                        .sorted(Comparator.comparing(ReadEdge::fromModule).thenComparing(ReadEdge::toModule))
                        .collect(Collectors.toList())
        )));
    }

    Params params() { return params; }

    boolean containsHybridModule(HybridModuleId id) { return hybridModules.containsKey(id2String(id)); }
    boolean containsPlatformModule(String name) { return platformModules.containsKey(name); }

    boolean hybridModuleInUniverse(HybridModuleId id) {
        return (hybridModuleUniverse == null || hybridModuleUniverse.contains(id)) &&
                !params.moduleExcluded(id2String(id));
    }

    boolean platformModuleInUniverse(String name) {
        return (platformModuleUniverse == null || platformModuleUniverse.contains(name)) &&
                !params.excludePlatformModules() &&
                !params.moduleExcluded(name);
    }

    void setHybridModuleUniverse(Set<HybridModuleId> hybridModuleUniverse) { this.hybridModuleUniverse = hybridModuleUniverse; }
    void setPlatformModuleUniverse(Set<String> platformModuleUniverse) { this.platformModuleUniverse = platformModuleUniverse; }

    void markAsRootHybridModule(HybridModuleId module) { rootHybridModules.add(id2String(module)); }
    void addHybridModule(HybridModuleId module, List<String> unqualifiedExports) { hybridModules.put(id2String(module), new HybridModuleNode(module, unqualifiedExports)); }
    void addHybridModule(HybridModuleId module) { addHybridModule(module, List.of()); }
    void addPlatformModule(String module, List<String> unqualifiedExports) { platformModules.put(module, new PlatformModuleNode(module, unqualifiedExports)); }
    void addPlatformModule(String module) { addPlatformModule(module, List.of()); }

    void addReadEdge(HybridModuleId fromModule, HybridModuleId toModule, List<String> exported) { addReadEdge(id2String(fromModule), id2String(toModule), exported); }
    void addReadEdge(HybridModuleId fromModule, String toModule, List<String> exported) { addReadEdge(id2String(fromModule), toModule, exported); }
    void addReadEdge(String fromModule, String toModule, List<String> exported) { readEdges.computeIfAbsent(fromModule, __ -> new ArrayList<>()).add(new ReadEdge(fromModule, toModule, exported)); }

    void addReadEdge(HybridModuleId fromModule, HybridModuleId toModule) { addReadEdge(id2String(fromModule), id2String(toModule)); }
    void addReadEdge(HybridModuleId fromModule, String toModule) { addReadEdge(id2String(fromModule), toModule); }
    void addReadEdge(String fromModule, String toModule) { addReadEdge(fromModule, toModule, List.of()); }

    private static String id2String(HybridModuleId id) { return id.toString(); }
}
