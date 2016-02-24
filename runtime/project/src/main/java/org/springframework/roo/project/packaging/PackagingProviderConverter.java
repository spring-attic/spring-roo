package org.springframework.roo.project.packaging;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * A {@link Converter} for {@link PackagingProvider}s
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class PackagingProviderConverter implements Converter<PackagingProvider> {

    @Reference PackagingProviderRegistry packagingProviderRegistry;

    public PackagingProvider convertFromText(final String value,
            final Class<?> targetType, final String optionContext) {
        final PackagingProvider packagingProvider = packagingProviderRegistry
                .getPackagingProvider(value);
        Validate.notNull(packagingProvider, "Unsupported packaging id '%s'",
                value);
        return packagingProvider;
    }

    public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> targetType, final String existingData,
            final String optionContext, final MethodTarget target) {
        for (final PackagingProvider packagingProvider : packagingProviderRegistry
                .getAllPackagingProviders()) {
            completions.add(new Completion(packagingProvider.getId()
                    .toUpperCase()));
        }
        return true;
    }

    public boolean supports(final Class<?> type, final String optionContext) {
        return PackagingProvider.class.isAssignableFrom(type);
    }
}