package org.springframework.roo.addon.dbre;

import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.model.Schema;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * Provides conversion to and from database schemas.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DatabaseSchemaConverter implements Converter {
	@Reference private DatabaseModelService databaseModelService;

	public boolean supports(Class<?> requiredType, String optionContext) {
		return Schema.class.isAssignableFrom(requiredType);
	}

	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		return new Schema(value);
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		Set<Schema> schemas = databaseModelService.getDatabaseSchemas();
		for (Schema schema : schemas) {
			completions.add(schema.getName());
		}

		return true;
	}
}
