package org.springframework.roo.project.layers;

/**
 * Convenience class for addon developers wishing to implement their own {@link LayerProvider}.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2
 */
public abstract class LayerAdapter implements LayerProvider {

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
