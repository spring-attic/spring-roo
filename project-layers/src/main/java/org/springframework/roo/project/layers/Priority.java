package org.springframework.roo.project.layers;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public enum Priority {

	LOW (0),
	MEDIUM (50),
	HIGH (100);
	
	private int priority;
	
	private Priority(int priority) {
		this.priority = priority;
	}
	
	public int getNumericValue() {
		return priority;
	}
}
