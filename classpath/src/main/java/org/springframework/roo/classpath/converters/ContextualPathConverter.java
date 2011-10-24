package org.springframework.roo.classpath.converters;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.ContextualPath;
import org.springframework.roo.project.PathInformation;
import org.springframework.roo.project.PomManagementService;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

@Component
@Service
public class ContextualPathConverter implements Converter<ContextualPath> {

	@Reference PomManagementService pomManagementService;

	public boolean supports(final Class<?> requiredType, final String optionContext) {
		return ContextualPath.class.isAssignableFrom(requiredType);
	}

	public ContextualPath convertFromText(final String value, final Class<?> targetType, final String optionContext) {
		ContextualPath contextualPath = ContextualPath.getInstance(value);
		if (contextualPath.getModule().equals("FOCUSED")) {
			contextualPath = ContextualPath.getInstance(contextualPath.getPath(), pomManagementService.getFocusedModuleName());
		}
		return contextualPath;
	}

	public boolean getAllPossibleValues(final List<Completion> completions, final Class<?> targetType, final String existingData, final String optionContext, final MethodTarget target) {
		for (Pom pom : pomManagementService.getPomMap().values()) {
			for (PathInformation pathInformation : pom.getPathInformation()) {
				completions.add(new Completion(pathInformation.getContextualPath().getName()));
			}
		}
		return false;
	}
}
