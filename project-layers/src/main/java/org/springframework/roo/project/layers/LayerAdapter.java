package org.springframework.roo.project.layers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public abstract class LayerAdapter implements LayerProvider {

	public MemberTypeAdditions getPersistMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType) {
		return null;
	}

	public MemberTypeAdditions getUpdateMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType) {
		return null;
	}

	public MemberTypeAdditions getDeleteMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType) {
		return null;
	}

	public MemberTypeAdditions getFindMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType) {
		return null;
	}

	public MemberTypeAdditions getFindAllMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType) {
		return null;
	}

	public Map<String, MemberTypeAdditions> getFinderMethods(String declaredByMetadataId, JavaType entityType, LayerType layerType, String ... finderNames) {
		return new HashMap<String, MemberTypeAdditions>();
	}

	public MemberTypeAdditions getFindEntriesMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType) {
		return null;
	}

	public MemberTypeAdditions getCountMethod(String declaredByMetadataId, JavaSymbolName entityVariableName, JavaType entityType, LayerType layerType) {
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
