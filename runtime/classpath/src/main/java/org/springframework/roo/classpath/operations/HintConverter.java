package org.springframework.roo.classpath.operations;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;

/**
 * {@link Converter} for {@link String} that understands the "topics" option
 * context.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Service
@Component
public class HintConverter implements Converter<String> {

    @Reference private HintOperations hintOperations;

    public String convertFromText(final String value,
            final Class<?> requiredType, final String optionContext) {
        return value;
    }

    public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> requiredType, final String existingData,
            final String optionContext, final MethodTarget target) {
        for (final String currentTopic : hintOperations.getCurrentTopics()) {
            completions.add(new Completion(currentTopic));
        }
        return false;
    }

    public boolean supports(final Class<?> requiredType,
            final String optionContext) {
        return String.class.isAssignableFrom(requiredType)
                && optionContext.contains("topics");
    }
}
