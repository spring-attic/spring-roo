package org.springframework.roo.project.layers;

import java.util.Map;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
interface Crud {

	MemberTypeAdditions getPersistMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType);

	MemberTypeAdditions getUpdateMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType);

	MemberTypeAdditions getDeleteMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType);

	MemberTypeAdditions getFindMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType);

	MemberTypeAdditions getFindAllMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType);
	
	MemberTypeAdditions getFindEntriesMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType);
	
	MemberTypeAdditions getCountMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType);
	
	/** 
	 * integration with custom finders registered on the domain entity 
	 */
	Map<String, MemberTypeAdditions> getFinderMethods(String declaredByMetadataId, JavaType entityType, LayerType layerType, String ... finderNames);
}
