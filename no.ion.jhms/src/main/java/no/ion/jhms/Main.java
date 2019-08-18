package no.ion.jhms;

import java.lang.module.FindException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static no.ion.jhms.ExceptionUtil.uncheck;

public class Main {
    public static final String MODULE_GRAPH_FILE_PREFIX = "file:";
    private Path graphModuleOutputPath = null;
    private String modulePath = null;
    private String hybridModuleName = null;
    private String mainClass = null;
    private ModuleGraph.Params moduleGraphParams = null;

    public static void main(String... args) {
        new Main().run(args);
    }

    private void run(String... args) {
        int index = 0;
        for (; index < args.length; ++index) {
            final String arg = args[index];
            switch (arg) {
                case "--module-graph":
                case "-g":
                    failIf(index + 1 >= args.length, () -> "Missing argument to " + arg);
                    ++index;
                    moduleGraphParams = parseModuleGraphOptionValue(args[index]);
                    continue;
                case "--module-path":
                case "-p":
                    failIf(index + 1 >= args.length, () -> "Missing argument to " + arg);
                    ++index;
                    modulePath = args[index];
                    continue;
                case "--module":
                case "-m":
                    failIf(index + 1 >= args.length, () -> "Missing argument to " + arg);
                    ++index;
                    String value = args[index];
                    int slashIndex = value.indexOf('/');
                    if (slashIndex == -1) {
                        hybridModuleName = value;
                        mainClass = null;
                    } else {
                        hybridModuleName = value.substring(0, slashIndex);
                        mainClass = value.substring(slashIndex + 1);
                    }
                    ++index;
                    break;
                case "--":
                    ++index;
                    break;
                default:
                    if (arg.startsWith("-")) {
                        userError("Unknown option: " + arg);
                    } else {
                        userError("Missing --module");
                    }
            }

            // Use 'continue' to loop around.
            break;
        }

        String[] mainArgs = Arrays.copyOfRange(args, index, args.length);

        if (modulePath == null) {
            userError("Missing --module-path");
        }

        if (hybridModuleName == null) {
            userError("Missing --module");
        }

        // Avoid closing container when returning from main(), since daemon threads may have been spawned.
        var container = new HybridModuleContainer();

        try {
            container.discoverHybridModulesFromModulePath(modulePath);
            var params = new HybridModuleContainer.ResolveParams(hybridModuleName);
            RootHybridModule rootModule = container.resolve(params);

            if (moduleGraphParams == null) {
                rootModule.main(mainClass, mainArgs);
            } else {
                for (String id : moduleGraphParams.modulesExcluded()) {
                    if (!container.isObservable(id)) {
                        userError("The module " + id + " is not observable");
                    }
                }

                ModuleGraph moduleGraph = container.getModuleGraph(moduleGraphParams);
                GraphvizDigraph graph = GraphvizDigraph.fromModuleGraph(moduleGraph);
                String dot = graph.toDot();

                if (graphModuleOutputPath == null) {
                    System.out.println(dot);
                } else {
                    uncheck(() -> Files.writeString(graphModuleOutputPath, dot, StandardCharsets.UTF_8));
                }
            }
        } catch (IllegalArgumentException | FindException | InvalidHybridModuleException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private ModuleGraph.Params parseModuleGraphOptionValue(String optionValue) {
        var params = new ModuleGraph.Params();

        Stream.of(optionValue.split(",", -1))
                .map(String::strip)
                .filter(Predicate.not(String::isEmpty))
                .forEach(graphOption -> {
                    switch (graphOption) {
                        case "exports":
                            params.includeExports(true);
                            break;
                        case "noplatform":
                            params.excludePlatformModules(true);
                            break;
                        case "self":
                            params.includeSelf(true);
                            break;
                        case "visible":
                            params.excludeUnreadable(true);
                            break;
                        default:
                            if (graphOption.startsWith("-")) {
                                String module = graphOption.substring(1);
                                params.excludeModule(module);
                            } else if (graphOption.startsWith(MODULE_GRAPH_FILE_PREFIX)) {
                                graphModuleOutputPath = Path.of(graphOption.substring(MODULE_GRAPH_FILE_PREFIX.length()));
                            } else {
                                userError("Unknown --graph-module option: '" + graphOption + "'");
                            }
                    }
                });

        return params;
    }

    private static void userError(String message) {
        System.err.println(message);
        System.exit(1);
    }

    private static void failIf(boolean fail, Supplier<String> message) {
        if (fail) {
            userError(message.get());
        }
    }
}
