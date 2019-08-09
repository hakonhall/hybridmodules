package no.ion.jhms;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Optional;

/**
 * API of a root module from outside the hybrid module container.
 */
public class RootHybridModule {
    private final HybridModule root;

    RootHybridModule(HybridModule hybridModule) { this.root = hybridModule; }

    /** Load Class from an unqualified exported package. */
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        String packageName = PackageUtil.getPackageName(name);
        if (!root.packageVisibleToAll(packageName)) {
            throw new ClassNotFoundException(name);
        }

        return root.getClassLoader().loadExportedClass(name);
    }

    /** Invoke {@code public static void main(String...)} in the main class of the module. */
    public void main(String... args) { main(null, args); }

    /** Get the main class of the root hybrid module. */
    public Optional<String> mainClass() { return root.getMainClass(); }

    /** Invoke a main method in the root hybrid module. */
    public void main(String mainClassName, String... args) {
        if (mainClassName == null) {
            mainClassName = mainClass()
                    .orElseThrow(() -> new IllegalArgumentException("The root hybrid module " +
                            root.id() + " does not have a main class defined in the module descriptor"));
        }

        Class<?> mainClass;
        try {
            mainClass = loadClass(mainClassName);
        } catch (ClassNotFoundException e) {
            NoClassDefFoundError error = new NoClassDefFoundError(mainClassName +
                    " not found in an exported package of hybrid module " + root.id());
            error.initCause(e);
            throw error;
        }

        int mainClassModifiers = mainClass.getModifiers();
        // Note: We're allowing the main method to be defined in an abstract class or on an interface.
        if (!Modifier.isPublic(mainClassModifiers)) {
            throw new IllegalAccessError("The main class " + mainClassName + " in hybrid module " +
                    root.id() + " is not public");
        }

        Method method;
        try {
            method = mainClass.getDeclaredMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("There is no main(String[]) method in class " + mainClassName +
                    " in hybrid module " + root.id());
        }

        int modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers) || method.getReturnType() != void.class) {
            throw new IllegalArgumentException("The main(String[]) method in class " + mainClassName +
                    " in hybrid module " + root.id() + " is not public static void");
        }

        try {
            method.invoke(null, (Object) args);
        } catch (IllegalAccessException e) {
            // This should never happen as we have successfully loaded a public and exported type.
            IllegalAccessError error = new IllegalAccessError("The public static void main(String[]) method in " + mainClassName +
                    " in hybrid module " + root.id());
            error.initCause(e);
            throw error;
        } catch (InvocationTargetException e) {
            throw new UndeclaredThrowableException(e.getCause());
        }
    }

    @Override
    public String toString() { return root.id().toString(); }
}