package root;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.Set;

public class Main {
    public static void main(String... args) {
        ModuleFinder finder = ModuleFinder.of(Path.of(args[0]));
        Set<ModuleReference> all = finder.findAll();
        ModuleDescriptor descriptor = all.iterator().next().descriptor();
        System.out.println(descriptor.packages());
    }
}
