package no.ion.hybridmodules;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Main {
    public static void main(String... args)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Path[] hybridModulePath = null;
        String hybridModuleName = null;
        String mainClass = null;
        boolean printGraph = false;

        int index = 0;
        for (; index < args.length; ++index) {
            String arg = args[index];
            switch (arg) {
                case "--hybrid-module-path":
                case "--module-path":
                case "-p":
                    ++index;
                    String[] pathStrings = args[index].split(":");
                    hybridModulePath = new Path[pathStrings.length];
                    for (int i = 0; i < pathStrings.length; ++i) {
                        hybridModulePath[i] = Paths.get(pathStrings[i]);
                    }
                    continue;
                case "--hybrid-module":
                case "--module":
                case "-m":
                    ++index;
                    arg = args[index];
                    int slashIndex = arg.indexOf('/');
                    if (slashIndex == -1) {
                        userError("Argument to --hybrid-module is of the form: MODULE/MAINCLASS");
                    } else {
                        hybridModuleName = arg.substring(0, slashIndex);
                        mainClass = arg.substring(slashIndex + 1);
                    }
                    continue;
                case "--graph":
                    printGraph = true;
                    continue;
                case "--":
                    ++index;
                    break;
            }

            // Use 'continue' to loop around.
            break;
        }

        String[] mainArgs = Arrays.copyOfRange(args, index, args.length);

        if (hybridModulePath == null) {
            userError("Missing --hybrid-module-path");
        }

        if (hybridModuleName == null || mainClass == null) {
            userError("Missing --hybrid-module");
        }

        var params = new HybridModuleContainer.ResolveParams(hybridModuleName, hybridModulePath);
        try (var container = HybridModuleContainer.resolve(params)) {
            if (printGraph) {
                printGraphOf(container);
            } else {
                Class<?> klass = container.loadClass(mainClass);
                Method main = klass.getMethod("main", String[].class);
                main.invoke(null, mainArgs);
            }
        }
    }

    private static void printGraphOf(HybridModuleContainer container) {
        System.out.print(container.getDependencyGraphDescription());
    }

    private static void userError(String message) {
        System.out.println(message);
        System.exit(0);
    }
}
