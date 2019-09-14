import java.net.URL;

import m1.C;

public class Main {
    public static void main(String... args) {
        String mf = "/META-INF/MANIFEST.MF";
        // Both String and java.util.List give the same result.
        URL ur1 = String.class.getResource(mf);
        System.out.println("w/String: " + ur1);

        URL url2 = C.class.getResource(mf);
        System.out.println("w/C: " + url2);
    }
}
