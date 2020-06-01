package one.exported;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class PackagePrivateTest {
    @Test
    void verify() {
        assertEquals(11, PackagePrivate.PACKAGE_PRIVATE);
    }
}
