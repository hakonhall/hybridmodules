package no.ion.jhms;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class HybridModule {
    private final HybridModuleId id;
    private final Jar jar;
    private final HybridModuleClassLoader classLoader;
    private final HashMap<String, HybridModule> hybridModulesByPackage = new HashMap<>();
    private final HashMap<String, HybridModule> hybridModulesByExportedPackage = new HashMap<>();

    HybridModule(Jar jar,
                 Map<String, HybridModule> hybridModulesByPackage,
                 Map<String, HybridModule> hybridModulesByExportedPackage) {
        this.id = jar.moduleId();
        this.jar = jar;

        this.hybridModulesByExportedPackage.putAll(hybridModulesByExportedPackage);
        for (var exports : jar.descriptor().exports()) {
            if (exports.isQualified()) {
                throw new InvalidHybridModuleException("Qualified exports are not yet supported");
            }

            HybridModuleResolver.addToHybridModuleByPackage(id, this.hybridModulesByExportedPackage, exports.source(), this);
        }

        this.hybridModulesByPackage.putAll(hybridModulesByPackage);
        for (var packageName : jar.descriptor().packages()) {
            HybridModuleResolver.addToHybridModuleByPackage(id, this.hybridModulesByPackage, packageName, this);
        }

        this.classLoader = new HybridModuleClassLoader(jar, this, this.hybridModulesByPackage);
    }

    Optional<String> getMainClass() {
        return jar.mainClass();
    }

    HybridModuleId getHybridModuleId() {
        return id;
    }

    HybridModuleClassLoader getClassLoader() {
        return classLoader;
    }

    /** Maps each exported package to its hybrid module. */
    Map<String, HybridModule> getExportedPackages() {
        return hybridModulesByExportedPackage;
    }

    /** Maps each package in each hybrid module H this hybrid module reads to the hybrid module H. */
    Map<String, HybridModule> getReads() {
        return hybridModulesByPackage;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
