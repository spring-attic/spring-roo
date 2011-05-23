package org.springframework.roo.classpath.operations;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link String} that understands the "topics" option context.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Service
@Component
public class HintConverter implements Converter<String> {
	
	@Reference private HintOperations hintOperations;

	public String convertFromText(String value, Class<?> requiredType, String optionContext) {
		return value;
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		completions.addAll(hintOperations.getCurrentTopics());
		return false;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return String.class.isAssignableFrom(requiredType) && optionContext.contains("topics");
	}
}
