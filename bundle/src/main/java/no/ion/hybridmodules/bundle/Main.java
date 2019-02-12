package no.ion.hybridmodules.bundle;

import java.io.IOException;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class Main {
    private final StringBuilder info = new StringBuilder();

    public static void main(String... args) throws IOException {
        System.out.print(new Main().moduleInfo(args));
    }

    String moduleInfo(String... args) throws IOException {
        for (String arg : args) {
            String symbolicName;
            String version;
            List<ExportPackage> exportPackages;

            try (JarFile jarFile = new JarFile(arg)) {
                Manifest manifest = jarFile.getManifest();
                Attributes mainAttributes = manifest.getMainAttributes();
                symbolicName = mainAttributes.getValue("Bundle-SymbolicName");
                version = mainAttributes.getValue("Bundle-Version");
                String exportPackageLine = mainAttributes.getValue("Export-Package");
                exportPackages = ExportPackage.fromLine(exportPackageLine);
            }

            println("open module " + symbolicName + '@' + version + " {");
            for (var exportPackage : exportPackages) {
                println("  exports " + exportPackage.getClassName() + ";");
            }
            println("}");
        }

        return info.toString();
    }

    private void println(String line) {
        info.append(line).append('\n');
    }
}
