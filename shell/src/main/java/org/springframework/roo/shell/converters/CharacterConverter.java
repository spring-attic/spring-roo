package org.springframework.roo.shell.converters;

import java.util.List;

import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link Character}.
 *
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class CharacterConverter implements Converter<Character> {

	public Character convertFromText(String value, Class<?> requiredType, String optionContext) {
		return new Character(value.charAt(0));
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		return false;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return Character.class.isAssignableFrom(requiredType) || char.class.isAssignableFrom(requiredType);
	}

}