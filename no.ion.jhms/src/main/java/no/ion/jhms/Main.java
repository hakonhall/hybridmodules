package no.ion.jhms;

import java.lang.module.FindException;
import java.util.Arrays;
import java.util.function.Supplier;

public class Main {
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
                    continue;
                case "--":
                    ++index;
                    break;
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
                ModuleGraph moduleGraph = container.getModuleGraph(moduleGraphParams);
                GraphvizDigraph graph = GraphvizDigraph.fromModuleGraph(moduleGraph);
                System.out.println(graph.toDot());
            }
        } catch (IllegalArgumentException | FindException | InvalidHybridModuleException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static ModuleGraph.Params parseModuleGraphOptionValue(String optionValue) {
        var params = new ModuleGraph.Params();

        if (!optionValue.isEmpty() && optionValue.startsWith("-")) {
            userError("Bad option value of --graph-module");
        }

        params.includeExports(true);
        params.includeSelf(true);

        return params;
    }

    private static void userError(String message) {
        System.out.println(message);
        System.exit(0);
    }

    private static void failIf(boolean fail, Supplier<String> message) {
        if (fail) {
            userError(message.get());
        }
    }
}
