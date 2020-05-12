package no.ion.jhms;

/**
 * Thrown when a modular JAR does not conform to a hybrid module.
 */
class InvalidHybridModuleException extends RuntimeException {
    InvalidHybridModuleException(String message) {
        super(message);
    }
}
