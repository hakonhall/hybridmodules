package one.exported;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PublicTest {
    @Test
    void willPass() {
        assertEquals(1, Public.PUBLIC);
    }
}
