package org.springframework.roo.support.util;

/**
 * A class which contains a number of number manipulation operations
 * 
 * @author James Tyrrell
 * @since 1.2.0
 */
public class MathUtils {

    public static double round(final double valueToRound,
            final int numberOfDecimalPlaces) {
        final double multiplicationFactor = Math.pow(10, numberOfDecimalPlaces);
        final double interestedInZeroDPs = valueToRound * multiplicationFactor;
        return Math.round(interestedInZeroDPs) / multiplicationFactor;
    }
}
