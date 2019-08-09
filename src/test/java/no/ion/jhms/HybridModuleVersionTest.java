package no.ion.jhms;

import org.junit.Test;

import static org.junit.Assert.*;

public class HybridModuleVersionTest {
    @Test
    public void basics() {
        HybridModuleVersion nullVersion = HybridModuleVersion.fromNull();
        assertEquals(nullVersion, HybridModuleVersion.fromNull());
        assertEquals(0, nullVersion.compareTo(HybridModuleVersion.fromNull()));
        assertEquals("", nullVersion.toString());

        // This is to allow a null versioned hybrid module ID (foo@) to be restored as a null version.
        // This is MEANINGFUL because it is not possible to set an empty version with javac/jar,
        // and so an empty string version can be used for our purposes.
        assertEquals(nullVersion, HybridModuleVersion.from(""));

        HybridModuleVersion invalidVersion = HybridModuleVersion.from("not a valid version");
        assertEquals(invalidVersion, HybridModuleVersion.from("not a valid version"));
        assertTrue(invalidVersion.compareTo(nullVersion) > 0);
        assertTrue(invalidVersion.compareTo(HybridModuleVersion.from("b: also invalid")) > 0);

        HybridModuleVersion version = HybridModuleVersion.from("1.20.3");
        assertEquals(version, HybridModuleVersion.from("1.20.3"));
        assertTrue(version.compareTo(nullVersion) > 0);
        assertTrue(version.compareTo(invalidVersion) > 0);
        assertTrue(version.compareTo(HybridModuleVersion.from("1.3")) > 0);
    }
}