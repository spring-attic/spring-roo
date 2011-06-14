package org.springframework.roo.project.layers;

import java.util.Map;

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
public interface LayerService extends Crud {

	/**
	 * Returns a map of source code modifications for all supported
	 * persistence-related operations of the given entity.
	 * 
	 * @param declaredByMetadataId
	 * @param entityVariableName
	 * @param entityType the domain type for which persistence operations are being requested
	 * @param layerPosition the position of the layer invoking this method;
	 * higher values mean higher architectural layers
	 * @return a non-<code>null</code> map
	 */
	Map<CrudKey, MemberTypeAdditions> collectMemberTypeAdditions(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, int layerPosition);
}
