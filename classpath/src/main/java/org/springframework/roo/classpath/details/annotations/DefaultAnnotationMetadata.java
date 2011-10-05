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
public class DefaultAnnotationMetadata implements AnnotationMetadata {

	// Fields
	private final JavaType annotationType;
	private final List<AnnotationAttributeValue<?>> attributes;
	private final Map<JavaSymbolName, AnnotationAttributeValue<?>> attributeMap;

	/**
	 * Constructor
	 *
	 * @param annotationType the type of annotation for which these are the
	 * metadata (required)
	 * @param attributeValues the given annotation's values; can be <code>null</code>
	 */
	DefaultAnnotationMetadata(final JavaType annotationType, final List<AnnotationAttributeValue<?>> attributeValues) {
		Assert.notNull(annotationType, "Annotation type required");
		this.annotationType = annotationType;
		this.attributes = new ArrayList<AnnotationAttributeValue<?>>();
		this.attributeMap = new HashMap<JavaSymbolName, AnnotationAttributeValue<?>>();
		if (attributeValues!= null) {
			this.attributes.addAll(attributeValues);
			for (final AnnotationAttributeValue<?> value : attributeValues) {
				this.attributeMap.put(value.getName(), value);
			}
		}
	}

	public AnnotationAttributeValue<?> getAttribute(final JavaSymbolName attributeName) {
		Assert.notNull(attributeName, "Attribute name required");
		return attributeMap.get(attributeName);
	}

	@SuppressWarnings("unchecked")
	public AnnotationAttributeValue<?> getAttribute(final String attributeName) {
		return getAttribute(new JavaSymbolName(attributeName));
	}

	public List<JavaSymbolName> getAttributeNames() {
		final List<JavaSymbolName> result = new ArrayList<JavaSymbolName>();
		for (AnnotationAttributeValue<?> value : attributes) {
			result.add(value.getName());
		}
		return result;
	}

	public JavaType getAnnotationType() {
		return annotationType;
	}

	@Override
	public String toString() {
		final ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("annotationType", annotationType);
		tsc.append("attributes", attributes);
		return tsc.toString();
	}
}
