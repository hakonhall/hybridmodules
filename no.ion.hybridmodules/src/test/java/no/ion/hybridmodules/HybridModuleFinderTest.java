package no.ion.hybridmodules;

import org.junit.Test;

import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Paths;
import java.util.Optional;

import static no.ion.hybridmodules.SetUtil.asSet;
import static org.junit.Assert.*;

public class HybridModuleFinderTest {
    @Test
    public void testHybridModuleOne() {
        try (var finder = HybridModuleFinder.of(Paths.get("src", "test", "resources"))) {
            Jar jar = finder.getHybridModuleJar("find.hybrid.module.one");
            assertEquals("find.hybrid.module.one@1.2.3", jar.moduleId().toString());
            assertTrue(jar.uri().toString().endsWith("find.hybrid.module.one-1.2.3.jar"));
            ModuleDescriptor descriptor = jar.descriptor();
            assertFalse(descriptor.isAutomatic());
            assertTrue(descriptor.version().isPresent());
            assertEquals("1.2.3", descriptor.version().get().toString());
            assertEquals(descriptor.packages(), asSet(
                    "no.ion.hybridmodules.test.FindHybridModule.one.exported",
                    "no.ion.hybridmodules.test.FindHybridModule"));
            var requiresSet = descriptor.requires();
            assertEquals(1, requiresSet.size());
            var requires = requiresSet.iterator().next();
            assertEquals("java.base", requires.name());
        }
    }

    @Test
    public void testHybridModuleTwo() {
        try (var finder = HybridModuleFinder.of(Paths.get("src", "test", "resources"))) {
            Jar jar = finder.getHybridModuleJar("find.hybrid.module.two");

            ModuleDescriptor descriptor = jar.descriptor();
            ModuleDescriptor.Requires oneRequires = descriptor.requires().stream()
                    .filter(r -> r.name().equals("find.hybrid.module.one"))
                    .findFirst()
                    .orElseThrow();
            assertEquals("1.2.3", oneRequires.compiledVersion().orElseThrow().toString());
        }
    }

    @Test
    public void testMissingHybridModule() {
        try (var finder = HybridModuleFinder.of(Paths.get("src", "test", "resources"))) {
            try {
                Jar jar = finder.getHybridModuleJar("non.existing");
                fail();
            } catch (FindException e) {
                assertEquals(
                        "Failed to find hybrid module 'non.existing' in hybrid module path 'src/test/resources'",
                        e.getMessage());
            }
        }
    }

    @Test
    public void testWrongVersion() {
        try (var finder = HybridModuleFinder.of(Paths.get("src", "test", "resources"))) {
            try {
                finder.getHybridModuleJar("find.hybrid.module.two", Optional.of(ModuleDescriptor.Version.parse("1.1")));
                fail();
            } catch (FindException e) {
                assertEquals(
                        "Failed to find version 1.1 of hybrid module 'find.hybrid.module.two', available versions: 1.2.3",
                        e.getMessage());
            }
        }
    }

    @Test
    public void testMissingVersion() {
        try (var finder = HybridModuleFinder.of(Paths.get("src", "test", "resources"))) {
            try {
                finder.getHybridModuleJar("find.hybrid.module.two", Optional.empty());
                fail();
            } catch (FindException e) {
                assertEquals(
                        "Failed to find hybrid module 'find.hybrid.module.two' without version, but some with versions: 1.2.3",
                        e.getMessage());
            }
        }
    }
}