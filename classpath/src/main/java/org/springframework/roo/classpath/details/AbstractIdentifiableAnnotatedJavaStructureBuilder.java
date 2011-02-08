package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.Builder;

/**
 * Assists in the creation of a {@link Builder} for types that eventually implement {@link IdentifiableAnnotatedJavaStructure}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public abstract class AbstractIdentifiableAnnotatedJavaStructureBuilder<T extends IdentifiableAnnotatedJavaStructure> extends AbstractIdentifiableJavaStructureBuilder<T> {
	private List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

	protected AbstractIdentifiableAnnotatedJavaStructureBuilder(String declaredbyMetadataId) {
		super(declaredbyMetadataId);
	}

	protected AbstractIdentifiableAnnotatedJavaStructureBuilder(IdentifiableAnnotatedJavaStructure existing) {
		super(existing);
		init(existing);
	}
	
	protected AbstractIdentifiableAnnotatedJavaStructureBuilder(String declaredbyMetadataId, IdentifiableAnnotatedJavaStructure existing) {
		super(declaredbyMetadataId, existing);
		init(existing);
	}

	private void init(IdentifiableAnnotatedJavaStructure existing) {
		for (AnnotationMetadata element : existing.getAnnotations()) {
			this.annotations.add(new AnnotationMetadataBuilder(element));
		}
	}

	public final boolean addAnnotation(AnnotationMetadata annotationMetadata) {
		if (annotationMetadata == null) return false;
		return addAnnotation(new AnnotationMetadataBuilder(annotationMetadata));
	}

	public final boolean addAnnotation(AnnotationMetadataBuilder annotationMetadata) {
		if (annotationMetadata == null) return false;
		onAddAnnotation(annotationMetadata);
		return annotations.add(annotationMetadata);
	}

	protected void onAddAnnotation(AnnotationMetadataBuilder annotationMetadata) {}
	
	public final List<AnnotationMetadataBuilder> getAnnotations() {
		return annotations;
	}

	public final List<AnnotationMetadata> buildAnnotations() {
		List<AnnotationMetadata> result = new ArrayList<AnnotationMetadata>();
		for (AnnotationMetadataBuilder builder : annotations) {
			result.add(builder.build());
		}
		return result;
	}

	public final void setAnnotations(List<AnnotationMetadataBuilder> annotations) {
		this.annotations = annotations;
	}
}
