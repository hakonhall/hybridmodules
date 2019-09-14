package no.ion.jhms;

import java.lang.module.FindException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
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
        } else {
            int atIndex = hybridModuleName.indexOf('@');
            if (atIndex == -1) {
                try {
                    BaseModule.validateModuleName(hybridModuleName);
                } catch (IllegalArgumentException e) {
                    userError(e.getMessage());
                }
            } else {
                try {
                    HybridModuleId.validateHybridModuleId(hybridModuleName);
                } catch (IllegalArgumentException e) {
                    userError(e.getMessage());
                }
            }
        }

        // Avoid closing container when returning from main(), since daemon threads may have been spawned.
        var container = new HybridModuleContainer();

        try {
            container.discoverHybridModulesFromModulePath(modulePath);
        } catch (FindException | InvalidHybridModuleException e) {
            userError(e.getMessage());
        }

        var params = new HybridModuleContainer.ResolveParams(hybridModuleName);
        RootHybridModule rootModule;
        try {
            rootModule = container.resolve(params);
        } catch (RuntimeException e) {
            userError(e.getMessage());
            return; // for compiler
        }

        if (moduleGraphParams == null) {
            try {
                rootModule.main(mainClass, mainArgs);
            } catch (IllegalAccessError | IllegalArgumentException | NoClassDefFoundError e) {
                userError(e.getMessage());
            } // pass through UndeclaredThrowableException...
        } else {
            for (String module : moduleGraphParams.modulesExcluded()) {
                boolean observable;
                try {
                    observable = container.isObservable(module);
                } catch (IllegalArgumentException e) {
                    userError(e.getMessage());
                    return; // for compiler
                }

                if (!observable) {
                    userError("The module " + module + " is not observable");
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
