package org.springframework.roo.layers;

import java.util.Map;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public interface LayerService extends Crud {

	Map<CrudKey, MemberTypeAdditions> collectMemberTypeAdditions(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType);
}
