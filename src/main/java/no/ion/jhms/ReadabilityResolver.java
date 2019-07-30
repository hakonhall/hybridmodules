// Copyright 2019 Oath Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package no.ion.jhms;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author hakonhall
 */
public class ReadabilityResolver {
    private final Map<String, HybridModule2> readableHybridModulesByName = new TreeMap<>();
    private final Map<String, PlatformModule> readablePlatformModulesByName = new TreeMap<>();
    private final HybridModule2 root;

    public ReadabilityResolver(HybridModule2 root) {
        this.root = root;
    }

    void resolve() {
        readableHybridModulesByName.put(root.id().name(), root);
        root.getDirectHybridModuleDependencies().forEach(this::addHybridModuleDependency);
        root.getDirectPlatformModulesDependencies().forEach(this::addPlatformModuleDependency);
    }

    private void addHybridModuleDependency(HybridModule2 hybridModuleDependency) {
        String name = hybridModuleDependency.id().name();
        var existingHybridModule = readableHybridModulesByName.get(name);
        if (existingHybridModule != null) {
            if (hybridModuleDependency.id().version().equals(existingHybridModule.id().version())) {
                return; // already added
            } else {
                throw new InvalidHybridModuleException("Hybrid module " + root.id() + " reads " + name +
                        " both at version " + existingHybridModule.id().version() + " and " +
                        hybridModuleDependency.id().version());
            }
        }

        readableHybridModulesByName.put(name, hybridModuleDependency);

        hybridModuleDependency.getDirectTransitiveHybridModules().forEach(this::addHybridModuleDependency);
        hybridModuleDependency.getDirectTransitivePlatformModules().forEach(this::addPlatformModuleDependency);
    }

    private void addPlatformModuleDependency(PlatformModule platformModuleDependency) {
        String name = platformModuleDependency.name();
        PlatformModule existingPlatformModule = readablePlatformModulesByName.get(name);
        if (existingPlatformModule != null) return;
        readablePlatformModulesByName.put(name, platformModuleDependency);
        platformModuleDependency.getAllTransitivePlatformDependencies().forEach(this::addPlatformModuleDependency);
    }
}
