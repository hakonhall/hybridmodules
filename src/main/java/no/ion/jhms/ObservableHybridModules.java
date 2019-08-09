package no.ion.jhms;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.FindException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static no.ion.jhms.ExceptionUtil.uncheck;

class ObservableHybridModules implements AutoCloseable {
    /** Hybrid module JAR by name and version. */
    private TreeMap<String, TreeMap<HybridModuleVersion, HybridModuleJar>> jars = new TreeMap<>();

    void discoverHybridModulesFromModulePath(String modulePath) {
        for (String element : modulePath.split(":")) {
            // Non-existing elements in --module-path are ignored by both javac and java.
            discoverHybridModules(Path.of(element), true);
        }
    }

    void discoverHybridModules(List<Path> paths) { paths.forEach(this::discoverHybridModules); }

    void discoverHybridModules(Path path) { discoverHybridModules(path, false); }

    List<HybridModuleId> getHybridModuleIds() {
        return jars.entrySet().stream()
                .flatMap(entry -> entry.getValue().keySet().stream().map(version -> new HybridModuleId(entry.getKey(), version)))
                .sorted()
                .collect(Collectors.toList());
    }

    HybridModuleJar getJar(HybridModuleId id) {
        return Optional
                .ofNullable(jars.get(id.name()))
                .map(m -> m.get(id.version()))
                // Consistent with JPMS: "java.lang.module.FindException: Module foo not found"
                .orElseThrow(() -> new FindException("Hybrid module " + id + " not found"));
    }

    List<HybridModuleJar> getJarsWithName(String name) {
        Map<HybridModuleVersion, HybridModuleJar> jarsByVersion = jars.get(name);
        return jarsByVersion == null ? List.of() : new ArrayList<>(jarsByVersion.values());
    }

    String getEffectiveModulePath() {
        return jars.values().stream()
                .flatMap(x -> x.values().stream())
                .map(HybridModuleJar::path)
                .map(Path::toString)
                .collect(Collectors.joining(":"));
    }

    @Override
    public void close() { jars.values().stream().flatMap(m -> m.values().stream()).forEach(HybridModuleJar::close); }

    private void discoverHybridModules(Path path, boolean ignoreBadPath) {
        BasicFileAttributes attributes = uncheck(() -> Files.readAttributes(path, BasicFileAttributes.class));

        if (attributes.isRegularFile()) {
            readHybridModule(path);
        } else if (attributes.isDirectory()) {
            try (DirectoryStream<Path> directoryFiles = Files.newDirectoryStream(path)) {
                for (Path directoryFile : directoryFiles) {
                    // subPath.endsWith() is unnecessary expensive
                    if (Files.isRegularFile(directoryFile) && directoryFile.toString().endsWith(".jar")) {
                        readHybridModule(directoryFile);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else if (!ignoreBadPath) {
            throw new FindException(path + " is neither a regular file nor directory");
        }
    }

    private void readHybridModule(Path jarPath) {
        HybridModuleJar jar = HybridModuleJar.open(jarPath);
        try {
            HybridModuleId id = jar.hybridModuleId();
            HybridModuleJar currentJar = jars.computeIfAbsent(id.name(), key -> new TreeMap<>()).putIfAbsent(id.version(), jar);
            if (currentJar == null) {
                jar = null; // avoid close in 'finally'
            } else if (!jar.checksumEqual(currentJar)) {
                throw new FindException("Both " + jarPath + " and " + currentJar.path() + " claim to be hybrid module " + id.toString());
            }
        } finally {
            if (jar != null) {
                // We get here if there already was a HybridModuleJar in 'jars' for the hybrid module ID.
                jar.close();
            }
        }
    }
}
