package org.springframework.roo.classpath.operations.jsr303;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

public class BooleanField extends FieldDetails {

	/** Whether the JSR 303 @AssertTrue annotation will be added */
	private boolean assertTrue = false;

	/** Whether the JSR 303 @AssertFalse annotation will be added */
	private boolean assertFalse = false;

	public BooleanField(String physicalTypeIdentifier, JavaType fieldType, JavaSymbolName fieldName) {
		super(physicalTypeIdentifier, fieldType, fieldName);
	}

	public void decorateAnnotationsList(List<AnnotationMetadata> annotations) {
		super.decorateAnnotationsList(annotations);
		if (assertTrue) {
			annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.validation.constraints.AssertTrue"), new ArrayList<AnnotationAttributeValue<?>>()));
		}
		if (assertFalse) {
			annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.validation.constraints.AssertFalse"), new ArrayList<AnnotationAttributeValue<?>>()));
		}
	}

	public boolean isAssertTrue() {
		return assertTrue;
	}

	public void setAssertTrue(boolean assertTrue) {
		this.assertTrue = assertTrue;
	}

	public boolean isAssertFalse() {
		return assertFalse;
	}

	public void setAssertFalse(boolean assertFalse) {
		this.assertFalse = assertFalse;
	}
}
