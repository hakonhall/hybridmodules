package root;

import java.lang.module.ModuleFinder;

public class Main {
    public static void main(String... args) {
	System.out.println("Hello world!");
        ModuleFinder finder = ModuleFinder.ofSystem();
        System.out.println("all system modules: " + finder.findAll());
    }
}
