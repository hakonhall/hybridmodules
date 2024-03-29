package no.ion.jhms;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.compare;
import static java.util.Objects.requireNonNull;
import static no.ion.jhms.ExceptionUtil.uncheck;

public class HybridModuleContainer implements AutoCloseable {
    private final PlatformModuleContainer platformModuleContainer;
    private final ObservableHybridModules observableHybridModules;
    private final TreeMap<HybridModuleId, HybridModule> hybridModules = new TreeMap<>();
    private final Set<HybridModuleId> roots = new HashSet<>();

    // As soon as the resolution of a hybrid module starts, it is added here to detect cycles.
    private final Set<HybridModuleId> startedResolutions = new HashSet<>();

    public HybridModuleContainer() {
        this.platformModuleContainer = new PlatformModuleContainer();
        this.observableHybridModules = new ObservableHybridModules();
    }

    /**
     * Make all modular JARs at the given paths observable (JHMS §2.3, §2.8).
     *
     * <p>A path either refers to a regular file, which must be a modular JAR, or a directory in case
     * all *.jar files must be modular JARs. Either way, these are then made observable and ready
     * to be resolved if necessary during resolution {@link #resolve(ResolveParams) resolve()}.
     *
     * @throws FindException if e.g. two hybrid modular JAR are found with for the same hybrid module name and version.
     * @throws java.io.UncheckedIOException
     * @throws InvalidHybridModuleException
     */
    public void discoverHybridModulesFromModulePath(String modulePath) { observableHybridModules.discoverHybridModulesFromModulePath(modulePath); }
    public void discoverHybridModules(String... paths) { discoverHybridModules(Stream.of(paths).map(Paths::get).collect(Collectors.toList()));}
    public void discoverHybridModules(Path... paths) { discoverHybridModules(Arrays.asList(paths)); }
    public void discoverHybridModules(List<Path> paths) { observableHybridModules.discoverHybridModules(paths); }

    /**
     * Make all modular JARs and exploded modules in the current class loader's META-INF/mod/ directory observable.
     */
    public void discoverEmbeddedHybridModules() {
        getClass().getClassLoader()
                  .resources("META-INF/mod/")
                  .forEach(url -> {
                      if (Objects.equals(url.getProtocol(), "file")) {
                          String path = requireNonNull(url.getPath(), "Empty path of file url: " + url);
                          discoverHybridModules(path);
                      } else if (Objects.equals(url.getProtocol(), "jar")) {
                          String urlPath = url.getPath();
                          int separatorIndex = urlPath.indexOf("!/");
                          if (separatorIndex == -1)
                              throw new IllegalArgumentException("Invalid JAR URL: Missing separator: " + url);
                          String jarFileUrlString = urlPath.substring(0, separatorIndex);
                          URI jarFileUri = URI.create(jarFileUrlString);
                          String entryPath = urlPath.substring(separatorIndex + 2);
                          if (!Objects.equals("jar:" + jarFileUri + "!/" + entryPath, url.toString()))
                              throw new IllegalArgumentException("Invalid JAR URL: " + url.toString());

                          final URI uri;
                          try {
                              uri = url.toURI();
                          } catch (URISyntaxException e) {
                              throw new IllegalArgumentException("Invalid URI: " + url, e);
                          }
                          FileSystem fileSystem = uncheck(() -> FileSystems.newFileSystem(uri, Map.of()));
                          try {
                              Path modDirectory = fileSystem.getPath(entryPath);
                              discoverHybridModules(modDirectory);
                          } finally {
                              uncheck(fileSystem::close);
                          }
                      } else {
                          throw new IllegalArgumentException("/META-INF/mod/ resource in the " + getClass().getClassLoader() +
                                                             " class loader is neither a file nor jar");
                      }
                  });
    }

    /**
     * Returns true if {@code module} is the ID of an observable hybrid module, or the name of an
     * (observable) platform module.
     *
     * @throws IllegalArgumentException if {@code module} is ill-formed
     */
    public boolean isObservable(String module) {
        int atIndex = module.indexOf('@');
        if (atIndex == -1) {
            // Is presumably a platform module
            BaseModule.validateModuleName(module);
            return platformModuleContainer.get(module).isPresent();
        } else {
            // Is presumably a hybrid module
            HybridModuleId.validateHybridModuleId(module);
            HybridModuleId id = HybridModuleId.fromId(module);
            return observableHybridModules.has(id);
        }
    }

