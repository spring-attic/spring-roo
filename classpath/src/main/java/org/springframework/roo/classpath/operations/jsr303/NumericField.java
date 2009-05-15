package org.springframework.roo.classpath.operations.jsr303;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.LongAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

public class NumericField extends StringOrNumericField {

	/** Whether the JSR 303 @Min annotation will be added */
	private Long min = null;
	
	/** Whether the JSR 303 @Max annotation will be added */
	private Long max = null;

	public NumericField(String physicalTypeIdentifier, JavaType fieldType, JavaSymbolName fieldName) {
		super(physicalTypeIdentifier, fieldType, fieldName);
	}

	public void decorateAnnotationsList(List<AnnotationMetadata> annotations) {
		super.decorateAnnotationsList(annotations);
		if (min != null) {
			List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
			attrs.add(new LongAttributeValue(new JavaSymbolName("value"), min));
			annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.validation.constraints.Min"), attrs));
		}
		if (max != null) {
			List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
			attrs.add(new LongAttributeValue(new JavaSymbolName("value"), max));
			annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.validation.constraints.Max"), attrs));
		}
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
