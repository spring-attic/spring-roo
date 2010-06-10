package org.springframework.roo.shell.converters;

import java.util.List;

import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link Long}.
 *
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class LongConverter implements Converter {

	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		return new Long(value);
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		return false;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return Long.class.isAssignableFrom(requiredType) || long.class.isAssignableFrom(requiredType);
	}

}