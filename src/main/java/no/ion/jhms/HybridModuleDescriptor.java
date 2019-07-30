package no.ion.jhms;

import java.lang.module.ModuleDescriptor;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

class HybridModuleDescriptor {
    private final HybridModuleId2 id;
    private final TreeMap<HybridModuleId2, HybridRequires> hybridRequiresMap;
    private final TreeMap<String, PlatformRequires> platformRequiresMap;
    private final TreeMap<String, Set<String>> modulesExportedToByPackage;
    private final Set<String> packages;

    static HybridModuleDescriptor read(ModuleDescriptor descriptor, PlatformModuleContainer platformModuleContainer) {
        String name = descriptor.name();

        ModuleDescriptor.Version version = descriptor.version()
                .orElseThrow(() -> new InvalidHybridModuleException("Hybrid module " + name + " is missing version"));
        HybridModuleId2 id = new HybridModuleId2(name, version);

        if (descriptor.isAutomatic()) {
            throw new InvalidHybridModuleException("Hybrid module " + id + " cannot be automatic");
        }

        if (descriptor.isOpen()) {
            throw new InvalidHybridModuleException("Hybrid module " + id + " is open");
        }

        if (descriptor.opens().size() > 0) {
            throw new InvalidHybridModuleException("Hybrid module " + id + " has 'opens' directives");
        }

        if (descriptor.provides().size() > 0) {
            throw new InvalidHybridModuleException("Hybrid module " + id + " has 'provides' directives");
        }

        if (descriptor.uses().size() > 0) {
            throw new InvalidHybridModuleException("Hybrid module " + id + " has 'uses' directives");
        }

        TreeMap<HybridModuleId2, HybridRequires> hybridRequiresMap = new TreeMap<>();
        TreeMap<String, PlatformRequires> platformRequiresMap = new TreeMap<>();
        for (ModuleDescriptor.Requires requires : descriptor.requires()) {
            // A module dependence is assumed to be a hybrid module iff it is not a platform module.
            Optional<PlatformModule> platformModule = platformModuleContainer.resolve(requires.name());

            if (platformModule.isPresent()) {
                platformRequiresMap.put(requires.name(), new PlatformRequires(platformModule.get(), requires.modifiers()));
            } else {
                Optional<ModuleDescriptor.Version> compiledVersion = requires.compiledVersion();
                if (compiledVersion.isEmpty()) {
                    throw new InvalidHybridModuleException("Hybrid module " + id + " requires " + requires.name() +
                            ", but it has no compile version");
                }
                HybridModuleId2 requiresId = new HybridModuleId2(requires.name(), compiledVersion.get());

                var hybridRequires = new HybridRequires(requiresId, requires.modifiers());
                hybridRequiresMap.put(hybridRequires.id(), hybridRequires);
            }
        }

        TreeMap<String, Set<String>> exportsMap = new TreeMap<>();
        for (ModuleDescriptor.Exports exports : descriptor.exports()) {
            // exports is qualified iff targets set is non-empty
            exportsMap.put(exports.source(), exports.targets());
        }

        return new HybridModuleDescriptor(id, hybridRequiresMap, platformRequiresMap, exportsMap, descriptor.packages());
    }

    static class HybridRequires extends BaseRequires {
        private final HybridModuleId2 id;

        HybridRequires(HybridModuleId2 id, Set<ModuleDescriptor.Requires.Modifier> modifiers) {
            super(modifiers);
            this.id = id;
        }

        HybridModuleId2 id() { return id; }
    }

    /** Unlike requires for hybrid modules, the requires for platform modules are backed by a module descriptor. */
    static class PlatformRequires extends BaseRequires {
        private final PlatformModule platformModule;

        PlatformRequires(PlatformModule platformModule, Set<ModuleDescriptor.Requires.Modifier> modifiers) {
            super(modifiers);
            this.platformModule = platformModule;
        }

        public PlatformModule getPlatformModule() {
            return platformModule;
        }
    }

    static class BaseRequires {
        private final Set<ModuleDescriptor.Requires.Modifier> modifiers;

        protected BaseRequires(Set<ModuleDescriptor.Requires.Modifier> modifiers) {
            this.modifiers = modifiers;
        }

        boolean isTransitive() { return modifiers.contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE); }
        boolean isStatic() { return modifiers.contains(ModuleDescriptor.Requires.Modifier.STATIC); }
    }

    private HybridModuleDescriptor(HybridModuleId2 id,
                                   TreeMap<HybridModuleId2, HybridRequires> hybridRequiresMap,
                                   TreeMap<String, PlatformRequires> platformRequiresMap,
                                   TreeMap<String, Set<String>> exportsMap,
                                   Set<String> packages) {
        this.id = id;
        this.hybridRequiresMap = hybridRequiresMap;
        this.platformRequiresMap = platformRequiresMap;
        this.modulesExportedToByPackage = exportsMap;
        this.packages = packages;
    }

    HybridModuleId2 id() { return id; }
    Collection<HybridRequires> hybridRequires() { return hybridRequiresMap.values(); }
    Collection<PlatformRequires> platformRequires() { return platformRequiresMap.values(); }
    TreeMap<String, Set<String>> exports() { return modulesExportedToByPackage; }
    Set<String> packages() { return packages; }
}
