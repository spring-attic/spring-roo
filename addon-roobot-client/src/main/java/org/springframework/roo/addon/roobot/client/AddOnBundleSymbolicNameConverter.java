package org.springframework.roo.addon.roobot.client;

import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.roobot.client.model.Bundle;
import org.springframework.roo.addon.roobot.client.model.BundleVersion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link AddOnBundleSymbolicName}.
 *
 * @author Stefan Schmidt
 * @since 1.1
 *
 */
@Component
@Service
public class AddOnBundleSymbolicNameConverter implements Converter {

	private @Reference AddOnRooBotOperations addonManagerOperations;
	
	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		return new AddOnBundleSymbolicName(value.trim());
	}
	
	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String originalUserInput, String optionContext, MethodTarget target) {
		Map<String, Bundle> bundles = addonManagerOperations.getAddOnCache(false);
		for (String bsn : bundles.keySet()) {
			Bundle bundle = bundles.get(bsn);
			if (bundle.getVersions().size() > 1) {
				for (BundleVersion bundleVersion : bundle.getVersions()) {
					completions.add(bsn + ";" + bundleVersion.getVersion());
				}
			} 
			completions.add(bsn);
		}
		return false;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return AddOnBundleSymbolicName.class.isAssignableFrom(requiredType);
	}
}