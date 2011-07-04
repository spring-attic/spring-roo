package org.springframework.roo.addon.cloud.foundry.converter;

import com.vmware.appcloud.client.CloudService;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.cloud.foundry.CloudFoundrySession;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

import java.util.List;

/**
 * Provides conversion to and from Cloud Foundry model classes.
 *
 * @author James Tyrrell
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class CloudServiceConverter implements Converter<CloudService> {
	@Reference
	private CloudFoundrySession session;

	public CloudService convertFromText(String value, Class<?> requiredType, String optionContext) {
		if (value == null || "".equals(value)) {
			return null;
		}
		return session.getProvisionedService(value);
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return CloudService.class.isAssignableFrom(requiredType);
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		completions.addAll(session.getProvisionedServices());
		return false;
	}
}
