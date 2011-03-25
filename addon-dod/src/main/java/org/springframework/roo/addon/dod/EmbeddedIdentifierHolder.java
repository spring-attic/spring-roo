package org.springframework.roo.addon.dod;

import java.util.List;

import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Holder for embedded identifier attributes
 * 
 * @author Alan Stewart
 * @since 1.1.3
 */
public class EmbeddedIdentifierHolder {
	private FieldMetadata embeddedIdentifierField;
	private List<FieldMetadata> identifierFields;
	private ConstructorMetadata identifierConstructor;

	public EmbeddedIdentifierHolder(FieldMetadata embeddedIdentifierField, List<FieldMetadata> identifierFields, ConstructorMetadata identifierConstructor) {
		Assert.notNull(embeddedIdentifierField, "Identifier type required");
		Assert.notNull(identifierFields, "Fields for " + embeddedIdentifierField.getFieldType().getFullyQualifiedTypeName() + " required");
		Assert.isTrue(identifierConstructor != null && identifierConstructor.getParameterNames().size() == identifierFields.size(), "Constructor for " + embeddedIdentifierField.getFieldType().getFullyQualifiedTypeName() + " required and must have the same number of parameters as the number of identifier fields");
		this.embeddedIdentifierField = embeddedIdentifierField;
		this.identifierFields = identifierFields;
		this.identifierConstructor = identifierConstructor;
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

	public ConstructorMetadata getIdentifierConstructor() {
		return identifierConstructor;
	}
}
