package root;

import java.net.http.HttpTimeoutException;

public class Main {
    public static void main(String[] args) {
        HttpTimeoutException e = new HttpTimeoutException("foo");
        System.out.println(e);
    }
}
