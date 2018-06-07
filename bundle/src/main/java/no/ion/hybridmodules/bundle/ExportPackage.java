package no.ion.hybridmodules.bundle;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ExportPackage {
    private final String className;

    public static List<ExportPackage> fromLine(String line) {
        return Arrays.stream(line.split(",", -1))
                .map(ExportPackage::fromSpec)
                .collect(Collectors.toList());
    }

    public String getClassName() {
        return className;
    }

    private ExportPackage(String className) {
        this.className = className;
    }

    private static ExportPackage fromSpec(String spec) {
        String[] elements = spec.split(";", -1);
        if (elements.length == 0) {
            throw new IllegalArgumentException("Bad element: '" + spec + "'");
        }

        return new ExportPackage(elements[0]);
    }
}
