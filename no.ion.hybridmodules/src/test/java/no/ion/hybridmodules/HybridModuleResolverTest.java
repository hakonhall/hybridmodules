package no.ion.hybridmodules;

import org.junit.Test;

import java.nio.file.Paths;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HybridModuleResolverTest {
    @Test
    public void test() throws ClassNotFoundException {
        try (var finder = HybridModuleFinder.of(Paths.get("src", "test", "resources"))) {
            var resolver = new HybridModuleResolver(finder);
            var hybridModule = resolver.resolve("find.hybrid.module.two", null);
            Map<String, HybridModule> reads = hybridModule.getReads();

            assertContains(reads, "no.ion.hybridmodules.test.FindHybridModule.two.exported", "find.hybrid.module.two@1.2.3");
            assertContains(reads, "no.ion.hybridmodules.test.FindHybridModule", "find.hybrid.module.two@1.2.3");
            assertContains(reads, "no.ion.hybridmodules.test.FindHybridModule.one.exported", "find.hybrid.module.one@1.2.3");
            assertEquals(3, reads.size());

            HybridModuleClassLoader classLoader = hybridModule.getClassLoader();
            Class<?> c1 = classLoader.loadExportedClass("no.ion.hybridmodules.test.FindHybridModule.two.exported.TwoExportedPublic");
        }
    }

    private void assertContains(Map<String, HybridModule> reads, String packageName, String hybridModule) {
        assertTrue(reads.containsKey(packageName));
        assertEquals(hybridModule, reads.get(packageName).toString());
    }
}