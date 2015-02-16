package org.springframework.roo.classpath.customdata.tagkeys;

import java.util.List;

import org.springframework.roo.classpath.details.InvocableMemberMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * {@link InvocableMemberMetadata}-specific implementation of
 * {@link IdentifiableAnnotatedJavaStructureCustomDataKey}.
 * 
 * @author James Tyrrell
 * @since 1.1.3
 */
public abstract class InvocableMemberMetadataCustomDataKey<T extends InvocableMemberMetadata>
        extends IdentifiableAnnotatedJavaStructureCustomDataKey<T> {
    private List<JavaSymbolName> parameterNames;
    private List<AnnotatedJavaType> parameterTypes;
    private List<JavaType> throwsTypes;

    protected InvocableMemberMetadataCustomDataKey() {
        super();
    }

    protected InvocableMemberMetadataCustomDataKey(final Integer modifier,
            final List<AnnotationMetadata> annotations,
            final List<AnnotatedJavaType> parameterTypes,
            final List<JavaSymbolName> parameterNames,
            final List<JavaType> throwsTypes) {
        super(modifier, annotations);
        this.parameterTypes = parameterTypes;
        this.parameterNames = parameterNames;
        this.throwsTypes = throwsTypes;
    }

    public List<JavaSymbolName> getParameterNames() {
        return parameterNames;
    }

    public List<AnnotatedJavaType> getParameterTypes() {
        return parameterTypes;
    }

    public List<JavaType> getThrowsTypes() {
        return throwsTypes;
    }

    @Override
    public boolean meets(final T invocableMemberMetadata)
            throws IllegalStateException {
        // TODO: Add in validation logic for parameterTypes, parameterNames,
        // throwsTypes
        return super.meets(invocableMemberMetadata);
    }
}
