package org.springframework.roo.project.layers;

import org.springframework.roo.support.util.ObjectUtils;

/**
 * Convenience class for addon developers wishing to implement their own
 * {@link LayerProvider}.
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public abstract class LayerAdapter<M extends Enum<M>> implements LayerProvider<M> {

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final LayerProvider<?> other = (LayerProvider<?>) obj;
		return getLayerPosition() == other.getLayerPosition() && getPriority() == other.getPriority();
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(new int[] {getLayerPosition(), getPriority()});
	}
}
