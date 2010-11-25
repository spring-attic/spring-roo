package org.springframework.roo.addon.dbre.converters;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * Provides conversion between a space-separated list of table names to a set of table names.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class IncludeExcludeTablesConverter implements Converter {

	public boolean supports(Class<?> requiredType, String optionContext) {
		return Set.class.isAssignableFrom(requiredType) && (optionContext.contains("include-tables") || optionContext.contains("exclude-tables"));
	}

	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		Set<String> tables = new LinkedHashSet<String>();
		StringTokenizer st = new StringTokenizer(value, " ");
		while (st.hasMoreTokens()) {
			tables.add(st.nextToken());
		}
		return tables;
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		return false;
	}
}
