package org.springframework.roo.addon.cloud.foundry.converter;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

@Service
@Component
public class StringListConverter implements Converter<List<String>> {

    public List<String> convertFromText(final String value,
            final Class<?> requiredType, final String optionContext) {
        if (value == null) {
            return null;
        }
        final String[] tokens = value.split(",");
        final List<String> tokenList = new ArrayList<String>();
        for (final String token : tokens) {
            tokenList.add(token.trim());
        }
        return tokenList;
    }

    public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> requiredType, final String existingData,
            final String optionContext, final MethodTarget target) {
        return false;
    }

    public boolean supports(final Class<?> requiredType,
            final String optionContext) {
        return List.class.isAssignableFrom(requiredType);
    }
}
