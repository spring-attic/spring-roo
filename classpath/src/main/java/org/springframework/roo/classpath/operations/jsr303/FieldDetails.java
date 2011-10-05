package org.springframework.roo.classpath.operations.jsr303;

import static org.springframework.roo.model.JpaJavaType.COLUMN;
import static org.springframework.roo.model.Jsr303JavaType.NOT_NULL;
import static org.springframework.roo.model.Jsr303JavaType.NULL;
import static org.springframework.roo.model.SpringJavaType.VALUE;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Base class containing common JSR 303 and JSR 220 properties that can be auto-generated.
 *
 * @author Ben Alex
 * @since 1.0
 */
public class FieldDetails {

	// Fields
	/** The JPA @Column value */
	private String column;

	/** The type that will receive the field */
	private final String physicalTypeIdentifier;

	/** The type of field to be added */
	private final JavaType fieldType;

	/** The name of the field to be added */
	private final JavaSymbolName fieldName;

	/** Whether the JSR 303 @NotNull annotation will be added */
	private boolean notNull;

	/** Whether the JSR 303 @Null annotation will be added */
	private boolean nullRequired;

	/** Any JavaDoc comments (reserved for future expansion) */
	protected String comment = "";

	/** Whether unique = true is added to the @Column annotation */
	private boolean unique;

	/** The Spring @Value value **/
	private String value;

	/**
	 * Constructor
	 *
	 * @param physicalTypeIdentifier
	 * @param fieldType
	 * @param fieldName
	 */
	public FieldDetails(final String physicalTypeIdentifier, final JavaType fieldType, final JavaSymbolName fieldName) {
		Assert.isTrue(PhysicalTypeIdentifier.isValid(physicalTypeIdentifier), "Destination physical type identifier is invalid");
		Assert.notNull(fieldType, "Field type required");
		Assert.notNull(fieldName, "Field name required");
		this.physicalTypeIdentifier = physicalTypeIdentifier;
		this.fieldType = fieldType;
		this.fieldName = fieldName;
	}

	public void decorateAnnotationsList(final List<AnnotationMetadataBuilder> annotations) {
		Assert.notNull(annotations);

		if (notNull) {
			annotations.add(new AnnotationMetadataBuilder(NOT_NULL));
		}

		if (nullRequired) {
			annotations.add(new AnnotationMetadataBuilder(NULL));
		}

		AnnotationMetadataBuilder columnBuilder = null;
		if (column != null) {
			List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
			attrs.add(new StringAttributeValue(new JavaSymbolName("name"), column));
			columnBuilder = new AnnotationMetadataBuilder(COLUMN, attrs);
		}
		if (unique) {
			if (columnBuilder != null) {
				columnBuilder.addBooleanAttribute("unique", true);
			} else {
				List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
				attrs.add(new BooleanAttributeValue(new JavaSymbolName("unique"), true));
				columnBuilder = new AnnotationMetadataBuilder(COLUMN, attrs);
			}
		}
		if (columnBuilder != null) {
			annotations.add(columnBuilder);
		}

		if (value != null) {
			List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
			attrs.add(new StringAttributeValue(new JavaSymbolName("value"), value));
			annotations.add(new AnnotationMetadataBuilder(VALUE, attrs));
		}
	}

	public boolean isNotNull() {
		return notNull;
	}

	public void setNotNull(final boolean notNull) {
		this.notNull = notNull;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(final String comment) {
		if (comment != null) {
			this.comment = comment;
		}
	}

	public String getColumn() {
		return column;
	}

	public void setColumn(final String column) {
		this.column = column;
	}

	public boolean isNullRequired() {
		return nullRequired;
	}

	public void setNullRequired(final boolean nullRequired) {
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

	public boolean isUnique() {
		return unique;
	}

	public void setUnique(final boolean unique) {
		this.unique = unique;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
