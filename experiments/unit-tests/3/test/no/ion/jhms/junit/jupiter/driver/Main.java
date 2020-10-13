package no.ion.jhms.junit.jupiter.driver;

import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    public static void main(String... args) {

        var config = LauncherConfig.builder();
        config.addTestEngines(new JupiterTestEngine());
        Launcher factory = LauncherFactory.create(config.build());

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder
                .request()
                //.selectors(DiscoverySelectors.selectClass(one.exported.PublicTest.class))
                .selectors(ofClasses("one.exported.PublicTest", "one.exported.PackagePrivateTest", "one.internal.PublicTest"))
                .build();

        // Jupiter use context class loader if set, or otherwise the app/system class loader.
        // Instead, it should use the hybrid module class loader.  Hope threads are not crossed...
        Thread.currentThread().setContextClassLoader(Main.class.getClassLoader());

        var listener = new SummaryGeneratingListener();
        factory.registerTestExecutionListeners(listener);
        factory.execute(request);

        TestExecutionSummary summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out));
        summary.getFailures().forEach(failure -> {
            System.out.println(failure.getTestIdentifier());
            failure.getException().printStackTrace();
        });
    }

    private static List<ClassSelector> ofClasses(String... classNames) {
        return Arrays.stream(classNames).map(Main::ofClass).collect(Collectors.toList());
    }

    private static ClassSelector ofClass(String className) {
        return DiscoverySelectors.selectClass(findClass(className));
    }

    private static Class<?> findClass(String binaryClassName) {
        ClassLoader classLoader = Main.class.getClassLoader();
        try {
            return classLoader.loadClass(binaryClassName);
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(binaryClassName);
        }
    }
}
