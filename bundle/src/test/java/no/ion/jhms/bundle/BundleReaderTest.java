package no.ion.jhms.bundle;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class BundleReaderTest {
    @Test
    public void testCommonsCollections() throws IOException {
        ModuleInfo info = BundleReader.readModuleInfo(Path.of("src/test/resources/commons-collections4-4.1.jar"));

        assertEquals("4.1.0", info.version());

        assertEquals("module org.apache.commons.collections4 /* @4.1.0 */ {\n" +
                "  exports org.apache.commons.collections4;\n" +
                "  exports org.apache.commons.collections4.bag;\n" +
                "  exports org.apache.commons.collections4.bidimap;\n" +
                "  exports org.apache.commons.collections4.collection;\n" +
                "  exports org.apache.commons.collections4.comparators;\n" +
                "  exports org.apache.commons.collections4.functors;\n" +
                "  exports org.apache.commons.collections4.iterators;\n" +
                "  exports org.apache.commons.collections4.keyvalue;\n" +
                "  exports org.apache.commons.collections4.list;\n" +
                "  exports org.apache.commons.collections4.map;\n" +
                "  exports org.apache.commons.collections4.multimap;\n" +
                "  exports org.apache.commons.collections4.multiset;\n" +
                "  exports org.apache.commons.collections4.queue;\n" +
                "  exports org.apache.commons.collections4.sequence;\n" +
                "  exports org.apache.commons.collections4.set;\n" +
                "  exports org.apache.commons.collections4.splitmap;\n" +
                "  exports org.apache.commons.collections4.trie;\n" +
                "  exports org.apache.commons.collections4.trie.analyzer;\n" +
                "}\n",
                info.generateModuleInfoJavaContent(true));

        assertEquals("module org.apache.commons.collections4 {\n" +
                        "  exports org.apache.commons.collections4;\n" +
                        "  exports org.apache.commons.collections4.bag;\n" +
                        "  exports org.apache.commons.collections4.bidimap;\n" +
                        "  exports org.apache.commons.collections4.collection;\n" +
                        "  exports org.apache.commons.collections4.comparators;\n" +
                        "  exports org.apache.commons.collections4.functors;\n" +
                        "  exports org.apache.commons.collections4.iterators;\n" +
                        "  exports org.apache.commons.collections4.keyvalue;\n" +
                        "  exports org.apache.commons.collections4.list;\n" +
                        "  exports org.apache.commons.collections4.map;\n" +
                        "  exports org.apache.commons.collections4.multimap;\n" +
                        "  exports org.apache.commons.collections4.multiset;\n" +
                        "  exports org.apache.commons.collections4.queue;\n" +
                        "  exports org.apache.commons.collections4.sequence;\n" +
                        "  exports org.apache.commons.collections4.set;\n" +
                        "  exports org.apache.commons.collections4.splitmap;\n" +
                        "  exports org.apache.commons.collections4.trie;\n" +
                        "  exports org.apache.commons.collections4.trie.analyzer;\n" +
                        "}\n",
                info.generateModuleInfoJavaContent(false));
    }
}
