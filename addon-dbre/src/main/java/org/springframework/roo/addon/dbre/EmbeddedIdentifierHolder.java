package org.springframework.roo.addon.dbre;

import java.util.List;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.support.util.Assert;

/**
 * Holder for embedded identifier attributes
 * 
 * @author Alan Stewart
 * @since 1.1.3
 */
public class EmbeddedIdentifierHolder {
	private FieldMetadata embeddedIdentifierField;
	private List<FieldMetadata> identifierFields;

	public EmbeddedIdentifierHolder(FieldMetadata embeddedIdentifierField, List<FieldMetadata> identifierFields) {
		Assert.notNull(embeddedIdentifierField, "Embedded identifier field required");
		Assert.notNull(identifierFields, "Fields for " + embeddedIdentifierField.getFieldType().getFullyQualifiedTypeName() + " required");
		this.embeddedIdentifierField = embeddedIdentifierField;
		this.identifierFields = identifierFields;
	}

	public FieldMetadata getEmbeddedIdentifierField() {
		return embeddedIdentifierField;
	}

	public void setEmbeddedIdentifierField(FieldMetadata embeddedIdentifierField) {
		this.embeddedIdentifierField = embeddedIdentifierField;
	}

	public List<FieldMetadata> getIdentifierFields() {
		return identifierFields;
	}

	public void setIdentifierFields(List<FieldMetadata> identifierFields) {
		this.identifierFields = identifierFields;
	}
}
