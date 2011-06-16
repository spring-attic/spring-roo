package org.springframework.roo.project.layers;

import java.util.LinkedHashMap;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Provides upper-layer code (such as MVC, GWT, and tests) with the
 * {@link MemberTypeAdditions} they need to make to their source code in order
 * to invoke persistence-related operations such as <code>persist</code> and
 * <code>find</code>.
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public interface LayerService {
	
	/**
	 * Returns source code modifications for a requested operation offered by a layer provider
	 * 
	 * @param metadataId Id of calling metadata provider
	 * @param methodIdentifier specifies the method which is being requested
	 * @param targetEntity specifies the target entity
	 * @param methodParams parameters which are passed in to the method
	 * @param layerPosition the position of the layer invoking this method
	 * @return {@link MemberTypeAdditions} if a layer provider can offer this functionality, null otherwise
	 */
	MemberTypeAdditions getMemberTypeAdditions(String metadataId, String methodIdentifier, JavaType targetEntity, LinkedHashMap<JavaSymbolName, Object> methodParams, int layerPosition);
}
