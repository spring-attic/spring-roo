package org.springframework.roo.classpath.operations.jsr303;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Base class containing common JSR 303 and JSR 220 properties that can be auto-generated.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public abstract class FieldDetails {
	/** The JPA @Column value */
	private String column = null;
	
	/** The type that will receive the field */
	private String physicalTypeIdentifier;
	
	/** The type of field to be added */
	private JavaType fieldType;
	
	/** The name of the field to be added */
	private JavaSymbolName fieldName;
	
	/** Whether the JSR 303 @NotNull annotation will be added */
	private boolean notNull = false;
	
	/** Whether the JSR 303 @Null annotation will be added */
	private boolean nullRequired = false;
	
	/** Any JavaDoc comments (reserved for future expansion) */
	protected String comment = "";
	
	public FieldDetails(String physicalTypeIdentifier, JavaType fieldType, JavaSymbolName fieldName) {
		Assert.isTrue(PhysicalTypeIdentifier.isValid(physicalTypeIdentifier), "Destination physical type identifier is invalid");
		Assert.notNull(fieldType, "Field type required");
		Assert.notNull(fieldName, "Field name required");
		this.physicalTypeIdentifier = physicalTypeIdentifier;
		this.fieldType = fieldType;
		this.fieldName = fieldName;
	}

	public void decorateAnnotationsList(List<AnnotationMetadata> annotations) {
		Assert.notNull(annotations);
		if (notNull) {
			annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.validation.constraints.NotNull"), new ArrayList<AnnotationAttributeValue<?>>()));
		}
		if (nullRequired) {
			annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.validation.constraints.Null"), new ArrayList<AnnotationAttributeValue<?>>()));
		}
		if (column != null) {
			List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
			attrs.add(new StringAttributeValue(new JavaSymbolName("name"), column));
			annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.Column"), attrs));
		}
	}
	
	public boolean isNotNull() {
		return notNull;
	}

	public void setNotNull(boolean notNull) {
		this.notNull = notNull;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		if (comment != null) {
			this.comment = comment;
		}
	}

	public String getColumn() {
		return column;
	}

	public void setColumn(String column) {
		this.column = column;
	}

	public boolean isNullRequired() {
		return nullRequired;
	}

	public void setNullRequired(boolean nullRequired) {
		this.nullRequired = nullRequired;
	}

	public String getPhysicalTypeIdentifier() {
		return physicalTypeIdentifier;
	}

	public JavaType getFieldType() {
		return fieldType;
	}

	public JavaSymbolName getFieldName() {
		return fieldName;
	}

}
