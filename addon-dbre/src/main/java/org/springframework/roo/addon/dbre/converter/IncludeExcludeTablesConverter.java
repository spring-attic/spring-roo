package org.springframework.roo.addon.dbre.converter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * Provides conversion between a space-separated list of table names to a set of
 * table names.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class IncludeExcludeTablesConverter implements Converter<Set<String>> {

    public Set<String> convertFromText(final String value,
            final Class<?> requiredType, final String optionContext) {
        final Set<String> tables = new LinkedHashSet<String>();
        final StringTokenizer st = new StringTokenizer(value, " ");
        while (st.hasMoreTokens()) {
            tables.add(st.nextToken());
        }
        return tables;
    }

    public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> requiredType, final String existingData,
            final String optionContext, final MethodTarget target) {
        return false;
    }

    public boolean supports(final Class<?> requiredType,
            final String optionContext) {
        return Set.class.isAssignableFrom(requiredType)
                && (optionContext.contains("include-tables") || optionContext
                        .contains("exclude-tables"));
    }
}
