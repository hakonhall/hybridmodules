package no.ion.moduleinfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class Main {
    private final ModuleDescriptor descriptor;
    private final boolean makeValid;
    private int indentation = 0;
    private boolean bol = true;

    private static void usage() {
        userError("""
Usage: module-info [-v|--valid] PATH
Print module descriptor.

PATH must be a path to a modular JAR, a module-info.class, or exploded module
directory.

Options:
  -v,--valid  Print a valid module descriptor omitting main class and versions.""");
    }

    private Main(ModuleDescriptor descriptor, boolean makeValid) {
        this.descriptor = descriptor;
        this.makeValid = makeValid;
    }

    @FunctionalInterface
    private interface IOThrowingSupplier<T> {
        T get() throws IOException;
    }

    private static <T> T uncheck(IOThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @FunctionalInterface
    private interface IOThrowingRunnable {
        void get() throws IOException;
    }

    private static void uncheck(IOThrowingRunnable supplier) {
        try {
            supplier.get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void main(String... args) {
        boolean makeValid = false;
        int index = 0;

        for (; index < args.length; ++index) {

            String arg = args[index];

            switch (arg) {
                case "--help":
                case "-h":
                    usage();
                    continue;
                case "--valid":
                case "-v":
                    makeValid = true;
                    continue;
                default:
                    // fall-through intentional
            }

            break; // this is how a switch-case can break out of the for-loop
        }

        if (index < args.length - 1) {
            userError("Too many arguments");
        } else if (index > args.length - 1) {
            userError("Too few arguments");
        }

        final Path path;
        {
            Path argPath = Path.of(args[index]);
            if (Files.isDirectory(argPath)) {
                path = argPath.resolve("module-info.class");
                if (!Files.isRegularFile(path))
                    userError("Not an exploded module directory: missing module-info.class: " + argPath);
            } else {
                if (!Files.isRegularFile(argPath))
                    userError("No such file: " + argPath);
                path = argPath;
            }
        }

        final ModuleDescriptor descriptor;
        if (path.getFileName().toString().endsWith(".jar")) {
            JarFile jarFile = uncheck(() -> new JarFile(path.toFile()));
            try {
                ZipEntry moduleInfoEntry = jarFile.getEntry("module-info.class");
                if (moduleInfoEntry == null)
                    userError("Not a modular JAR: Missing module-info.class: " + path);
                InputStream inputStream = uncheck(() -> jarFile.getInputStream(moduleInfoEntry));
                try {
                    descriptor = uncheck(() -> ModuleDescriptor.read(inputStream));
                } finally {
                    uncheck(inputStream::close);
                }
            } finally {
                uncheck(jarFile::close);
            }
        } else if (path.getFileName().toString().equals("module-info.class")) {
            InputStream inputStream = uncheck(() -> Files.newInputStream(path));
            try {
                descriptor = uncheck(() -> ModuleDescriptor.read(inputStream));
            } finally {
                uncheck(inputStream::close);
            }
        } else {
            userError("Neither a JAR nor a module-info.class: " + path);
            throw new IllegalStateException();  // Makes compiler happy
        }

        new Main(descriptor, makeValid).run();
    }

    private void run() {
        if (descriptor.isOpen()) print("open ");
        print("module " + descriptor.name());
        printVersion(descriptor.rawVersion());
        println(" {");
        indent();

        if (makeValid) {
            descriptor.mainClass().ifPresent(mainClass -> println("// mainClass " + mainClass + ";"));
        } else {
            descriptor.mainClass().ifPresent(mainClass -> println("mainClass " + mainClass + ";"));
        }

        List<ModuleDescriptor.Requires> requiresList = descriptor.requires().stream()
                .sorted(Comparator.comparing(ModuleDescriptor.Requires::name))
                .collect(Collectors.toList());
        for (var requires : requiresList) {
            print("requires ");

            Set<ModuleDescriptor.Requires.Modifier> modifiers = requires.modifiers();
            if (modifiers.contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE)) {
                print("transitive ");
            }

            print(requires.name());
            printVersion(requires.rawCompiledVersion());
            println(";");
        }

        List<ModuleDescriptor.Exports> exportsList = descriptor.exports().stream()
                .sorted(Comparator.comparing(ModuleDescriptor.Exports::source))
                .collect(Collectors.toList());
        for (var exports : exportsList) {
            print("exports " + exports.source());

            Set<String> targets = exports.targets();
            if (targets.size() > 0) {
                println(" to " + String.join(", ", targets) + ";");
            } else {
                println(";");
            }
        }

        dedent();
        println("}");
    }

    private void printVersion(Optional<String> rawVersion) {
        if (makeValid) {
            print(" /* @" + rawVersion.orElse("") + " */");
        } else {
            print("@" + rawVersion.orElse(""));
        }
    }

    private void indent() { indentation += 4; }
    private void dedent() { indentation -= 4; }

    private void print(String format, String... args) {
        write(format.length() == 0 ? format : String.format(format, (Object[]) args));
    }

    private void println(String format, String... args) {
        print(format, args);
        println();
    }

    private void println() {
        System.out.println();
        bol = true;
    }

    private void write(String text) {
        if (text.length() == 0) return;
        if (bol) text = " ".repeat(indentation) + text;
        System.out.print(text);
        bol = false;
    }

    private static void userError(String message) {
        System.err.println(message);
        System.exit(1);
    }
}
