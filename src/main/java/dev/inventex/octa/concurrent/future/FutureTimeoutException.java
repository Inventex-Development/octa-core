package dev.inventex.octa.concurrent.future;

/**
 * Represents a future exception caused by exceeding the wait time limit.
 */
public class FutureTimeoutException extends Exception {
    private final long timeout;
    public FutureTimeoutException(long timeout) {
        super("Timeout of " + timeout + "ms exceeded.");
        this.timeout = timeout;
    }

    /**
     * Returns the timeout that has been exceeded.
     * @return the timeout in milliseconds
     */
    public long getTimeout() {
        return timeout;
    }
}
