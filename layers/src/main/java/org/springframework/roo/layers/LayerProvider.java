package org.springframework.roo.layers;


/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public interface LayerProvider extends Crud {
	
	LayerType getLayerType();
	
	int priority();
}
