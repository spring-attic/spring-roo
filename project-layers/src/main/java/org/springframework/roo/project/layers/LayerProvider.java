package org.springframework.roo.project.layers;

import org.springframework.roo.model.JavaType;

/**
 * Provides {@link MemberTypeAdditions} for some or all methods of a given type
 * <code>M</code>.
 *
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @param <M> an enumeration of the methods recognised (but not necessarily
 * implemented) by this provider. If an addon wishes to support multiple sets of
 * methods (represented by several distinct enums), they need only create a 
 * separate {@link LayerProvider} implementation for each such enum.
 * @since 1.2
 */
public interface LayerProvider<M extends Enum<M>> {
	
	/**
	 * The priority of the core layers.
	 */
	int CORE_LAYER_PRIORITY = 0;
	
	/**
	 * Indicates whether this provider supports the given type of method
	 * 
	 * @param methodType an enum class
	 * @return see above
	 */
	boolean supports(Class<?> methodType);
	
	/**
	 * Returns the additions for the given method
	 * 
	 * @param metadataId Id of calling metadata provider
	 * @param targetEntity specifies the target entity
	 * @param method the method for which the additions are required
	 * @return <code>null</code> if this provider doesn't support this method
	 */
	MemberTypeAdditions getAdditions(String metadataId, JavaType targetEntity, M method);
	
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
