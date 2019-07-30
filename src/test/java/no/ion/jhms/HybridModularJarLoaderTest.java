package no.ion.jhms;

import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class HybridModularJarLoaderTest {
    @Test
    public void test() {
        var oneId = new HybridModuleId2("find.hybrid.module.one", "1.2.3");
        var loader = new HybridModularJarLoader();
        loader.loadFromModulePath("src/test/resources");
        List<String> hybridModuleIds = loader.getHybridModuleIds().stream().map(Object::toString).collect(Collectors.toList());
        assertThat(hybridModuleIds, equalTo(List.of(oneId.toString(), "find.hybrid.module.two@1.2.3")));
        Optional<HybridModularJar> oneJar = loader.getHybridModularJar(oneId);
        assertTrue(oneJar.isPresent());
        assertEquals(oneId, oneJar.get().id());
    }
}
