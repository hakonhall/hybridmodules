package no.ion.jhms;

import org.junit.Test;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.module.ModuleDescriptor.Requires;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HybridModuleJarTest {
    @Test
    public void richDescriptor() {
        Path path = Path.of("src/test/resources/rich.descriptor-1.3.4.jar");
        try (HybridModuleJar jar = HybridModuleJar.open(path)) {
            assertEquals(path, jar.path());
            assertEquals("rich.descriptor@1.3.4", jar.hybridModuleId().toString());

            ModuleDescriptor descriptor = jar.descriptor();
            assertFalse(descriptor.isAutomatic());
            assertEquals("rich.descriptor", descriptor.name());
            assertEquals(Optional.of("1.3.4"), descriptor.rawVersion());
            assertEquals(Optional.of("rich.descriptor.Main"), descriptor.mainClass());
            assertEquals(Set.of("rich.descriptor", "rich.descriptor.exported", "rich.descriptor.qualified"), descriptor.packages());

            Map<String, Requires> requires = descriptor.requires().stream().collect(Collectors.toMap(Requires::name, Function.identity()));
            assertEquals(3, requires.size());
            assertTrue(requires.containsKey("java.base"));
            assertTrue(requires.containsKey("java.logging"));
            var requiredRequires = requires.get("required");
            assertNotNull(requiredRequires);
            assertEquals(Optional.of("3.1"), requiredRequires.rawCompiledVersion());
            assertEquals(Set.of(Requires.Modifier.TRANSITIVE), requiredRequires.modifiers());

            Map<String, Set<String>> exports = descriptor.exports().stream()
                    .collect(Collectors.toMap(ModuleDescriptor.Exports::source, ModuleDescriptor.Exports::targets));
            assertEquals(2, exports.size());
            assertEquals(Set.of(), exports.get("rich.descriptor.exported"));
            assertEquals(Set.of("friend"), exports.get("rich.descriptor.qualified"));

            // Gotten from sha256sum(1). Recreating the JAR file will change the checksum.
            assertEquals("ec7dcd6565a85b2d5e826ed5a4643191559dd3fbeeb51b0cfdd15712d69ea790", jar.sha256String().toLowerCase());

            String uri = jar.uri().toString();
            assertTrue(uri.endsWith("src/test/resources/rich.descriptor-1.3.4.jar"));

            try (HybridModuleJar jarCopy = HybridModuleJar.open(("src/test/resources/copies/rich.descriptor-1.3.4-copy.jar"))) {
                assertTrue(jarCopy.checksumEqual(jar));
            }

            try (HybridModuleJar jarCopy = HybridModuleJar.open(("src/test/resources/copies/rich.descriptor-1.3.4-nocopy.jar"))) {
                assertFalse(jarCopy.checksumEqual(jar));
            }
        }
    }
}
