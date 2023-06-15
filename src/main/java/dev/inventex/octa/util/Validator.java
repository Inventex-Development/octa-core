package dev.inventex.octa.util;

public class Validator {
    public static void notNull(Object o, String name) {
        if (o == null)
            throw new IllegalStateException("'" + name + "' must not be null");
    }
}
