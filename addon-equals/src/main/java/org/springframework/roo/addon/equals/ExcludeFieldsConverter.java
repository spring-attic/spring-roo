package org.springframework.roo.addon.equals;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * Provides conversion between a space-separated list of field names to a set of field names.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component
@Service
public class ExcludeFieldsConverter implements Converter<Set<String>> {

	public boolean supports(final Class<?> requiredType, final String optionContext) {
		return Set.class.isAssignableFrom(requiredType) && optionContext.contains("exclude-fields");
	}

	public Set<String> convertFromText(final String value, final Class<?> requiredType, final String optionContext) {
		Set<String> fields = new LinkedHashSet<String>();
		StringTokenizer st = new StringTokenizer(value, " ");
		while (st.hasMoreTokens()) {
			fields.add(st.nextToken());
		}
		return fields;
	}

	public boolean getAllPossibleValues(final List<String> completions, final Class<?> requiredType, final String existingData, final String optionContext, final MethodTarget target) {
		return false;
	}
}
