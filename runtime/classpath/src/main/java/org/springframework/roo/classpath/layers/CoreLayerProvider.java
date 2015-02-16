package org.springframework.roo.classpath.layers;

/**
 * A built-in {@link LayerAdapter}, in other words one that ships with Spring
 * Roo.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public abstract class CoreLayerProvider extends LayerAdapter {

    /**
     * This implementation returns {@link LayerProvider#CORE_LAYER_PRIORITY}
     */
    public int getPriority() {
        return CORE_LAYER_PRIORITY;
    }
}