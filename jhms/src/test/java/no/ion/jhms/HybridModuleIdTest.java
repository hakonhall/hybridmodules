package no.ion.jhms;

import org.junit.Test;

import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

public class HybridModuleIdTest {
    @Test
    public void test() {
        var id1 = new HybridModuleId("foo", HybridModuleVersion.from("1.2.3"));
        var id2 = new HybridModuleId("foo", HybridModuleVersion.from("1.3.2"));
        assertEquals(-1, id1.compareTo(id2));

        assertEquals("foo@1.2.3", id1.toString());

        var nullId = new HybridModuleId("foo", HybridModuleVersion.fromNull());
        assertEquals("foo@", nullId.toString());
        assertEquals(nullId, HybridModuleId.fromId("foo@"));

        var map = new TreeMap<HybridModuleId, Integer>();
        map.put(HybridModuleId.fromId("find.hybrid.module.one@1.2.3"), 1);
        map.put(HybridModuleId.fromId("find.hybrid.module.two@1.2.3"), 2);
        assertEquals(2, map.size());
    }
}
