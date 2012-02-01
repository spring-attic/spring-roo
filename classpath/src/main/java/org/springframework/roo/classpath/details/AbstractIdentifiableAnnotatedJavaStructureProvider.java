package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Abstract class for {@link IdentifiableAnnotatedJavaStructure} subclasses.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public abstract class AbstractIdentifiableAnnotatedJavaStructureProvider extends
        AbstractIdentifiableJavaStructureProvider implements
        IdentifiableAnnotatedJavaStructure {

    private final List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();

    /**
     * Constructor
     * 
     * @param customData
     * @param declaredByMetadataId
     * @param modifier
     * @param annotations can be <code>null</code> for none
     */
    protected AbstractIdentifiableAnnotatedJavaStructureProvider(
            final CustomData customData, final String declaredByMetadataId,
            final int modifier, final Collection<AnnotationMetadata> annotations) {
        super(customData, declaredByMetadataId, modifier);
        CollectionUtils.populate(this.annotations, annotations);
    }

    public AnnotationMetadata getAnnotation(final JavaType type) {
        Validate.notNull(type, "Annotation type to locate required");
        for (final AnnotationMetadata md : getAnnotations()) {
            if (md.getAnnotationType().equals(type)) {
                return md;
            }
        }
        return null;
    }

    public List<AnnotationMetadata> getAnnotations() {
        return Collections.unmodifiableList(annotations);
    }

    public AnnotationMetadata getTypeAnnotation(final JavaType annotationType) {
        Validate.notNull(annotationType, "Annotation type required");
        IdentifiableAnnotatedJavaStructure current = this;
        while (current != null) {
            final AnnotationMetadata result = current
                    .getAnnotation(annotationType);
            if (result != null) {
                return result;
            }
            if (current instanceof ClassOrInterfaceTypeDetails) {
                current = ((ClassOrInterfaceTypeDetails) current)
                        .getSuperclass();
            }
            else {
                current = null;
            }
        }
        return null;
    }
}
