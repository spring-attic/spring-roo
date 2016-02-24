package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.comments.CommentStructure;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Assists in the development of builders that build objects that extend
 * {@link AbstractInvocableMemberMetadata}.
 * 
 * @author Ben Alex
 * @author Juan Carlos García
 * @since 1.1
 */
public abstract class AbstractInvocableMemberMetadataBuilder<T extends InvocableMemberMetadata>
        extends AbstractIdentifiableAnnotatedJavaStructureBuilder<T> {

    private InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    private List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    private List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    private List<JavaType> throwsTypes = new ArrayList<JavaType>();
    private CommentStructure commentStructure;
    private String genericDefinition;

    protected AbstractInvocableMemberMetadataBuilder(
            final InvocableMemberMetadata existing) {
        super(existing);
        this.commentStructure = existing.getCommentStructure();
        this.genericDefinition = existing.getGenericDefinition();
        this.parameterNames = new ArrayList<JavaSymbolName>(
                existing.getParameterNames());
        this.parameterTypes = new ArrayList<AnnotatedJavaType>(
                existing.getParameterTypes());
        this.throwsTypes = new ArrayList<JavaType>(existing.getThrowsTypes());
        bodyBuilder.append(existing.getBody());
    }

    protected AbstractInvocableMemberMetadataBuilder(
            final String declaredbyMetadataId) {
        super(declaredbyMetadataId);
    }

    protected AbstractInvocableMemberMetadataBuilder(
            final String declaredbyMetadataId,
            final InvocableMemberMetadata existing) {
        super(declaredbyMetadataId, existing);
        this.commentStructure = existing.getCommentStructure();
        this.genericDefinition = existing.getGenericDefinition();
        this.parameterNames = new ArrayList<JavaSymbolName>(
                existing.getParameterNames());
        this.parameterTypes = new ArrayList<AnnotatedJavaType>(
                existing.getParameterTypes());
        this.throwsTypes = new ArrayList<JavaType>(existing.getThrowsTypes());
        bodyBuilder.append(existing.getBody());
    }

    public void addParameter(final String parameterName,
            final JavaType parameterType) {
        addParameterName(new JavaSymbolName(parameterName));
        addParameterType(AnnotatedJavaType.convertFromJavaType(parameterType));
    }

    public boolean addParameterName(final JavaSymbolName parameterName) {
        return parameterNames.add(parameterName);
    }

    public boolean addParameterType(final AnnotatedJavaType parameterType) {
        return parameterTypes.add(parameterType);
    }

    public boolean addThrowsType(final JavaType throwsType) {
        return throwsTypes.add(throwsType);
    }

    public String getBody() {
        if (bodyBuilder != null) {
            return bodyBuilder.getOutput();
        }
        return null;
    }

    public InvocableMemberBodyBuilder getBodyBuilder() {
        if (bodyBuilder == null) {
            bodyBuilder = new InvocableMemberBodyBuilder();
        }
        return bodyBuilder;
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

    public void setBodyBuilder(final InvocableMemberBodyBuilder bodyBuilder) {
        this.bodyBuilder = bodyBuilder;
    }

    public void setParameterNames(final List<JavaSymbolName> parameterNames) {
        this.parameterNames = parameterNames;
    }

    public void setParameterTypes(final List<AnnotatedJavaType> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public void setThrowsTypes(final List<JavaType> throwsTypes) {
        this.throwsTypes = throwsTypes;
    }

    public CommentStructure getCommentStructure() {
        return commentStructure;
    }

    public void setCommentStructure(CommentStructure commentStructure) {
        this.commentStructure = commentStructure;
    }
    
    public String getGenericDefinition() {
        return genericDefinition;
    }

    public void setGenericDefinition(String genericDefinition) {
        this.genericDefinition = genericDefinition;
    }
}
