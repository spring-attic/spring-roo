package org.springframework.roo.classpath.operations.jsr303;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.IntegerAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

public abstract class CollectionField extends FieldDetails {
	
	/** Whether the JSR 303 @Size annotation will be added; provides the "min" attribute (defaults to 0) */
	private Integer sizeMin = null;
	
	/** Whether the JSR 303 @Size annotation will be added; provides the "max" attribute (defaults to {@link Integer#MAX_VALUE}) */
	private Integer sizeMax = null;
	
	/** The generic type that will be used within the collection */
	private JavaType genericParameterTypeName;
	
	public CollectionField(String physicalTypeIdentifier, JavaType fieldType, JavaSymbolName fieldName, JavaType genericParameterTypeName) {
		super(physicalTypeIdentifier, fieldType, fieldName);
		Assert.notNull(genericParameterTypeName, "Generic parameter type name is required");
		this.genericParameterTypeName = genericParameterTypeName;		
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
	}

	public abstract JavaType getInitializer();
	
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

	public JavaType getGenericParameterTypeName() {
		return genericParameterTypeName;
	}

	public void setGenericParameterTypeName(JavaType genericParameterTypeName) {
		this.genericParameterTypeName = genericParameterTypeName;
	}	
}
