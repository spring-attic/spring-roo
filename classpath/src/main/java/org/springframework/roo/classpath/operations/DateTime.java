package org.springframework.roo.classpath.operations;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Provides date format options for {@link Date} and {@link Calendar} types.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class DateTime implements Comparable<DateTime> {

	private String key;
	private char shortKey;

	public static final DateTime SHORT = 	new DateTime("SHORT", 	'S');
	public static final DateTime MEDIUM = 	new DateTime("MEDIUM", 	'M');
//	disabled due to incompatibility between Dojo and JDK dateformat handling
//	public static final DateTime LONG = 	new DateTime("LONG", 	'L');
//	public static final DateTime FULL = 	new DateTime("FULL", 	'F');
	public static final DateTime NONE = 	new DateTime("NONE", 	'-');

	public DateTime(String key, char shortKey) {
		Assert.hasText(key, "Key required");
		Assert.hasText(key, "Short key required");
		this.key = key;
		this.shortKey = shortKey;
	}

	public String getKey() {
		return key;
	}
	
	public char getShortKey() {
		return shortKey;
	}
	
	/**
	 * This method will return the DateTime style for the character of the style argument. 
	 * If no style is recognized it will return DateFormat.SHORT
	 * 
	 * @param style the date or time style, ie 'S' 
	 * @return
	 */
	public static int parseDateFormat(char style) {
		switch (style) {
			case 'M' : return DateFormat.MEDIUM;
			case 'L' : return DateFormat.LONG;
			case 'F' : return DateFormat.FULL;
			default : return DateFormat.SHORT;
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
			case 'S' : return DateTime.SHORT;
			case 'M' : return DateTime.MEDIUM;
//			disabled due to incompatibility between Dojo and JDK dateformat handling
//			case 'L' : return DateTime.LONG;
//			case 'F' : return DateTime.FULL;
		}
		return DateTime.NONE;
	}

	public final int hashCode() {
		return this.key.hashCode();
	}

	public final boolean equals(Object obj) {
		return obj != null && obj instanceof DateTime && this.compareTo((DateTime)obj) == 0;
	}

	public final int compareTo(DateTime o) {
		if (o == null) return -1;
		return this.key.compareTo(o.key);
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("key", key);
		return tsc.toString();
	}
}
