package no.ion.hybridmodules;

/**
 * Thrown when a modular JAR does not conform to a hybrid module.
 */
class InvalidHybridModuleException extends RuntimeException {
    InvalidHybridModuleException() {
    }

    InvalidHybridModuleException(String message) {
        super(message);
    }

    InvalidHybridModuleException(String message, Throwable cause) {
        super(message, cause);
    }

    InvalidHybridModuleException(Throwable cause) {
        super(cause);
    }
}
