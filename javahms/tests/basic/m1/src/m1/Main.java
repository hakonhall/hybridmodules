package m1;

public class Main {
    public Main() {}
    public static void main(String... args) {
        System.out.println("Hello from " + Main.class.getName());
        for (int i = 0; i < args.length; ++i) {
            System.out.println(i + ": " + args[i]);
        }
    }
}
