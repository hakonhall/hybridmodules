// JUnit 5 needs reflective access to package-private test classes and methods,
// which is typical of JUnit 5, so open the test module.
open module one {
    // Module requirements:
    exports one.exported;

    // Additional unit test requirements:
    requires org.junit.platform.console.standalone;
}
