package org.springframework.roo.classpath.details;

/**
 * Metadata concerning an initializer.
 * 
 * @author James Tyrrell
 * @since 1.1.1
 *
 */
public interface InitializerMetadata extends IdentifiableJavaStructure {

    boolean isStatic();

    String getBody();
}
