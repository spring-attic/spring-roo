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

	MemberTypeAdditions getPersistMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, int layerPosition);

	MemberTypeAdditions getUpdateMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, int layerPosition);

	MemberTypeAdditions getDeleteMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, int layerPosition);

	MemberTypeAdditions getFindMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, int layerPosition);

	MemberTypeAdditions getFindAllMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, int layerPosition);
	
	MemberTypeAdditions getFindEntriesMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, int layerPosition);
	
	MemberTypeAdditions getCountMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, int layerPosition);
	
	/** 
	 * integration with custom finders registered on the domain entity 
	 */
	Map<String, MemberTypeAdditions> getFinderMethods(String declaredByMetadataId, JavaType entityType, int layerPosition, String ... finderNames);
	
	//TODO id and version accessor method definitions needed here
}
