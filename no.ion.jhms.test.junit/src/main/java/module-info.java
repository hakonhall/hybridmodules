module no.ion.jhms.test.junit {
    exports no.ion.jhms.test.junit;

    requires org.junit.jupiter.engine /* @5.9.1 */;
    requires org.junit.platform.launcher /* @1.9.1 */;
    requires org.junit.platform.engine /* @1.9.1 */;
    // Transitive test runner dependencies
    //requires org.junit.jupiter.api /* @5.9.1 */;
    //requires org.apiguardian.api /* @1.1.2 */;
    //requires org.junit.platform.commons /* @1.9.1 */;
    //requires org.opentest4j /* @1.2.0 */;
}
