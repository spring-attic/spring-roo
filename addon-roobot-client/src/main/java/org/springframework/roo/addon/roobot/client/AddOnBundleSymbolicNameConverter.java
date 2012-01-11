package org.springframework.roo.addon.roobot.client;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.roobot.client.model.Bundle;
import org.springframework.roo.addon.roobot.client.model.BundleVersion;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link AddOnBundleSymbolicName}.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class AddOnBundleSymbolicNameConverter implements
        Converter<AddOnBundleSymbolicName> {

    @Reference private AddOnRooBotOperations addonManagerOperations;

    public AddOnBundleSymbolicName convertFromText(final String value,
            final Class<?> requiredType, final String optionContext) {
        return new AddOnBundleSymbolicName(value.trim());
    }

    public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> requiredType, final String originalUserInput,
            final String optionContext, final MethodTarget target) {
        final Map<String, Bundle> bundles = addonManagerOperations
                .getAddOnCache(false);
        for (final Entry<String, Bundle> entry : bundles.entrySet()) {
            final String bsn = entry.getKey();
            final Bundle bundle = entry.getValue();
            if (bundle.getVersions().size() > 1) {
                for (final BundleVersion bundleVersion : bundle.getVersions()) {
                    completions.add(new Completion(bsn + ";"
                            + bundleVersion.getVersion()));
                }
            }
            completions.add(new Completion(bsn));
        }
        return false;
    }

    public boolean supports(final Class<?> requiredType,
            final String optionContext) {
        return AddOnBundleSymbolicName.class.isAssignableFrom(requiredType);
    }
}