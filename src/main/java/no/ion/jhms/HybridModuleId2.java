package no.ion.jhms;

import java.lang.module.ModuleDescriptor.Version;
import java.util.Objects;

/**
 * Uniquely identifies a hybrid module.
 */
// Immutable
class HybridModuleId2 implements Comparable<HybridModuleId2> {
    private final String name;
    private final Version version;

    HybridModuleId2(String name, String version) { this(name, Version.parse(version)); }
    HybridModuleId2(String name, Version version) {
        this.name = Objects.requireNonNull(name);
        this.version = Objects.requireNonNull(version);
    }

    String name() {
        return name;
    }

    Version version() {
        return version;
    }

    @Override
    public int compareTo(HybridModuleId2 that) {
        int diff = this.name.compareTo(that.name);
        if (diff != 0) return diff;
        return this.version.compareTo(that.version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HybridModuleId2 that = (HybridModuleId2) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version);
    }

    @Override
    public String toString() {
        return name + "@" + version;
    }
}
