package org.springframework.roo.addon.dod;

import java.util.List;

import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Holder for embedded attributes
 * 
 * @author Greg Turnquist
 * @since 1.1.4
 */
public class EmbeddedHolder {
	private FieldMetadata embeddedField;
	private List<FieldMetadata> fields;
	private ConstructorMetadata constructor;

	public EmbeddedHolder(FieldMetadata embeddedField, List<FieldMetadata> fields, ConstructorMetadata constructor) {
		Assert.notNull(embeddedField, "Field type required");
		Assert.notNull(fields, "Fields for " + embeddedField.getFieldType().getFullyQualifiedTypeName() + " required");
		Assert.isTrue(constructor != null && constructor.getParameterNames().size() == fields.size(), "Constructor for " + embeddedField.getFieldType().getFullyQualifiedTypeName() + " required and must have the same number of parameters as the number of identifier fields");
		this.embeddedField = embeddedField;
		this.fields = fields;
		this.constructor = constructor;
	}

	public FieldMetadata getEmbeddedField() {
		return embeddedField;
	}

	public JavaSymbolName getEmbeddedMutator() {
		return new JavaSymbolName("set" + StringUtils.capitalize(embeddedField.getFieldName().getSymbolName()));
	}
	
	public List<FieldMetadata> getFields() {
		return fields;
	}

	public ConstructorMetadata getConstructor() {
		return constructor;
	}
}
