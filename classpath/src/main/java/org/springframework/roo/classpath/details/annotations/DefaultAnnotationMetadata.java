package org.springframework.roo.classpath.details.annotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Default implementation of {@link AnnotationMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public final class DefaultAnnotationMetadata implements AnnotationMetadata {
	private JavaType annotationType;
	private List<AnnotationAttributeValue<?>> attributes;
	private Map<JavaSymbolName, AnnotationAttributeValue<?>> attributeMap = new HashMap<JavaSymbolName, AnnotationAttributeValue<?>>();
	
	// Package protected to enforce use of AnnotationMetadataBuilder
	DefaultAnnotationMetadata(JavaType annotationType, List<AnnotationAttributeValue<?>> attributes) {
		Assert.notNull(annotationType, "Annotation type required");
		Assert.notNull(attributes, "Attributes required");
		this.annotationType = annotationType;
		this.attributes = attributes;
		for (AnnotationAttributeValue<?> v : attributes) {
			this.attributeMap.put(v.getName(), v);
		}
	}
	
	public AnnotationAttributeValue<?> getAttribute(JavaSymbolName attributeName) {
		Assert.notNull(attributeName, "Attribute name required");
		return attributeMap.get(attributeName);
	}

	public List<JavaSymbolName> getAttributeNames() {
		List<JavaSymbolName> result = new ArrayList<JavaSymbolName>();
		for (AnnotationAttributeValue<?> value : attributes) {
			result.add(value.getName());
		}
		return result;
	}

	public JavaType getAnnotationType() {
		return annotationType;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("annotationType", annotationType);
		tsc.append("attributes", attributes);
		return tsc.toString();
	}
}
