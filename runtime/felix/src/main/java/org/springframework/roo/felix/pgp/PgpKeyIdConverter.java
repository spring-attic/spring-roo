package org.springframework.roo.felix.pgp;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link PgpKeyId}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class PgpKeyIdConverter implements Converter<PgpKeyId> {

    @Reference private PgpService pgpService;

    public PgpKeyId convertFromText(final String value,
            final Class<?> requiredType, final String optionContext) {
        return new PgpKeyId(value.trim());
    }

    public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> requiredType, final String originalUserInput,
            final String optionContext, final MethodTarget target) {
        for (final PgpKeyId candidate : pgpService.getDiscoveredKeyIds()) {
            final String id = candidate.getId();
            if (id.toUpperCase().startsWith(originalUserInput.toUpperCase())) {
                completions.add(new Completion(id));
            }
        }

        return false;
    }

    public boolean supports(final Class<?> requiredType,
            final String optionContext) {
        return PgpKeyId.class.isAssignableFrom(requiredType);
    }
}