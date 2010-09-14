package org.springframework.roo.classpath.operations.jsr303;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

public class StringOrNumericField extends FieldDetails {
		
	/** Whether the JSR 303 @DecimalMin annotation will be added */
	private String decimalMin = null;
	
	/** Whether the JSR 303 @DecimalMax annotation will be added */
	private String decimalMax = null;
	
	public StringOrNumericField(String physicalTypeIdentifier, JavaType fieldType, JavaSymbolName fieldName) {
		super(physicalTypeIdentifier, fieldType, fieldName);
	}

	public void decorateAnnotationsList(List<AnnotationMetadataBuilder> annotations) {
		super.decorateAnnotationsList(annotations);
		if (decimalMin != null) {
			List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
			attrs.add(new StringAttributeValue(new JavaSymbolName("value"), decimalMin));
			annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.validation.constraints.DecimalMin"), attrs));
		}
		if (decimalMax != null) {
			List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
			attrs.add(new StringAttributeValue(new JavaSymbolName("value"), decimalMax));
			annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.validation.constraints.DecimalMax"), attrs));
		}
	}

	public String getDecimalMin() {
		return decimalMin;
	}

	public void setDecimalMin(String decimalMin) {
		this.decimalMin = decimalMin;
	}

	public String getDecimalMax() {
		return decimalMax;
	}

	public void setDecimalMax(String decimalMax) {
		this.decimalMax = decimalMax;
	}
}
