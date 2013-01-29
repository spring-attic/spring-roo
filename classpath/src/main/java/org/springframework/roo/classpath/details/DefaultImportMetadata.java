package org.springframework.roo.classpath.details;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.details.comments.CommentStructure;
import org.springframework.roo.classpath.details.comments.CommentedJavaStructure;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * Default implementation of {@link ImportMetadata}.
 * 
 * @author James Tyrrell
 * @since 1.1.1
 */
public class DefaultImportMetadata extends
        AbstractIdentifiableJavaStructureProvider implements ImportMetadata, CommentedJavaStructure {

    private final JavaPackage importPackage;
    private final JavaType importType;
    private CommentStructure commentStructure;
    private boolean isAsterisk = false;
    private boolean isStatic = false;

    // Package protected to mandate the use of ImportMetadataBuilder
    DefaultImportMetadata(final CustomData customData,
            final String declaredByMetadataId, final int modifier,
            final JavaPackage importPackage, final JavaType importType,
            final boolean isStatic, final boolean isAsterisk) {
        super(customData, declaredByMetadataId, modifier);
        this.importPackage = importPackage;
        this.importType = importType;
        this.isStatic = isStatic;
        this.isAsterisk = isAsterisk;
    }

    public JavaPackage getImportPackage() {
        return importPackage;
    }

    public JavaType getImportType() {
        return importType;
    }

    public boolean isAsterisk() {
        return isAsterisk;
    }

    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("declaredByMetadataId", getDeclaredByMetadataId());
        builder.append("typePackage", importPackage);
        builder.append("type", importType);
        builder.append("isStatic", isStatic);
        builder.append("isAsterisk", isAsterisk);
        return builder.toString();
    }

    @Override
    public CommentStructure getCommentStructure() {
        return commentStructure;
    }

    @Override
    public void setCommentStructure(CommentStructure commentStructure) {
        this.commentStructure = commentStructure;
    }
}
