package no.ion.jhms;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static no.ion.jhms.ExceptionUtil.uncheck;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PlatformModuleTest {

    static final String SUN_NET_WWW_RESOURCE = "sun/net/www/content-types.properties";
    static final String INTEGER = "java/lang/Integer.class";

    @Test
    public void getResourceAsStream() {
        var javaBaseModule = new PlatformModule.Builder("java.base").build();

        // (§2.9) says only .class files can be loaded for platform modules (as of OpenJDK 17).  Sanity-check.
        InputStream integerClassStream = javaBaseModule.getResourceAsStream(INTEGER);
        assertNotNull(integerClassStream);
        byte[] bytes = uncheck(integerClassStream::readAllBytes);
        assertTrue(bytes.length > 100);

        // Since content-type.properties is NOT in an open package, but it is in a package of java.base,
        // this will return null.
        assertNull(javaBaseModule.getResourceAsStream(SUN_NET_WWW_RESOURCE));
    }
}