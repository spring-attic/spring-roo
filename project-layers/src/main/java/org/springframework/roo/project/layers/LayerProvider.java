package org.springframework.roo.project.layers;

/**
 * Provides persistence-related methods at a given layer of the application.
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public interface LayerProvider extends Crud {
	
	/**
	 * The priority of the core layers.
	 */
	int CORE_LAYER_PRIORITY = 0;
	
	/**
	 * Returns the position of this layer relative to others. 
	 * 
	 * @return a large number for higher-level layers, a smaller number for lower-level layers
	 */
	int getLayerPosition();
	
	/**
	 * Returns the priority of this layer relative to other implementations with the same
	 * position.
	 * 
	 * @return a value greater than {@link #CORE_LAYER_PRIORITY} in order to take
	 * precedence over the core {@link LayerProvider}s
	 * @see #getLayerPosition()
	 */
	int getPriority();
}
