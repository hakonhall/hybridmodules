package no.ion.jhms;

import java.util.Objects;

/**
 * Uniquely identifies a hybrid module.
 */
// Immutable
class HybridModuleId implements Comparable<HybridModuleId> {
    private final String name;
    private final HybridModuleVersion version;

    public static HybridModuleId fromId(String id) {
        int atIndex = id.indexOf('@');
        if (atIndex == -1) {
            throw new IllegalArgumentException("Bad hybrid module ID: " + id);
        }

        return new HybridModuleId(id.substring(0, atIndex), id.substring(atIndex + 1));
    }

    HybridModuleId(String name, String version) {
        this(name, HybridModuleVersion.from(version));
    }

    HybridModuleId(String name, HybridModuleVersion version) {
        this.name = Objects.requireNonNull(name);
        this.version = Objects.requireNonNull(version);
    }

    String name() { return name; }
    HybridModuleVersion version() { return version; }

    @Override
    public int compareTo(HybridModuleId that) {
        int c = this.name.compareTo(that.name);
        if (c != 0) return c;

        return this.version.compareTo(that.version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HybridModuleId that = (HybridModuleId) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version);
    }

    /** Returns NAME if version is null (absent), and otherwise NAME@VERSION. */
    String toString2() {
        return version.isNull() ? name : name + '@' + version;
    }

    /** Returns NAME@VERSION (VERSION is absent if null). */
    @Override
    public String toString() {
        return name + "@" + version;
    }
}
