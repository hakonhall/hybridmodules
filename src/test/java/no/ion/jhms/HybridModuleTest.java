package no.ion.jhms;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class HybridModuleTest {
    @Test
    public void test() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        try (var finder = HybridModuleFinder.of(Paths.get("src", "test", "resources"))) {
            Jar jar = finder.getHybridModuleJar("find.hybrid.module.one");
            var hybridModule = new HybridModule(jar, new HashMap<>(), new HashMap<>());
            assertEquals("find.hybrid.module.one@1.2.3", hybridModule.getHybridModuleId().toString());

            {
                HashMap<String, String> expectedExportedPackages = new HashMap<>();
                expectedExportedPackages.put("no.ion.jhms.test.FindHybridModule.one.exported", "find.hybrid.module.one@1.2.3");
                Map<String, String> actualExportedPackages = hybridModule.getExportedPackages().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
                assertEquals(expectedExportedPackages, actualExportedPackages);
            }

            {
                HashMap<String, String> expectedReads = new HashMap<>();
                expectedReads.put("no.ion.jhms.test.FindHybridModule.one.exported", "find.hybrid.module.one@1.2.3");
                expectedReads.put("no.ion.jhms.test.FindHybridModule", "find.hybrid.module.one@1.2.3");
                Map<String, String> actualReads = hybridModule.getReads().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
                assertEquals(expectedReads, actualReads);
            }

            // Construct public exported class.
            HybridModuleClassLoader classLoader = hybridModule.getClassLoader();
            Class<?> oneExportedPublicClass = classLoader.loadExportedClass("no.ion.jhms.test.FindHybridModule.one.exported.OneExportedPublic");
            Object oneExportedPublic = oneExportedPublicClass.getDeclaredConstructor().newInstance();
            assertEquals("no.ion.jhms.test.FindHybridModule.one.exported.OneExportedPublic", oneExportedPublic.toString());

            // Package-private class in exported package is not visible
            try {
                Class<?> oneExportedClass = classLoader.loadExportedClass("no.ion.jhms.test.FindHybridModule.one.exported.OneExported");
                fail();
            } catch (ClassNotFoundException e) {
                assertEquals("no.ion.jhms.test.FindHybridModule.one.exported.OneExported", e.getMessage());
            }

            /* If non-public types in exported and unopened packages are visible:
            Class<?> oneExportedClass = classLoader.loadExportedClass("no.ion.jhms.test.FindHybridModule.one.exported.OneExported");
            Constructor<?> constructor = oneExportedClass.getDeclaredConstructor();
            try {
                constructor.newInstance();
                fail();
            } catch (IllegalAccessException e) {
                assertEquals(
                        "class no.ion.jhms.HybridModuleTest cannot access a member of class no.ion.jhms.test.FindHybridModule.one.exported.OneExported with modifiers \"\"",
                        e.getMessage());
            }*/

            // Non-exported class not found
            try {
                classLoader.loadExportedClass("no.ion.jhms.test.FindHybridModule.OneInternalPublic");
                fail();
            } catch (ClassNotFoundException e) {
                // as expected
            }
        }
    }
}