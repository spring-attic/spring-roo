package org.springframework.roo.shell.converters;

import java.util.List;

import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;

/**
 * {@link Converter} for {@link Enum}.
 *
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public class EnumConverter implements Converter {

	@SuppressWarnings("unchecked")
	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		Class<Enum> enumClass = (Class<Enum>) requiredType;
		return Enum.valueOf(enumClass, value);
	}

	@SuppressWarnings("unchecked")
	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		Class<Enum> enumClass = (Class<Enum>) requiredType;
		for (Enum enumValue : enumClass.getEnumConstants()) {
			String candidate = enumValue.name();
			if ("".equals(existingData) || candidate.startsWith(existingData) || existingData.startsWith(candidate)) {
				completions.add(enumValue.name());
			}
		}
		return true;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return Enum.class.isAssignableFrom(requiredType);
	}
}