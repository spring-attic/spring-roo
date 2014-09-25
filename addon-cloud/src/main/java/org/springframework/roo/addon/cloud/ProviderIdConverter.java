package org.springframework.roo.addon.cloud;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.cloud.providers.CloudProviderId;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * 
 * Cloud Providers ID converter
 * 
 * @author Juan Carlos García del Canto
 * @since 1.2.6
 *
 */
@Component
@Service
public class ProviderIdConverter implements Converter<CloudProviderId> {

    @Reference
    private CloudOperations operations;

    protected void bindOperations(CloudOperations operations) {
        this.operations = operations;
    }

    protected void unbindOperations(CloudOperations operations) {
        this.operations = null;
    }

    @Override
    public CloudProviderId convertFromText(String value, Class<?> targetType,
            String optionContext) {
        return operations.getProviderIdByName(value);
    }

    @Override
    public boolean getAllPossibleValues(List<Completion> completions,
            Class<?> targetType, String existingData, String optionContext,
            MethodTarget target) {
        for (final CloudProviderId id : operations.getProvidersId()) {
            if (existingData.isEmpty() || id.getId().equals(existingData)
                    || id.getId().startsWith(existingData)) {
                completions.add(new Completion(id.getId()));
            }
        }
        return true;
    }

    @Override
    public boolean supports(Class<?> type, String optionContext) {
        return CloudProviderId.class.isAssignableFrom(type);
    }

}
