package org.springframework.roo.support.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Provides extra functionality for Java Number classes.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public final class NumberUtils {

	/**
	 * Returns the minimum value in the array.
	 * 
	 * @param array an array of Numbers
	 * @return the minimum value in the array, or null if all the elements are null
	 */
	public static BigDecimal min(final Number... array) {
		return minOrMax(true, array);
	}
	
	/**
	 * Returns the maximum value in the array.
	 * 
	 * @param array an array of Numbers
	 * @return the maximum value in the array, or null if all the elements are null
	 */
	public static BigDecimal max(final Number... array) {
		return minOrMax(false, array);
	}

	private static BigDecimal minOrMax(final boolean isMin, final Number... array) {
		if (array == null || array.length == 0) {
			return null;
		}
		// Find first non-null element
		int start = 0;
		for (int i = 1; i < array.length; i++) {
			if (array[i] != null) {
				start = i;
				break;
			}
		}
		
		BigDecimal minOrMax = getBigDecimal(array[start]);
		for (int i = start + 1; i < array.length; i++) {
			BigDecimal number = getBigDecimal(array[i]);
			BigDecimal value = minOrMax != null ? new BigDecimal(String.valueOf(minOrMax)) : null;
			if (number != null && (isMin ? number.compareTo(value) < 0 : number.compareTo(value) > 0)) {
				minOrMax = number;
			}
		}
		return minOrMax;
	}
	
	private static BigDecimal getBigDecimal(Number number) {
		BigDecimal value;
		if (number == null) {
			value = null;
		} else if (number instanceof BigDecimal) {
			value = (BigDecimal) number;
		} else if (number instanceof BigInteger) {
			value = new BigDecimal((BigInteger) number);
		} else {
			value = new BigDecimal(String.valueOf(number));
		}
		return value;
	}
	
	/**
	 * Constructor is private to prevent instantiation
	 */
	private NumberUtils() {}
}
