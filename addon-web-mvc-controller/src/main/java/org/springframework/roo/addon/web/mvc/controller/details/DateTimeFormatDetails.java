package org.springframework.roo.addon.web.mvc.controller.details;

/**
 * Simple detail holder for date formats.
 * 
 * @author Rossen Stoyanchev
 * @since 1.1.2
 */
public class DateTimeFormatDetails {
	public String style;
	public String pattern;

	public static DateTimeFormatDetails withStyle(String style) {
		DateTimeFormatDetails d = new DateTimeFormatDetails();
		d.style = style;
		return d;
	}

	public static DateTimeFormatDetails withPattern(String pattern) {
		DateTimeFormatDetails d = new DateTimeFormatDetails();
		d.pattern = pattern;
		return d;
	}
	
}
