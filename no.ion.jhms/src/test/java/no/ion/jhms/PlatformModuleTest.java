package no.ion.jhms;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static no.ion.jhms.ExceptionUtil.uncheck;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PlatformModuleTest {

    static final String SUN_NET_WWW_RESOURCE = "sun/net/www/content-types.properties";
    static final String JAVA_TIME_CHRONO_RESOURCE = "java/time/chrono/hijrah-config-islamic-umalqura.properties";

    @Test
    public void getResourceAsStream() throws IOException {
        // To explore platform modules use:
        /*
        FileSystem fileSystem = FileSystems.newFileSystem(URI.create("jrt:/"), Map.of());
        try (Stream<Path> list = Files.list(fileSystem.getPath("/modules/java.base/sun/net/www"))) {
            list.forEach(System.out::println);
        }

        System.out.println(Files.readString(fileSystem.getPath("/modules/java.base/sun/net/www/content-types.properties")));
        */

        var javaBaseModule = new PlatformModule.Builder("java.base").build();

        // sun.net.www is NOT an exported package of java.base. But since content-types.properties is not a valid
        // Java identifier, the 'name' of getResourceAsStream() (§2.9 2) is looked for in the java.base module
        // (and found).
        InputStream typesStream = javaBaseModule.getResourceAsStream(SUN_NET_WWW_RESOURCE);
        assertNotNull(typesStream);
        assertTrue(readUtf8FromInputStream(typesStream).contains("text/plain"));

        // java.time.chrono IS an exported package of java.base. But since hijrah-config-islamic-umalqura.properties
        // is not a valid Java identifier, the 'name' of getResourceAsStream() (§2.9 2) is looked for in the java.base
        // module (and found). I.e. same reason as for content-types.properties.
        InputStream configStream = javaBaseModule.getResourceAsStream(JAVA_TIME_CHRONO_RESOURCE);
        assertNotNull(configStream);
        assertTrue(readUtf8FromInputStream(configStream).contains("iso-start"));
    }

    private String readUtf8FromInputStream(InputStream inputStream) {
        byte[] bytes = uncheck(inputStream::readAllBytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}