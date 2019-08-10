package no.ion.jhms;

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import static no.ion.jhms.ExceptionUtil.uncheck;

class HybridModuleJar implements AutoCloseable {
    private final Path path;
    private final URI uri;
    private final ModuleDescriptor descriptor;
    private final ModuleReader reader;
    private final HybridModuleId hybridModuleId;

    private volatile byte[] sha256Cache = null;

    /** {@code path} should refer to a regular file being a hybrid modular JAR. */
    static HybridModuleJar open(Path path) {
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
                throw new FindException("The regular file '" + path + "' contains " + references.size() + " modular JARs!?");
        }
        ModuleReference reference = references.iterator().next();

        URI uri = reference.location().orElseThrow(() -> new InvalidHybridModuleException("Hybrid module missing URI: " + path));
        ModuleReader reader = uncheck(reference::open);

        return new HybridModuleJar(path, uri, reference.descriptor(), reader);
    }

    static HybridModuleJar open(String path) { return open(Path.of(path)); }

    private HybridModuleJar(Path path, URI uri, ModuleDescriptor descriptor, ModuleReader reader) {
        this.path = path;
        this.uri = uri;
        this.descriptor = descriptor;
        this.reader = reader;
        this.hybridModuleId = new HybridModuleId(descriptor.name(), HybridModuleVersion.fromRaw(descriptor.rawVersion()));
    }

    Path path() { return path; }
    URI uri() { return uri; }
    ModuleDescriptor descriptor() { return descriptor; }
    HybridModuleId hybridModuleId() { return hybridModuleId; }

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

    /** Two JARs are {@code probablyEqual} if they are the same file (device ID and i-node), or if they have the same SHA-256 checksum. */
    boolean checksumEqual(HybridModuleJar that) {
        if (this == that) {
            return true;
        }

        // If the checksums have already been computet, use those.
        if (this.sha256Cache != null && that.sha256Cache != null) {
            return Arrays.equals(this.sha256Cache, that.sha256Cache);
        }

        // Otherwise, the JARs may actually be the same file.
        if (uncheck(() -> Files.isSameFile(this.path, that.path))) {
            return true;
        }

        // Fall back to the full checksum computation.
        return Arrays.equals(this.computeSha256(), that.computeSha256());
    }

    @Override
    public void close() {
        uncheck(reader::close);
    }

    private static String resourceNameFromBinaryClassName(String className) {
        return className.replaceAll("\\.", "/") + ".class";
    }

    /** Compute the SHA-256 checksum of the JAR file. */
    private byte[] computeSha256() {
        if (sha256Cache == null) {
            MessageDigest sha256;
            try {
                sha256 = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

            byte[] fileContent = uncheck(() -> Files.readAllBytes(path));
            sha256Cache = sha256.digest(fileContent);
        }

        return sha256Cache;
    }

    String sha256String() {
        return bytesToHex(computeSha256());
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
