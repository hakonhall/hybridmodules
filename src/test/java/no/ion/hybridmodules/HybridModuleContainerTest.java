package no.ion.hybridmodules;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.fail;

public class HybridModuleContainerTest {
    @Test
    public void test() throws ClassNotFoundException {
        Path path = Paths.get("src", "test", "resources");
        var params = new HybridModuleContainer.ResolveParams("find.hybrid.module.two", path);
        try (var container = HybridModuleContainer.resolve(params)) {
            container.loadClass("no.ion.hybridmodules.test.FindHybridModule.two.exported.TwoExportedPublic");
            container.loadExportedClass("no.ion.hybridmodules.test.FindHybridModule.two.exported.TwoExportedPublic");
            container.loadClass("no.ion.hybridmodules.test.FindHybridModule.TwoInternalPublic");
            try {
                container.loadExportedClass("no.ion.hybridmodules.test.FindHybridModule.TwoInternalPublic");
                fail();
            } catch (ClassNotFoundException e) {
                // ok
            }
        }
    }
}