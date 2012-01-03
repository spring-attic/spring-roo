package org.springframework.roo.model;

/**
 * Constants for Hibernate {@link JavaType}s. Use them in preference to creating
 * new instances of these types.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public final class HibernateJavaType {

    public static final JavaType VALIDATOR_CONSTRAINTS_EMAIL = new JavaType(
            "org.hibernate.validator.constraints.Email");

    /**
     * Constructor is private to prevent instantiation
     */
    private HibernateJavaType() {
    }
}