package org.springframework.roo.project.layers;

import java.util.Map;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Convenience class for addon developers wishing to implement their own {@link LayerProvider}.
 * 
 * This implementation returns <code>null</code> for all methods, indicating by default that
 * the subclass does not provide any persistence-related methods. Subclasses should override
 * individual methods below according to which persistence methods they do provide.
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public abstract class LayerAdapter implements LayerProvider {
	
	public MemberTypeAdditions getMemberTypeAdditions(String methodIdentifier, Map<JavaSymbolName, JavaType> methodParams) {
		return null;
	}

	@Override
	public int hashCode() {
		return getLayerPosition();
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
		if (getLayerPosition() == other.getLayerPosition()) {
			return true;
		} else {
			return false;
		}
	}
}
