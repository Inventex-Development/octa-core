package dev.inventex.octa.function;

import java.util.Objects;

public interface ThrowableConsumer<T, E extends Throwable> {
    void accept(T t) throws E;

    default ThrowableConsumer<T, E> andThen(ThrowableConsumer<? super T, E> after) {
        Objects.requireNonNull(after);
        return (T t) -> { accept(t); after.accept(t); };
    }
}
