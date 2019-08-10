package no.ion.jhms;

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
}
