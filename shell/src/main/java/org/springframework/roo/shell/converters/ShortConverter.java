package org.springframework.roo.shell.converters;

import java.util.List;

import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link Short}.
 *
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class ShortConverter implements Converter {

	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		return new Short(value);
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		return false;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return Short.class.isAssignableFrom(requiredType) || short.class.isAssignableFrom(requiredType);
	}

}