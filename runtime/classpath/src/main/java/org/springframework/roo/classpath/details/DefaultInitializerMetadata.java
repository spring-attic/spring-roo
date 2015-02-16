package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.model.CustomData;

/**
 * Default implementation of {@link InitializerMetadata}.
 * 
 * @author James Tyrrell
 * @since 1.1.1
 */
public class DefaultInitializerMetadata extends
        AbstractIdentifiableJavaStructureProvider implements
        InitializerMetadata {

    private final String body;
    private final boolean isStatic;

    // Package protected to mandate the use of InitializerMetadataBuilder
    DefaultInitializerMetadata(final CustomData customData,
            final String declaredByMetadataId, final int modifier,
            final boolean isStatic, final String body) {
        super(customData, declaredByMetadataId, modifier);
        this.isStatic = isStatic;
        this.body = body;
    }

    public final String getBody() {
        return body;
    }

    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("declaredByMetadataId", getDeclaredByMetadataId());
        builder.append("modifier", Modifier.toString(getModifier()));
        builder.append("customData", getCustomData());
        builder.append("isStatic", isStatic());
        builder.append("body", getBody());
        return builder.toString();
    }
}
