package org.springframework.roo.addon.dod;

import java.util.List;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Holder for embedded identifier attributes
 * 
 * @author Alan Stewart
 * @author Greg Turnquist
 * @since 1.1.3
 */
public class EmbeddedIdentifierHolder {
	private FieldMetadata embeddedIdentifierField;
	private List<FieldMetadata> identifierFields;

	public EmbeddedIdentifierHolder(FieldMetadata embeddedIdentifierField, List<FieldMetadata> identifierFields) {
		Assert.notNull(embeddedIdentifierField, "Identifier type required");
		Assert.notNull(identifierFields, "Fields for " + embeddedIdentifierField.getFieldType().getFullyQualifiedTypeName() + " required");
		this.embeddedIdentifierField = embeddedIdentifierField;
		this.identifierFields = identifierFields;
	}

	public FieldMetadata getEmbeddedIdentifierField() {
		return embeddedIdentifierField;
	}

	public JavaSymbolName getEmbeddedIdentifierMutator() {
		return new JavaSymbolName("set" + StringUtils.capitalize(embeddedIdentifierField.getFieldName().getSymbolName()));
	}
	
	public List<FieldMetadata> getIdentifierFields() {
		return identifierFields;
	}

}
