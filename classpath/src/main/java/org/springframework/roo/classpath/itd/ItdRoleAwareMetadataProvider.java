package org.springframework.roo.classpath.itd;

import java.util.Set;

import org.springframework.roo.model.JavaType;

/**
 * An {@link ItdMetadataProvider} that is aware it provides particular {@link ItdProviderRole}s.
 * 
 * <p>
 * {@link ItdProviderRole}s allows a simple discovery mechanism so that {@link ItdMetadataProvider}s 
 * can locate {@link ItdRoleAwareMetadataProvider}s that they may wish to form a relationship with.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface ItdRoleAwareMetadataProvider extends ItdMetadataProvider {

	/**
	 * @return the roles this provider offers (never null, but may be empty)
	 */
	Set<ItdProviderRole> getRoles();
	
	void addMetadataTrigger(JavaType javaType);
	
	void removeMetadataTrigger(JavaType javaType);
}
