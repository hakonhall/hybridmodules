package main;

import m.VersionOneClass;
import intermediate.Intermediate;

public class Main {
    public Main() {}
    public static void main(String... args) {
        System.out.println("From module m@1: " + VersionOneClass.info());
        System.out.println("From module m@2: " + Intermediate.info() +
                           " (through Intermediate class)");
    }
}
