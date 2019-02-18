package no.ion.hybridmodules;

import java.lang.module.ModuleDescriptor.Version;
import java.util.Objects;
import java.util.Optional;

class HybridModuleId {
    private final String name;
    private final Version version;

    HybridModuleId(String name, Version version) {
        this.name = name;
        this.version = version;
    }

    HybridModuleId(String name, Optional<Version> version) {
        this(name, version.orElse(null));
    }

    HybridModuleId(String name, String version) {
        this(name, versionFromString(version));
        validateName();
    }

    HybridModuleId(String id) {
        int atIndex = id.indexOf('@');
        if (atIndex == -1) {
            throw new IllegalArgumentException("No @ found in id: '" + id + "'");
        }

        this.name = id.substring(0, atIndex);
        this.version = versionFromString(id.substring(atIndex + 1));
        validateName();
    }

    String name() {
        return name;
    }

    Optional<Version> version() {
        return Optional.ofNullable(version);
    }

    @Override
    public String toString() {
        // This matches getNameAndVersion in ModuleDescriptor
        if (version == null) {
            return name;
        } else {
            return name + '@' + version;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HybridModuleId hybridModuleId = (HybridModuleId) o;
        return Objects.equals(name, hybridModuleId.name) &&
                Objects.equals(version, hybridModuleId.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version);
    }

    private static Version versionFromString(String version) {
        if (version == null || version.equals("")) {
            return null;
        } else {
            return Version.parse(version);
        }
    }

    private void validateName() {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name must be non-empty");
        } else if (name.indexOf('@') != -1) {
            throw new IllegalArgumentException("Name cannot contain @");
        }
    }
}
