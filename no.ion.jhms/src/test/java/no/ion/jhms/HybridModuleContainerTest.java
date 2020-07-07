package no.ion.jhms;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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
    public void getResourceAsStreamFromHybridModule() throws ClassNotFoundException, IOException {
        try (var container = new HybridModuleContainer()) {
            container.discoverHybridModules(Paths.get("src/test/resources"));
            HybridModuleContainer.ResolveParams params = new HybridModuleContainer.ResolveParams("rich.descriptor");
            RootHybridModule root = container.resolve(params);
            assertEquals("rich.descriptor@1.3.4", root.toString());
            Class<?> aClass = root.loadClass("rich.descriptor.exported.E");
            assertNotNull(aClass);

            assertResource("This file is exported.\n", aClass.getResourceAsStream("exported.txt"));
            assertResource("This file is exported.\n", aClass.getResourceAsStream("/rich/descriptor/exported/exported.txt"));

            // A class in hybrid module rich.descriptor is able to access an exported resource in
            // a required hybrid module 'required'.
            assertResource("Exported file.\n", aClass.getResourceAsStream("/required/exported/exported.txt"));

            // A class in hybrid module rich.descriptor is unable to access an unexported resource in
            // a required hybrid module 'required'.
            String unexportedResource = "/required/unexported.txt";
            assertResource(null, aClass.getResourceAsStream(unexportedResource));

            // aClass is a class in rich.descriptor. It is able to load a class RequiredExported since
            // rich.descriptor requires the "required" hybrid module.
            Class<?> requiredClass = aClass.getClassLoader().loadClass("required.exported.RequiredExported");
            assertNotNull(requiredClass);

            // An instance of RequiredClass will be able to load internal resources even if the package is not exported.
            assertResource("Unexported resource file.\n", requiredClass.getResourceAsStream(unexportedResource));
        }
    }

    @Test
    public void getResourceAsStreamFromPlatformModule() throws ClassNotFoundException, IOException {
        try (var container = new HybridModuleContainer()) {
            container.discoverHybridModules(Paths.get("src/test/resources"));
            HybridModuleContainer.ResolveParams params = new HybridModuleContainer.ResolveParams("rich.descriptor");
            RootHybridModule root = container.resolve(params);
            assertEquals("rich.descriptor@1.3.4", root.toString());
            Class<?> aClass = root.loadClass("rich.descriptor.exported.E");
            assertNotNull(aClass);

            assertResourceContains("iso-start", aClass.getResourceAsStream('/' + PlatformModuleTest.JAVA_TIME_CHRONO_RESOURCE));
            assertResourceContains(null, aClass.getResourceAsStream('/' + PlatformModuleTest.SUN_NET_WWW_RESOURCE));
        }
    }

    @Test
    public void getResource() throws ClassNotFoundException, IOException {
        try (var container = new HybridModuleContainer()) {
            container.discoverHybridModules(Paths.get("src/test/resources"));
            HybridModuleContainer.ResolveParams params = new HybridModuleContainer.ResolveParams("rich.descriptor");
            RootHybridModule root = container.resolve(params);
            assertEquals("rich.descriptor@1.3.4", root.toString());
            Class<?> aClass = root.loadClass("rich.descriptor.exported.E");
            assertNotNull(aClass);

            // Verify Class.getResource(String) works in the trivial case of getInputStream().
            URL resourceUrl = aClass.getResource("exported.txt");
            assertEquals("jhms:/rich.descriptor@1.3.4/rich/descriptor/exported/exported.txt", resourceUrl.toString());
            assertEquals("/rich.descriptor@1.3.4/rich/descriptor/exported/exported.txt", resourceUrl.getPath());
            URLConnection urlConnection = resourceUrl.openConnection();
            urlConnection.connect();
            assertNull(urlConnection.getContentType());
            assertResource("This file is exported.\n", urlConnection.getInputStream());
        }
    }

    private static void assertResourceContains(String expectedUtf8ContentSubstring, InputStream actualInputStream)
            throws IOException {
        if (expectedUtf8ContentSubstring == null) {
            assertNull(actualInputStream);
        } else {
            assertNotNull(actualInputStream);
            String actualUtf8Content = new String(actualInputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(actualUtf8Content, containsString(expectedUtf8ContentSubstring));
        }
    }

    private static void assertResource(String expectedUtf8Content, InputStream actualInputStream) throws IOException {
        if (expectedUtf8Content == null) {
            assertNull(actualInputStream);
        } else {
            assertNotNull(actualInputStream);
            String actualUtf8Content = new String(actualInputStream.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(expectedUtf8Content, actualUtf8Content);
        }
    }

    @Test
    public void testGraph3() {
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

    @Test
    public void testExcludeJavaBase() {
        try (var container = new HybridModuleContainer()) {
            container.discoverHybridModules(Paths.get("src/test/resources"));

            var resolveParams = new HybridModuleContainer.ResolveParams("find.hybrid.module.two");
            RootHybridModule root = container.resolve(resolveParams);

            var graphParams = new ModuleGraph.Params();
            graphParams.excludeModule("java.base");
            ModuleGraph moduleGraph = container.getModuleGraph(graphParams);

            assertEquals(Set.of("find.hybrid.module.two@1.2.3"), moduleGraph.rootHybridModules());

            List<ModuleGraph.HybridModuleNode> hybridModuleNodes = moduleGraph.hybridModules();
            assertEquals(List.of("find.hybrid.module.one@1.2.3", "find.hybrid.module.two@1.2.3"),
                    hybridModuleNodes.stream().map(ModuleGraph.HybridModuleNode::id).collect(Collectors.toList()));

            List<ModuleGraph.PlatformModuleNode> platformModuleNodes = moduleGraph.platformModules();
            assertEquals(List.of(),
                    platformModuleNodes.stream().map(ModuleGraph.PlatformModuleNode::name).collect(Collectors.toList()));

            TreeMap<String, List<ModuleGraph.ReadEdge>> readEdges = moduleGraph.readEdges();
            assertEquals(Set.of("find.hybrid.module.two@1.2.3"), readEdges.keySet());

            assertEquals(List.of("find.hybrid.module.two@1.2.3 -> find.hybrid.module.one@1.2.3: []"),
                    serializeEdgeList(readEdges.get("find.hybrid.module.two@1.2.3")));
        }
    }

    @Test
    public void testExcludePlatformModules() {
        try (var container = new HybridModuleContainer()) {
            container.discoverHybridModules(Paths.get("src/test/resources"));

            var resolveParams = new HybridModuleContainer.ResolveParams("find.hybrid.module.two");
            RootHybridModule root = container.resolve(resolveParams);

            var graphParams = new ModuleGraph.Params();
            graphParams.excludePlatformModules(true);
            ModuleGraph moduleGraph = container.getModuleGraph(graphParams);

            assertEquals(Set.of("find.hybrid.module.two@1.2.3"), moduleGraph.rootHybridModules());

            List<ModuleGraph.HybridModuleNode> hybridModuleNodes = moduleGraph.hybridModules();
            assertEquals(List.of("find.hybrid.module.one@1.2.3", "find.hybrid.module.two@1.2.3"),
                    hybridModuleNodes.stream().map(ModuleGraph.HybridModuleNode::id).collect(Collectors.toList()));

            List<ModuleGraph.PlatformModuleNode> platformModuleNodes = moduleGraph.platformModules();
            assertEquals(List.of(),
                    platformModuleNodes.stream().map(ModuleGraph.PlatformModuleNode::name).collect(Collectors.toList()));

            TreeMap<String, List<ModuleGraph.ReadEdge>> readEdges = moduleGraph.readEdges();
            assertEquals(Set.of("find.hybrid.module.two@1.2.3"), readEdges.keySet());

            assertEquals(List.of("find.hybrid.module.two@1.2.3 -> find.hybrid.module.one@1.2.3: []"),
                    serializeEdgeList(readEdges.get("find.hybrid.module.two@1.2.3")));
        }
    }

    @Test
    public void testIncludeExports() {
        try (var container = new HybridModuleContainer()) {
            container.discoverHybridModules(Paths.get("src/test/resources"));

            var resolveParams = new HybridModuleContainer.ResolveParams("find.hybrid.module.two");
            RootHybridModule root = container.resolve(resolveParams);

            var graphParams = new ModuleGraph.Params();
            graphParams.includeExports(true);
            ModuleGraph moduleGraph = container.getModuleGraph(graphParams);

            assertEquals(Set.of("find.hybrid.module.two@1.2.3"), moduleGraph.rootHybridModules());

            List<ModuleGraph.HybridModuleNode> hybridModuleNodes = moduleGraph.hybridModules();
            assertEquals(List.of("find.hybrid.module.one@1.2.3", "find.hybrid.module.two@1.2.3"),
                    hybridModuleNodes.stream().map(ModuleGraph.HybridModuleNode::id).collect(Collectors.toList()));
            assertEquals(List.of("no.ion.jhms.test.FindHybridModule.one.exported"), hybridModuleNodes.get(0).unqualifiedExports());
            assertEquals(List.of("no.ion.jhms.test.FindHybridModule.two.exported"), hybridModuleNodes.get(1).unqualifiedExports());

            List<ModuleGraph.PlatformModuleNode> platformModuleNodes = moduleGraph.platformModules();
            assertEquals(List.of("java.base"),
                    platformModuleNodes.stream().map(ModuleGraph.PlatformModuleNode::name).collect(Collectors.toList()));
            // The exact list is not tested since it changes from one Java major version to the next.
            assertTrue("Unexpected list of java.base exports: " + platformModuleNodes.get(0).unqualifiedExports(),
                    platformModuleNodes.get(0).unqualifiedExports().containsAll(List.of(
                    "java.io",
                    "java.lang",
                    "java.lang.annotation",
                    "java.lang.invoke",
                    "java.lang.module",
                    "java.lang.ref",
                    "java.lang.reflect",
                    "java.math",
                    "java.net",
                    "java.net.spi",
                    "java.nio",
                    "java.nio.channels",
                    "java.nio.channels.spi",
                    "java.nio.charset",
                    "java.nio.charset.spi",
                    "java.nio.file",
                    "java.nio.file.attribute",
                    "java.nio.file.spi",
                    "java.security",
                    "java.security.acl",
                    "java.security.cert",
                    "java.security.interfaces",
                    "java.security.spec",
                    "java.text",
                    "java.text.spi",
                    "java.time",
                    "java.time.chrono",
                    "java.time.format",
                    "java.time.temporal",
                    "java.time.zone",
                    "java.util",
                    "java.util.concurrent",
                    "java.util.concurrent.atomic",
                    "java.util.concurrent.locks",
                    "java.util.function",
                    "java.util.jar",
                    "java.util.regex",
                    "java.util.spi",
                    "java.util.stream",
                    "java.util.zip",
                    "javax.crypto",
                    "javax.crypto.interfaces",
                    "javax.crypto.spec",
                    "javax.net",
                    "javax.net.ssl",
                    "javax.security.auth",
                    "javax.security.auth.callback",
                    "javax.security.auth.login",
                    "javax.security.auth.spi",
                    "javax.security.auth.x500",
                    "javax.security.cert")));

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

    @Test
    public void testIncludeSelf() {
        try (var container = new HybridModuleContainer()) {
            container.discoverHybridModules(Paths.get("src/test/resources"));

            var resolveParams = new HybridModuleContainer.ResolveParams("find.hybrid.module.two");
            RootHybridModule root = container.resolve(resolveParams);

            var graphParams = new ModuleGraph.Params();
            graphParams.includeSelf(true);
            ModuleGraph moduleGraph = container.getModuleGraph(graphParams);

            assertEquals(Set.of("find.hybrid.module.two@1.2.3"), moduleGraph.rootHybridModules());

            List<ModuleGraph.HybridModuleNode> hybridModuleNodes = moduleGraph.hybridModules();
            assertEquals(List.of("find.hybrid.module.one@1.2.3", "find.hybrid.module.two@1.2.3"),
                    hybridModuleNodes.stream().map(ModuleGraph.HybridModuleNode::id).collect(Collectors.toList()));

            List<ModuleGraph.PlatformModuleNode> platformModuleNodes = moduleGraph.platformModules();
            assertEquals(List.of("java.base"),
                    platformModuleNodes.stream().map(ModuleGraph.PlatformModuleNode::name).collect(Collectors.toList()));

            TreeMap<String, List<ModuleGraph.ReadEdge>> readEdges = moduleGraph.readEdges();
            assertEquals(Set.of("find.hybrid.module.one@1.2.3", "find.hybrid.module.two@1.2.3", "java.base"), readEdges.keySet());

            assertEquals(List.of(
                    "find.hybrid.module.one@1.2.3 -> find.hybrid.module.one@1.2.3: []",
                    "find.hybrid.module.one@1.2.3 -> java.base: []"),
                    serializeEdgeList(readEdges.get("find.hybrid.module.one@1.2.3")));

            assertEquals(List.of(
                    "find.hybrid.module.two@1.2.3 -> find.hybrid.module.one@1.2.3: []",
                    "find.hybrid.module.two@1.2.3 -> find.hybrid.module.two@1.2.3: []",
                    "find.hybrid.module.two@1.2.3 -> java.base: []"),
                    serializeEdgeList(readEdges.get("find.hybrid.module.two@1.2.3")));

            assertEquals(List.of(
                    "java.base -> java.base: []"),
                    serializeEdgeList(readEdges.get("java.base")));
        }
    }

    @Test
    public void testIncludeSelfExports() {
        try (var container = new HybridModuleContainer()) {
            container.discoverHybridModules(Paths.get("src/test/resources"));

            var resolveParams = new HybridModuleContainer.ResolveParams("find.hybrid.module.two");
            RootHybridModule root = container.resolve(resolveParams);

            var graphParams = new ModuleGraph.Params();
            graphParams.includeSelf(true);
            graphParams.includeExports(true);
            ModuleGraph moduleGraph = container.getModuleGraph(graphParams);

            assertEquals(Set.of("find.hybrid.module.two@1.2.3"), moduleGraph.rootHybridModules());

            List<ModuleGraph.HybridModuleNode> hybridModuleNodes = moduleGraph.hybridModules();
            assertEquals(List.of("find.hybrid.module.one@1.2.3", "find.hybrid.module.two@1.2.3"),
                    hybridModuleNodes.stream().map(ModuleGraph.HybridModuleNode::id).collect(Collectors.toList()));
            assertEquals(List.of("no.ion.jhms.test.FindHybridModule.one.exported"), hybridModuleNodes.get(0).unqualifiedExports());
            assertEquals(List.of("no.ion.jhms.test.FindHybridModule.two.exported"), hybridModuleNodes.get(1).unqualifiedExports());

            List<ModuleGraph.PlatformModuleNode> platformModuleNodes = moduleGraph.platformModules();
            assertEquals(List.of("java.base"),
                    platformModuleNodes.stream().map(ModuleGraph.PlatformModuleNode::name).collect(Collectors.toList()));
            // The exact list is not tested since it changes from one Java major version to the next.
            assertTrue("Unexpected list of java.base exports: " + platformModuleNodes.get(0).unqualifiedExports(),
                    platformModuleNodes.get(0).unqualifiedExports().containsAll(List.of(
                            "java.io",
                            "java.lang",
                            "java.lang.annotation",
                            "java.lang.invoke",
                            "java.lang.module",
                            "java.lang.ref",
                            "java.lang.reflect",
                            "java.math",
                            "java.net",
                            "java.net.spi",
                            "java.nio",
                            "java.nio.channels",
                            "java.nio.channels.spi",
                            "java.nio.charset",
                            "java.nio.charset.spi",
                            "java.nio.file",
                            "java.nio.file.attribute",
                            "java.nio.file.spi",
                            "java.security",
                            "java.security.acl",
                            "java.security.cert",
                            "java.security.interfaces",
                            "java.security.spec",
                            "java.text",
                            "java.text.spi",
                            "java.time",
                            "java.time.chrono",
                            "java.time.format",
                            "java.time.temporal",
                            "java.time.zone",
                            "java.util",
                            "java.util.concurrent",
                            "java.util.concurrent.atomic",
                            "java.util.concurrent.locks",
                            "java.util.function",
                            "java.util.jar",
                            "java.util.regex",
                            "java.util.spi",
                            "java.util.stream",
                            "java.util.zip",
                            "javax.crypto",
                            "javax.crypto.interfaces",
                            "javax.crypto.spec",
                            "javax.net",
                            "javax.net.ssl",
                            "javax.security.auth",
                            "javax.security.auth.callback",
                            "javax.security.auth.login",
                            "javax.security.auth.spi",
                            "javax.security.auth.x500",
                            "javax.security.cert")));

            TreeMap<String, List<ModuleGraph.ReadEdge>> readEdges = moduleGraph.readEdges();
            assertEquals(Set.of("find.hybrid.module.one@1.2.3", "find.hybrid.module.two@1.2.3", "java.base"), readEdges.keySet());

            assertEquals(List.of(
                    "find.hybrid.module.one@1.2.3 -> find.hybrid.module.one@1.2.3: [no.ion.jhms.test.FindHybridModule]",
                    "find.hybrid.module.one@1.2.3 -> java.base: []"),
                    serializeEdgeList(readEdges.get("find.hybrid.module.one@1.2.3")));

            assertEquals(List.of(
                    "find.hybrid.module.two@1.2.3 -> find.hybrid.module.one@1.2.3: []",
                    "find.hybrid.module.two@1.2.3 -> find.hybrid.module.two@1.2.3: [no.ion.jhms.test.FindHybridModule]",
                    "find.hybrid.module.two@1.2.3 -> java.base: []"),
                    serializeEdgeList(readEdges.get("find.hybrid.module.two@1.2.3")));

            // Unexported packages in java.base: The list of packages java.base (implicitly) exports to
            // itself is not tested for equality, since it changes between java versions.
            List<ModuleGraph.ReadEdge> javaBaseReadEdges = readEdges.get("java.base");
            assertEquals(1, javaBaseReadEdges.size());
            ModuleGraph.ReadEdge javaBaseSelfReadEdge = javaBaseReadEdges.get(0);
            assertEquals("java.base", javaBaseSelfReadEdge.toModule());
            assertTrue(javaBaseSelfReadEdge.exports().contains("jdk.internal.util"));
        }
    }

    @Test
    public void testExcludeUnreadable() {
        try (var container = new HybridModuleContainer()) {
            container.discoverHybridModules(Paths.get("src/test/resources"));

            var resolveParams = new HybridModuleContainer.ResolveParams("find.hybrid.module.two");
            RootHybridModule root = container.resolve(resolveParams);

            var graphParams = new ModuleGraph.Params();
            graphParams.excludeUnreadable(true);
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

    @Test
    public void testPicks() {
        try (var container = new HybridModuleContainer()) {
            container.discoverHybridModules(Paths.get("src/test/resources"));

            var resolveParams = new HybridModuleContainer.ResolveParams("find.hybrid.module.two");
            RootHybridModule root = container.resolve(resolveParams);

            var graphParams = new ModuleGraph.Params();
            graphParams.excludeModule("java.base");
            graphParams.excludeModule("find.hybrid.module.one@1.2.3");
            ModuleGraph moduleGraph = container.getModuleGraph(graphParams);

            assertEquals(Set.of("find.hybrid.module.two@1.2.3"), moduleGraph.rootHybridModules());

            List<ModuleGraph.HybridModuleNode> hybridModuleNodes = moduleGraph.hybridModules();
            assertEquals(List.of("find.hybrid.module.two@1.2.3"),
                    hybridModuleNodes.stream().map(ModuleGraph.HybridModuleNode::id).collect(Collectors.toList()));

            List<ModuleGraph.PlatformModuleNode> platformModuleNodes = moduleGraph.platformModules();
            assertEquals(List.of(),
                    platformModuleNodes.stream().map(ModuleGraph.PlatformModuleNode::name).collect(Collectors.toList()));

            TreeMap<String, List<ModuleGraph.ReadEdge>> readEdges = moduleGraph.readEdges();
            assertEquals(Set.of(), readEdges.keySet());
        }
    }

    private List<String> serializeEdgeList(List<ModuleGraph.ReadEdge> edges) {
        return edges.stream().map(edge -> edge.fromModule() + " -> " + edge.toModule() + ": " + edge.exports()).collect(Collectors.toList());
    }
}