package org.springframework.roo.shell.converters;

import java.util.List;

import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.shell.SimpleParser;

/**
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class AvailableCommandsConverter implements Converter<String> {

	public String convertFromText(String text, Class<?> requiredType, String optionContext) {
		return text;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return String.class.isAssignableFrom(requiredType) && "availableCommands".equals(optionContext);
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		if (target.target instanceof SimpleParser) {
			SimpleParser cmd = (SimpleParser) target.target;

			// Only include the first word of each command
			for (String s : cmd.getEveryCommand()) {
				if (s.contains(" ")) {
					completions.add(s.substring(0, s.indexOf(" ")));
				} else {
					completions.add(s);
				}
			}
		}
		return true;
	}
}
