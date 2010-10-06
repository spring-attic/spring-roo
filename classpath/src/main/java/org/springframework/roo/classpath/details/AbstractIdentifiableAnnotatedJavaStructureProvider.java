package org.springframework.roo.classpath.details;

import java.util.Collections;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.support.util.Assert;

/**
 * Abstract class for {@link IdentifiableAnnotatedJavaStructure} subclasses.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public abstract class AbstractIdentifiableAnnotatedJavaStructureProvider extends AbstractIdentifiableJavaStructureProvider implements IdentifiableAnnotatedJavaStructure {
	private List<AnnotationMetadata> annotations;
	
	public AbstractIdentifiableAnnotatedJavaStructureProvider(CustomData customData, String declaredByMetadataId, int modifier, List<AnnotationMetadata> annotations) {
		super(customData, declaredByMetadataId, modifier);
		Assert.notNull(annotations, "Annotations required");
		this.annotations = annotations;
	}

	public final List<AnnotationMetadata> getAnnotations() {
		return Collections.unmodifiableList(annotations);
	}
}
