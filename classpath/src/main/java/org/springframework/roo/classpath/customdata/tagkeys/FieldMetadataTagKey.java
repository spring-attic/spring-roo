package org.springframework.roo.classpath.customdata.tagkeys;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

import java.util.List;

/**
 * {@link FieldMetadata} specific  implementation of {@link IdentifiableAnnotatedJavaStructureTagKey}.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public class FieldMetadataTagKey extends IdentifiableAnnotatedJavaStructureTagKey<FieldMetadata> {

	private JavaType fieldType;
	private JavaSymbolName fieldName;
	private String fieldInitializer;
	private String tag;

	public FieldMetadataTagKey(Integer modifier, List<AnnotationMetadata> annotations, JavaType fieldType, JavaSymbolName fieldName, String fieldInitializer) {
		super(modifier, annotations);
		this.fieldType = fieldType;
		this.fieldName = fieldName;
		this.fieldInitializer = fieldInitializer;
	}

	public FieldMetadataTagKey(Integer modifier, List<AnnotationMetadata> annotations) {
		super(modifier, annotations);
	}

	public FieldMetadataTagKey(String tag) {
		super(null, null);
		this.tag = tag;
	}

	public void validate(FieldMetadata taggedInstance) {
		//TODO: Add in validation logic for fieldType, fieldName, fieldInitializer
	}

	public JavaType getFieldType() {
		return fieldType;
	}

	public JavaSymbolName getFieldName() {
		return fieldName;
	}

	public String getFieldInitializer() {
		return fieldInitializer;
	}

	@Override
	public String toString() {
		return tag;
	}
}
