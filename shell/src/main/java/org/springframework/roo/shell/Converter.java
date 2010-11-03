package org.springframework.roo.shell;

import java.util.List;

public interface Converter {
	
	boolean supports(Class<?> requiredType, String optionContext);
	
	Object convertFromText(String value, Class<?> requiredType, String optionContext);
	
	boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target);
}
