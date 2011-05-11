package org.springframework.roo.addon.dbre.converter;

import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.model.DbreModelService;
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
public class SchemaConverter implements Converter {
	@Reference private DbreModelService dbreModelService;

	public boolean supports(Class<?> requiredType, String optionContext) {
		return Schema.class.isAssignableFrom(requiredType);
	}

	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		return new Schema(value);
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String existingData, String optionContext, MethodTarget target) {
		try {
			if (dbreModelService.supportsSchema(false)) {
				Set<Schema> schemas = dbreModelService.getSchemas(false);
				for (Schema schema : schemas) {
					completions.add(schema.getName());
				}
			} else {
				completions.add(dbreModelService.getNoSchemaString());
			}
		} catch (Exception e) {
			completions.add("unable-to-obtain-connection");
		}

		return true;
	}
}
