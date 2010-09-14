package org.springframework.roo.classpath.operations.jsr303;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.IntegerAttributeValue;
import org.springframework.roo.classpath.details.annotations.LongAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

public class NumericField extends StringOrNumericField {

	/** Whether the JSR 303 @Min annotation will be added */
	private Long min = null;
	
	/** Whether the JSR 303 @Max annotation will be added */
	private Long max = null;
	
	/** Whether the JSR 303 @Digits annotation will be added (you must also set digitsFractional) */
	private Integer digitsInteger = null;
	
	/** Whether the JSR 303 @Digits annotation will be added (you must also set digitsInteger) */
	private Integer digitsFraction = null;

	public NumericField(String physicalTypeIdentifier, JavaType fieldType, JavaSymbolName fieldName) {
		super(physicalTypeIdentifier, fieldType, fieldName);
	}

	public void decorateAnnotationsList(List<AnnotationMetadataBuilder> annotations) {
		super.decorateAnnotationsList(annotations);
		if (min != null) {
			List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
			attrs.add(new LongAttributeValue(new JavaSymbolName("value"), min));
			annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.validation.constraints.Min"), attrs));
		}
		if (max != null) {
			List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
			attrs.add(new LongAttributeValue(new JavaSymbolName("value"), max));
			annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.validation.constraints.Max"), attrs));
		}
		Assert.isTrue(isDigitsSetCorrectly(), "Validation constraints for @Digit are not correctly set");
		if (digitsInteger != null) {
			List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
			attrs.add(new IntegerAttributeValue(new JavaSymbolName("integer"), digitsInteger));
			attrs.add(new IntegerAttributeValue(new JavaSymbolName("fraction"), digitsFraction));
			annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.validation.constraints.Digits"), attrs));
		}
	}
	
	public boolean isDigitsSetCorrectly() {
		return (digitsInteger == null && digitsFraction == null) || (digitsInteger != null && digitsFraction != null);
	}

	public Integer getDigitsInteger() {
		return digitsInteger;
	}

	public void setDigitsInteger(Integer digitsInteger) {
		this.digitsInteger = digitsInteger;
	}

	public Integer getDigitsFraction() {
		return digitsFraction;
	}

	public void setDigitsFraction(Integer digitsFractional) {
		this.digitsFraction = digitsFractional;
	}

	public Long getMin() {
		return min;
	}

	public void setMin(Long min) {
		this.min = min;
	}

	public Long getMax() {
		return max;
	}

	public void setMax(Long max) {
		this.max = max;
	}
}
