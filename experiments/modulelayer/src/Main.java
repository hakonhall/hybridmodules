public class Main {
    public static void main(String... args) {
        var layer = ModuleLayer.boot();
        for (var module: layer.modules()) {
            System.out.println(module);
        }
    }
}
