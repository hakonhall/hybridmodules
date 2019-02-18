package no.ion.hybridmodules;

import java.io.IOException;
import java.io.UncheckedIOException;

class ExceptionUtil {
    /** Wrap any IOException thrown by supplier in an UncheckedIOException. */
    static <T> T uncheck(SupplierThrowingIOException<T> supplier) {
        try {
            return supplier.get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @FunctionalInterface
    interface SupplierThrowingIOException<T> {
        T get() throws IOException;
    }

    /** Wrap any IOException thrown by runnable in an UncheckedIOException. */
    static void uncheck(RunnableThrowingIOException runnable) {
        try {
            runnable.run();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @FunctionalInterface
    interface RunnableThrowingIOException {
        void run() throws IOException;
    }

    private ExceptionUtil() {}
}