    public static class ResolveParams {
        final String moduleName;

        Optional<HybridModuleVersion> version = Optional.empty();

        public ResolveParams(String rootHybridModuleName) {
            this.moduleName = requireNonNull(rootHybridModuleName);
        }

        /**
         * Require a particular version of the root hybrid module, null meaning without version.
         *
         * <p>If a particular version is NOT required, there must be exactly one observable version of the module.
         */
        public ResolveParams requireVersion(String version) {
            this.version = Optional.of(HybridModuleVersion.from(version));
            return this;
        }

        /**
         * Require a particular version of the root hybrid module, null meaning without version.
         *
         * <p>If a particular version is NOT required, there must be exactly one observable version of the module.
         */
        public ResolveParams requireVersion(ModuleDescriptor.Version version) {
            Objects.requireNonNull(version, "version cannot be null");
            this.version = Optional.of(HybridModuleVersion.from(version));
            return this;
        }
    }

    public RootHybridModule resolve(ResolveParams params) {
        HybridModuleId id = resolveHybridModuleId(params);
        HybridModule root = resolveHybridModule(id);
        roots.add(id);
        // TODO: Maintain a usage counter?
        return new RootHybridModule(root);
    }

    public ModuleGraph getModuleGraph(ModuleGraph.Params params) {

        Set<HybridModule> roots = this.roots.stream().map(id -> {
            var hybridModule = hybridModules.get(id);
            if (hybridModule == null) {
                throw new IllegalStateException("Root has not been resolved: " + hybridModule);
            }
            return hybridModule;
        }).collect(Collectors.toCollection(HashSet::new));

        ModuleGraph graph = new ModuleGraph(params);

        if (params.excludeUnreadable()) {
            HashSet<HybridModuleId> hybridModuleUniverse = new HashSet<>();
            HashSet<String> platformModuleUniverse = new HashSet<>();
            roots.forEach(hybridModule -> {
                hybridModule.hybridReads().stream().map(HybridModule::id).forEach(hybridModuleUniverse::add);
                hybridModule.platformReads().stream().map(PlatformModule::name).forEach(platformModuleUniverse::add);
            });
            graph.setHybridModuleUniverse(hybridModuleUniverse);
            graph.setPlatformModuleUniverse(platformModuleUniverse);
        }

        roots.forEach(hybridModule -> hybridModule.fillModuleGraph(graph));

        return graph;
    }

    public static class GraphParams {
        private boolean includeReads = true;
        private boolean includeSelfReadEdge = false;
        private boolean includeJavaBaseReadEdge = false;

        private boolean includePackageVisibility = true;

        public void includeSelfReads() {
            includeSelfReadEdge = true;
        }
    }

    public String moduleGraph2(GraphParams params) {
        StringBuilder builder = new StringBuilder(1024);

        if (params.includeReads) {
            // hybridModules happens to be sorted due to its type
            for (var hybridModule : hybridModules.values()) {
                TreeMap<HybridModuleId, List<String>> visiblePackagesByHybridModuleId = null;
                if (params.includePackageVisibility) {
                    var classLoader = hybridModule.getClassLoader();
                    visiblePackagesByHybridModuleId = classLoader.hybridModulesByPackage().entrySet().stream()
                            .collect(Collectors.groupingBy(entry -> entry.getValue().id()))
                            .entrySet().stream()
                            .collect(Collectors.toMap(
                                    entry -> entry.getKey(),
                                    entry -> entry.getValue().stream()
                                            .map(x -> x.getKey())
                                            .collect(Collectors.toList()),
                                    (o, n) -> n,
                                    TreeMap::new
                            ));
                }

                List<HybridModuleId> hybridReadIds = hybridModule.hybridReads().stream().map(HybridModule::id).sorted().collect(Collectors.toList());
                for (var hybridDependencyId : hybridReadIds) {
                    if (hybridDependencyId.equals(hybridModule.id()) && !params.includeSelfReadEdge) {
                        continue;
                    }

                    builder.append(hybridModule.id() + " reads " + hybridDependencyId);

                    if (visiblePackagesByHybridModuleId != null) {
                        builder.append(" [" + String.join(",", visiblePackagesByHybridModuleId.getOrDefault(hybridDependencyId, List.of())) + "]");
                    }
                    builder.append('\n');
                }

                List<String> platformDependencyNames = hybridModule.platformReads().stream().map(PlatformModule::name).sorted().collect(Collectors.toList());
                for (var platformDependencyName : platformDependencyNames) {
                    if (platformDependencyName.equals("java.base") && !params.includeJavaBaseReadEdge) {
                        continue;
                    }

                    builder.append(hybridModule.id() + " reads " + platformDependencyName + '\n');
                }
            }
        }

        return builder.toString();
    }

