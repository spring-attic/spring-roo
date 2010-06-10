package org.springframework.roo.shell.converters;

import java.util.List;

import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link Double}.
 *
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class DoubleConverter implements Converter {

	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		return new Double(value);
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		return false;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return Double.class.isAssignableFrom(requiredType) || double.class.isAssignableFrom(requiredType);
	}

}