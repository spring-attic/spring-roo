package org.springframework.roo.project.converter;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.PomManagementService;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.support.util.StringUtils;

@Component
@Service
public class PomConverter implements Converter<Pom>{

	// Constants
	private static final String ROOT_MODULE_SYMBOL = "~";
	
	// Fields
	@Reference private PomManagementService pomManagementService;

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
		return pomManagementService.getPomFromModuleName(moduleName);
	}

	public boolean getAllPossibleValues(final List<Completion> completions, final Class<?> targetType, final String existingData, final String optionContext, final MethodTarget target) {
		final Pom focusedModule = pomManagementService.getFocusedModule();
		if (focusedModule == null) {
			return false;
		}
		final String focusedModuleName = focusedModule.getModuleName();
		for (final Pom pom : pomManagementService.getPoms()) {
			final String nonEmptyModuleName = StringUtils.defaultIfEmpty(pom.getModuleName(), ROOT_MODULE_SYMBOL);
			if (!nonEmptyModuleName.equals(focusedModuleName)) {
				completions.add(new Completion(nonEmptyModuleName));
			}
		}
		return false;
	}
}
