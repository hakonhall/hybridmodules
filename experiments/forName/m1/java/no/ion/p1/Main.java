package no.ion.p1;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.util.Arrays;

public class Main {
	public static void main(String... args) throws ClassNotFoundException {
		open("no.ion.p2.exportedandopen.PackagePrivate");
		open("no.ion.p2.exportedandopen.Public");
		open("no.ion.p2.open.PackagePrivate");
		open("no.ion.p2.open.Public");
		open("no.ion.p2.exported.PackagePrivate");
		open("no.ion.p2.exported.Public");
		open("no.ion.p2.internal.PackagePrivate");
		open("no.ion.p2.internal.Public");
	}
	private static void open(String classname) {
		try {
			Class<?> c = Class.forName(classname);
			System.out.println(c);
			for (Field field : c.getDeclaredFields()) {
				printField(field);
			}
		} catch (ClassNotFoundException e) {
			System.out.println(classname + " not found: " + e);
		}
	}

	private static void printField(Field field) {
		String name = field.getName();

		try {
			System.out.println("  " + name + " = " + field.get(null));
		} catch (IllegalAccessException e) {
			System.out.println("  Accessing " + name + " error: " + e);
		} catch (InaccessibleObjectException e) {
			System.out.println("  Accessing " + name + " inaccessible: " + e);
		}

		try {
			field.setAccessible(true);
			System.out.println("  " + name + " = " + field.get(null));
		} catch (IllegalAccessException e) {
			System.out.println("  Accessing " + name + " error: " + e);
		} catch (InaccessibleObjectException e) {
			System.out.println("  Accessing " + name + " inaccessible: " + e);
		}
	}
}
