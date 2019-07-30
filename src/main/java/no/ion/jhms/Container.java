package no.ion.jhms;

import java.lang.module.FindException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

class Container {
    private final PlatformModuleContainer platformModuleContainer = new PlatformModuleContainer();
    private final HybridModularJarLoader jarLoader = new HybridModularJarLoader(platformModuleContainer);
    private final TreeMap<HybridModuleId2, HybridModule2> hybridModules = new TreeMap<>();
    private final TreeMap<String, PlatformModule> platformModules = new TreeMap<>();

    /** Keeps track of ongoing resolutions. */
    private final Set<HybridModuleId2> resolving = new HashSet<>();

    /** Keeps track of ongoing resolutions. */
    private final Set<String> resolvingPlatformModuleNames = new HashSet<>();

    Container() { }

    HybridModule2 resolve(HybridModuleId2 rootId) {
        return hybridModules.computeIfAbsent(rootId, this::resolveNewAcyclic);
    }

    private HybridModule2 resolveNewAcyclic(HybridModuleId2 id) {
        if (resolving.contains(id)) {
            throw new InvalidHybridModuleException("Circular dependency detected for hybrid module " + id +
                    ", the set of hybrid modules under resolution are: " + resolving);
        }
        resolving.add(id);

        try {
            return resolveNew(id);
        } finally {
            resolving.remove(id);
        }
    }

    private HybridModule2 resolveNew(HybridModuleId2 id) {
        Optional<HybridModularJar> jar = jarLoader.getHybridModularJar(id);
        if (jar.isEmpty()) {
            throw new FindException("There is no hybrid module " + id + " in " + jarLoader.getEffectiveModulePath());
        }

        var builder = new HybridModule2.Builder(id, jar.get());

        HybridModuleDescriptor descriptor = jar.get().descriptor();

        for (HybridModuleDescriptor.HybridRequires hybridRequires : descriptor.hybridRequires()) {
            // Ignoring static is consistent with JPMS.
            if (hybridRequires.isStatic()) continue;
            builder.addHybridModuleRequires(resolve(hybridRequires.id()), hybridRequires.isTransitive());
        }

        for (HybridModuleDescriptor.PlatformRequires platformRequires : descriptor.platformRequires()) {
            // Ignoring static is consistent with JPMS.
            if (platformRequires.isStatic()) continue;
            builder.addPlatformModuleRequires(platformRequires.getPlatformModule(), platformRequires.isTransitive());
        }

        for (var entry : descriptor.exports().entrySet()) {
            builder.addExports(entry.getKey(), entry.getValue());
        }

        builder.addPackages(descriptor.packages());

        return builder.build();
    }

}
