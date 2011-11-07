package org.springframework.roo.project.converter;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.support.util.StringUtils;

@Component
@Service
public class PomConverter implements Converter<Pom>{

	// Constants
	static final String ROOT_MODULE_SYMBOL = "~";
	
	// Fields
	@Reference ProjectOperations projectOperations;

	public boolean supports(final Class<?> type, final String optionContext) {
		return Pom.class.isAssignableFrom(type);
	}

	public Pom convertFromText(final String value, final Class<?> targetType, final String optionContext) {
		final String moduleName;
		if (ROOT_MODULE_SYMBOL.equals(value)) {
			moduleName = "";
		} else {
			moduleName = value;
		}
		return projectOperations.getPomFromModuleName(moduleName);
	}

	public boolean getAllPossibleValues(final List<Completion> completions, final Class<?> targetType, final String existingData, final String optionContext, final MethodTarget target) {
		final String focusedModuleName = projectOperations.getFocusedModuleName();
		for (final String moduleName : projectOperations.getModuleNames()) {
			if (!moduleName.equals(focusedModuleName)) {
				final String nonEmptyModuleName = StringUtils.defaultIfEmpty(moduleName, ROOT_MODULE_SYMBOL);
				completions.add(new Completion(nonEmptyModuleName));
			}
		}
		return true;
	}
}
