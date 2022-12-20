package no.ion.jhms;

import java.lang.module.FindException;
import java.util.Arrays;

import static no.ion.jhms.ProgramUtil.failIf;
import static no.ion.jhms.ProgramUtil.runContainer;
import static no.ion.jhms.ProgramUtil.userError;
import static no.ion.jhms.ProgramUtil.validateRootHybridModule;

/**
 * Entrypoint for executing a "fat JAR".  A fat JAR is intended to be run on the class path, containing
 * this module's content, and is supposed to start a JHMS container with a root hybrid module given by $1,
 * and main class $2, and with a module path of all the *.jar files in the fat JAR's META-INF/mod/ directory.
 */
public class FatMain {
    public static void main(String... args) {
        new FatMain().run(args);
    }

    private void usage() {
        System.out.print("Usage: java -cp FATJAR no.ion.jhms.FatMain MODULE MAINCLASS\n" +
                         "Run the JHMS application with hybrid module MODULE and main-class MAINCLASS\n" +
                         "\n" +
                         "The module path is the META-INF/mod/ directory in FATJAR. FATJAR is an extension\n" +
                         "of the no.ion.jhms JAR.  MODULE is either the name of a hybrid module, or an ID\n" +
                         "of the form NAME@VERSION.\n");
        System.exit(0);
    }

    private void run(String... args) {
        int argi = 0;
        for (; argi < args.length; ++argi) {
            String arg = args[argi];
            switch (arg) {
                case "-h":
                case "--help":
                    usage();
                    continue;  // Never reached.
                default:
                    if (arg.startsWith("-")) {
                        userError("Unknown option: " + arg);
                    }
                    // fall-through & break
            }

            break;
        }

        failIf(argi >= args.length, "Missing MODULE, see '--help'");
        String rootHybridModule = args[argi++];
        HybridModuleContainer.ResolveParams params = validateRootHybridModule(rootHybridModule);

        failIf(argi >= args.length, "Missing MAINCLASS, see '--help'");
        String mainClass = args[argi++];

        String[] downstreamArgs = Arrays.copyOfRange(args, argi, args.length);


        // Avoid closing container when returning from main(), since daemon threads may have been spawned.
        var container = new HybridModuleContainer();

        try {
            container.discoverEmbeddedHybridModules();
        } catch (FindException | InvalidHybridModuleException e) {
            userError(e.getMessage());
        }

        RootHybridModule rootModule;
        try {
            rootModule = container.resolve(params);
        } catch (RuntimeException e) {
            userError(e.getMessage());
            return; // for compiler
        }

        runContainer(null, container, rootModule, mainClass, downstreamArgs);
    }
}
