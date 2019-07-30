package no.ion.jhms;

import org.junit.Test;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainTest {
    @Test
    public void testLongOptions() throws InvocationTargetException {
        Main.main("--module-path", jarsPath("with-main-class/jar/out.jar"), "--module", "root/root.Main");
    }

    @Test
    public void testShortOptions() throws InvocationTargetException {
        Main.main("-p", jarsPath("with-main-class/jar/out.jar"), "-m", "root/root.Main");
    }

    @Test
    public void testFindingMainClassFromModule() throws InvocationTargetException {
        Main.main("-p", jarsPath("with-main-class/jar/out.jar"), "-m", "root");
    }

    public String jarsPath(String relativePath) {
        Path path = Paths.get("src/test/jars", relativePath);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Failed to find '" + path + "': " +
                    "Perhaps 'make' has not been run under src/main/jars?");
        }

        return path.toString();
    }
}
