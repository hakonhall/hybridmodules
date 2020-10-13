package one.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class PublicTest {
    @Test
    void willPass() {
        assertEquals(11, PackagePrivate.PACKAGE_PRIVATE);
    }
}
