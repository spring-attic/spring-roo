package org.springframework.roo.project.layers;


/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public interface LayerProvider extends Crud {
	
	/**
	 * The priority of the core layers.
	 */
	public static final int CORE_LAYER_PRIORITY = 0;
	
	/**
	 * Returns the position of this layer relative to others. Third-party implementations
	 * should return 
	 * 
	 * @return
	 */
	int getLayerPosition();
	
	/**
	 * Returns the priority of this layer relative to other implementations with the same
	 * position.
	 * 
	 * @return a value greater than {@link #CORE_LAYER_PRIORITY} in order to take
	 * precedence over the core layers
	 * @see #getLayerPosition()
	 */
	int getPriority();
}
