public class Foo {
    public static void main(String... args) {
        System.out.println("String: " + String.class.getClassLoader());
        System.out.println("List: " + java.util.List.class.getClassLoader());
        System.out.println("Class: " + Class.class.getClassLoader());
    }
}
