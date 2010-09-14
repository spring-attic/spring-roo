package org.springframework.roo.classpath.operations.jsr303;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.operations.EnumType;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * This field is intended for use with JSR 220 and will create a @Enumerated annotation.
 *
 * @author Ben Alex
 * @since 1.0
 */
public class EnumField extends FieldDetails {
	private EnumType enumType;

	public EnumField(String physicalTypeIdentifier, JavaType fieldType, JavaSymbolName fieldName) {		
		super(physicalTypeIdentifier, fieldType, fieldName);
	}

	public EnumType getEnumType() {
		return enumType;
	}

	public void setEnumType(EnumType enumType) {
		this.enumType = enumType;
	}

	public void decorateAnnotationsList(List<AnnotationMetadataBuilder> annotations) {
		super.decorateAnnotationsList(annotations);
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		
		if (enumType != null) {
			JavaSymbolName value = new JavaSymbolName("ORDINAL");
			if (enumType == EnumType.STRING) {
				value = new JavaSymbolName("STRING");
			}
			attributes.add(new EnumAttributeValue(new JavaSymbolName("value"), new EnumDetails(new JavaType("javax.persistence.EnumType"), value)));
		}
		
		annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.Enumerated"), attributes));
	}
}
