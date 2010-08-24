package org.springframework.roo.classpath.operations;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import org.springframework.roo.support.style.ToStringCreator;

/**
 * Provides date format options for {@link Date} and {@link Calendar} types.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
public enum DateTime {
	SHORT('S'), MEDIUM('M'), NONE('-');
	// Disabled due to incompatibility between Dojo and JDK dateformat handling
	// LONG('L'), FULL('F');
	private char shortKey;

	private DateTime(char shortKey) {
		this.shortKey = shortKey;
	}

	public char getShortKey() {
		return shortKey;
	}

	/**
	 * This method will return the DateTime style for the character of the style argument. If no style 
	 * is recognized it will return DateFormat.SHORT.
	 * 
	 * @param style the date or time style, ie 'S'
	 * @return the DateTime style.
	 */
	public static int parseDateFormat(char style) {
		switch (style) {
		case 'M':
			return DateFormat.MEDIUM;
		case 'L':
			return DateFormat.LONG;
		case 'F':
			return DateFormat.FULL;
		default:
			return DateFormat.SHORT;
		}
	}

	/**
	 * This method will return the DateTime style for the character of the style argument. 
	 * For example style of '-' will return DateTime.NULL.
	 * 
	 * @param style the date or time style, ie 'S'
	 * @return the DateTime style for the provided style argument
	 */
	public static DateTime parseDateTimeFormat(char style) {
		switch (style) {
		case 'S':
			return DateTime.SHORT;
		case 'M':
			return DateTime.MEDIUM;
			// Disabled due to incompatibility between Dojo and JDK dateformat handling
			// case 'L' : return DateTime.LONG;
			// case 'F' : return DateTime.FULL;
		}
		return DateTime.NONE;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("name", name());
		tsc.append("shortKey", shortKey);
		return tsc.toString();
	}
}
