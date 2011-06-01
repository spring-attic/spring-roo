package org.springframework.roo.layers;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public abstract class LayerAdapter implements LayerProvider {

	public MemberTypeAdditions integratePersistMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType) {
		return null;
	}

	public MemberTypeAdditions integrateUpdateMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType) {
		return null;
	}

	public MemberTypeAdditions integrateDeleteMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType) {
		return null;
	}

	public MemberTypeAdditions integrateFindMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType) {
		return null;
	}

	public MemberTypeAdditions integrateFindAllMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType) {
		return null;
	}

	public MemberTypeAdditions integrateFinderMethod(String declaredByMetadataId, String finderName, JavaType entityType) {
		return null;
	}

	public MemberTypeAdditions integrateFindEntriesMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType) {
		return null;
	}

	public MemberTypeAdditions integrateCountMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType) {
		return null;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getLayerType() == null) ? 0 : getLayerType().hashCode());
		result = prime * result + priority();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LayerProvider other = (LayerProvider) obj;
		if (getLayerType() == null) {
			if (other.getLayerType() != null)
				return false;
		} else if (!getLayerType().equals(other.getLayerType()))
			return false;
		if (priority() != other.priority()) {
			return false;
		} 
		return true;
	}
}
