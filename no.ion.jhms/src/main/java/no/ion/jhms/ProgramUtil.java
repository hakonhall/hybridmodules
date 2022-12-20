package no.ion.jhms;

import java.util.Objects;
import java.util.function.Supplier;

public class ProgramUtil {
    static void userError(String message) {
        System.err.println(message);
        System.exit(1);
    }

    static void failIf(boolean fail, Supplier<String> message) {
        if (fail)
            userError(message.get());
    }

    static void failIf(boolean fail, String message) {
        if (fail)
            userError(message);
    }

    /** Prerequisite: rootHybridModule cannot be null. */
    static HybridModuleContainer.ResolveParams validateRootHybridModule(String rootHybridModule) {
        Objects.requireNonNull(rootHybridModule, "rootHybridModule cannot be null");

        int atIndex = rootHybridModule.indexOf('@');
        if (atIndex == -1) {
            try {
                BaseModule.validateModuleName(rootHybridModule);
            } catch (IllegalArgumentException e) {
                userError(e.getMessage());
            }
        } else {
            try {
                HybridModuleId.validateHybridModuleId(rootHybridModule);
            } catch (IllegalArgumentException e) {
                userError(e.getMessage());
            }
        }

        return new HybridModuleContainer.ResolveParams(rootHybridModule);
    }

    static void runContainer(String contextId, HybridModuleContainer container, RootHybridModule rootModule, String mainClass, String... mainArgs) {
        ClassLoader savedClassLoader = null;
        try {
            // JHMS §2.8 2.b.
            if (contextId == null) {
                Thread thread = Thread.currentThread();
                savedClassLoader = thread.getContextClassLoader();
                thread.setContextClassLoader(rootModule.getClassLoader());
            } else if (!contextId.isEmpty()) {
                var contextParams = new HybridModuleContainer.ResolveParams(contextId);
                HybridModuleClassLoader classLoader = container.resolve(contextParams).getClassLoader();
                Thread thread = Thread.currentThread();
                savedClassLoader = thread.getContextClassLoader();
                thread.setContextClassLoader(classLoader);
            }

            try {
                rootModule.mainIn(mainClass, mainArgs);
            } catch (IllegalAccessError | IllegalArgumentException | NoClassDefFoundError e) {
                userError(e.getMessage());
            } // pass through UndeclaredThrowableException...
        } finally {
            if (savedClassLoader != null)
                Thread.currentThread().setContextClassLoader(savedClassLoader);
        }
    }
}
