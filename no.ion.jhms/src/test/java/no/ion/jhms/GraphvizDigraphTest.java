package no.ion.jhms;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class GraphvizDigraphTest {
    private final ModuleGraph.Params params = new ModuleGraph.Params();
    private final Set<String> readableByRoots = new HashSet<>();

    private ModuleGraph makeModuleGraph() {
        var mainId = new HybridModuleId("main", HybridModuleVersion.fromNull());
        var dep1Id = new HybridModuleId("dep", "1");
        var dep2Id = new HybridModuleId("dep", "2");
        var intermediateId = new HybridModuleId("intermediate", HybridModuleVersion.fromNull());
        var transientId = new HybridModuleId("transient", "3");
        var javaLoggingName = "java.logging";
        var javaBaseName = "java.base";
        List<String> intermediateUnqualifiedExports =
                includeExports() ?
                List.of("no.ion.jhms.intermediate.a", "no.ion.jhms.intermediate.b") :
                List.of();
        List<String> javaBaseUnqualifiedExports = includeExports() ?
                List.of("java.lang.util", "java.lang.module") :
                List.of();
        List<String> packagesExportedFromDep1ToMain = includeExports() ?
                List.of("no.ion.jhms.intermediate.d", "no.ion.jhms.intermediate.e", "no.ion.jhms.intermediate.f") :
                List.of();

        ModuleGraph graph = new ModuleGraph();
        graph.markAsRootHybridModule(mainId);

        graph.addHybridModule(mainId);
        graph.addHybridModule(dep1Id);
        if (includeUnreadableByRoots()) graph.addHybridModule(dep2Id);
        graph.addHybridModule(intermediateId, intermediateUnqualifiedExports);
        graph.addHybridModule(transientId);
        if (includePlatformModule()) graph.addPlatformModule(javaLoggingName);
        if (includeJavaBase()) graph.addPlatformModule(javaBaseName, javaBaseUnqualifiedExports);

        if (includeSelf()) graph.addReadEdge(mainId, mainId);
        graph.addReadEdge(mainId, dep1Id, packagesExportedFromDep1ToMain);
        graph.addReadEdge(mainId, intermediateId);
        graph.addReadEdge(mainId, transientId);
        if (includePlatformModule()) graph.addReadEdge(mainId, javaLoggingName);
        if (includeJavaBase()) graph.addReadEdge(mainId, javaBaseName);

        if (includeSelf()) graph.addReadEdge(dep1Id, dep1Id);
        if (includeJavaBase()) graph.addReadEdge(dep1Id, javaBaseName);

        if (includeUnreadableByRoots() && includeSelf()) graph.addReadEdge(dep2Id, dep2Id);
        if (includeUnreadableByRoots() && includeJavaBase())  graph.addReadEdge(dep2Id, javaBaseName);

        if (includeSelf()) graph.addReadEdge(intermediateId, intermediateId);
        if (includeUnreadableByRoots()) graph.addReadEdge(intermediateId, dep2Id);
        graph.addReadEdge(intermediateId, transientId);
        if (includePlatformModule()) graph.addReadEdge(intermediateId, javaLoggingName);
        if (includeJavaBase()) graph.addReadEdge(intermediateId, javaBaseName);

        if (includeSelf()) graph.addReadEdge(transientId, transientId);
        if (includePlatformModule()) graph.addReadEdge(transientId, javaLoggingName);
        if (includeJavaBase()) graph.addReadEdge(transientId, javaBaseName);

        if (includeSelf() && includePlatformModule()) graph.addReadEdge(javaLoggingName, javaLoggingName);
        if (includeJavaBase()) graph.addReadEdge(javaLoggingName, javaBaseName);

        if (includeSelf() && includeJavaBase()) graph.addReadEdge(javaBaseName, javaBaseName);

        return graph;
    }

    private boolean includeUnreadableByRoots() { return !params.excludeUnreadable(); }
    private boolean includeSelf() { return params.includeSelf(); }
    private boolean includeJavaBase() { return !params.excludePlatformModules() && !params.moduleExcluded("java.base"); }
    private boolean includePlatformModule() { return !params.excludePlatformModules(); }
    private boolean includeExports() { return params.includeExports(); }

    private void assertDot(String expectedDot) {
        ModuleGraph graph = makeModuleGraph();
        GraphvizDigraph digraph = GraphvizDigraph.fromModuleGraph(graph);
        String actualDot = digraph.toDot();
        assertEquals(expectedDot, actualDot);
    }

    @Test
    public void testDefaultParams() {
        assertDot("digraph \"module graph\" {\n" +
                "  subgraph cluster_hybrid {\n" +
                "    graph [ style=dotted; label=<<font face=\"Helvetica\">HYBRID MODULES</font>>; ]\n" +
                "    node [ color=blue; ]\n" +
                "    \"dep@1\"\n" +
                "    \"dep@2\"\n" +
                "    \"intermediate@\"\n" +
                "    \"main@\" [ style=bold; label=<<b><u>main@</u></b>>; ]\n" +
                "    \"transient@3\"\n" +
                "  }\n" +
                "  subgraph cluster_platform {\n" +
                "    graph [ style=dotted; label=<<font face=\"Helvetica\">PLATFORM MODULES</font>>; ]\n" +
                "    node [ color=red; ]\n" +
                "    \"java.base\"\n" +
                "    \"java.logging\"\n" +
                "  }\n" +
                "  \"dep@1\" -> \"java.base\"\n" +
                "  \"dep@2\" -> \"java.base\"\n" +
                "  \"intermediate@\" -> \"dep@2\"\n" +
                "  \"intermediate@\" -> \"java.base\"\n" +
                "  \"intermediate@\" -> \"java.logging\"\n" +
                "  \"intermediate@\" -> \"transient@3\"\n" +
                "  \"java.logging\" -> \"java.base\"\n" +
                "  \"main@\" -> \"dep@1\"\n" +
                "  \"main@\" -> \"intermediate@\"\n" +
                "  \"main@\" -> \"java.base\"\n" +
                "  \"main@\" -> \"java.logging\"\n" +
                "  \"main@\" -> \"transient@3\"\n" +
                "  \"transient@3\" -> \"java.base\"\n" +
                "  \"transient@3\" -> \"java.logging\"\n" +
                "}\n");
    }

    @Test
    public void testExcludeJavaBase() {
        params.excludeModule("java.base");
        assertDot("digraph \"module graph\" {\n" +
                "  subgraph cluster_hybrid {\n" +
                "    graph [ style=dotted; label=<<font face=\"Helvetica\">HYBRID MODULES</font>>; ]\n" +
                "    node [ color=blue; ]\n" +
                "    \"dep@1\"\n" +
                "    \"dep@2\"\n" +
                "    \"intermediate@\"\n" +
                "    \"main@\" [ style=bold; label=<<b><u>main@</u></b>>; ]\n" +
                "    \"transient@3\"\n" +
                "  }\n" +
                "  subgraph cluster_platform {\n" +
                "    graph [ style=dotted; label=<<font face=\"Helvetica\">PLATFORM MODULES</font>>; ]\n" +
                "    node [ color=red; ]\n" +
                "    \"java.logging\"\n" +
                "  }\n" +
                "  \"intermediate@\" -> \"dep@2\"\n" +
                "  \"intermediate@\" -> \"java.logging\"\n" +
                "  \"intermediate@\" -> \"transient@3\"\n" +
                "  \"main@\" -> \"dep@1\"\n" +
                "  \"main@\" -> \"intermediate@\"\n" +
                "  \"main@\" -> \"java.logging\"\n" +
                "  \"main@\" -> \"transient@3\"\n" +
                "  \"transient@3\" -> \"java.logging\"\n" +
                "}\n");
    }

    @Test
    public void testIncludeSelf() {
        params.includeSelf(true);
        assertDot("digraph \"module graph\" {\n" +
                "  subgraph cluster_hybrid {\n" +
                "    graph [ style=dotted; label=<<font face=\"Helvetica\">HYBRID MODULES</font>>; ]\n" +
                "    node [ color=blue; ]\n" +
                "    \"dep@1\"\n" +
                "    \"dep@2\"\n" +
                "    \"intermediate@\"\n" +
                "    \"main@\" [ style=bold; label=<<b><u>main@</u></b>>; ]\n" +
                "    \"transient@3\"\n" +
                "  }\n" +
                "  subgraph cluster_platform {\n" +
                "    graph [ style=dotted; label=<<font face=\"Helvetica\">PLATFORM MODULES</font>>; ]\n" +
                "    node [ color=red; ]\n" +
                "    \"java.base\"\n" +
                "    \"java.logging\"\n" +
                "  }\n" +
                "  \"dep@1\" -> \"dep@1\"\n" +
                "  \"dep@1\" -> \"java.base\"\n" +
                "  \"dep@2\" -> \"dep@2\"\n" +
                "  \"dep@2\" -> \"java.base\"\n" +
                "  \"intermediate@\" -> \"dep@2\"\n" +
                "  \"intermediate@\" -> \"intermediate@\"\n" +
                "  \"intermediate@\" -> \"java.base\"\n" +
                "  \"intermediate@\" -> \"java.logging\"\n" +
                "  \"intermediate@\" -> \"transient@3\"\n" +
                "  \"java.base\" -> \"java.base\"\n" +
                "  \"java.logging\" -> \"java.base\"\n" +
                "  \"java.logging\" -> \"java.logging\"\n" +
                "  \"main@\" -> \"dep@1\"\n" +
                "  \"main@\" -> \"intermediate@\"\n" +
                "  \"main@\" -> \"java.base\"\n" +
                "  \"main@\" -> \"java.logging\"\n" +
                "  \"main@\" -> \"main@\"\n" +
                "  \"main@\" -> \"transient@3\"\n" +
                "  \"transient@3\" -> \"java.base\"\n" +
                "  \"transient@3\" -> \"java.logging\"\n" +
                "  \"transient@3\" -> \"transient@3\"\n" +
                "}\n");
    }

    @Test
    public void testExcludePlatformModules() {
        params.excludePlatformModules(true);
        assertDot("digraph \"module graph\" {\n" +
                "    node [ color=blue; ]\n" +
                "    \"dep@1\"\n" +
                "    \"dep@2\"\n" +
                "    \"intermediate@\"\n" +
                "    \"main@\" [ style=bold; label=<<b><u>main@</u></b>>; ]\n" +
                "    \"transient@3\"\n" +
                "  \"intermediate@\" -> \"dep@2\"\n" +
                "  \"intermediate@\" -> \"transient@3\"\n" +
                "  \"main@\" -> \"dep@1\"\n" +
                "  \"main@\" -> \"intermediate@\"\n" +
                "  \"main@\" -> \"transient@3\"\n" +
                "}\n");
    }

    @Test
    public void testExcludeUnreadableByRoots() {
        params.excludeUnreadable(true);
        assertDot("digraph \"module graph\" {\n" +
                "  subgraph cluster_hybrid {\n" +
                "    graph [ style=dotted; label=<<font face=\"Helvetica\">HYBRID MODULES</font>>; ]\n" +
                "    node [ color=blue; ]\n" +
                "    \"dep@1\"\n" +
                "    \"intermediate@\"\n" +
                "    \"main@\" [ style=bold; label=<<b><u>main@</u></b>>; ]\n" +
                "    \"transient@3\"\n" +
                "  }\n" +
                "  subgraph cluster_platform {\n" +
                "    graph [ style=dotted; label=<<font face=\"Helvetica\">PLATFORM MODULES</font>>; ]\n" +
                "    node [ color=red; ]\n" +
                "    \"java.base\"\n" +
                "    \"java.logging\"\n" +
                "  }\n" +
                "  \"dep@1\" -> \"java.base\"\n" +
                "  \"intermediate@\" -> \"java.base\"\n" +
                "  \"intermediate@\" -> \"java.logging\"\n" +
                "  \"intermediate@\" -> \"transient@3\"\n" +
                "  \"java.logging\" -> \"java.base\"\n" +
                "  \"main@\" -> \"dep@1\"\n" +
                "  \"main@\" -> \"intermediate@\"\n" +
                "  \"main@\" -> \"java.base\"\n" +
                "  \"main@\" -> \"java.logging\"\n" +
                "  \"main@\" -> \"transient@3\"\n" +
                "  \"transient@3\" -> \"java.base\"\n" +
                "  \"transient@3\" -> \"java.logging\"\n" +
                "}\n");
    }

    @Test
    public void testIncludeExports() {
        params.includeExports(true);
        assertDot("digraph \"module graph\" {\n" +
                "  subgraph cluster_hybrid {\n" +
                "    graph [ style=dotted; label=<<font face=\"Helvetica\">HYBRID MODULES</font>>; ]\n" +
                "    node [ color=blue; ]\n" +
                "    \"dep@1\"\n" +
                "    \"dep@2\"\n" +
                "    \"intermediate@\" [ label=<<table border=\"0\"><tr><td>intermediate@</td></tr><tr><td><font color=\"dimgray\"><table border=\"1\" color=\"yellowgreen\"><tr><td balign=\"left\" valign=\"top\" border=\"0\"><i>no.ion.jhms.intermediate.a<br/>no.ion.jhms.intermediate.b<br/></i></td></tr></table></font></td></tr></table>>; ]\n" +
                "    \"main@\" [ style=bold; label=<<b><u>main@</u></b>>; ]\n" +
                "    \"transient@3\"\n" +
                "  }\n" +
                "  subgraph cluster_platform {\n" +
                "    graph [ style=dotted; label=<<font face=\"Helvetica\">PLATFORM MODULES</font>>; ]\n" +
                "    node [ color=red; ]\n" +
                "    \"java.base\" [ label=<<table border=\"0\"><tr><td>java.base</td></tr><tr><td><font color=\"dimgray\"><table border=\"1\" color=\"yellowgreen\"><tr><td balign=\"left\" valign=\"top\" border=\"0\"><i>java.lang.module<br/>java.lang.util<br/></i></td></tr></table></font></td></tr></table>>; ]\n" +
                "    \"java.logging\"\n" +
                "  }\n" +
                "  \"dep@1\" -> \"java.base\"\n" +
                "  \"dep@2\" -> \"java.base\"\n" +
                "  \"intermediate@\" -> \"dep@2\"\n" +
                "  \"intermediate@\" -> \"java.base\"\n" +
                "  \"intermediate@\" -> \"java.logging\"\n" +
                "  \"intermediate@\" -> \"transient@3\"\n" +
                "  \"java.logging\" -> \"java.base\"\n" +
                "  \"main@\" -> \"dep@1\" [ label=<<font color=\"dimgray\"><table border=\"1\" color=\"yellowgreen\"><tr><td balign=\"left\" valign=\"top\" border=\"0\"><i>no.ion.jhms.intermediate.d<br/>no.ion.jhms.intermediate.e<br/>no.ion.jhms.intermediate.f<br/></i></td></tr></table></font>>; ]\n" +
                "  \"main@\" -> \"intermediate@\"\n" +
                "  \"main@\" -> \"java.base\"\n" +
                "  \"main@\" -> \"java.logging\"\n" +
                "  \"main@\" -> \"transient@3\"\n" +
                "  \"transient@3\" -> \"java.base\"\n" +
                "  \"transient@3\" -> \"java.logging\"\n" +
                "}\n");
    }
}