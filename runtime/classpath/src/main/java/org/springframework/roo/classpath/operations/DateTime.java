package org.springframework.roo.classpath.operations;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Provides date format options for {@link Date} and {@link Calendar} types.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
public enum DateTime {
    MEDIUM('M'), NONE('-'), SHORT('S');
    // Disabled due to incompatibility between Dojo and JDK dateformat handling
    // LONG('L'), FULL('F');

    /**
     * This method will return the DateTime style for the character of the style
     * argument. If no style is recognized it will return DateFormat.SHORT.
     * 
     * @param style the date or time style, ie 'S'
     * @return the DateTime style.
     */
    public static int parseDateFormat(final char style) {
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
     * This method will return the DateTime style for the character of the style
     * argument. For example style of '-' will return DateTime.NULL.
     * 
     * @param style the date or time style, ie 'S'
     * @return the DateTime style for the provided style argument
     */
    public static DateTime parseDateTimeFormat(final char style) {
        switch (style) {
        case 'S':
            return DateTime.SHORT;
        case 'M':
            return DateTime.MEDIUM;
            // Disabled due to incompatibility between Dojo and JDK dateformat
            // handling
            // case 'L' : return DateTime.LONG;
            // case 'F' : return DateTime.FULL;
        }
        return DateTime.NONE;
    }

    private char shortKey;

    private DateTime(final char shortKey) {
        this.shortKey = shortKey;
    }

    public char getShortKey() {
        return shortKey;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("name", name());
        builder.append("shortKey", shortKey);
        return builder.toString();
    }
}
