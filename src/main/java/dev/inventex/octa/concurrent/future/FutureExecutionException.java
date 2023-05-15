package dev.inventex.octa.concurrent.future;

/**
 * An exception, that represents if a {@link Future} has completed
 * unsuccessfully / with an exception. The original cause can be retrieved
 * with {@link #getCause()}.
 */
public class FutureExecutionException extends Exception {
    public FutureExecutionException() {
    }

    public FutureExecutionException(String message) {
        super(message);
    }

    public FutureExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public FutureExecutionException(Throwable cause) {
        super(cause);
    }

    public FutureExecutionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
