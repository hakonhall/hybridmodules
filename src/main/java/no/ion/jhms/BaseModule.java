package no.ion.jhms;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

abstract class BaseModule {
    private final String name;
    private final Set<String> packages;
    private final Map<String, Set<String>> exports;

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

    boolean packageVisibleToAll(String packageName) {
        Set<String> friends = exports.get(packageName);
        return friends != null && friends.isEmpty();
    }
}
