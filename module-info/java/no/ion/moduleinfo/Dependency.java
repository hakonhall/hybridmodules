package no.ion.moduleinfo;

import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.List;

/**
 * A dependency of a module M.
 */
class Dependency {
    private final ModuleVersion dependee;
    private final ModuleVersion directDependee;
    private final ModuleVersion module;
    private final List<Dependency> chain;
    private final List<Dependency> fullChain;
    private final boolean isDirect;
    private final boolean isTransitive;
    private final boolean isStatic;

    /**
     * Constructs a dependency object.
     *  @param dependee     the module this requires.name() is a dependency of.
     * @param chain         the chain of dependencies, from top-level down to direct dependee of 'requires'.
     * @param descriptor   the descriptor with the requires.
     * @param requires     the 'requires' of the dependency.
     * @param isDirect     whether descriptor belongs to dependee.
     * @param isTransitive whether a requires on 'dependee' implies a dependency on requires.name().
     * @param isStatic     whether the dependency is optional at runtime.
     */
    static Dependency of(ModuleVersion dependee,
                         List<Dependency> chain,
                         ModuleDescriptor descriptor,
                         ModuleDescriptor.Requires requires,
                         boolean isDirect,
                         boolean isTransitive,
                         boolean isStatic) {
        return new Dependency(dependee,
                              new ModuleVersion(descriptor.name(), descriptor.version()),
                              new ModuleVersion(requires.name(), requires.compiledVersion()),
                              chain,
                              isDirect,
                              isTransitive,
                              isStatic);
    }

    private Dependency(ModuleVersion dependee, ModuleVersion directDependee, ModuleVersion module, List<Dependency> chain,
                       boolean isDirect, boolean isTransitive, boolean isStatic) {
        this.dependee = dependee;
        this.directDependee = directDependee;
        this.module = module;
        this.chain = List.copyOf(chain);
        this.isDirect = isDirect;
        this.isTransitive = isTransitive;
        this.isStatic = isStatic;

        List<Dependency> fullChain = new ArrayList<>(chain);
        fullChain.add(this);
        this.fullChain = List.copyOf(fullChain);
    }

    /** The module this dependency is a dependency of. */
    ModuleVersion dependee() { return dependee; }

    /** The module that directly depends on this dependency. */
    ModuleVersion directDependee() { return directDependee; }

    /** The module of this dependency. */
    ModuleVersion module() { return module; }

    /** Returns the list of dependencies leading to this dependency, starting with the top-most {@link #dependee()} requires. */
    List<Dependency> chain() { return chain; }

    List<Dependency> fullChain() { return fullChain; }

    /** If the module declaration of {@link #dependee()} has a requires on {@link #module()}. */
    boolean isDirect() { return isDirect; }

    /** If true, then any module which requires {@link #dependee()} has an implied dependency on {@link #module()}. */
    boolean isTransitive() { return isTransitive; }

    /** If true, the dependency is optional at run time. */
    boolean isStatic() { return isStatic; }

    @Override
    public String toString() {
        return "Dependency{" +
               "dependee=" + dependee +
               ", directDependee=" + directDependee +
               ", module=" + module +
               ", isDirect=" + isDirect +
               ", isTransitive=" + isTransitive +
               ", isStatic=" + isStatic +
               '}';
    }
}
