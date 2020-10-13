// JUnit 5 needs reflective access to package-private test classes and methods,
// which is typical of JUnit 5, so open the test module.
open module one {
    // Module requirements:
    exports one.exported;

    // even internal packages must be exported in the standard unit tests.
    exports one.internal;

    // Additional unit test requirements:
    requires org.junit.jupiter.api;
    // Transitive requires:
    // requires transitive org.apiguardian.api;
    // requires transitive org.junit.platform.commons;
    // requires transitive org.opentest4j;

    // Additional unit test driver requirements:
    requires org.junit.platform.launcher;
    // Transitive requires:
    // requires transitive java.logging;
    // requires transitive org.apiguardian.api;
    // requires transitive org.junit.platform.commons;
    // requires transitive org.junit.platform.engine;

    // Pulls in JupiterTestEngine
    requires org.junit.jupiter.engine;
}
