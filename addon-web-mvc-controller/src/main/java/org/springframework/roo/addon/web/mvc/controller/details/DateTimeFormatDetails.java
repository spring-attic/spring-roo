package org.springframework.roo.addon.web.mvc.controller.details;

/**
 * Simple detail holder for date formats.
 * 
 * @author Rossen Stoyanchev
 * @since 1.1.2
 */
public class DateTimeFormatDetails {

    /**
     * Factory method for a {@link DateTimeFormatDetails} with the given pattern
     * 
     * @param the pattern to set (can be <code>null</code>)
     * @return a non-<code>null</code> instance
     */
    public static DateTimeFormatDetails withPattern(final String pattern) {
        final DateTimeFormatDetails instance = new DateTimeFormatDetails();
        instance.pattern = pattern;
        return instance;
    }

    /**
     * Factory method for a {@link DateTimeFormatDetails} with the given style
     * 
     * @param style the style to set (can be <code>null</code>)
     * @return a non-<code>null</code> instance
     */
    public static DateTimeFormatDetails withStyle(final String style) {
        final DateTimeFormatDetails instance = new DateTimeFormatDetails();
        instance.style = style;
        return instance;
    }

    public String pattern;
    public String style;

    @Override
    public String toString() {
        // For debugging
        return style + ":" + pattern;
    }
}
