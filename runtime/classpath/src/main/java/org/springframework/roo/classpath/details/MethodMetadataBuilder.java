package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.comments.CommentStructure;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Builder for {@link MethodMetadata}.
 * 
 * @author Ben Alex
 * @author Juan Carlos García
 * @since 1.1
 */
public final class MethodMetadataBuilder extends
        AbstractInvocableMemberMetadataBuilder<MethodMetadata> {

    private JavaSymbolName methodName;
    private JavaType returnType;

    public MethodMetadataBuilder(final MethodMetadata existing) {
        super(existing);
        init(existing.getMethodName(), existing.getReturnType());
    }

    public MethodMetadataBuilder(final String declaredbyMetadataId) {
        super(declaredbyMetadataId);
    }

    /**
     * Constructor for a method with no parameters
     * 
     * @param declaredbyMetadataId
     * @param modifier
     * @param methodName
     * @param returnType
     * @param bodyBuilder
     */
    public MethodMetadataBuilder(final String declaredbyMetadataId,
            final int modifier, final JavaSymbolName methodName,
            final JavaType returnType,
            final InvocableMemberBodyBuilder bodyBuilder) {
        this(declaredbyMetadataId, modifier, methodName, returnType,
                new ArrayList<AnnotatedJavaType>(),
                new ArrayList<JavaSymbolName>(), bodyBuilder);
    }

    /**
     * Constructor for a method with parameters
     * 
     * @param declaredbyMetadataId
     * @param modifier
     * @param methodName
     * @param returnType
     * @param parameterTypes
     * @param parameterNames
     * @param bodyBuilder
     */
    public MethodMetadataBuilder(final String declaredbyMetadataId,
            final int modifier, final JavaSymbolName methodName,
            final JavaType returnType,
            final List<AnnotatedJavaType> parameterTypes,
            final List<JavaSymbolName> parameterNames,
            final InvocableMemberBodyBuilder bodyBuilder) {
        this(declaredbyMetadataId);
        setModifier(modifier);
        setParameterTypes(parameterTypes);
        setParameterNames(parameterNames);
        init(methodName, returnType);
        setBodyBuilder(bodyBuilder);
    }

    public MethodMetadataBuilder(final String declaredbyMetadataId,
            final MethodMetadata existing) {
        super(declaredbyMetadataId, existing);
        init(existing.getMethodName(), existing.getReturnType());
    }

    public MethodMetadata build() {
        DefaultMethodMetadata methodMetadata = new DefaultMethodMetadata(
                getCustomData().build(), getDeclaredByMetadataId(),
                getModifier(), buildAnnotations(), getMethodName(),
                getReturnType(), getParameterTypes(), getParameterNames(),
                getThrowsTypes(), getBodyBuilder().getOutput());
        
        methodMetadata.setCommentStructure(this.getCommentStructure());
        // ROO-3648: Add support to generate Generic Methods
        methodMetadata.setGenericDefinition(this.getGenericDefinition());

        return methodMetadata;
    }

    public JavaSymbolName getMethodName() {
        return methodName;
    }

    public JavaType getReturnType() {
        return returnType;
    }

    private void init(final JavaSymbolName methodName, final JavaType returnType) {
        this.methodName = methodName;
        this.returnType = returnType;
    }

    public void setMethodName(final JavaSymbolName methodName) {
        this.methodName = methodName;
    }

    public void setReturnType(final JavaType returnType) {
        this.returnType = returnType;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                // Append the parts of the method that make up the Java
                // signature
                .append("methodName", methodName)
                .append("parameterTypes", getParameterTypes()).toString();
    }
}
