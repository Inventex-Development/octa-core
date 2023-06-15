package dev.inventex.octa.data.primitive;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a concentrated holder of two values.
 * @param <A> the type of the first value
 * @param <B> the type of the second value
 */
@AllArgsConstructor
@Getter
public class Tuple<A, B> {
    /**
     * The first element of the tuple.
     */
    private final A first;

    /**
     * The second element of the tuple.
     */
    private final B second;
}
