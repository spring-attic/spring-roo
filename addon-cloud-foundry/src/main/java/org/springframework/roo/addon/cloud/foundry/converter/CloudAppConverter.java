package org.springframework.roo.addon.cloud.foundry.converter;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.cloud.foundry.CloudFoundrySession;
import org.springframework.roo.addon.cloud.foundry.model.CloudApp;
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
public class CloudAppConverter implements Converter<CloudApp> {
	@Reference private CloudFoundrySession session;

	public CloudApp convertFromText(final String value, final Class<?> requiredType, final String optionContext) {
		if (value == null || "".equals(value)) {
			return null;
		}
		return new CloudApp(value);
	}

	public boolean supports(final Class<?> requiredType, final String optionContext) {
		return CloudApp.class.isAssignableFrom(requiredType);
	}

	public boolean getAllPossibleValues(final List<Completion> completions, final Class<?> requiredType, final String existingData, final String optionContext, final MethodTarget target) {
		for (String appName : session.getApplicationNames()) {
			completions.add(new Completion(appName));
		}
		return false;
	}
}
