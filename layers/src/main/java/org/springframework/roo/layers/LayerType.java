package org.springframework.roo.layers;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public enum LayerType implements Comparable<LayerType> {
	
	FIRST (0),
	SERVICE (20),
	REPOSITORY (40),
	DAO (60),
	ACTIVE_RECORD (80);
	
	private int order;
	
	private LayerType(int order) {
		this.order = order;
	}
	
	public int getOrder() {
		return order;
	}
}
