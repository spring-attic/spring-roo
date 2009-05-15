package org.springframework.roo.shell.converters;

import java.io.File;
import java.util.List;

import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;

/**
 * {@link Converter} for {@link File}.
 *
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@ScopeDevelopment
public class FileConverter implements Converter {

	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		return new File(value);
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		// Present what they've typed, plus "*", to get the next level of completions
		if (!existingData.endsWith("*")) {
			existingData = existingData + "*";
		}		
		return false;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return File.class.isAssignableFrom(requiredType);
	}
}