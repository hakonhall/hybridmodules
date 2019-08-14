package no.ion.jhms;

import java.lang.module.ModuleDescriptor;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

class HybridModuleVersion implements Comparable<HybridModuleVersion> {
    /**
     * It is not possible to set an empty version with javac/jar. We therefore represent an absent version
     * as the empty string. This allows us to avoid special-casing null.
     */
    private final String versionString;
    private final Optional<ModuleDescriptor.Version> version;

    /** A module with a null version is one without an explicit set version. Aka absent. */
    static HybridModuleVersion fromNull() { return new HybridModuleVersion(); }

    static HybridModuleVersion fromRaw(Optional<String> rawVersion) {
        return rawVersion.isEmpty() ? new HybridModuleVersion() : new HybridModuleVersion(rawVersion.get());
    }
    static HybridModuleVersion from(String nullableVersion) {
        return nullableVersion == null ? new HybridModuleVersion() : new HybridModuleVersion(nullableVersion);
    }

    private HybridModuleVersion() { this("", Optional.empty()); }

    private HybridModuleVersion(String versionString) { this(versionString, descriptorVersionFromString(versionString)); }

    private HybridModuleVersion(String versionString, Optional<ModuleDescriptor.Version> version) {
        this.versionString = requireNonNull(versionString);
        this.version = requireNonNull(version);
    }

    @Override
    public int compareTo(HybridModuleVersion that) {
        // Any proper version is greater than an improper.
        if (this.version.isPresent() || that.version.isPresent()) {
            if (this.version.isPresent() && that.version.isPresent()) {
                return this.version.get().compareTo(that.version.get());
            }

            return this.version.isPresent() ? 1 : -1;
        }

        // Any version is greater than no version.
        if (this.versionString.length() > 0 || that.versionString.length() > 0) {
            if (this.versionString.length() > 0 && that.versionString.length() > 0) {
                return this.versionString.compareTo(that.versionString);
            }

            return this.versionString.length() > 0 ? 1 : -1;
        }

        return 0;
    }

    boolean isNull() { return versionString.isEmpty(); }

    @Override
    public String toString() { return versionString; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HybridModuleVersion that = (HybridModuleVersion) o;
        return versionString.equals(that.versionString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionString);
    }

    private static Optional<ModuleDescriptor.Version> descriptorVersionFromString(String versionString) {
        try {
            return Optional.of(ModuleDescriptor.Version.parse(versionString));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
