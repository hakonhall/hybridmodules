package one.exported;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class PackagePrivateTest {
    @Test
    void willFail() {
        assertEquals(-1, PackagePrivate.PACKAGE_PRIVATE);
    }
}
