package org.springframework.roo.shell.converters;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link Date}. 
 *
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class DateConverter implements Converter<Date> {
	
	private DateFormat dateFormat;
	
	public DateConverter() {
		this.dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());
	}
	
	public DateConverter(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}

	public Date convertFromText(String value, Class<?> requiredType, String optionContext) {
		try {
			return dateFormat.parse(value);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Could not parse date: " + e.getMessage());		
		}
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		return false;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return Date.class.isAssignableFrom(requiredType);
	}
}