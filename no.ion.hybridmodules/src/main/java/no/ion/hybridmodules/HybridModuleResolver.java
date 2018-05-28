package no.ion.hybridmodules;

import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReference;
import java.util.*;

class HybridModuleResolver {
    private HybridModuleFinder finder;
    private Set<HybridModuleId> ongoingResolutions = new HashSet<>();

    HybridModuleResolver(HybridModuleFinder finder) {
        this.finder = finder;
    }

    /**
     * @param moduleName The hybrid module name.
     * @param version    The required version, or null if any would do. Empty requires a hybrid module without version.
     * @return The hybrid module of the given name and version, fully resolved and ready to execute code.
     */
    HybridModule resolve(String moduleName, Optional<ModuleDescriptor.Version> version) {
        // this would have created cycles in the graph. instead, we don't support automatic h.m. just yet.
        return resolve(moduleName, version, new HashMap<>());
    }

    /**
     * Resolve the hybrid module with the given name and version.
     *
     * A HybridModule may be in the middle of its resolution when it needs to resolve the given moduleName/version
     * that it reads, and calls this method. It passes all the hybrid modules it has read so far (not including itself)
     * such that moduleName/version can reuse those hybrid modules for whatever hybrid modules it also needs to resolve
     * (if it has some transitive dependencies).
     *
     * @param moduleName
     * @param version
     * @param parentReadsById
     * @return
     */
    private HybridModule resolve(String moduleName,
                                 Optional<ModuleDescriptor.Version> version,
                                 Map<HybridModuleId, HybridModule> parentReadsById) {
        // version may be null, in case the version used is specified by whatever JAR is available.
        Jar jar = finder.getHybridModuleJar(moduleName, version);

        HybridModuleId id = jar.moduleId();
        if (!ongoingResolutions.add(id)) {
            throw new FindException("Cyclic dependency on hybrid module " + id + " detected");
        }

        ModuleDescriptor descriptor = jar.descriptor();
        if (descriptor.isAutomatic()) {
            throw new InvalidHybridModuleException("Automatic hybrid modules is not supported");
        }

        Map<HybridModuleId, HybridModule> readsById = new HashMap<>();

        Map<String, HybridModule> hybridModuleByPackage = new HashMap<>();
        Map<String, HybridModule> hybridModuleByExportedPackage = new HashMap<>();

        for (var requires : descriptor.requires()) {
            String subName = requires.name();
            Set<ModuleDescriptor.Requires.Modifier> modifiers = requires.modifiers();

            Optional<ModuleReference> systemModule = finder.findSystemModuleReference(subName);
            if (systemModule.isPresent()) {
                // todo: model package -> module descriptor
                // for now, assume requires on all modules.
                // addSystemModuleDependencies(subName, systemModule.get(), modifiers, moduleReads, moduleReadsByName);
                continue;
            } else {
                HybridModule subHybridModule;

                if (modifiers.contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE)) {
                    // Symbols exported by subName should be linked with the parent's reads:
                    //  - If the parent already has defined a hybrid module H, and subName is H or
                    //    H is a transitive dependency of subName, the same H must be used.
                    //  - If not, subName may also be a dependency elsewhere in the parent (just not
                    //    yet resolved), and in case it's important to add subName to parentReads.
                    HybridModuleId subId = new HybridModuleId(subName, requires.compiledVersion());
                    if (parentReadsById.containsKey(subId)) {
                        subHybridModule = parentReadsById.get(subId);
                    } else {
                        // We should send down parentReads, but we should also maintain our own reads
                        subHybridModule = resolve(subName, requires.compiledVersion(), parentReadsById);
                    }

                    addToHybridModuleByPackage(id, hybridModuleByExportedPackage, subHybridModule.getExportedPackages());
                } else {
                    // Pass along all those modules we have resolved so far
                    subHybridModule = resolve(subName, requires.compiledVersion(), readsById);
                }

                addToHybridModuleByPackage(id, hybridModuleByPackage, subHybridModule.getExportedPackages());
            }
        }

        ongoingResolutions.remove(id);

        return new HybridModule(jar, hybridModuleByPackage, hybridModuleByExportedPackage);
    }

    static void addToHybridModuleByPackage(HybridModuleId hybridModuleId,
                                           Map<String, HybridModule> hybridModuleByPackage,
                                           Map<String, HybridModule> toAdd) {
        for (var entry : toAdd.entrySet()) {
            addToHybridModuleByPackage(hybridModuleId, hybridModuleByPackage, entry.getKey(), entry.getValue());
        }
    }

    static void addToHybridModuleByPackage(HybridModuleId hybridModuleId,
                                           Map<String, HybridModule> hybridModuleByPackage,
                                           String packageEntry,
                                           HybridModule hybridModuleEntry) {
        HybridModule existingHybridModule = hybridModuleByPackage.putIfAbsent(packageEntry, hybridModuleEntry);
        if (existingHybridModule != null) {
            throw new InvalidHybridModuleException("Hybrid module " + hybridModuleId + " depends on both " +
                    existingHybridModule.getHybridModuleId() + " and " + hybridModuleEntry.getHybridModuleId() +
                    " but both of these export package " + packageEntry);
        }
    }
}
