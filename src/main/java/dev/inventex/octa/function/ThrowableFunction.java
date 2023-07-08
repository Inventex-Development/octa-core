package dev.inventex.octa.function;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@FunctionalInterface
public interface ThrowableFunction<T, R, E extends Throwable> {
    R apply(T t) throws E;

    @SneakyThrows
    default <V> ThrowableFunction<V, R, E> compose(@NotNull ThrowableFunction<? super V, ? extends T, ? extends E> before) {
        Objects.requireNonNull(before);
        return (V v) -> apply(before.apply(v));
    }

    @SneakyThrows
    default <V> ThrowableFunction<T, V, E> andThen(ThrowableFunction<? super R, ? extends V, ? extends E> after) {
        Objects.requireNonNull(after);
        return (T t) -> after.apply(apply(t));
    }

    static <T, E extends Throwable> ThrowableFunction<T, T, E> identity() {
        return t -> t;
    }
}
