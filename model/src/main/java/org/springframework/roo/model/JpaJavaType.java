package org.springframework.roo.model;

/**
 * Constants for javax.persistence {@link JavaType}s.
 * 
 * Use them in preference to creating new instances of these types.
 * 
 * @author Alan
 * @since 1.2.0
 */
public final class JpaJavaType {

	// Constants
	public static final JavaType COLUMN = new JavaType("javax.persistence.Column");
	public static final JavaType JOIN_COLUMN = new JavaType("javax.persistence.JoinColumn");
	public static final JavaType MANY_TO_ONE = new JavaType("javax.persistence.ManyToOne");
	public static final JavaType MANY_TO_MANY = new JavaType("javax.persistence.ManyToMany");
	public static final JavaType ONE_TO_ONE = new JavaType("javax.persistence.OneToOne");
	public static final JavaType ONE_TO_MANY = new JavaType("javax.persistence.OneToMany");
	public static final JavaType TEMPORAL = new JavaType("javax.persistence.Temporal");
	public static final JavaType TEMPORAL_TYPE = new JavaType("javax.persistence.TemporalType");

	/**
	 * Constructor is private to prevent instantiation
	 */
	private JpaJavaType() {
	}
}