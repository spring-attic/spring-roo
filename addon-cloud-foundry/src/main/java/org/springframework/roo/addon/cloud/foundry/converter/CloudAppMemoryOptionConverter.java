package org.springframework.roo.addon.cloud.foundry.converter;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.cloud.foundry.CloudFoundrySession;
import org.springframework.roo.addon.cloud.foundry.model.CloudAppMemoryOption;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * Provides conversion to and from Cloud Foundry model classes.
 *
 * @author James Tyrrell
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class CloudAppMemoryOptionConverter implements Converter<CloudAppMemoryOption> {
	private static final String MEMORY_OPTION_SUFFIX = "MB";
	@Reference private CloudFoundrySession session;

	public CloudAppMemoryOption convertFromText(final String value, final Class<?> requiredType, final String optionContext) {
		if (value == null || "".equals(value)) {
			return null;
		}
		return new CloudAppMemoryOption(Integer.valueOf(value.replaceAll(MEMORY_OPTION_SUFFIX, "")));
	}

	public boolean supports(final Class<?> requiredType, final String optionContext) {
		return CloudAppMemoryOption.class.isAssignableFrom(requiredType);
	}

	public boolean getAllPossibleValues(final List<Completion> completions, final Class<?> requiredType, final String existingData, final String optionContext, final MethodTarget target) {
		for (Integer memoryOption : session.getApplicationMemoryOptions()) {
			completions.add(new Completion(memoryOption + MEMORY_OPTION_SUFFIX));
		}
		return false;
	}
}
