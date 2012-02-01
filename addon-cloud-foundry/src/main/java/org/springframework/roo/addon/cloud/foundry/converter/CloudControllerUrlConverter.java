package org.springframework.roo.addon.cloud.foundry.converter;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.cloud.foundry.CloudFoundrySession;
import org.springframework.roo.addon.cloud.foundry.model.CloudControllerUrl;
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
public class CloudControllerUrlConverter implements
        Converter<CloudControllerUrl> {
    @Reference private CloudFoundrySession session;

    public CloudControllerUrl convertFromText(final String value,
            final Class<?> requiredType, final String optionContext) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return new CloudControllerUrl(value);
    }

    public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> requiredType, final String existingData,
            final String optionContext, final MethodTarget target) {
        for (final String storedUrl : session.getStoredUrls()) {
            completions.add(new Completion(storedUrl));
        }
        return false;
    }

    public boolean supports(final Class<?> requiredType,
            final String optionContext) {
        return CloudControllerUrl.class.isAssignableFrom(requiredType);
    }
}
