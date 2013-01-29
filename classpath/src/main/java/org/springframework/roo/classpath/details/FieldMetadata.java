package org.springframework.roo.classpath.details;

import org.springframework.roo.classpath.details.comments.CommentedJavaStructure;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Metadata concerning a particular field.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface FieldMetadata extends IdentifiableAnnotatedJavaStructure, CommentedJavaStructure {

    /**
     * @return the field initializer, if known (may be null if there is no
     *         initializer)
     */
    String getFieldInitializer();

    /**
     * @return the name of the field (never null)
     */
    JavaSymbolName getFieldName();

    /**
     * @return the type of field (never null)
     */
    JavaType getFieldType();
}
