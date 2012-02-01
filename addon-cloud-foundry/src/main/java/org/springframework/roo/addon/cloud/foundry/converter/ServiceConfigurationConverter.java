package org.springframework.roo.addon.cloud.foundry.converter;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.cloud.foundry.CloudFoundrySession;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

import com.vmware.appcloud.client.ServiceConfiguration;

/**
 * Provides conversion to and from Cloud Foundry model classes.
 * 
 * @author James Tyrrell
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class ServiceConfigurationConverter implements
        Converter<ServiceConfiguration> {
    @Reference private CloudFoundrySession session;

    public ServiceConfiguration convertFromText(final String value,
            final Class<?> requiredType, final String optionContext) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return session.getService(value);
    }

    public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> requiredType, final String existingData,
            final String optionContext, final MethodTarget target) {
        for (final String serviceType : session.getServiceTypes()) {
            completions.add(new Completion(serviceType));
        }
        return false;
    }

    public boolean supports(final Class<?> requiredType,
            final String optionContext) {
        return ServiceConfiguration.class.isAssignableFrom(requiredType);
    }
}
