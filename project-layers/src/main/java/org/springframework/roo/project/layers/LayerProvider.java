package org.springframework.roo.project.layers;


/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public interface LayerProvider extends Crud {
	
	LayerType getLayerType();
	
	int priority();
}
