package no.ion.jhms;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static no.ion.jhms.ExceptionUtil.uncheck;

/**
 * Loads hybrid modular JAR(s) from disk without resolving them, typically based on {@code --module-path}.
 */
class HybridModularJarLoader {
    private final PlatformModuleContainer platformModuleContainer;
    private final TreeMap<String, TreeMap<ModuleDescriptor.Version, HybridModularJar>> jars = new TreeMap<>();

    HybridModularJarLoader(PlatformModuleContainer platformModuleContainer) {
        this.platformModuleContainer = platformModuleContainer;
    }

    /** Load hybrid modular JARs as specified by the value of the {@code --module-path/-p} option. */
    void loadFromModulePath(String modulePath) {
        for (String element : modulePath.split(":")) {
            try {
                load(Path.of(element));
            } catch (FindException ignored) {
                // Non-existing elements in --module-path are ignored by both javac and java.
            }
        }
    }

    void load(String... hybridModulePaths) { load(Arrays.stream(hybridModulePaths).map(Path::of).collect(Collectors.toList())); }
    void load(List<Path> paths) { paths.forEach(this::load); }

    /**
     * Load hybrid modular JAR(s).
     *
     * <ol>
     *     <li>If {@code path} refers to a file, it must be a hybrid modular JAR.</li>
     *     <li>If {@code path} refers to a directory, all {@code .jar} files in the directory must be
     *     hybrid modular JARs and will be loaded. There is no recursion into subdirectories.</li>
     *     <li>Otherwise, a {@link FindException} is thrown.</li>
     * </ol>
     *
     * @param path
     * @throws FindException if {@code path} was not a hybrid modular JAR or it was not a directory.
     * @throws UncheckedIOException wrapping an {@link IOException} on I/O failures.
     */
    void load(Path path) {
        BasicFileAttributes attributes = uncheck(() -> Files.readAttributes(path, BasicFileAttributes.class));

        if (attributes.isRegularFile()) {
            loadHybridModularJAR(path);
        } else if (attributes.isDirectory()) {
            try (DirectoryStream<Path> subdirectoryPaths = Files.newDirectoryStream(path)) {
                for (Path subPath : subdirectoryPaths) {
                    // subPath.endsWith() is unnecessary expensive
                    if (Files.isRegularFile(subPath) && subPath.toString().endsWith(".jar")) {
                        loadHybridModularJAR(subPath);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            throw new FindException(path + " is neither a regular file nor directory");
        }
    }

    List<HybridModuleId2> getHybridModuleIds() {
        return jars.entrySet().stream()
                .flatMap(entry -> entry.getValue().keySet().stream().map(e2 -> new HybridModuleId2(entry.getKey(), e2)))
                .collect(Collectors.toList());
    }

    Optional<HybridModularJar> getHybridModularJar(HybridModuleId2 id) {
        return Optional.ofNullable(jars.get(id.name())).map(m -> m.get(id.version()));
    }

    private void loadHybridModularJAR(Path path) {
        HybridModularJar jar = HybridModularJar.open(path, platformModuleContainer);
        HybridModuleId2 id = jar.id();
        HybridModularJar oldJar = jars.computeIfAbsent(id.name(), key -> new TreeMap<>()).putIfAbsent(id.version(), jar);
        if (oldJar != null) {
            if (uncheck(() -> Files.isSameFile(jar.path(), oldJar.path()))) return;
            if (Arrays.equals(jar.calculateJarFileChecksum(), oldJar.calculateJarFileChecksum())) return;
            throw new FindException("Two hybrid modular JARs with the same name and version " + id +
                    " have different checksums: " + oldJar.path() + " and " + jar.path());
        }
    }

    String getEffectiveModulePath() {
        return jars.values().stream()
                .flatMap(x -> x.values().stream())
                .map(HybridModularJar::path)
                .map(Path::toString)
                .collect(Collectors.joining(":"));
    }
}
