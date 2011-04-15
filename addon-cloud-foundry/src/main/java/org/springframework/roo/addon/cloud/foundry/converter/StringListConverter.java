package org.springframework.roo.addon.cloud.foundry.converter;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

import java.util.ArrayList;
import java.util.List;

@Service
@Component
public class StringListConverter implements Converter {

	public boolean supports(Class<?> requiredType, String optionContext) {
		return List.class.isAssignableFrom(requiredType);
	}

	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		if (value == null) {
			return null;
		}
		String[] tokens = value.split(",");
		List<String> tokenList = new ArrayList<String>();
		for (String token : tokens) {
			tokenList.add(token.trim());
		}
		return tokenList;
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		return false;
	}
}
