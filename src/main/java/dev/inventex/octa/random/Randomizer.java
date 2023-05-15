package dev.inventex.octa.random;

import java.util.Random;

/**
 * Represents a simple randomizing utility that generates random url-safe strings.
 */
public class Randomizer {
    /**
     * The randomizer used for string generating.
     */
    private static final Random random = new Random();

    /**
     * The collection of the lowercase letters.
     */
    private static final String lower = "abcdefghijklmnopqrstuvxyz";

    /**
     * The collection of the digits.
     */
    private static final String digits = "0123456789";

    /**
     * The characters used for the token generation.
     */
    private static final String alphanumeric = lower + digits;

    /**
     * The string generation characters in an array.
     */
    private static final char[] characters = alphanumeric.toCharArray();

    /**
     * Generate a random url safe string.
     * @param length string length
     * @return generated random string
     */
    public static String randomString(int length) {
        char[] buffer = new char[length];
        for (int i = 0; i < length; i++) {
            buffer[i] = characters[random.nextInt(characters.length)];
        }
        return new String(buffer);
    }

    /**
     * Generate a random integer between the given range.
     * @param min range minimum (inclusive)
     * @param max range maximum (exclusive)
     * @return random integer in range [min;max[
     */
    public static int randomInt(int min, int max) {
        return random.nextInt(max - min) + min;
    }

    /**
     * Generate a random integer between the given range.
     * @param max range maximum (exclusive)
     * @return random integer in range [0;max[
     */
    public static int randomInt(int max) {
        return random.nextInt(max);
    }
}
