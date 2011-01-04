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
public class DefaultInitializerMetadata extends AbstractIdentifiableJavaStructureProvider implements InitializerMetadata {

    private String body;
    private boolean isStatic;

	// Package protected to mandate the use of InitializerMetadataBuilder
	DefaultInitializerMetadata(CustomData customData, String declaredByMetadataId, int modifier,boolean isStatic, String body) {
        super(customData, declaredByMetadataId, modifier);
        this.isStatic = isStatic;
        this.body = body;
	}

    public boolean isStatic() {
        return isStatic;
    }

    public final String getBody() {
		return body;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("declaredByMetadataId", getDeclaredByMetadataId());
		tsc.append("modifier", Modifier.toString(getModifier()));
		tsc.append("customData", getCustomData());
        tsc.append("isStatic", isStatic());
        tsc.append("body", getBody());
		return tsc.toString();
	}
}
