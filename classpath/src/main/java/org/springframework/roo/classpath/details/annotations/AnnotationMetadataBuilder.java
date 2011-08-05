package org.springframework.roo.classpath.details.annotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.roo.model.Builder;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Builder for {@link AnnotationMetadata}.
 * 
 * <p>
 * The "add" method will replace any existing annotation attribute with the same
 * name, taking care to preserve its location. 
 * 
 * @author Ben Alex
 * @author Andrew Swan
 * @since 1.1
 */
public final class AnnotationMetadataBuilder implements Builder<AnnotationMetadata> {
	
	// Constants for valueless JPA annotations (using literal class names so as not to make Roo depend on JPA)
	public static final AnnotationMetadata JPA_COLUMN_ANNOTATION = getInstance("javax.persistence.Column");
	public static final AnnotationMetadata JPA_EMBEDDED_ANNOTATION = getInstance("javax.persistence.Embedded");
	public static final AnnotationMetadata JPA_EMBEDDED_ID_ANNOTATION = getInstance("javax.persistence.EmbeddedId");
	public static final AnnotationMetadata JPA_ENUMERATED_ANNOTATION = getInstance("javax.persistence.Enumerated");
	public static final AnnotationMetadata JPA_ID_ANNOTATION = getInstance("javax.persistence.Id");
	public static final AnnotationMetadata JPA_LOB_ANNOTATION = getInstance("javax.persistence.Lob");
	public static final AnnotationMetadata JPA_MANY_TO_MANY_ANNOTATION = getInstance("javax.persistence.ManyToMany");
	public static final AnnotationMetadata JPA_MANY_TO_ONE_ANNOTATION = getInstance("javax.persistence.ManyToOne");
	public static final AnnotationMetadata JPA_ONE_TO_MANY_ANNOTATION = getInstance("javax.persistence.OneToMany");
	public static final AnnotationMetadata JPA_ONE_TO_ONE_ANNOTATION = getInstance("javax.persistence.OneToOne");
	public static final AnnotationMetadata JPA_TRANSIENT_ANNOTATION = getInstance("javax.persistence.Transient");
	public static final AnnotationMetadata JPA_VERSION_ANNOTATION = getInstance("javax.persistence.Version");

	/**
	 * Returns the metadata for the existing annotation, with no attribute
	 * values
	 * 
	 * @param annotationType the fully-qualified name of the annotation type (required)
	 * @return a non-<code>null</code> instance
	 * @since 1.2
	 */
	public static AnnotationMetadata getInstance(final String annotationType) {
		return new AnnotationMetadataBuilder(annotationType).build();
	}
	
	/**
	 * Returns the metadata for the existing annotation, with no attribute
	 * values
	 * 
	 * @param annotationType the annotation type (required)
	 * @return a non-<code>null</code> instance
	 * @since 1.2
	 */
	public static AnnotationMetadata getInstance(final Class<?> annotationType) {
		return new AnnotationMetadataBuilder(annotationType).build();
	}
	
	// Fields
	private JavaType annotationType;
	private final List<AnnotationAttributeValue<?>> attributeValues = new ArrayList<AnnotationAttributeValue<?>>();
	
	/**
	 * Constructor. The caller must set the annotation type via
	 * {@link #setAnnotationType(JavaType)} before calling {@link #build()}
	 */
	public AnnotationMetadataBuilder() {
	}

	/**
	 * Constructor for using an existing {@link AnnotationMetadata} as a
	 * baseline for building a new instance.
	 *
	 * @param existing required
	 */
	public AnnotationMetadataBuilder(final AnnotationMetadata existing) {
		Assert.notNull(existing);
		this.annotationType = existing.getAnnotationType();
		for (JavaSymbolName attributeName : existing.getAttributeNames()) {
			attributeValues.add(existing.getAttribute(attributeName));
		}
	}

	/**
	 * Constructor for no initial attribute values
	 *
	 * @param annotationType the annotation class (required)
	 * @since 1.2
	 */
	public AnnotationMetadataBuilder(final Class<?> annotationType) {
		this(new JavaType(annotationType));
	}
	
	/**
	 * Constructor for no initial attribute values
	 *
	 * @param annotationType the fully-qualified name of the annotation type (required)
	 */
	public AnnotationMetadataBuilder(final String annotationType) {
		this(new JavaType(annotationType));
	}
	
