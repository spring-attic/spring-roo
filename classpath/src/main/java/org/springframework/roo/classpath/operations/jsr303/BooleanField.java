package org.springframework.roo.classpath.operations.jsr303;

import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
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

	public void decorateAnnotationsList(List<AnnotationMetadataBuilder> annotations) {
		super.decorateAnnotationsList(annotations);
		if (assertTrue) {
			annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.validation.constraints.AssertTrue")));
		}
		if (assertFalse) {
			annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.validation.constraints.AssertFalse")));
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
