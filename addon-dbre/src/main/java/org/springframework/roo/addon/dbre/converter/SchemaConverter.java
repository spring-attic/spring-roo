package org.springframework.roo.addon.dbre.converter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.model.DbreModelService;
import org.springframework.roo.addon.dbre.model.Schema;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.support.util.StringUtils;

/**
 * Provides conversion to and from database schemas.
 *
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class SchemaConverter implements Converter<Set<Schema>> {

	// Fields
	@Reference private DbreModelService dbreModelService;

	public boolean supports(final Class<?> requiredType, final String optionContext) {
		return Set.class.isAssignableFrom(requiredType) && optionContext.contains("schema");
	}

	public Set<Schema> convertFromText(final String value, final Class<?> requiredType, final String optionContext) {
		Set<Schema> schemas = new HashSet<Schema>();
		for (String schemaName : StringUtils.delimitedListToStringArray(value, " ")) {
			schemas.add(new Schema(schemaName));
		}
		return schemas;
	}

	public boolean getAllPossibleValues(final List<Completion> completions, final Class<?> requiredType, final String existingData, final String optionContext, final MethodTarget target) {
		try {
			if (dbreModelService.supportsSchema(false)) {
				Set<Schema> schemas = dbreModelService.getSchemas(false);
				for (Schema schema : schemas) {
					completions.add(new Completion(schema.getName()));
				}
			} else {
				completions.add(new Completion(DbreModelService.NO_SCHEMA_REQUIRED));
			}
		} catch (Exception e) {
			completions.add(new Completion("unable-to-obtain-connection"));
		}

		return true;
	}
}
