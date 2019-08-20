// Copyright 2019 Oath Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package no.ion.jhms;

import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Responsible for resolving a platform module name to {@link PlatformModule}.
 *
 * @author hakonhall
 */
public class PlatformModuleContainer {
    private final ModuleFinder systemModuleFinder;
    private final TreeMap<String, PlatformModule> platformModules = new TreeMap<>();

    PlatformModuleContainer() {
        this(ModuleFinder.ofSystem());
    }

    private PlatformModuleContainer(ModuleFinder systemModuleFinder) {
        this.systemModuleFinder = systemModuleFinder;
    }

    TreeMap<String, PlatformModule> platformModules() { return new TreeMap<>(platformModules); }

    Optional<PlatformModule> resolve(String name) {
        PlatformModule platformModule = platformModules.get(name);
        if (platformModule != null) return Optional.of(platformModule);

        // Because jlink is not supported, the system module finder is 1:1 with platform modules:
        //   1. The Java SE Platform modules that must start with "java.", and
        //   2. the other OpenJDK modules must start with "jdk." (JEP200). But non-OpenJDK may have other modules.

        Optional<ModuleReference> moduleReference = systemModuleFinder.find(name);
        if (moduleReference.isPresent()) {
            platformModule = resolve(moduleReference.get().descriptor());
            platformModules.put(name, platformModule);
            return Optional.of(platformModule);
        } else {
            if (name.startsWith("java.") || // JLS11 §6.1
                    name.startsWith("jdk.")) { // JEP200
                throw new FindException("Failed to find platform module: " + name);
            }

            return Optional.empty();
        }
    }

    Optional<PlatformModule> get(String name) { return Optional.ofNullable(platformModules.get(name)); }

    private PlatformModule resolve(ModuleDescriptor descriptor) {
        var builder = new PlatformModule.Builder(descriptor.name());

        builder.setPackages(descriptor.packages());

        for (var requires : descriptor.requires()) {
            if (requires.modifiers().contains(ModuleDescriptor.Requires.Modifier.STATIC)) {
                // The behavior is mandated by java.lang.module package documentation, and is difficult to
                // get exactly right. From a scan of OpenJDK 12, no platform modules use the static modifier,
                // except the jdk.scripting.nashorn.shell module which is deprecated since 11.
                //
                // We therefore take some freedom in handling 'static' as we wish - and choose to ignore it.
                continue;
            }

            String requiresName = requires.name();
            Optional<PlatformModule> requiresModule = resolve(requiresName);
            if (requiresModule.isPresent()) {
                builder.addRequires(
                        requiresModule.get(),
                        requires.modifiers().contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE));
            } else {
                throw new FindException("Platform module " + descriptor.name() + " requires " + requiresName +
                        ", but it was not found with ModuleFinder.ofSystem()");
            }
        }

        for (var exports : descriptor.exports()) {
            builder.addExports(exports.source(), exports.targets());
        }

        return builder.build();
    }
}
