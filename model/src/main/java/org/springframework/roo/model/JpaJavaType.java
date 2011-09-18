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
	public static final JavaType CASCADE_TYPE = new JavaType("javax.persistence.CascadeType");
	public static final JavaType COLUMN = new JavaType("javax.persistence.Column");
	public static final JavaType ELEMENT_COLLECTION = new JavaType("javax.persistence.ElementCollection");
	public static final JavaType EMBEDDABLE = new JavaType("javax.persistence.Embeddable");
	public static final JavaType EMBEDDED_ID = new JavaType("javax.persistence.EmbeddedId");
	public static final JavaType ENTITY = new JavaType("javax.persistence.Entity");
	public static final JavaType FETCH_TYPE = new JavaType("javax.persistence.FetchType");
	public static final JavaType ID = new JavaType("javax.persistence.Id");
	public static final JavaType INHERITANCE = new JavaType("javax.persistence.Inheritance");
	public static final JavaType INHERITANCE_TYPE = new JavaType("javax.persistence.InheritanceType");
	public static final JavaType JOIN_COLUMN = new JavaType("javax.persistence.JoinColumn");
	public static final JavaType JOIN_TABLE = new JavaType("javax.persistence.JoinTable");
	public static final JavaType LOB = new JavaType("javax.persistence.Lob");
	public static final JavaType MANY_TO_ONE = new JavaType("javax.persistence.ManyToOne");
	public static final JavaType MANY_TO_MANY = new JavaType("javax.persistence.ManyToMany");
	public static final JavaType ONE_TO_ONE = new JavaType("javax.persistence.OneToOne");
	public static final JavaType ONE_TO_MANY = new JavaType("javax.persistence.OneToMany");
	public static final JavaType TEMPORAL = new JavaType("javax.persistence.Temporal");
	public static final JavaType TEMPORAL_TYPE = new JavaType("javax.persistence.TemporalType");
	public static final JavaType TRANSIENT = new JavaType("javax.persistence.Transient");
	public static final JavaType VERSION = new JavaType("javax.persistence.Version");

	/**
	 * Constructor is private to prevent instantiation
	 */
	private JpaJavaType() {
	}
}