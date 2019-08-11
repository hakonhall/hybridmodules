package no.ion.jhms.bundle;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

class ModuleInfo {
    private final String name;
    private final String version;
    private final List<String> exports;

    ModuleInfo(String name, String version, List<String> exports) {
        this.name = name;
        this.version = version;
        this.exports = exports;
    }

    String version() { return version; }

    String generateModuleInfoJavaContent(boolean includeVersion) {
        StringBuilder content = new StringBuilder();

        content.append("module " + name + (includeVersion ? " /* @" + version + " */ {\n" : " {\n"));
        exports.forEach(exportedPackage -> content.append("  exports " + exportedPackage + ";\n"));
        content.append("}\n");

        return content.toString();
    }

    static class Builder {
        private final String name;
        private final String version;
        private final List<String> exports = new ArrayList<>();

        /** Pass null if the module has no version (absent). */
        Builder(String moduleName, String nullableVersion) {
            this.name = requireNonNull(moduleName, "The module name cannot be null");

            // Note: empty string is not a valid version with --module-version
            this.version = nullableVersion == null ? "" : nullableVersion;
        }

        void addExports(String packageName) {
            exports.add(packageName);
        }

        ModuleInfo build() {
            return new ModuleInfo(name, version, exports);
        }
    }
}
