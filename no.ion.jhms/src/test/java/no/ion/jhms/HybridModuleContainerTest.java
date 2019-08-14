package no.ion.jhms;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class HybridModuleContainerTest {
    @Test
    public void testClassLoading() throws ClassNotFoundException {
        try (var container = new HybridModuleContainer()) {
            container.discoverHybridModules("src/test/resources");
            HybridModuleContainer.ResolveParams params = new HybridModuleContainer.ResolveParams("find.hybrid.module.two");
            RootHybridModule root = container.resolve(params);
            Class<?> twoExportedPublic = root.loadClass("no.ion.jhms.test.FindHybridModule.two.exported.TwoExportedPublic");
            try {
                root.loadClass("no.ion.jhms.test.FindHybridModule.TwoInternalPublic");
                fail();
            } catch (ClassNotFoundException e) {
                // expected
            }

            // twoExportedPublic's class loader has access to internal classes.
            ClassLoader internalClassLoader = twoExportedPublic.getClassLoader();
            internalClassLoader.loadClass("no.ion.jhms.test.FindHybridModule.TwoInternalPublic");
        }
    }

    @Test
    public void testAccecssibility() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        try (var container = new HybridModuleContainer()) {
            container.discoverHybridModules(Paths.get("src/test/resources"));
            HybridModuleContainer.ResolveParams params = new HybridModuleContainer.ResolveParams("find.hybrid.module.one");
            RootHybridModule root = container.resolve(params);
            assertEquals("find.hybrid.module.one@1.2.3", root.toString());

            // Public class & constructor can be invoked
            {
                Class<?> oneExportedPublicClass = root.loadClass("no.ion.jhms.test.FindHybridModule.one.exported.OneExportedPublic");
                Object oneExportedPublic = oneExportedPublicClass.getDeclaredConstructor().newInstance();
                assertEquals("no.ion.jhms.test.FindHybridModule.one.exported.OneExportedPublic", oneExportedPublic.toString());
            }

            // Package private class is visible and therefore its Class can be retrieved,
            // but invoking the constructor is illegal.
            {
                Class<?> oneExportedClass = root.loadClass("no.ion.jhms.test.FindHybridModule.one.exported.OneExported");
                Constructor<?> oneExportedConstructor = oneExportedClass.getDeclaredConstructor();
                try {
                    Object oneExported = oneExportedConstructor.newInstance();
                    fail();
                } catch (IllegalAccessException e) {
                    assertEquals("class no.ion.jhms.HybridModuleContainerTest cannot access a member of " +
                                    "class no.ion.jhms.test.FindHybridModule.one.exported.OneExported with modifiers \"\"",
                            e.getMessage());
                }
            }
        }
    }

    @Test
    public void testGraph3() throws ClassNotFoundException {
        try (var container = new HybridModuleContainer()) {
            container.discoverHybridModules(Paths.get("src/test/resources"));

            var params = new HybridModuleContainer.ResolveParams("find.hybrid.module.two");
            RootHybridModule root = container.resolve(params);

            HybridModuleContainer.GraphParams graphParams = new HybridModuleContainer.GraphParams();
            graphParams.includeSelfReads();
            assertEquals("find.hybrid.module.one@1.2.3 reads find.hybrid.module.one@1.2.3 [no.ion.jhms.test.FindHybridModule,no.ion.jhms.test.FindHybridModule.one.exported]\n" +
                            "find.hybrid.module.two@1.2.3 reads find.hybrid.module.one@1.2.3 [no.ion.jhms.test.FindHybridModule.one.exported]\n" +
                            "find.hybrid.module.two@1.2.3 reads find.hybrid.module.two@1.2.3 [no.ion.jhms.test.FindHybridModule,no.ion.jhms.test.FindHybridModule.two.exported]\n",
                    container.moduleGraph2(graphParams));
        }
    }

    @Test
    public void testDefaultGraph() {
        try (var container = new HybridModuleContainer()) {
            container.discoverHybridModules(Paths.get("src/test/resources"));

            var resolveParams = new HybridModuleContainer.ResolveParams("find.hybrid.module.two");
            RootHybridModule root = container.resolve(resolveParams);

            var graphParams = new ModuleGraph.Params();
            ModuleGraph moduleGraph = container.getModuleGraph(graphParams);

            assertEquals(Set.of("find.hybrid.module.two@1.2.3"), moduleGraph.rootHybridModules());

            List<ModuleGraph.HybridModuleNode> hybridModuleNodes = moduleGraph.hybridModules();
            assertEquals(List.of("find.hybrid.module.one@1.2.3", "find.hybrid.module.two@1.2.3"),
                    hybridModuleNodes.stream().map(ModuleGraph.HybridModuleNode::id).collect(Collectors.toList()));

            List<ModuleGraph.PlatformModuleNode> platformModuleNodes = moduleGraph.platformModules();
            assertEquals(List.of("java.base"),
                    platformModuleNodes.stream().map(ModuleGraph.PlatformModuleNode::name).collect(Collectors.toList()));

            TreeMap<String, List<ModuleGraph.ReadEdge>> readEdges = moduleGraph.readEdges();
            assertEquals(Set.of("find.hybrid.module.one@1.2.3", "find.hybrid.module.two@1.2.3"), readEdges.keySet());

            assertEquals(List.of(
                    "find.hybrid.module.one@1.2.3 -> java.base: []"),
                    serializeEdgeList(readEdges.get("find.hybrid.module.one@1.2.3")));

            assertEquals(List.of(
                    "find.hybrid.module.two@1.2.3 -> find.hybrid.module.one@1.2.3: []",
                    "find.hybrid.module.two@1.2.3 -> java.base: []"),
                    serializeEdgeList(readEdges.get("find.hybrid.module.two@1.2.3")));
        }
    }

    private List<String> serializeEdgeList(List<ModuleGraph.ReadEdge> edges) {
        return edges.stream().map(edge -> edge.fromModule() + " -> " + edge.toModule() + ": " + edge.exports()).collect(Collectors.toList());
    }
}