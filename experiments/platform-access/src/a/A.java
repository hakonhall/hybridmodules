package a;

import java.util.logging.Logger;
import java.lang.reflect.Field;

public class A {
    private static final Logger logger = Logger.getLogger(A.class.getName());

    public static void main(String... args) throws Exception {
        Field field = Logger.class.getDeclaredField("offValue");
        field.setAccessible(true);
        System.out.println("Hello");
    }
}
