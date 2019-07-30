package no.ion.jhms;

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.FindException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.Set;

import static no.ion.jhms.ExceptionUtil.uncheck;

/**
 * Represents an opened hybrid modular JAR.
 */
class HybridModularJar implements AutoCloseable {
    private final Path path;
    private final ModuleReference reference;
    private final HybridModuleDescriptor descriptor;
    private final URI uri;
    private final ModuleReader reader;
    private volatile byte[] sha256OfJarFile = null;

    /** {@code path} should refer to a regular file being a hybrid modular JAR. */
    static HybridModularJar open(Path path, PlatformModuleContainer platformModuleContainer) {
        Set<ModuleReference> references = ModuleFinder.of(path).findAll();
        switch (references.size()) {
            case 0:
                // This may happen if the file has been removed since e.g. isRegularFile was tested.
                throw new FindException("No modular JAR file found at path " + path);
            case 1: break;
            default:
                // Not sure how this can happen, except if 'path' was a regular file at the time isRegularFile()
                // was called, but it has since been replaced with a directory before the above ModuleFinder.of()
                // call. Or, we have totally misunderstood ModuleFinder.of().
                throw new FindException("The regular file '" + path + "' contains " + references.size() + " modular JARs");
        }
        ModuleReference reference = references.iterator().next();

        URI uri = reference.location().orElseThrow(() -> new IllegalStateException("Hybrid module missing URI: " + path));

        HybridModuleDescriptor descriptor;
        try {
            descriptor = HybridModuleDescriptor.read(reference.descriptor(), platformModuleContainer);
        } catch (InvalidHybridModuleException e) {
            throw new InvalidHybridModuleException("For hybrid modular JAR at path " + path, e);
        }

        return new HybridModularJar(path, reference, descriptor, uri);
    }

    private HybridModularJar(Path path, ModuleReference reference, HybridModuleDescriptor descriptor, URI uri) {
        this.path = path;
        this.reference = reference;
        this.descriptor = descriptor;
        this.uri = uri;
        this.reader = uncheck(reference::open);
    }

    HybridModuleId2 id() { return descriptor.id(); }
    Path path() { return path; }
    HybridModuleDescriptor descriptor() { return descriptor; }
    /** Prefer this over descriptor().mainClass(), in case we need to start looking at Main-Class from the manifest. */
    Optional<String> mainClass() { return reference.descriptor().mainClass(); }

    byte[] calculateJarFileChecksum() {
        if (sha256OfJarFile == null) {
            MessageDigest sha256;
            try {
                sha256 = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

            byte[] fileContent = uncheck(() -> Files.readAllBytes(path));
            sha256OfJarFile = sha256.digest(fileContent);
        }

        return sha256OfJarFile;
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
    private static String resourceNameFromBinaryClassName(String className) {
        return className.replaceAll("\\.", "/") + ".class";
    }

    @Override
    public void close() { uncheck(reader::close); }
}
