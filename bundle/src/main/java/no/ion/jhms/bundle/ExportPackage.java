package no.ion.jhms.bundle;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents an Export-Package OSGi bundle manifest entry.
 *
 * <p>The value of an Exports-Package could be e.g.
 * <pre>
 *   {@code Export-Package: no.ion.jhms.bundle; uses:="javax.servlet"; version="1.2.3"}
 * </pre>
 *
 * The {@code uses} would with JHMS/JPMS become a {@code requires transitive} on the module the
 * {@code no.ion.jhms.bundle} package is exported from.
 */
public class ExportPackage {
    private final String packageName;

    static List<ExportPackage> fromLine(String line) {
        return Arrays.stream(line.split(",", -1))
                .map(ExportPackage::fromSpec)
                .collect(Collectors.toList());
    }

    String getPackageName() {
        return packageName;
    }

    private ExportPackage(String packageName) {
        this.packageName = packageName;
    }

    private static ExportPackage fromSpec(String spec) {
        String[] elements = spec.split(";", -1);
        if (elements.length == 0) {
            throw new IllegalArgumentException("Bad element: '" + spec + "'");
        }

        return new ExportPackage(elements[0]);
    }
}
