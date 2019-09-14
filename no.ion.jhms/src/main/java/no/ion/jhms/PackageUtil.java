package no.ion.jhms;

import java.util.Optional;
import java.util.Set;

class PackageUtil {
    private PackageUtil() {}

    /**
     *
     * @param className must be of the form [package.]simpleName
     * @return Package name, or "" if not found (top-level package)
     */
    static String getPackageName(String className) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        } else {
            return className.substring(0, lastDot);
        }
    }

    /**
     * 'name' is "/" separated. The last component (and any "/" with it) is stripped, and the prefix is returned,
     * with "/" replaced by ".".
     *
     * <p>This is almost the same as extracting the package name of 'name', except that the components of the package
     * name may not be a valid identifier. And the returned String may be a valid package name, even though 'name'
     * may not have a valid package name. For instance if 'name' is "a.b/c", there is no valid package name since
     * "a.b" is not a Java identifier, but this method returns "a.b" which is a valid package name.</p>
     *
     * <p>Example: "META-INF/MANIFEST.MF" would return "META-INF".</p>
     */
    static String getPackageNameishFromResourceName(String name) {
        int lastSlashIndex = name.lastIndexOf('/');
        return lastSlashIndex == -1 ? "" : name.substring(0, lastSlashIndex).replace('/', '.');
    }

    /**
     * Returns the package name of an absolute name of a resource (as defined in {@link Class#getResource(String)}).
     *
     * @return the ("." separated) package name of {@code absoluteName}, the empty string for the unnamed package,
     *         or empty if there is no valid package name.
     */
    static Optional<String> getPackageNameFromAbsoluteNameOfResource(String absoluteName) {
        // The empty string denotes the root directory and has the unnamed package as its package
        if (absoluteName.isEmpty()) {
            return Optional.of("");
        }

        StringBuilder stringBuilder = new StringBuilder(absoluteName.length());
        int startIndex = 0;

        while (true) {
            int endIndex = absoluteName.indexOf('/', startIndex);
            if (endIndex == -1) {
                // [endIndex + 1, name.length()) does not have to be a Java identifier.
                // This also works if absoluteName had no "/" => toString() is "" <=> unnamed package.
                return Optional.of(stringBuilder.toString());
            }

            String component = absoluteName.substring(startIndex, endIndex);
            if (!isJavaIdentifier(component)) {
                return Optional.empty();
            }

            if (stringBuilder.length() > 0) {
                stringBuilder.append('.');
            }
            stringBuilder.append(component);

            startIndex = endIndex + 1;
        }
    }

    private static final Set<String> RESERVED_WORDS = Set.of(
            "_",
            "abstract",
            "assert",
            "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "false",
            "final",
            "finally",
            "float",
            "for",
            "goto",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "null",
            "package",
            "private",
            "protected",
            "public",
            "return",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "true",
            "try",
            "void",
            "volatile",
            "while");

    static boolean isJavaIdentifier(String id) {
        if (id.isEmpty()) return false;
        if (RESERVED_WORDS.contains(id)) return false;

        int c = Character.codePointAt(id, 0);
        if (!Character.isJavaIdentifierStart(c)) return false;

        for (int i = Character.charCount(c); i < id.length(); i += Character.charCount(c)) {
            c = Character.codePointAt(id, i);
            if (!Character.isJavaIdentifierPart(c)) return false;
        }

        return true;
    }
}
