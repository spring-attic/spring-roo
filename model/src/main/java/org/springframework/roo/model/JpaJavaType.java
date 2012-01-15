package org.springframework.roo.model;

/**
 * Constants for javax.persistence {@link JavaType}s. Use them in preference to
 * creating new instances of these types.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public final class JpaJavaType {

    // javax.persistence
    public static final JavaType CASCADE_TYPE = new JavaType(
            "javax.persistence.CascadeType");
    public static final JavaType COLUMN = new JavaType(
            "javax.persistence.Column");
    public static final JavaType DISCRIMINATOR_COLUMN = new JavaType(
            "javax.persistence.DiscriminatorColumn");
    public static final JavaType ELEMENT_COLLECTION = new JavaType(
            "javax.persistence.ElementCollection");
    public static final JavaType EMBEDDABLE = new JavaType(
            "javax.persistence.Embeddable");
    public static final JavaType EMBEDDED = new JavaType(
            "javax.persistence.Embedded");
    public static final JavaType EMBEDDED_ID = new JavaType(
            "javax.persistence.EmbeddedId");
    public static final JavaType ENTITY = new JavaType(
            "javax.persistence.Entity");
    public static final JavaType ENTITY_MANAGER = new JavaType(
            "javax.persistence.EntityManager");
    public static final JavaType ENUM_TYPE = new JavaType(
            "javax.persistence.EnumType");
    public static final JavaType ENUMERATED = new JavaType(
            "javax.persistence.Enumerated");
    public static final JavaType FETCH_TYPE = new JavaType(
            "javax.persistence.FetchType");
    public static final JavaType GENERATED_VALUE = new JavaType(
            "javax.persistence.GeneratedValue");
    public static final JavaType GENERATION_TYPE = new JavaType(
            "javax.persistence.GenerationType");
    public static final JavaType ID = new JavaType("javax.persistence.Id");
    public static final JavaType INHERITANCE = new JavaType(
            "javax.persistence.Inheritance");
    public static final JavaType INHERITANCE_TYPE = new JavaType(
            "javax.persistence.InheritanceType");
    public static final JavaType JOIN_COLUMN = new JavaType(
            "javax.persistence.JoinColumn");
    public static final JavaType JOIN_COLUMNS = new JavaType(
            "javax.persistence.JoinColumns");
    public static final JavaType JOIN_TABLE = new JavaType(
            "javax.persistence.JoinTable");
    public static final JavaType LOB = new JavaType("javax.persistence.Lob");
    public static final JavaType MANY_TO_MANY = new JavaType(
            "javax.persistence.ManyToMany");
    public static final JavaType MANY_TO_ONE = new JavaType(
            "javax.persistence.ManyToOne");
    public static final JavaType MAPPED_SUPERCLASS = new JavaType(
            "javax.persistence.MappedSuperclass");
    public static final JavaType ONE_TO_MANY = new JavaType(
            "javax.persistence.OneToMany");
    public static final JavaType ONE_TO_ONE = new JavaType(
            "javax.persistence.OneToOne");
    public static final JavaType PERSISTENCE_CONTEXT = new JavaType(
            "javax.persistence.PersistenceContext");
    public static final JavaType POST_PERSIST = new JavaType(
            "javax.persistence.PostPersist");
    public static final JavaType POST_UPDATE = new JavaType(
            "javax.persistence.PostUpdate");
    public static final JavaType PRE_REMOVE = new JavaType(
            "javax.persistence.PreRemove");
    public static final JavaType QUERY = new JavaType("javax.persistence.Query");
    public static final JavaType SEQUENCE_GENERATOR = new JavaType(
            "javax.persistence.SequenceGenerator");
    public static final JavaType TABLE = new JavaType("javax.persistence.Table");
    public static final JavaType TEMPORAL = new JavaType(
            "javax.persistence.Temporal");
    public static final JavaType TEMPORAL_TYPE = new JavaType(
            "javax.persistence.TemporalType");
    public static final JavaType TRANSIENT = new JavaType(
            "javax.persistence.Transient");
    public static final JavaType TYPED_QUERY = new JavaType(
            "javax.persistence.TypedQuery");
    public static final JavaType VERSION = new JavaType(
            "javax.persistence.Version");

    /**
     * Constructor is private to prevent instantiation
     */
    private JpaJavaType() {
    }
}