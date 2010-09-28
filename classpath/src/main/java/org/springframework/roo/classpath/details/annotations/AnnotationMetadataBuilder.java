package org.springframework.roo.classpath.details.annotations;

import java.util.ArrayList;
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
 * The "add" method will replace any existing annotation attribute with the same name, taking
 * care to preserve its location. 
 * 
 * @author Ben Alex
 * @since 1.1
 */
public final class AnnotationMetadataBuilder implements Builder<AnnotationMetadata> {
	private JavaType annotationType;
	private List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
	
	public AnnotationMetadataBuilder() {
	}

	public AnnotationMetadataBuilder(AnnotationMetadata existing) {
		Assert.notNull(existing);
		this.annotationType = existing.getAnnotationType();
		for (JavaSymbolName attributeName : existing.getAttributeNames()) {
			attributes.add(existing.getAttribute(attributeName));
		}
	}

	public AnnotationMetadataBuilder(JavaType annotationType) {
		this.annotationType = annotationType;
	}

	public AnnotationMetadataBuilder(JavaType annotationType, List<AnnotationAttributeValue<?>> attributes) {
		this.annotationType = annotationType;
		this.attributes = attributes;
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
		for (AnnotationAttributeValue<?> element : attributes) {
			index++;
			if (element.getName().equals(value.getName())) {
				// Match found
				foundAt = index;
				break;
			}
		}
		if (foundAt == -1) {
			// Not found
			attributes.add(value);
		} else {
			// Found
			// Remove the existing element
			attributes.remove(foundAt);
			// Put this element in its place
			attributes.add(foundAt, value);
		}
	}

	public JavaType getAnnotationType() {
		return annotationType;
	}

	public void setAnnotationType(JavaType annotationType) {
		this.annotationType = annotationType;
	}

	public List<AnnotationAttributeValue<?>> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<AnnotationAttributeValue<?>> attributes) {
		this.attributes = attributes;
	}

	public AnnotationMetadata build() {
		return new DefaultAnnotationMetadata(getAnnotationType(), getAttributes());
	}
}
