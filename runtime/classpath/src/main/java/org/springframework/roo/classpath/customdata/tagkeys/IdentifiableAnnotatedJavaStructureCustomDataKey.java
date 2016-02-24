package org.springframework.roo.classpath.customdata.tagkeys;

import java.util.List;

import org.springframework.roo.classpath.details.IdentifiableAnnotatedJavaStructure;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;

/**
 * {@link IdentifiableAnnotatedJavaStructure}-specific implementation of
 * {@link IdentifiableJavaStructureCustomDataKey}.
 * 
 * @author James Tyrrell
 * @since 1.1.3
 */
public abstract class IdentifiableAnnotatedJavaStructureCustomDataKey<T extends IdentifiableAnnotatedJavaStructure>
        extends IdentifiableJavaStructureCustomDataKey<T> {
    private List<AnnotationMetadata> annotations;

    protected IdentifiableAnnotatedJavaStructureCustomDataKey() {
        super();
    }

    protected IdentifiableAnnotatedJavaStructureCustomDataKey(
            final Integer modifier, final List<AnnotationMetadata> annotations) {
        super(modifier);
        this.annotations = annotations;
    }

    public List<AnnotationMetadata> getAnnotations() {
        return annotations;
    }

    @Override
    public boolean meets(final T identifiableAnnotatedJavaStructure)
            throws IllegalStateException {
        // TODO: Add in validation logic for annotations
        return super.meets(identifiableAnnotatedJavaStructure);
    }
}
