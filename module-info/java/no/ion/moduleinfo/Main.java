package no.ion.moduleinfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class Main {
    private final ModuleDescriptor descriptor;
    private final boolean makeValid;
    private final Optional<ModulePath> modulePath;
    private int indentation = 0;
    private boolean bol = true;

    private static void usage() {
        userError("""
Usage: module-info [-v|--valid] PATH...
Print module descriptor of JAR, exploded directory, or module-info.class.

Options:
  -p,--module-path PATH   Module path to resolve implicit dependencies.
  -V,--valid              Print a valid module declaration.
""");
    }

    private Main(ModuleDescriptor descriptor, boolean makeValid, Optional<ModulePath> modulePath) {
        this.descriptor = descriptor;
        this.makeValid = makeValid;
        this.modulePath = modulePath;
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
        try {
            innerMain(args);
        } catch (FailException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    public static void innerMain(String[] args) {
        boolean makeValid = false;
        ModulePath modulePath = null;
        int index = 0;

        for (; index < args.length; ++index) {

            String arg = args[index];

            switch (arg) {
                case "--help":
                case "-h":
                    usage();
                    continue;
                case "-p":
                case "--module-path":
                    try {
                        modulePath = ModulePath.of(requireArg(args, ++index, "Missing argument to " + arg));
                    } catch (IllegalArgumentException e) {
                        userError(e.getMessage());
                    }
                    continue;
                case "--valid":
                case "-V":
                    makeValid = true;
                    continue;
                default:
                    // fall-through intentional
            }

            break; // this is how a switch-case can break out of the for-loop
        }

        if (index > args.length - 1) {
            userError("Too few arguments");
        }

        List<ModuleDescriptor> descriptors = new ArrayList<>();
        for (; index < args.length; ++index) {
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

            descriptors.add(descriptor);
        }

        for (int k = 0; k < descriptors.size(); ++k)
            new Main(descriptors.get(k), makeValid, Optional.ofNullable(modulePath)).run(k > 0);
    }

    private static String requireArg(String[] args, int argi, String errorMessage) {
        if (argi < 0 || argi >= args.length)
            userError(errorMessage);
        return args[argi];
    }

    private void run(boolean addSpace) {
        if (addSpace) println();

        if (descriptor.isOpen()) print("open ");
        print("module " + descriptor.name());
        printVersion(descriptor.rawVersion());
        println(" {");
        indent();

        if (descriptor.mainClass().isPresent()) {
            println();
            if (makeValid) {
                println("// mainClass " + descriptor.mainClass().get() + ";");
            } else {
                println("mainClass " + descriptor.mainClass().get() + ";");
            }
        }

        Set<String> exportedPackages = new HashSet<>();
        if (!descriptor.exports().isEmpty()) {
            println();

            List<ModuleDescriptor.Exports> exportsList = descriptor
                    .exports()
                    .stream()
                    .sorted(Comparator.comparing(ModuleDescriptor.Exports::source))
                    .collect(Collectors.toList());

            for (var exports : exportsList) {
                exportedPackages.add(exports.source());

                print("exports " + exports.source());

                List<String> targets = exports.targets()
                                              .stream()
                                              .sorted()
                                              .collect(Collectors.toList());
                if (targets.size() > 0) {
                    println(" to");
                    for (int i = 0; i < targets.size(); ++i) {
                        if (i < targets.size() - 1) {
                            println("    " + targets.get(i) + ",");
                        } else {
                            println("    " + targets.get(i) + ";");
                        }
                    }
                } else {
                    println(";");
                }
            }
        }

        Set<String> unexportedPackages = new HashSet<>(descriptor.packages());
        unexportedPackages.removeAll(exportedPackages);
        if (!unexportedPackages.isEmpty()) {
            println();

            unexportedPackages.stream()
                              .sorted()
                              .forEach(pkg -> {
                                  if (makeValid) {
                                      println("// package " + pkg + ";");
                                  } else {
                                      println("package " + pkg + ";");
                                  }
                              });
        }

        if (!descriptor.provides().isEmpty()) {
            println();

            List<ModuleDescriptor.Provides> providesList = descriptor
                    .provides()
                    .stream()
                    .sorted(Comparator.comparing(ModuleDescriptor.Provides::service))
                    .collect(Collectors.toList());

            for (var provides : providesList) {
                println("provides " + provides.service() + " with");
                List<String> providersList = provides.providers()
                                               .stream()
                                               .sorted()
                                               .collect(Collectors.toList());
                for (int i = 0; i < providersList.size(); i++) {
                    print("    " + providersList.get(i));
                    if (i < providersList.size() - 1) {
                        println(",");
                    } else {
                        println(";");
                    }
                }
            }
        }

        if (modulePath.isPresent()) {
            List<Dependency> allDependencies = modulePath.get().dependenciesOf(descriptor);
            if (!allDependencies.isEmpty()) {
                println();

                allDependencies.stream()
                               .collect(Collectors.groupingBy(dep -> dep.module().name()))
                               .entrySet()
                               .stream()
                               .sorted(Map.Entry.comparingByKey())
                               .forEach(entry -> {
                                   // Verify all versions match.
                                   Dependency firstDependency = null;
                                   for (Dependency dependency : entry.getValue()) {
                                       if (firstDependency == null) {
                                           firstDependency = dependency;
                                       } else if (!dependency.module().equals(firstDependency.module())) {
                                           throw new FailException("Module " + entry.getKey() + " depends on different " +
                                                                   "versions of module " + firstDependency.module().name() +
                                                                   ": " + firstDependency.directDependee() +
                                                                   " requires " + firstDependency.module() +
                                                                   ", while " + dependency.directDependee() +
                                                                   " requires " + dependency.module());
                                       }
                                   }
                                   Objects.requireNonNull(firstDependency, "No values grouped for " + entry.getKey());

                                   // ANALYSING MODULE DEPENDENCY FLATTENING
                                   //
                                   // module under consideration - module M:
                                   //
                                   //   requires A;
                                   //   requires transitive B;
                                   //   requires static C;
                                   //   requires static transitive D;
                                   //
                                   // module A:
                                   //
                                   //   requires A1;
                                   //   requires transitive A2;
                                   //   requires static A3;
                                   //   requires static transitive A4;
                                   //
                                   // and so on for module B, C, and D.
                                   //
                                   // TRANSITIVENESS - if a module N 'requires M', which modules does it actually depend on?
                                   //
                                   //   M
                                   //   B, B2, B4*
                                   //   D*, D2*, D4*
                                   //
                                   //   *) static, so may be missing runtime.
                                   //
                                   // READABILITY - which modules does M read?
                                   //
                                   //   M
                                   //   A, A2, A4*
                                   //   B, B2, B4*
                                   //   C*, C2*, C4*
                                   //   D*, D2*, D4*
                                   //
                                   // EFFECTIVE MODULE DECLARATION
                                   //
                                   //   requires A;
                                   //   requires A2;
                                   //   requires static A4;
                                   //   requires transitive B;
                                   //   requires transitive B2;
                                   //   requires static transitive B4;
                                   //   requires static C;
                                   //   requires static C2;
                                   //   requires static C4;
                                   //   requires static transitive D;
                                   //   requires static transitive D2;
                                   //   requires static transitive D4;
                                   //
                                   // FLATTEN RULE
                                   //
                                   //   - any static on the path  =>  static
                                   //   - all transitive  =>  transitive.  Since only transitive implied deps are
                                   //     considered anyways, this is equivalent to only considering the top-most
                                   //     transitive.
                                   //
                                   // HIGHLIGHT IMPLIED DEPENDENCIES
                                   //
                                   // The dependencies pulled up by transitive dependencies in required modules
                                   // should be annotated.
                                   //
                                   //   requires A;
                                   //   requires A2 via A;
                                   //   requires static A4 via A;
                                   //   requires transitive B;
                                   //   requires transitive B2 via B;
                                   //   requires static transitive B4 via B;
                                   //   requires static C;
                                   //   requires static C2 via C;
                                   //   requires static C4 via C;
                                   //   requires static transitive D;
                                   //   requires static transitive D2 via D;
                                   //   requires static transitive D4 via D;
                                   //
                                   // CONFLICTS
                                   //
                                   // Applying the flattening rule to everything means the same module may be mentioned
                                   // more than one time.  there are only 4 different modifier sets possible:
                                   //
                                   //   1.  requires X;
                                   //   2.  requires transitive X;
                                   //   3.  requires static X;
                                   //   4.  requires static transitive X;
                                   //
                                   // There are 15 possible combinations.  If both 1&2 are present, only consider 2.
                                   // If both 3&4 are present, only consider 4.  If both 1&3 are present, only
                                   // consider 1.  If both 2&4 are present, only consider 2.  Therefore, the only
                                   // combos that may conflict are:
                                   //
                                   //   a.
                                   //   requires X;
                                   //   requires static transitive X;
                                   //
                                   //   b.
                                   //   requires transitive X;
                                   //   requires static X;
                                   //
                                   // b. is equivalent to only 'requires transitive X'.  a. cannot be expressed by
                                   // valid flat module declarations!
                                   //
                                   // What happens to 'via' when discarding redundant entries?  They cannot be moved
                                   // to the dominant requires.  So leave out-commented, and add comment what shadowed
                                   // them?
                                   //
                                   // SPECIAL CASE WITH 'VIA'
                                   //
                                   // module M:
                                   //   requires A;
                                   //   requires B;
                                   //
                                   // module B:
                                   //   requires A;
                                   //
                                   // Then how should this be flattened?  As follows:
                                   //   requires A and via B;
                                   // The 'and' is missing if A is not required in M.

                                   List<Dependency> notTransitiveNotStatic = find(entry, dep -> !dep.isTransitive() && !dep.isStatic());
                                   List<Dependency> transitiveNotStatic = find(entry, dep -> dep.isTransitive() && !dep.isStatic());
                                   List<Dependency> notTransitiveStatic = find(entry, dep -> !dep.isTransitive() && dep.isStatic());
                                   List<Dependency> transitiveStatic = find(entry, dep -> dep.isTransitive() && dep.isStatic());

                                   boolean showNotTNotS = !notTransitiveNotStatic.isEmpty();
                                   boolean showTNotS = !transitiveNotStatic.isEmpty();
                                   boolean showNotTS = !notTransitiveStatic.isEmpty();
                                   boolean showTS = !transitiveStatic.isEmpty();

                                   // transitive wins
                                   if (showNotTNotS && showTNotS)
                                       showNotTNotS = false;
                                   if (showNotTS && showTS)
                                       showNotTS = false;

                                   // non-static wins
                                   if (showNotTNotS && showNotTS)
                                       showNotTS = false;
                                   if (showTNotS && showTS)
                                       showTS = false;

                                   if (showTNotS && showNotTS)
                                       showNotTS = false;

                                   printRequires(entry.getKey(), transitiveNotStatic, showTNotS);
                                   printRequires(entry.getKey(), notTransitiveNotStatic, showNotTNotS);
                                   printRequires(entry.getKey(), transitiveStatic, showTS);
                                   printRequires(entry.getKey(), notTransitiveStatic, showNotTS);
                               });
            }
        } else {
            List<ModuleDescriptor.Requires> requiresList = descriptor
                    .requires()
                    .stream()
                    .filter(r -> !r.modifiers().contains(ModuleDescriptor.Requires.Modifier.MANDATED))
                    .sorted(Comparator.comparing(ModuleDescriptor.Requires::name))
                    .collect(Collectors.toList());
            if (!requiresList.isEmpty()) {
                println();

                for (var requires : requiresList) {
                    print("requires ");

                    Set<ModuleDescriptor.Requires.Modifier> modifiers = requires.modifiers();
                    if (modifiers.contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE)) {
                        print("transitive ");
                    }
                    if (modifiers.contains(ModuleDescriptor.Requires.Modifier.STATIC)) {
                        print("static ");
                    }

                    print(requires.name());
                    printVersion(requires.rawCompiledVersion());
                    println(";");
                }
            }
        }

        if (!descriptor.uses().isEmpty()) {
            println();

            descriptor.uses().forEach(klass -> println("uses " + klass + ";"));
        }

        println();
        dedent();
        println("}");
    }

    private void printRequires(String module, List<Dependency> dependencies, boolean show) {
        if (dependencies.isEmpty()) return;

        Dependency dependency = dependencies.stream()
                                            .filter(Dependency::isDirect)
                                            .findFirst()
                                            .orElseGet(() -> dependencies.get(0));

        if (dependency.isDirect()) {
            if (!show)
                print("// ");
            print("requires ");
            if (dependency.isStatic())
                print("static ");
            if (dependency.isTransitive())
                print("transitive ");
            print(dependency.module());
            println(";");
        }
    }

    private List<Dependency> find(Map.Entry<String, List<Dependency>> entry, Predicate<Dependency> filter) {
        return entry.getValue()
                .stream()
                .filter(filter)
                .sorted((left, right) -> {
                    if (left == right) return 0;  // optimization

                    // Sort by the module name of the top-most requires, and if equal by the module name of its requires,
                    // and so on, until and including the dependency's module name.
                    List<Dependency> l = left.fullChain();
                    List<Dependency> r = right.fullChain();
                    int prefixSize = Math.min(l.size(), r.size());
                    for (int i = 0; i < prefixSize; ++i) {
                        int c = l.get(i).module().compareTo(r.get(i).module());
                        if (c != 0) return c;
                    }

                    // With a common prefix, sort the shorter chain before the deeper, i.e. the natural order of the chain size.
                    if (l.size() != r.size()) return Integer.compare(l.size(), r.size());

                    throw new IllegalStateException("Duplicate chains: '" + l + "' and '" + r + "'");
                })
                .collect(Collectors.toList());
    }

    private void print(ModuleVersion moduleVersion) {
        if (makeValid) {
            print(moduleVersion.name() + " /* @" + moduleVersion.version().map(Object::toString).orElse("") + " */");
        } else {
            print(moduleVersion.versionedName());
        }
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
