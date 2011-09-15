package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Abstract class for {@link IdentifiableAnnotatedJavaStructure} subclasses.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public abstract class AbstractIdentifiableAnnotatedJavaStructureProvider extends AbstractIdentifiableJavaStructureProvider implements IdentifiableAnnotatedJavaStructure {
	
	// Fields
	private final List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
	
	/**
	 * Constructor
	 *
	 * @param customData
	 * @param declaredByMetadataId
	 * @param modifier
	 * @param annotations can be <code>null</code> for none
	 */
	protected AbstractIdentifiableAnnotatedJavaStructureProvider(final CustomData customData, final String declaredByMetadataId, final int modifier, final Collection<AnnotationMetadata> annotations) {
		super(customData, declaredByMetadataId, modifier);
		CollectionUtils.populate(this.annotations, annotations);
	}

	public List<AnnotationMetadata> getAnnotations() {
		return Collections.unmodifiableList(annotations);
	}
}
