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
	 * A field initializer is a class that provides an accessible no-argument constructor. This definition might
	 * be expanded in a future release of ROO (eg to support primitives and non-default constructors).
	 * 
	 * @return the field initializer, if known (may be null if there is no initializer)
	 */
	JavaType getFieldInitializer();
	
}
