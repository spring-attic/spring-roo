package org.springframework.roo.classpath.layers;

/**
 * Convenience class for addon developers wishing to implement their own
 * {@link LayerProvider}.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
public abstract class LayerAdapter implements LayerProvider {

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
        final LayerProvider other = (LayerProvider) obj;
        return getLayerPosition() == other.getLayerPosition();
    }

    @Override
    public int hashCode() {
        return getLayerPosition();
    }
}
