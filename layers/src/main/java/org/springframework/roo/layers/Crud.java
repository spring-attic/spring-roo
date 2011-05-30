package org.springframework.roo.layers;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
interface Crud {

	MemberTypeAdditions integratePersistMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType);

	MemberTypeAdditions integrateUpdateMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType);

	MemberTypeAdditions integrateDeleteMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType);

	MemberTypeAdditions integrateFindMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType);

	MemberTypeAdditions integrateFindAllMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType);
	
	MemberTypeAdditions integrateFindEntriesMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType);
	
	MemberTypeAdditions integrateCountMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType);
	
	/** 
	 * integration with custom finders registered on the domain entity 
	 * @param finderName
	 * @return
	 */
	MemberTypeAdditions integrateFinderMethod(String declaredByMetadataId, String finderName, JavaType entityType);
}
