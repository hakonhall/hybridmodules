package no;

import one.One;
import two.Two;

public class Main {
    public static void main(String... args) {
	System.out.println("one: " + One.get() + " module " + One.class.getModule());
	System.out.println("two: " + Two.get() + " module " + Two.class.getModule());
    }
}
