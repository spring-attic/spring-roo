package org.springframework.roo.addon.dod;

import java.util.List;

import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Holder for embedded identifier attributes
 * 
 * @author Alan Stewart
 * @since 1.1.3
 */
public class EmbeddedIdentifierMetadataHolder {
	private JavaType identifierType;
	private List<FieldMetadata> identifierFields;
	private ConstructorMetadata identifierConstructor;

	public EmbeddedIdentifierMetadataHolder(JavaType identifierType, List<FieldMetadata> identifierFields, ConstructorMetadata identifierConstructor) {
		Assert.notNull(identifierType, "Identifier type required");
		Assert.notNull(identifierFields, "Identifier fields for " + identifierType.getFullyQualifiedTypeName() + " required");
		Assert.isTrue(identifierConstructor != null && identifierConstructor.getParameterNames().size() == identifierFields.size(), "Identifier constructor for " + identifierType.getFullyQualifiedTypeName() + " must have the same number of parameters as the number of identifier fields");
		this.identifierType = identifierType;
		this.identifierFields = identifierFields;
		this.identifierConstructor = identifierConstructor;
	}

	public JavaType getIdentifierType() {
		return identifierType;
	}

	public List<FieldMetadata> getIdentifierFields() {
		return identifierFields;
	}

	public ConstructorMetadata getIdentifierConstructor() {
		return identifierConstructor;
	}
}