	/**
	 * Constructor for no initial attribute values
	 *
	 * @param annotationType
	 */
	public AnnotationMetadataBuilder(final JavaType annotationType) {
		this.annotationType = annotationType;
	}

	/**
	 * Constructor that accepts an optional list of values
	 *
	 * @param annotationType
	 * @param attributeValues can be <code>null</code>
	 */
	public AnnotationMetadataBuilder(JavaType annotationType, Collection<AnnotationAttributeValue<?>> attributeValues) {
		this.annotationType = annotationType;
		if (attributeValues != null) {
			this.attributeValues.addAll(attributeValues);
		}
	}

	public void addBooleanAttribute(String key, boolean value) {
		addAttribute(new BooleanAttributeValue(new JavaSymbolName(key), value));
	}

	public void addCharAttribute(String key, char value) {
		addAttribute(new CharAttributeValue(new JavaSymbolName(key), value));
	}

	public void addClassAttribute(String key, String fullyQualifiedTypeName) {
		addAttribute(new ClassAttributeValue(new JavaSymbolName(key), new JavaType(fullyQualifiedTypeName)));
	}

	public void addClassAttribute(String key, JavaType javaType) {
		addAttribute(new ClassAttributeValue(new JavaSymbolName(key), javaType));
	}

	public void addDoubleAttribute(String key, double value, boolean floatingPrecisionOnly) {
		addAttribute(new DoubleAttributeValue(new JavaSymbolName(key), value, floatingPrecisionOnly));
	}

	public void addEnumAttribute(String key, String fullyQualifiedTypeName, String enumConstant) {
		EnumDetails details = new EnumDetails(new JavaType(fullyQualifiedTypeName), new JavaSymbolName(enumConstant));
		addAttribute(new EnumAttributeValue(new JavaSymbolName(key), details));
	}

	public void addEnumAttribute(String key, JavaType fullyQualifiedTypeName, String enumConstant) {
		EnumDetails details = new EnumDetails(fullyQualifiedTypeName, new JavaSymbolName(enumConstant));
		addAttribute(new EnumAttributeValue(new JavaSymbolName(key), details));
	}

	public void addEnumAttribute(String key, JavaType fullyQualifiedTypeName, JavaSymbolName enumConstant) {
		EnumDetails details = new EnumDetails(fullyQualifiedTypeName, enumConstant);
		addAttribute(new EnumAttributeValue(new JavaSymbolName(key), details));
	}

	public void addEnumAttribute(String key, EnumDetails details) {
		addAttribute(new EnumAttributeValue(new JavaSymbolName(key), details));
	}

	public void addIntegerAttribute(String key, int value) {
		addAttribute(new IntegerAttributeValue(new JavaSymbolName(key), value));
	}

	public void addLongAttribute(String key, long value) {
		addAttribute(new LongAttributeValue(new JavaSymbolName(key), value));
	}

	public void addStringAttribute(String key, String value) {
		addAttribute(new StringAttributeValue(new JavaSymbolName(key), value));
	}
	
	public void addAttribute(AnnotationAttributeValue<?> value) {
		// Locate existing attribute with this key and replace it
		int foundAt = -1;
		int index = -1;
		for (AnnotationAttributeValue<?> element : attributeValues) {
			index++;
			if (element.getName().equals(value.getName())) {
				// Match found
				foundAt = index;
				break;
			}
		}
		if (foundAt == -1) {
			// Not found
			attributeValues.add(value);
		} else {
			// Found
			// Remove the existing element
			attributeValues.remove(foundAt);
			// Put this element in its place
			attributeValues.add(foundAt, value);
		}
	}

	public JavaType getAnnotationType() {
		return annotationType;
	}

	public void setAnnotationType(JavaType annotationType) {
		this.annotationType = annotationType;
	}

	public List<AnnotationAttributeValue<?>> getAttributes() {
		return attributeValues;
	}

	/**
	 * Sets the attribute values
	 * 
	 * @param attributeValue's the values to set; can be <code>null</code> for none
	 */
	public void setAttributes(final Collection<AnnotationAttributeValue<?>> attributeValues) {
		this.attributeValues.clear();
		if (attributeValues != null) {
			this.attributeValues.addAll(attributeValues);
		}
	}

	public AnnotationMetadata build() {
		return new DefaultAnnotationMetadata(getAnnotationType(), getAttributes());
	}
}