    @Override
    public void close() {
        observableHybridModules.close();
    }

    private HybridModuleId resolveHybridModuleId(ResolveParams params) {
        if (params.version.isPresent()) {
            return new HybridModuleId(params.moduleName, params.version.get());
        }

        List<HybridModuleJar> jars = observableHybridModules.getJarsWithName(params.moduleName);
        switch (jars.size()) {
            case 0:
                // Consistent with JPMS: "java.lang.module.FindException: Module foo not found"
                throw new FindException("Hybrid module " + params.moduleName + " not found");
            case 1:
                return jars.get(0).hybridModuleId();
            default:
                // JPMS will fail if there is more than one JAR in a directory with the same name, if that
                // directory is on the module path, with the error message:
                //     "java.lang.module.FindException: Two versions of module no.m1 found in . (no.m1.jar and no.m1-2.jar)"
                //
                // This is even if the second is a copy of the first JAR (so not really two *versions").
                //
                // However if two such JARs are in different directories, and even if they are of different versions
                // and/or have different contents, JPMS will silently pick the first it finds on the module path!
                //
                // This seems wrong: We will instead find all JARs, and require hybrid module ID be 1:1 with JAR checksum.
                //
                // However, if we get here, the caller is fine with any version of the hybrid module,
                // but we found more than one. This is therefore more like an illegal argument than a FindException.
                throw new IllegalArgumentException("Hybrid module " + params.moduleName + " requested but multiple versions found ("
                        + jars.stream().map(j -> j.path().toString()).collect(Collectors.joining(", "))
                        + ")");
        }
    }

    private HybridModule resolveHybridModule(HybridModuleId id) {
        HybridModule hybridModule = hybridModules.get(id);
        if (hybridModule != null) {
            return hybridModule;
        }

        if (startedResolutions.contains(id)) {
            throw new FindException("Cyclic dependency on hybrid module " + id + " detected");
        }
        startedResolutions.add(id);

        boolean successful = false;
        try {
            hybridModule = resolveNewHybridModule(id);
            hybridModules.put(id, hybridModule);

            successful = true;
            return hybridModule;
        } finally {
            if (!successful) {
                startedResolutions.remove(id);
            }
        }
    }

    private HybridModule resolveNewHybridModule(HybridModuleId id) {
        HybridModuleJar jar = observableHybridModules.getJar(id);
        HybridModule.Builder builder = new HybridModule.Builder(jar);
        ModuleDescriptor descriptor = jar.descriptor();

        if (descriptor.isAutomatic()) {
            throw new InvalidHybridModuleException("Automatic hybrid modules are not yet supported: " + jar.path());
        }

        builder.setPackages(descriptor.packages());

        for (var requires : descriptor.requires()) {
            if (requires.modifiers().contains(ModuleDescriptor.Requires.Modifier.STATIC)) {
                // JHMS §2.2: static requires are ignored at run time.
                continue;
            }

            boolean transitive = requires.modifiers().contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE);
            Optional<PlatformModule> requiredPlatformModule = platformModuleContainer.resolve(requires.name());
            if (requiredPlatformModule.isPresent()) {
                builder.addPlatformModuleRequires(requiredPlatformModule.get(), transitive);
            } else {
                HybridModuleVersion version = HybridModuleVersion.fromRaw(requires.rawCompiledVersion());
                HybridModuleId requiredHybridModuleId = new HybridModuleId(requires.name(), version);
                final HybridModule requiredHybridModule;
                try {
                    requiredHybridModule = resolveHybridModule(requiredHybridModuleId);
                } catch (FindException e) {
                    throw new FindException(e.getMessage() + ": Required by " + id);
                }
                builder.addHybridModuleRequires(requiredHybridModule, transitive);
            }
        }

        for (var exports : descriptor.exports()) {
            builder.addExports(exports.source(), exports.targets());
        }

        return builder.build();
    }
}
