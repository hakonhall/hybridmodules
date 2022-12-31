package no.ion.moduleinfo;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleReference;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

class ModuleVersion implements Comparable<ModuleVersion> {
    private final String name;
    private final Optional<Version> version;

    static ModuleVersion from(ModuleReference reference) {
        return from(reference.descriptor());
    }

    static ModuleVersion from(ModuleDescriptor descriptor) {
        return new ModuleVersion(descriptor.name(), descriptor.version());
    }

    ModuleVersion(String name, Optional<Version> version) {
        this.name = requireNonNull(name, "name cannot be null");
        this.version = requireNonNull(version, "version cannot be null");
    }

    String name() { return name; }
    Optional<Version> version() { return version; }
    String versionedName() { return name + '@' + version.map(Version::toString).orElse(""); }

    /** Orders by (1) name first and foremost, then (2) empty versions before non-empty, then (3) the version. */
    @Override
    public int compareTo(ModuleVersion that) {
        int diff = this.name.compareTo(that.name);
        if (diff != 0) return diff;

        // Empty version orders before non-empty
        if (this.version.isEmpty() || that.version.isEmpty()) {
            return (this.version.isEmpty() ? -1 : 0) +
                   (that.version.isEmpty() ? +1 : 0);
        }

        return this.version.get().compareTo(that.version.get());
    }

    @Override
    public String toString() { return versionedName(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModuleVersion that = (ModuleVersion) o;
        return name.equals(that.name) && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version);
    }
}
