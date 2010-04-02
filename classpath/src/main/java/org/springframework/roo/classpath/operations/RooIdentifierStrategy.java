package org.springframework.roo.classpath.operations;

/**
 * Provides identifier strategies for JPA entities.
 * 
 * <p>
 * AUTO, IDENTITY, SEQUENCE, and TABLE are the allowable values of the {@link javax.persistence.GenerationType} {@link Enum enumeration}. NONE is used to specify that no {@link javax.persistence.GeneratedValue) annotation is required.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public enum RooIdentifierStrategy {
	AUTO, IDENTITY, SEQUENCE, TABLE, NONE;
}
