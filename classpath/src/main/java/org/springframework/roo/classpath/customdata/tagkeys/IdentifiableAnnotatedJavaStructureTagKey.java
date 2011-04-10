package org.springframework.roo.classpath.customdata.tagkeys;

import org.springframework.roo.classpath.details.IdentifiableAnnotatedJavaStructure;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;

import java.util.List;

/**
 * {@link IdentifiableAnnotatedJavaStructure} specific implementation of
 * {@link IdentifiableJavaStructureTagKey}.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public abstract class IdentifiableAnnotatedJavaStructureTagKey<T extends IdentifiableAnnotatedJavaStructure> extends IdentifiableJavaStructureTagKey<T>{

	private List<AnnotationMetadata> annotations;

	protected IdentifiableAnnotatedJavaStructureTagKey(Integer modifier, List<AnnotationMetadata> annotations) {
		super(modifier);
		this.annotations = annotations;
	}

	protected IdentifiableAnnotatedJavaStructureTagKey() {
		super();
	}

	public List<AnnotationMetadata> getAnnotations() {
		return annotations;
	}

	public void validate(T taggedInstance) throws IllegalStateException {
		super.validate(taggedInstance);
		//TODO: Add in validation logic for annotations
	}
}
