package org.springframework.roo.addon.dod;

import java.util.List;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.Assert;

/**
 * Holder for embedded attributes
 * 
 * @author Greg Turnquist
 * @since 1.2.0
 */
public class EmbeddedHolder {
	private FieldMetadata embeddedField;
	private List<FieldMetadata> fields;

	public EmbeddedHolder(FieldMetadata embeddedField, List<FieldMetadata> fields) {
		Assert.notNull(embeddedField, "Identifier type required");
		Assert.notNull(fields, "Fields for " + embeddedField.getFieldType().getFullyQualifiedTypeName() + " required");
		this.embeddedField = embeddedField;
		this.fields = fields;
	}
	public FieldMetadata getEmbeddedField() {
		return embeddedField;
	}

	public JavaSymbolName getEmbeddedMutatorMethodName() {
		return new JavaSymbolName(embeddedField.getFieldName().getSymbolNameTurnedIntoMutatorMethodName());
	}
	
	public List<FieldMetadata> getFields() {
		return fields;
	}
}
