package org.springframework.roo.addon.layers.repository.mongo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigInteger;

/**
 * Marks the annotated type as a Spring Data Mongo domain entity.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface RooMongoEntity {

    /**
     * The name of this annotation's attribute that specifies the managed domain
     * type.
     */
    String ID_TYPE_ATTRIBUTE = "identifierType";

    /**
     * @return the class of identifier that should be used (defaults to
     *         {@link BigInteger}; must be provided)
     */
    Class<?> identifierType() default BigInteger.class;
}
