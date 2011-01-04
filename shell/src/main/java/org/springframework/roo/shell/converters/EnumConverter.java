package org.springframework.roo.shell.converters;

import java.util.List;

import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link Enum}.
 *
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
public class EnumConverter implements Converter {

	@SuppressWarnings("all")
	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		Class<Enum> enumClass = (Class<Enum>) requiredType;
		return Enum.valueOf(enumClass, value);
	}

	@SuppressWarnings("all")
	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		Class<Enum> enumClass = (Class<Enum>) requiredType;
		for (Enum enumValue : enumClass.getEnumConstants()) {
			String candidate = enumValue.name();
			if ("".equals(existingData) || candidate.startsWith(existingData) || existingData.startsWith(candidate) || candidate.toUpperCase().startsWith(existingData.toUpperCase()) || existingData.toUpperCase().startsWith(candidate.toUpperCase())) {
				completions.add(candidate);
			}
		}
		return true;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return Enum.class.isAssignableFrom(requiredType);
	}
}