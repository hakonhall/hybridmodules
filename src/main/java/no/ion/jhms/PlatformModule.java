package no.ion.jhms;

import java.lang.module.ResolutionException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

class PlatformModule {
    private final String moduleName;
    private final Set<PlatformModule> platformModulesDependencies;
    private final Set<PlatformModule> transitivePlatformModules;

    private PlatformModule(String moduleName,
                           Set<PlatformModule> platformModulesDependencies,
                           Set<PlatformModule> transitivePlatformModules) {
        this.moduleName = moduleName;
        this.platformModulesDependencies = Collections.unmodifiableSet(platformModulesDependencies);
        this.transitivePlatformModules = Collections.unmodifiableSet(transitivePlatformModules);
    }

    String name() { return moduleName; }

    Set<PlatformModule> getAllTransitivePlatformDependencies() {
        return transitivePlatformModules;
    }

    @Override
    public boolean equals(Object other) {
        // See HybridModule::equals
        return other == this;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    static class Builder {
        private final String moduleName;
        private final Set<PlatformModule> directPlatformModulesDependencies = new HashSet<>();

        // All modules in requires, plus the transitive dependencies of those, and so on recursively
        private final Set<PlatformModule> platformModulesDependencies = new HashSet<>();

        // The transitive modules, plus the transitive dependencies of those, and so on recursively
        private final Set<PlatformModule> transitivePlatformModules = new HashSet<>();

        private final TreeMap<String, Set<String>> exports = new TreeMap<>();

        Builder(String moduleName) {
            this.moduleName = moduleName;
        }

        void addRequires(PlatformModule platformModule, boolean transitive, boolean isStatic) {
            // At run time, a static requires is ignored. Consistent with JPMS.
            if (isStatic) return;

            if (!directPlatformModulesDependencies.add(platformModule)) {
                // JLS 11, 7.7.1 Dependences
                throw new ResolutionException("Platform module " + moduleName + " requires " +
                        platformModule.name() + " twice");
            }

            if (platformModulesDependencies.add(platformModule)) {
                platformModulesDependencies.addAll(platformModule.getAllTransitivePlatformDependencies());
            } else {
                // transitive dependencies have presumably been added already
            }

            if (transitive) {
                if (transitivePlatformModules.add(platformModule)) {
                    transitivePlatformModules.addAll(platformModule.getAllTransitivePlatformDependencies());
                } else {
                    // transitive dependencies have presumably been added already
                }
            }
        }

        PlatformModule build() {
            return new PlatformModule(moduleName, platformModulesDependencies, transitivePlatformModules);
        }
    }
}
