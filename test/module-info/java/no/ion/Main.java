package no.ion;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.util.Set;

public class Main {
    private final ModuleDescriptor descriptor;
    private int indentation = 0;
    private boolean bol = true;

    private Main(ModuleDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public static void main(String... args) {
        ModuleFinder finder = ModuleFinder.of(Paths.get(args[0]));
        ModuleReference reference = finder.find(args[1]).orElseThrow();
        ModuleDescriptor descriptor = reference.descriptor();
        new Main(descriptor).run();
    }

    private void run() {
        if (descriptor.isOpen()) print("open ");
        print("module " + descriptor.name());
        if (descriptor.version().isPresent()) {
            print("@" + descriptor.version().get().toString());
        } else if (descriptor.rawVersion().isPresent()) {
            print("#" + descriptor.rawVersion().get());
        }
        println(" {");
        indent();

        descriptor.mainClass().ifPresent(mainClass -> println("// main class: " + mainClass));

        for (var requires : descriptor.requires()) {
            print("requires ");

            Set<ModuleDescriptor.Requires.Modifier> modifiers = requires.modifiers();
            if (modifiers.contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE)) {
                print("transitive ");
            }

            print(requires.name());
            if (requires.compiledVersion().isPresent()) {
                print("@" + requires.compiledVersion().get().toString());
            } else if (requires.rawCompiledVersion().isPresent()) {
                print("#" + requires.rawCompiledVersion().get());
            }
            println(";");
        }

        dedent();
        println("}");
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
}
