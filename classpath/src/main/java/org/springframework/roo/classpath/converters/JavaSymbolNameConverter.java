package org.springframework.roo.classpath.converters;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * Provides conversion to and from {@link JavaSymbolName}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class JavaSymbolNameConverter implements Converter {

	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		if (value == null || "".equals(value)) {
			return null;
		}

		return new JavaSymbolName(value);
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return JavaSymbolName.class.isAssignableFrom(requiredType);
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		return false;
	}
}
