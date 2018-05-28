package no.ion.hybridmodules;

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.util.Optional;

import static no.ion.hybridmodules.ExceptionUtil.uncheck;

class Jar implements AutoCloseable {
    private final ModuleDescriptor descriptor;
    private final ModuleReader reader;
    private final HybridModuleId hybridModuleId;
    private final URI uri;

    Jar(ModuleReference reference) {
        this.descriptor = reference.descriptor();
        this.reader = uncheck(reference::open);
        String name = descriptor.name();
        Version version = descriptor.version().orElseThrow(() -> new IllegalArgumentException("Module " + name + " does not have a version"));
        this.hybridModuleId = new HybridModuleId(name, version);
        this.uri = reference.location().orElseThrow(() -> new IllegalStateException("Module " + name + " not associated with a URI"));
    }

    HybridModuleId moduleId() {
        return hybridModuleId;
    }

    ModuleDescriptor descriptor() {
        return descriptor;
    }

    URI uri() {
        return uri;
    }

    /** Get the class bytes given class name, or null if not found. */
    byte[] getClassBytes(String binaryName) {
        String resourceName = resourceNameFromBinaryClassName(binaryName);
        Optional<InputStream> inputStream = uncheck(() -> reader.open(resourceName));
        if (inputStream.isPresent()) {
            try {
                return uncheck(() -> inputStream.get().readAllBytes());
            } finally {
                try {
                    inputStream.get().close();
                } catch (IOException e) {
                    // ignore
                }
            }
        } else {
            return null;
        }
    }

    /** The resource name */
    static String resourceNameFromBinaryClassName(String className) {
        return className.replaceAll("\\.", "/") + ".class";
    }

    @Override
    public void close() {
        uncheck(reader::close);
    }
}
