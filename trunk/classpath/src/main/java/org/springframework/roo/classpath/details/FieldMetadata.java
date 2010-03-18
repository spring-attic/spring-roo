package org.springframework.roo.classpath.details;

import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Metadata concerning a particular field.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface FieldMetadata extends IdentifiableMember {

	/**
	 * @return the type of field (never null)
	 */
	JavaType getFieldType();
	
	/**
	 * @return the name of the field (never null)
	 */
	JavaSymbolName getFieldName();
	
	/**
	 * @return annotations on this field (never null, but may be empty)
	 */
	List<AnnotationMetadata> getAnnotations();
	
	/**
	 * @return the field initializer, if known (may be null if there is no initializer)
	 */
	String getFieldInitializer();
	
}
