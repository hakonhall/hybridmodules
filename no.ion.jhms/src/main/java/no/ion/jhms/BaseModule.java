package no.ion.jhms;

import java.lang.module.ModuleDescriptor;
import java.util.*;
import java.util.stream.Collectors;

abstract class BaseModule {
    private final String name;
    private final Set<String> packages;
    private final Map<String, Set<String>> exports;

    private volatile List<String> unexportedPackages = null;

    /** Verify 'name' is a valid JPMS module name, or otherwise throw an {@link IllegalArgumentException}. */
    static void validateModuleName(String name) {
        // It would be preferable to call jdk.internal.module.Checks.requireModuleName(), but that's not exported.
        // return Checks.requireModuleName(name);

        // Instead, ModuleDescriptor.newModule(String) invokes it, and does very little else.
        // requireModuleName() throws an "NAME: Invalid module name: 'ID' is not a Java identifier"
        // IllegalArgumentException, which suits us well. ID is the dot-separated component which is
        // invalid. E.g. "byte.foo" would throw an exception because ID = "byte" is not a valid
        // identifier.
        ModuleDescriptor.newModule(name);
    }

    BaseModule(String name, Set<String> packages, Map<String, Set<String>> exports) {
        this.name = name;
        this.packages = packages;
        this.exports = exports;
    }

    Set<String> packagesVisibleTo(BaseModule module) {
        // All packages in this module are visible to this module.
        if (module.name.equals(name)) {
            return packages;
        }

        return exports.entrySet().stream()
                .filter(entry -> {
                    Set<String> friends = entry.getValue();
                    return friends.isEmpty() || friends.contains(module.name);
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /** The set of packages explicitly exported to the given module (using 'exports ... to'). */
    List<String> qualifiedExportsTo(BaseModule module) {
        return exports.entrySet().stream()
                .filter(entry -> entry.getValue().contains(module.name))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    boolean packageVisibleToAll(String packageName) {
        Set<String> friends = exports.get(packageName);
        return friends != null && friends.isEmpty();
    }

    List<String> unexportedPackages() {
        List<String> unexportedPackages = this.unexportedPackages;
        if (unexportedPackages == null) {
            Set<String> unexportedPackageSet = new TreeSet<>(packages);
            unexportedPackageSet.removeAll(exports.keySet());
            unexportedPackages = new ArrayList<>(unexportedPackageSet);
        }
        return unexportedPackages;
    }

    List<String> unqualifiedExports() {
        return exports.entrySet().stream().filter(entry -> entry.getValue().isEmpty()).map(Map.Entry::getKey).collect(Collectors.toList());
    }
}
