package no.ion.jhms;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HybridModuleId2Test {
    @Test
    public void test() {
        var id1 = new HybridModuleId2("foo", "1.2.3");
        var id2 = new HybridModuleId2("foo", "1.3.2");
        assertEquals(-1, id1.compareTo(id2));
    }
}
