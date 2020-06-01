package no.ion;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {
    private final ModuleDescriptor descriptor;
    private final boolean makeValid;
    private int indentation = 0;
    private boolean bol = true;

    private static void usage() {
        userError(
                "Usage: module-info PATH\n" +
                "Print module descriptor of modular JAR, including versions and main class");
    }

    private Main(ModuleDescriptor descriptor, boolean makeValid) {
        this.descriptor = descriptor;
        this.makeValid = makeValid;
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

        Path path = Path.of(args[index]);
        if (!Files.isRegularFile(path)) {
            userError("There is no module JAR at " + path);
        }

        ModuleFinder finder = ModuleFinder.of(path);
        Set<ModuleReference> references = finder.findAll();
        switch (references.size()) {
            case 0:
                userError(path + " is not a modular JAR");
                break; // for syntax only
            case 1:
                break;
            default:
                // This should never happen to my knowledge.
                userError(path + " contains many modules");
        }

        ModuleReference reference = references.iterator().next();
        ModuleDescriptor descriptor = reference.descriptor();
        new Main(descriptor, makeValid).run();
    }

    private void run() {
        if (descriptor.isOpen()) print("open ");
        print("module " + descriptor.name());
        printVersion(descriptor.rawVersion());
        println(" {");
        indent();

        if (makeValid) {
            descriptor.mainClass().ifPresent(mainClass -> println("// mainclass " + mainClass + ";"));
        } else {
            descriptor.mainClass().ifPresent(mainClass -> println("mainclass " + mainClass + ";"));
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
