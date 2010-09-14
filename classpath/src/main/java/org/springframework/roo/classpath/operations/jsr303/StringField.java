package org.springframework.roo.classpath.operations.jsr303;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.IntegerAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Extra validation properties specified to String properties.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class StringField extends StringOrNumericField {
	
	/** Whether the JSR 303 @Size annotation will be added; provides the "min" attribute (defaults to 0) */
	private Integer sizeMin = null;
	
	/** Whether the JSR 303 @Size annotation will be added; provides the "max" attribute (defaults to {@link Integer#MAX_VALUE}) */
	private Integer sizeMax = null;

	/** Whether the JSR 3030 @Pattern annotation will be added */
	private String regexp = null;
	
	public StringField(String physicalTypeIdentifier, JavaType fieldType, JavaSymbolName fieldName) {
		super(physicalTypeIdentifier, fieldType, fieldName);
	}

	public void decorateAnnotationsList(List<AnnotationMetadataBuilder> annotations) {
		super.decorateAnnotationsList(annotations);
		if (sizeMin != null || sizeMax != null) {
			List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
			if (sizeMin != null) {
				attrs.add(new IntegerAttributeValue(new JavaSymbolName("min"), sizeMin));
			}
			if (sizeMax != null) {
				attrs.add(new IntegerAttributeValue(new JavaSymbolName("max"), sizeMax));
			}
			annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.validation.constraints.Size"), attrs));
		}
		if (regexp != null) {
			List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
			attrs.add(new StringAttributeValue(new JavaSymbolName("regexp"), regexp));
			annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.validation.constraints.Pattern"), attrs));
		}
	}

	public Integer getSizeMin() {
		return sizeMin;
	}

	public void setSizeMin(Integer sizeMin) {
		this.sizeMin = sizeMin;
	}

	public Integer getSizeMax() {
		return sizeMax;
	}

	public void setSizeMax(Integer sizeMax) {
		this.sizeMax = sizeMax;
	}

	public String getRegexp() {
		return regexp;
	}

	public void setRegexp(String regexp) {
		this.regexp = regexp;
	}	
}
