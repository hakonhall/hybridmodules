package no.ion.hybridmodules;

import java.io.IOException;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

class HybridModuleFinder implements AutoCloseable {
    /** Map from hybrid module name, to version, to Jar. */
    private final HashMap<String, Map<Optional<ModuleDescriptor.Version>, Jar>> modularJars = new HashMap<>();
    private final ModuleFinder parentFinder;
    private final Path[] paths;

    private volatile boolean closed = false;

    /**
     * Index all hybrid modules at the given paths.
     *
     * @param paths an array of paths, each path being a path to a hybrid module packaged
     *              as a modular JAR, or a directory containing such hybrid modules.
     */
    static HybridModuleFinder of(Path... paths) {
        HybridModuleFinder finder = new HybridModuleFinder(paths);
        finder.index();
        return finder;
    }

    private HybridModuleFinder(Path... paths) {
        // The platform class loader should observe classes exactly 1:1 with the ModuleFinder.ofSystem()
        // used to find modules not provided by the application, see HybridModuleClassLoader. It's not
        // known whether this is in fact the case.
        this.parentFinder = ModuleFinder.ofSystem();

        this.paths = paths;
    }

    Optional<ModuleReference> findSystemModuleReference(String name) {
        verifyNotClosed();
        return parentFinder.find(name);
    }

    /**
     *
     * @param name The name of the hybrid module. If it suffixed with @SUFFIX, then SUFFIX is
     *             parsed as a version. If SUFFIX is absent (but @ is present), a hybrid module
     *             without a version is retrieved (not encouraged).
     * @return
     */
    Jar getHybridModuleJar(String name) {
        verifyNotClosed();

        int index = name.indexOf('@');
        if (index == -1) {
            return getHybridModuleJar(name, null);
        } else if (index == name.length() - 1) {
            return getHybridModuleJar(name.substring(0, index), Optional.empty());
        } else {
            return getHybridModuleJar(name.substring(0, index), Optional.of(ModuleDescriptor.Version.parse(name.substring(index + 1))));
        }
    }

    /**
     * Find the modular JAR of a hybrid module.
     *
     * @param name    the name of the modular JAR.
     * @param version the version of the requested modular JAR. If null, any version is fine. If empty, a modular JAR
     *                without version must be found.
     * @return Modular JAR of the hybrid module with the given name and version.
     */
    Jar getHybridModuleJar(String name, Optional<ModuleDescriptor.Version> version) {
        verifyNotClosed();

        var byVersion = modularJars.get(name);
        if (byVersion == null) {
            throw new FindException("Failed to find hybrid module '" + name + "' in hybrid module path '" + getHybridModulePath() + "'");
        }

        if (version == null) {
            if (byVersion.size() == 1) {
                return byVersion.values().iterator().next();
            } else {
                throw new FindException(
                        "There are multiple versions of the hybrid module named '" + name + "': " +
                                byVersion.keySet().stream()
                                        .map(v -> v.map(ModuleDescriptor.Version::toString).orElse("(without version)"))
                                        .collect(Collectors.joining(", ")));
            }
        }

        var jar = byVersion.get(version);
        if (jar == null) {
            if (version.isPresent()) {
                throw new FindException(
                        "Failed to find version " + version.get() + " of hybrid module '" + name + "', available versions: " +
                        byVersion.keySet().stream()
                                .map(v -> v.map(ModuleDescriptor.Version::toString).orElse("(without version)"))
                                .collect(Collectors.joining(", ")));
            } else {
                throw new FindException(
                        "Failed to find hybrid module '" + name + "' without version, but some with versions: " +
                                byVersion.keySet().stream()
                                        .map(v -> v.map(ModuleDescriptor.Version::toString).orElseThrow())
                                        .collect(Collectors.joining(", ")));
            }
        } else {
            return jar;
        }
    }

    @Override
    public void close() {
        closed = true;
        modularJars.values().stream().flatMap(m -> m.values().stream()).forEach(Jar::close);
        modularJars.clear();
    }

    private void index() {
        // This duplicates some of the logic in jdk.internal.module.ModulePath, but it's necessary
        // since we'd like to link each module to it's filename.

        for (var path : paths) {
            BasicFileAttributes attributes;
            try {
                attributes = Files.readAttributes(path, BasicFileAttributes.class);
            } catch (IOException ioe) {
                // Note: ModuleFinder.of() would ignore a NoSuchFileException
                // This seems wrong: NSFException may indicate a configuration error, and it seems better to
                // warn (fail) as early and quickly as possible.
                throw new FindException(ioe);
            }

            if (attributes.isDirectory()) {
                readAllModulesInDirectory(path);
            } else if (attributes.isRegularFile()) {
                readModularJar(path, false);
            } else {
                // This case may end up with a strange error from ModuleFinder, so handle it here
                throw new FindException("Failed to read module(s) at " + path + ": neither directory nor regular file");
            }
        }
    }

    private void readAllModulesInDirectory(Path directory) {
        try {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
                for (Path entry : stream) {
                    readModularJar(entry, true);
                }
            }
        } catch (IOException e) {
            throw new FindException(e);
        }
    }

    private void readModularJar(Path path, boolean ignoreNonModularJar) {
        // WARNING: There's also a Path::endsWith, but that tests against the whole last path component.
        if (!path.toString().endsWith(".jar")) {
            if (ignoreNonModularJar) {
                return;
            } else {
                throw new FindException("Failed to read modular JAR file " + path + ": unknown file extension");
            }
        }

        ModuleFinder finder = ModuleFinder.of(path);
        var references = finder.findAll();
        switch (references.size()) {
            case 0:
                // This may happen if the file has been deleted(!).
                if (ignoreNonModularJar) {
                    return;
                } else {
                    throw new FindException("No modular JAR file found at path " + path);
                }
            case 1:
                break;
            default:
                // Not sure how this can happen, except... file replaced with directory of modular JARs.
                // Will not try to second-guess this.
                throw new IllegalStateException("Found several modular JAR files at path " + path);
        }

        ModuleReference reference = references.iterator().next();
        String name = reference.descriptor().name();
        if (parentFinder.find(name).isPresent()) {
            throw new FindException("Modular JAR at " + path + " has a name (" + name + ") that duplicates a system/parent module");
        }

        var jar = new Jar(reference);
        var byVersion = modularJars.computeIfAbsent(jar.moduleId().name(), (n) -> new HashMap<>());
        Jar duplicateJar = byVersion.putIfAbsent(jar.moduleId().version(), jar);
        if (duplicateJar != null) {
            // TODO: Don't fail on exact copies?
            if (!Objects.equals(jar, duplicateJar)) {
                throw new FindException("Module " + jar.moduleId() + " at " + jar.uri() + " has the same name and version as " + duplicateJar.uri());
            }
        }
    }

    private String getHybridModulePath() {
        return Arrays.stream(paths).map(Path::toString).collect(Collectors.joining(":"));
    }

    private void verifyNotClosed() {
        if (closed) {
            throw new IllegalStateException("Method called after instance has been closed");
        }
    }
}
