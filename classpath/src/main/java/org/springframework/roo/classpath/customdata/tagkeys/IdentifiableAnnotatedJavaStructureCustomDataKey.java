package org.springframework.roo.classpath.customdata.tagkeys;

import org.springframework.roo.classpath.details.IdentifiableAnnotatedJavaStructure;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;

import java.util.List;

/**
 * {@link IdentifiableAnnotatedJavaStructure}-specific implementation of {@link IdentifiableJavaStructureCustomDataKey}.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public abstract class IdentifiableAnnotatedJavaStructureCustomDataKey<T extends IdentifiableAnnotatedJavaStructure> extends IdentifiableJavaStructureCustomDataKey<T> {
	private List<AnnotationMetadata> annotations;

	protected IdentifiableAnnotatedJavaStructureCustomDataKey(Integer modifier, List<AnnotationMetadata> annotations) {
		super(modifier);
		this.annotations = annotations;
	}

	protected IdentifiableAnnotatedJavaStructureCustomDataKey() {
		super();
	}

	public List<AnnotationMetadata> getAnnotations() {
		return annotations;
	}

	public boolean meets(T identifiableAnnotatedJavaStructure) throws IllegalStateException {
		// TODO: Add in validation logic for annotations
		return super.meets(identifiableAnnotatedJavaStructure);
	}
}
