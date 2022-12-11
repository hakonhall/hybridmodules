package no.ion.jhms;

public class Argument<T> {
    private final Class<T> type;
    private final T value;

    public static <T> Argument<T> of(Class<T> type, T value) {
        return new Argument<>(type, value);
    }

    public Argument(Class<T> type, T value) {
        this.type = type;
        this.value = value;
    }

    public Class<T> type() { return type; }
    public T value() { return value; }
}
