package no.ion.jhms;

import org.junit.Test;

import java.lang.module.FindException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ObservableHybridModulesTest {
    @Test
    public void discoveFromModulePath() {
        var oneId = new HybridModuleId("find.hybrid.module.one", "1.2.3");
        try (var loader = new ObservableHybridModules()) {
            loader.discoverHybridModulesFromModulePath("src/test/resources");
            List<String> hybridModuleIds = loader.getHybridModuleIds().stream().map(Object::toString).collect(Collectors.toList());
            assertThat(hybridModuleIds, equalTo(List.of(
                    "find.hybrid.module.one@1.2.3", "find.hybrid.module.two@1.2.3", "rich.descriptor@1.3.4")));
            HybridModuleJar oneJar = loader.getJar(oneId);
            assertEquals(oneId, oneJar.hybridModuleId());
        }
    }

    @Test
    public void testHybridModuleOne() {
        try (var finder = new ObservableHybridModules()) {
            finder.discoverHybridModules(Paths.get("src", "test", "resources"));
            List<HybridModuleJar> jars = finder.getJarsWithName("find.hybrid.module.one");
            assertEquals(1, jars.size());
            assertEquals("find.hybrid.module.one@1.2.3", jars.get(0).hybridModuleId().toString());
        }
    }

    @Test
    public void testMissingHybridModule() {
        try (var finder = new ObservableHybridModules()) {
            finder.discoverHybridModules(Paths.get("src", "test", "resources"));

            try {
                HybridModuleJar jar = finder.getJar(HybridModuleId.fromId("non.existing@3"));
                fail();
            } catch (FindException e) {
                assertEquals(
                        "Hybrid module non.existing@3 not found",
                        e.getMessage());
            }
        }
    }

    @Test
    public void testWrongVersion() {
        try (var finder = new ObservableHybridModules()) {
            finder.discoverHybridModules(Paths.get("src", "test", "resources"));
            try {
                finder.getJar(new HybridModuleId("find.hybrid.module.two", "1.1"));
                fail();
            } catch (FindException e) {
                assertEquals(
                        "Hybrid module find.hybrid.module.two@1.1 not found",
                        e.getMessage());
            }
        }
    }

    @Test
    public void testMissingVersion() {
        try (var finder = new ObservableHybridModules()) {
            finder.discoverHybridModules(Paths.get("src", "test", "resources"));
            try {
                finder.getJar(new HybridModuleId("find.hybrid.module.two", HybridModuleVersion.fromNull()));
                fail();
            } catch (FindException e) {
                assertEquals(
                        "Hybrid module find.hybrid.module.two@ not found",
                        e.getMessage());
            }
        }
    }
}
