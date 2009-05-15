package org.springframework.roo.classpath.operations.jsr303;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Properties used by the one side of a many-to-one relationship (called a "set").
 * 
 * <p>
 * For example, an Order-LineItem link would have the Order contain a "set" of Orders.
 *  
 * <p>
 * Limited support for collection mapping is provided. This reflects the pragmatic goals of the tool and the fact a user can
 * edit the generated files by hand anyway.
 * 
 * <p>
 * This field is intended for use with JSR 220 and will create a @OneToMany annotation.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class SetField extends CollectionField {
	
	/** Whether the JSR 220 @OneToMany.mappedBy annotation attribute will be added */
	private JavaSymbolName mappedBy = null;
	
	public SetField(String physicalTypeIdentifier, JavaType fieldType, JavaSymbolName fieldName, JavaType genericParameterTypeName) {
		super(physicalTypeIdentifier, fieldType, fieldName, genericParameterTypeName);
	}

	public void decorateAnnotationsList(List<AnnotationMetadata> annotations) {
		super.decorateAnnotationsList(annotations);
		List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
		attrs.add(new EnumAttributeValue(new JavaSymbolName("cascade"), new EnumDetails(new JavaType("javax.persistence.CascadeType"), new JavaSymbolName("ALL"))));
		if (mappedBy != null) {
			attrs.add(new StringAttributeValue(new JavaSymbolName("mappedBy"), mappedBy.getSymbolName()));
		}
		annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.OneToMany"), attrs));
	}

	public JavaType getInitializer() {
		List<JavaType> params = new ArrayList<JavaType>();
		params.add(getGenericParameterTypeName());
		return new JavaType("java.util.HashSet", false, false, params);
	}

	public JavaSymbolName getMappedBy() {
		return mappedBy;
	}

	public void setMappedBy(JavaSymbolName mappedBy) {
		this.mappedBy = mappedBy;
	}
	
}
