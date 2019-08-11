package no.ion.jhms.bundle;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static java.util.Objects.requireNonNull;

public class BundleReader {
    public static ModuleInfo readModuleInfo(Path bundlePath) throws IOException {
        try (JarFile jarFile = new JarFile(bundlePath.toFile())) {
            Manifest manifest = jarFile.getManifest();
            Attributes mainAttributes = manifest.getMainAttributes();

            String symbolicName = requireNonNull(mainAttributes.getValue("Bundle-SymbolicName"),
                    "No Bundle-SymbolicName found in manifest of " + bundlePath);

            String versionOrNull = mainAttributes.getValue("Bundle-Version");

            ModuleInfo.Builder builder = new ModuleInfo.Builder(symbolicName, versionOrNull);

            String exportPackageLine = requireNonNull(mainAttributes.getValue("Export-Package"),
                    "No Export-Package found in manifest of " + bundlePath);

            List<ExportPackage> exportPackages = ExportPackage.fromLine(exportPackageLine);
            for (var exportPackage : exportPackages) {
                builder.addExports(exportPackage.getPackageName());
            }

            return builder.build();
        }
    }
}
