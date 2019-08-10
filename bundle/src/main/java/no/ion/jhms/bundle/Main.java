package no.ion.jhms.bundle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static java.util.Objects.requireNonNull;

public class Main {
    private final StringBuilder info = new StringBuilder();

    private static void usage() {
        System.out.print(
                "Usage: bundle -d DEST BUNDLE\n" +
                "Read OSGi bundle JAR, writee DEST/module-info.java, and print the version\n"
        );

        System.exit(0);
    }

    private static void fail(String message) {
        System.err.println(message);
        System.exit(1);
    }

    public static void main(String... args) throws IOException {

        if (args.length == 0) {
            usage();
        }

        Path moduleInfoParentDir = null;

        int argi = 0;
        argsloop:
        for (argi = 0; argi < args.length; ++argi) {
            switch (args[argi]) {
                case "-d":
                    if (argi + 1 >= args.length) {
                        fail("Missing argument to " + args[argi]);
                    }

                    moduleInfoParentDir = Path.of(args[++argi]);
                    break;
                case "--help":
                case "-h":
                    usage();
                    break;
                default:
                    break argsloop;
            }
        }

        if (moduleInfoParentDir == null) {
            fail("Missing DEST, see --help");
        } else if (!Files.isDirectory(moduleInfoParentDir)) {
            fail(moduleInfoParentDir + " does not exist");
        }

        if (argi + 1 != args.length) {
            fail("Mising BUNDLE, see --help");
        }

        Path bundlePath = Path.of(args[argi]);
        if (!Files.isRegularFile(bundlePath) || !Files.isReadable(bundlePath)) {
            fail(bundlePath + " is not a readable file");
        }

        ModuleInfo info = BundleReader.readModuleInfo(bundlePath);
        Files.writeString(
                moduleInfoParentDir.resolve("module-info.java"),
                info.generateModuleInfoJavaContent(true),
                StandardCharsets.UTF_8);
        System.out.println(info.version());
    }
}
