package org.springframework.roo.shell.converters;

import java.util.List;

import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link String}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class StringConverter implements Converter {

	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		return value;
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		return false;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return String.class.isAssignableFrom(requiredType) && (optionContext == null || !optionContext.contains("disable-string-converter"));
	}

}
