package no;

import one.One;
import common.Internal;

public class Main {
    public static void main(String... args) {
	
	System.out.println("case2: " + get() + " module " + Main.class.getModule());
	System.out.println("one: " + One.get() + " module " + One.class.getModule());
    }

    public static String get() {
	return Internal.name;
    }
}
