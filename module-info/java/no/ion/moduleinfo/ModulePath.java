package no.ion.moduleinfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

class ModulePath {
    private final ModuleFinder systemModuleFinder;
    private final TreeMap<String, Map<Optional<ModuleDescriptor.Version>, ModuleReference>> references;

    /** Handle that module path has a directory with different versions of a module. */
    static ModulePath of(String colonSeparatedModulePath) {
        if (colonSeparatedModulePath.isEmpty())
            throw new IllegalArgumentException("Empty module path");

        TreeMap<String, Map<Optional<ModuleDescriptor.Version>, ModuleReference>> references = new TreeMap<>();

        String[] pathStrings = colonSeparatedModulePath.split(":", -1);
        for (String pathString : pathStrings) {
            if (pathString.isEmpty())
                throw new FailException("Invalid module path: Found empty entry");
            Path path = Path.of(pathString);

            BasicFileAttributes attributes;
            try {
                attributes = Files.readAttributes(path, BasicFileAttributes.class);
            } catch (FileNotFoundException e) {
                throw new FailException("Invalid module path entry: no such file or directory: " + path);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            if (attributes.isRegularFile()) {
                if (!path.getFileName().toString().endsWith(".jar"))
                    throw new FailException("Non-JAR file on module path: " + path);
                updateFromJar(path, references);
            } else if (Files.isDirectory(path)) {
                // Directory may be an exploded JAR, or a directory of JARs and/or exploded JARs
                try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
                    for (var entry : directoryStream) {
                        BasicFileAttributes entryAttributes;
                        try {
                            entryAttributes = Files.readAttributes(entry, BasicFileAttributes.class);
                        } catch (FileNotFoundException e) {
                            throw new FailException("Invalid module path entry: no such file or directory: " + entry);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }

                        if (entryAttributes.isRegularFile()) {
                            // allow non-JAR files in directory
                            if (entry.getFileName().toString().endsWith(".jar"))
                                updateFromJar(entry, references);
                        } else if (entryAttributes.isDirectory()) {
                            // This time, it must be an exploded module. verify.  Require non-automatic.
                            if (!Files.isRegularFile(entry.resolve("module-info.class")))
                                throw new FailException("Not an exploded module: " + entry);
                        } else {
                            // ignore other types. not error.
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else {
                throw new FailException("Invalid module path entry: neither file nor directory: " + path);
            }
        }

        return new ModulePath(references);
    }

    private static void updateFromJar(Path path, TreeMap<String, Map<Optional<ModuleDescriptor.Version>, ModuleReference>> references) {
        Iterator<ModuleReference> iterator = ModuleFinder.of(path).findAll().iterator();
        if (!iterator.hasNext())
            throw new FailException("No module found: " + path);
        ModuleReference reference = iterator.next();
        if (iterator.hasNext())
            throw new FailException("More than one module found: " + path);

        ModuleVersion module = new ModuleVersion(reference.descriptor().name(), reference.descriptor().version());

        ModuleReference previousReference = references
                .computeIfAbsent(module.name(), __ -> new HashMap<>())
                .put(module.version(), reference);
        if (previousReference != null)
            throw new FailException("Both JARs refers to " + module + ": '" + previousReference.location() +
                                    "' and '" + reference.location() + "'");
    }

    ModulePath(TreeMap<String, Map<Optional<ModuleDescriptor.Version>, ModuleReference>> references) {
        this.systemModuleFinder = ModuleFinder.ofSystem();
        this.references = references;
    }

    List<Dependency> dependenciesOf(ModuleDescriptor descriptor) {
        return addDependenciesOf(ModuleVersion.from(descriptor),
                                 descriptor,
                                 true,  // isDirect
                                 true,  // isTransitive is true
                                 false, // isStatic
                                 false, // systemModule
                                 new ArrayList<>(),  // dependency chain
                                 new ArrayList<>());  // all dependencies
    }

    private List<Dependency> addDependenciesOf(ModuleVersion module,
                                               ModuleDescriptor descriptor,
                                               boolean isDirect,
                                               boolean isTransitive,
                                               boolean isStatic,
                                               boolean systemModule,
                                               ArrayList<Dependency> chain,
                                               List<Dependency> dependencies) {
        List<ModuleDescriptor.Requires> requiresList = descriptor
                .requires()
                .stream()
                .filter(r -> {
                    if (r.modifiers().contains(ModuleDescriptor.Requires.Modifier.MANDATED))
                        return false;
                    if (!isDirect && !r.modifiers().contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE))
                        return false;
                    return true;
                })
                .sorted(Comparator.comparing(ModuleDescriptor.Requires::name))
                .collect(Collectors.toList());

        for (ModuleDescriptor.Requires requires : requiresList) {
            boolean requiresIsTransitive = isTransitive && requires.modifiers().contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE);
            boolean requiresIsStatic = isStatic || requires.modifiers().contains(ModuleDescriptor.Requires.Modifier.STATIC);
            Dependency dependency = Dependency.of(module, chain, descriptor, requires, isDirect, requiresIsTransitive, requiresIsStatic);
            dependencies.add(dependency);
            chain.add(dependency);

            Optional<ModuleReference> reference = systemModuleFinder.find(dependency.module().name());
            if (reference.isPresent()) {
                addDependenciesOf(module, reference.get().descriptor(), false, requiresIsTransitive, requiresIsStatic, true, chain, dependencies);
            } else if (systemModule) {
                throw new FailException("Failed to find system module: " + dependency.module().name());
            } else {
                Map<Optional<ModuleDescriptor.Version>, ModuleReference> byVersion = references.get(dependency.module().name());
                if (byVersion == null)
                    throw new FailException("No such module on module path: " + dependency.module().name() + ": required by " +
                                            dependency.directDependee().name());

                ModuleReference dependencyReference = byVersion.get(dependency.module().version());
                if (dependencyReference == null) {
                    Collection<ModuleReference> refs = byVersion.values();
                    Iterator<ModuleReference> iterator = refs.iterator();
                    if (!iterator.hasNext())
                        throw new FailException("No such module on module path: " + dependency.module().name() + ": required by " +
                                                dependency.directDependee().name());
                    dependencyReference = iterator.next();
                    if (iterator.hasNext())
                        throw new FailException("Module " + dependency.directDependee().versionedName() + " requires " +
                                                dependency.module() + " which is not uniquely found: these are available: " +
                                                refs);
                }
                addDependenciesOf(module, dependencyReference.descriptor(), false, requiresIsTransitive, requiresIsStatic, false, chain, dependencies);
            }

            // Remove the last element of chain, 'dependency'
            Dependency removedDependency = chain.remove(chain.size() - 1);
            if (removedDependency != dependency)
                throw new IllegalStateException("Removed " + (removedDependency == null ? "null" : removedDependency.module()) +
                                                " but expected " + dependency.module());
        }

        return dependencies;
    }
}
