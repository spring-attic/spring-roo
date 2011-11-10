package org.springframework.roo.classpath.converters;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.PhysicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

@Component
@Service
public class ContextualPathConverter implements Converter<LogicalPath> {

	// Fields
	@Reference ProjectOperations projectOperations;

	public boolean supports(final Class<?> requiredType, final String optionContext) {
		return LogicalPath.class.isAssignableFrom(requiredType);
	}

	public LogicalPath convertFromText(final String value, final Class<?> targetType, final String optionContext) {
		LogicalPath contextualPath = LogicalPath.getInstance(value);
		if (contextualPath.getModule().equals("FOCUSED")) {
			contextualPath = LogicalPath.getInstance(contextualPath.getPath(), projectOperations.getFocusedModuleName());
		}
		return contextualPath;
	}

	public boolean getAllPossibleValues(final List<Completion> completions, final Class<?> targetType, final String existingData, final String optionContext, final MethodTarget target) {
		for (final Pom pom : projectOperations.getPoms()) {
			for (PhysicalPath pathInformation : pom.getPathInformation()) {
				completions.add(new Completion(pathInformation.getContextualPath().getName()));
			}
		}
		return false;
	}
}
