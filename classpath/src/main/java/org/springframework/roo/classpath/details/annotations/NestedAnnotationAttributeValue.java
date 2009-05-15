package org.springframework.roo.classpath.details.annotations;

import org.springframework.roo.model.JavaSymbolName;


/**
 * Represents a nested annotation attribute value.
 * 
 * @author Ben Alex
 * @since 1.0
 * 
 */
public class NestedAnnotationAttributeValue extends AbstractAnnotationAttributeValue<AnnotationMetadata> {

	private AnnotationMetadata value;
	
	public NestedAnnotationAttributeValue(JavaSymbolName name, AnnotationMetadata value) {
		super(name);
		this.value = value;
	}

	public AnnotationMetadata getValue() {
		return value;
	}
	
	public String toString() {
		return getName() + " -> " + value.toString();
	}
}
