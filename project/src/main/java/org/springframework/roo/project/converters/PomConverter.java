package org.springframework.roo.project.converters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.PomManagementService;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

@Component
@Service
public class PomConverter implements Converter<Pom>{

	// Fields
	@Reference private PomManagementService pomManagementService;

	public boolean supports(final Class<?> type, final String optionContext) {
		return Pom.class.isAssignableFrom(type);
	}

	public Pom convertFromText(String value, final Class<?> targetType, final String optionContext) {
		if ("~".equals(value)) {
			value = "";
		}
		final Map<String, Pom> abbreviationMap = new HashMap<String, Pom>();
		for (Pom pom : pomManagementService.getPomMap().values()) {
			abbreviationMap.put(pom.getModuleName(), pom);
		}
		return abbreviationMap.get(value);
	}

	public boolean getAllPossibleValues(final List<Completion> completions, final Class<?> targetType, final String existingData, final String optionContext, final MethodTarget target) {
		Pom focusedModule = pomManagementService.getFocusedModule();
		if (focusedModule == null) {
			return false;
		}
		String focusedModuleName = focusedModule.getModuleName();
		for (Pom pom : pomManagementService.getPomMap().values()) {
			String moduleName = pom.getModuleName();
			if (moduleName.equals("")) {
				moduleName = "~";
			}
			if (!moduleName.equals(focusedModuleName)) {
				completions.add(new Completion(moduleName));
			}
		}
		return false;
	}
}
