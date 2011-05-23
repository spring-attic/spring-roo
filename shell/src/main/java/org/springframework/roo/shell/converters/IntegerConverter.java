package org.springframework.roo.shell.converters;

import java.util.List;

import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link Integer}.
 *
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class IntegerConverter implements Converter<Integer> {

	public Integer convertFromText(String value, Class<?> requiredType, String optionContext) {
		return new Integer(value);
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		return false;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return Integer.class.isAssignableFrom(requiredType) || int.class.isAssignableFrom(requiredType);
	}
}