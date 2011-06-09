package org.springframework.roo.project.layers;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public enum LayerType {
	
	HIGHEST (100),
	SERVICE (80),
	REPOSITORY (60),
	DAO (40),
	ACTIVE_RECORD (20);
	
	private int order;
	
	private LayerType(int order) {
		this.order = order;
	}
	
	public int getPosition() {
		return order;
	}
}
