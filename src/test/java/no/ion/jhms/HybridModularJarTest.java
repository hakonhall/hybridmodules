package no.ion.jhms;

import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class HybridModularJarTest {
    @Test
    public void test() {
        Path path = Path.of("src/test/resources/find.hybrid.module.one-1.2.3.jar");
        try (HybridModularJar jar = HybridModularJar.open(path, platformModuleContainer)) {
            assertEquals(path, jar.path());
            assertEquals("find.hybrid.module.one@1.2.3", jar.id().toString());
            assertFalse(jar.mainClass().isPresent());
            assertEquals(32, jar.calculateJarFileChecksum().length);
        }
    }
}
