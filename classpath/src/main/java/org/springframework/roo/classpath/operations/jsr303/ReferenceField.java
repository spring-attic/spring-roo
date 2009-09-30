package org.springframework.roo.classpath.operations.jsr303;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Properties used by the many-to-one side of a relationship (called a "reference").
 * 
 * <p>
 * For example, an Order-LineItem link would have the LineItem contain a "reference" back to Order.
 *  
 * <p>
 * Limited support for collection mapping is provided. This reflects the pragmatic goals of ROO and the fact a user can
 * edit the generated files by hand anyway.
 * 
 * <p>
 * This field is intended for use with JSR 220 and will create a @ManyToOne and @JoinColumn annotation.
 *
 * @author Ben Alex
 * @since 1.0
 *
 */
public class ReferenceField extends FieldDetails {
	
	private JavaType fieldType;
	private String joinColumnName;

	public ReferenceField(String physicalTypeIdentifier, JavaType fieldType, JavaSymbolName fieldName) {		
		super(physicalTypeIdentifier, fieldType, fieldName);
		this.fieldType = fieldType;
	}

	public String getJoinColumnName() {
		return joinColumnName;
	}

	public void setJoinColumnName(String joinColumnName) {
		this.joinColumnName = joinColumnName;
	}

	public void decorateAnnotationsList(List<AnnotationMetadata> annotations) {
		super.decorateAnnotationsList(annotations);
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new ClassAttributeValue(new JavaSymbolName("targetEntity"), fieldType));
		annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.ManyToOne"), attributes));
		
		List<AnnotationAttributeValue<?>> jcAttrs = new ArrayList<AnnotationAttributeValue<?>>();
		if (joinColumnName != null) {
			jcAttrs.add(new StringAttributeValue(new JavaSymbolName("name"), joinColumnName));
		}
		annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.JoinColumn"), jcAttrs));
		
	}

}
