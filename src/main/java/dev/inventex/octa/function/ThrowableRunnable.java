package dev.inventex.octa.function;

@FunctionalInterface
public interface ThrowableRunnable<E extends Throwable> {
    void run() throws E;
}
