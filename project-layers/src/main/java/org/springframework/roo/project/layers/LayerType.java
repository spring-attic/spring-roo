package org.springframework.roo.project.layers;

/**
 * A level of layer within a user application
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public enum LayerType {
	
	HIGHEST (100),
	
	/**
	 * Domain type that implements an application's use-cases.
	 */
	SERVICE (80),
	
	/**
	 * Domain type that provides collection-like access to instances of aggregate roots;
	 * implementations are usually persistence agnostic.
	 */
	REPOSITORY (60),
	
	/**
	 * Infrastructure component that provides low-level persistence operations, e.g. to
	 * a single table of a relational database. Usually implemented via a specific
	 * persistence technology such as JPA or JDBC.
	 */
	DAO (40),
	
	/**
	 * The pattern by which entities provide their own persistence methods.
	 */
	ACTIVE_RECORD (20);
	
	// Fields
	private final int position;
	
	/**
	 * Constructor
	 *
	 * @param position the position of this layer relative to other layers
	 */
	private LayerType(int position) {
		this.position = position;
	}
	
	/**
	 * Returns the position of this layer relative to other layers
	 * 
	 * @return any integer
	 */
	public int getPosition() {
		return position;
	}
}
