package org.springframework.roo.addon.cloud.foundry.converter;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.cloud.foundry.CloudFoundrySession;
import org.springframework.roo.addon.cloud.foundry.model.CloudUri;
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
public class CloudUriConverter implements Converter<CloudUri> {
	@Reference private CloudFoundrySession session;

	public CloudUri convertFromText(String value, Class<?> requiredType, String optionContext) {
		if (value == null || "".equals(value)) {
			return null;
		}
		return new CloudUri(value);
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return CloudUri.class.isAssignableFrom(requiredType);
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		final String appName = ConverterUtils.getOptionValue("appName", target.remainingBuffer);
		final List<String> uris = session.getBoundUrlMap().get(appName);
		if (uris != null) {
			completions.addAll(uris);
		}
		return false;
	}
}
