package org.springframework.roo.classpath.operations.jsr303;

import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * This field is intended for use with JSR 220 and will create a @Embedded annotation.
 *
 * @author Alan Stewart
 * @since 1.1
 */
public class EmbeddedField extends FieldDetails {

	public EmbeddedField(String physicalTypeIdentifier, JavaType fieldType, JavaSymbolName fieldName) {
		super(physicalTypeIdentifier, fieldType, fieldName);
	}

	public void decorateAnnotationsList(List<AnnotationMetadataBuilder> annotations) {
		super.decorateAnnotationsList(annotations);
		annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.Embedded")));
	}
}
