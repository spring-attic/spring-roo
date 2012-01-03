package org.springframework.roo.classpath.converters;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.support.util.StringUtils;

/**
 * Provides conversion to and from {@link JavaSymbolName}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class JavaSymbolNameConverter implements Converter<JavaSymbolName> {

    public JavaSymbolName convertFromText(final String value,
            final Class<?> requiredType, final String optionContext) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        return new JavaSymbolName(value);
    }

    public boolean supports(final Class<?> requiredType,
            final String optionContext) {
        return JavaSymbolName.class.isAssignableFrom(requiredType);
    }

    public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> requiredType, final String existingData,
            final String optionContext, final MethodTarget target) {
        return false;
    }
}
