package org.springframework.roo.addon.entity;

import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;
import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.model.JavaType;

/**
 * Provides {@link EntityMetadata}.
 * 
 * @author Stefan Schmidt
 * @since 1.1
 */
public interface EntityMetadataProvider extends ItdTriggerBasedMetadataProvider {
	
	/**
	 * The trigger annotation for this {@link MetadataProvider}
	 */
	JavaType ENTITY_ANNOTATION = new JavaType(RooEntity.class);
	
	/**
	 * Returns the values of the {@link RooEntity} annotation on the given Java
	 * type (if any)
	 * 
	 * @param javaType can be <code>null</code>
	 * @return <code>null</code> if no values can be found
	 * @since 1.2
	 */
	EntityAnnotationValues getAnnotationValues(JavaType javaType);
}
