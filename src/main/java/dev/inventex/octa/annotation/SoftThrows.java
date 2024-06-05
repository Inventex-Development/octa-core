package dev.inventex.octa.annotation;

/**
 * Represents an annotation, that indicates, that the method may throw the specified {@link #value()} exception,
 * however the exception is not required to be caught.
 */
public @interface SoftThrows {
    /**
     * The exception that may be thrown.
     *
     * @return the exception that may be thrown
     */
    Class<? extends Throwable> value();
}
