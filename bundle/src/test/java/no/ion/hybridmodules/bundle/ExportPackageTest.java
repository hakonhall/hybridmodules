package no.ion.hybridmodules.bundle;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class ExportPackageTest {
    @Test
    public void basics() {
        String line = "org.apache.commons.collections4;version=\"4.1\",org.apache.commons.collections4.bag;version=\"4.1\"";
        List<ExportPackage> exportPackages = ExportPackage.fromLine(line);
        assertEquals(
                Arrays.asList("org.apache.commons.collections4", "org.apache.commons.collections4.bag"),
                exportPackages.stream().map(ExportPackage::getClassName).collect(Collectors.toList()));
    }

}