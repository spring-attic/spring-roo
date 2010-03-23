package org.springframework.roo.classpath.converters;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.Path;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * Provides conversion to and from {@link Path}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component
@Service
public class PathConverter implements Converter {

	private SortedSet<String> legalValues = new TreeSet<String>();
	
	public PathConverter() {
		legalValues.add(Path.ROOT.getName());
		legalValues.add(Path.SRC_MAIN_JAVA.getName());
		legalValues.add(Path.SRC_MAIN_RESOURCES.getName());
		legalValues.add(Path.SRC_MAIN_WEBAPP.getName());
		legalValues.add(Path.SRC_TEST_JAVA.getName());
		legalValues.add(Path.SRC_TEST_RESOURCES.getName());
		legalValues.add(Path.SPRING_CONFIG_ROOT.getName());
	}
	
	// TODO: Allow context to limit to source paths only, limit to resource paths only 
	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		if (value == null || "".equals(value) || !legalValues.contains(value)) {
			return null;
		}
		
		return new Path(value);
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return Path.class.isAssignableFrom(requiredType);
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		for (String candidate : legalValues) {
			if ("".equals(existingData) || candidate.startsWith(existingData)) {
				completions.add(candidate);
			}
		}
		return true;
	}

}
