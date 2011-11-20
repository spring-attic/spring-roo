package org.springframework.roo.classpath.operations.jsr303;

import static org.springframework.roo.model.JpaJavaType.LOB;
import static org.springframework.roo.model.RooJavaType.ROO_UPLOADED_FILE;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

public class UploadedFileField extends FieldDetails {

	// Fields
	private UploadedFileContentType contentType;
	private boolean autoUpload;

	public UploadedFileField(String physicalTypeIdentifier, JavaSymbolName fieldName, UploadedFileContentType contentType) {
		super(physicalTypeIdentifier, JavaType.BYTE_ARRAY_PRIMITIVE, fieldName);
		this.contentType = contentType;
	}

	@Override
	public void decorateAnnotationsList(final List<AnnotationMetadataBuilder> annotations) {
		super.decorateAnnotationsList(annotations);

		List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
		attrs.add(new StringAttributeValue(new JavaSymbolName("contentType"), contentType.getContentType()));

		if (autoUpload) {
			attrs.add(new BooleanAttributeValue(new JavaSymbolName("autoUpload"), autoUpload));
		}

		annotations.add(new AnnotationMetadataBuilder(ROO_UPLOADED_FILE, attrs));
		annotations.add(new AnnotationMetadataBuilder(LOB));
	}

	public UploadedFileContentType getContentType() {
		return contentType;
	}

	public void setAutoUpload(boolean autoUpload) {
		this.autoUpload = autoUpload;
	}
}
