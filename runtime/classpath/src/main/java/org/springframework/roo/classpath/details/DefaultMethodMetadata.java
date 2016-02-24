package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Default implementation of {@link MethodMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class DefaultMethodMetadata extends AbstractInvocableMemberMetadata
        implements MethodMetadata {

    private final JavaSymbolName methodName;
    private final JavaType returnType;

    // Package protected to mandate the use of MethodMetadataBuilder
    DefaultMethodMetadata(final CustomData customData,
            final String declaredByMetadataId, final int modifier,
            final List<AnnotationMetadata> annotations,
            final JavaSymbolName methodName, final JavaType returnType,
            final List<AnnotatedJavaType> parameterTypes,
            final List<JavaSymbolName> parameterNames,
            final List<JavaType> throwsTypes, final String body) {
        super(customData, declaredByMetadataId, modifier, annotations,
                parameterTypes, parameterNames, throwsTypes, body);
        Validate.notNull(methodName, "Method name required");
        Validate.notNull(returnType, "Return type required");
        this.methodName = methodName;
        this.returnType = returnType;
    }

    public JavaSymbolName getMethodName() {
        return methodName;
    }

    public final JavaType getReturnType() {
        return returnType;
    }

    public boolean hasSameName(final MethodMetadata... otherMethods) {
        for (final MethodMetadata otherMethod : otherMethods) {
            if (otherMethod != null
                    && methodName.equals(otherMethod.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isStatic() {
        return Modifier.isStatic(getModifier());
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("declaredByMetadataId", getDeclaredByMetadataId());
        builder.append("modifier", Modifier.toString(getModifier()));
        builder.append("methodName", methodName);
        builder.append("parameterTypes", getParameterTypes());
        builder.append("parameterNames", getParameterNames());
        builder.append("returnType", returnType);
        builder.append("annotations", getAnnotations());
        builder.append("throwsTypes", getThrowsTypes());
        builder.append("customData", getCustomData());
        builder.append("body", getBody());
        return builder.toString();
    }
}
