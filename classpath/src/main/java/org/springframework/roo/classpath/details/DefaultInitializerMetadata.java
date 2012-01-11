package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;

import org.springframework.roo.model.CustomData;
import org.springframework.roo.support.style.ToStringCreator;

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
        final ToStringCreator tsc = new ToStringCreator(this);
        tsc.append("declaredByMetadataId", getDeclaredByMetadataId());
        tsc.append("modifier", Modifier.toString(getModifier()));
        tsc.append("customData", getCustomData());
        tsc.append("isStatic", isStatic());
        tsc.append("body", getBody());
        return tsc.toString();
    }
}
