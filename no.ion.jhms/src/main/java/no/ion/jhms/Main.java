package no.ion.jhms;

import java.lang.module.FindException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class Main {
    public static void main(String... args) {
        String modulePath = null;
        String hybridModuleName = null;
        String mainClass = null;

        int index = 0;
        for (; index < args.length; ++index) {
            String arg = args[index];
            switch (arg) {
                case "--module-path":
                case "-p":
                    ++index;
                    modulePath = args[index];
                    continue;
                case "--module":
                case "-m":
                    ++index;
                    arg = args[index];
                    int slashIndex = arg.indexOf('/');
                    if (slashIndex == -1) {
                        hybridModuleName = arg;
                        mainClass = null;
                    } else {
                        hybridModuleName = arg.substring(0, slashIndex);
                        mainClass = arg.substring(slashIndex + 1);
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
            rootModule.main(mainClass, mainArgs);
        } catch (IllegalArgumentException | FindException | InvalidHybridModuleException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void userError(String message) {
        System.out.println(message);
        System.exit(0);
    }
}
