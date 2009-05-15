package org.springframework.roo.classpath.details;

import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Represents the mutable operations available on a {@link ClassOrInterfaceTypeDetails} instance.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface MutableClassOrInterfaceTypeDetails extends ClassOrInterfaceTypeDetails {

	/**
	 * @return the type annotations (never null, but may be empty)
	 */
	List<? extends AnnotationMetadata> getTypeAnnotations();

	/**
	 * Adds a new type-level annotation. There must not already be an existing annotation of this
	 * {@link JavaType} defined on the type.
	 * 
	 * @param annotation to add (required)
	 */
	void addTypeAnnotation(AnnotationMetadata annotation);
	
	/**
	 * Removes the type-level annotation of the {@link JavaType} indicated. This annotation must
	 * already exist.
	 * 
	 * @param annotationType to remove (required)
	 */
	void removeTypeAnnotation(JavaType annotationType);
	
	/**
	 * Adds a new field. There must not be a field of this name already existing.
	 * 
	 * @param fieldMetadata to add (required)
	 */
	void addField(FieldMetadata fieldMetadata);
	
	/**
	 * Removes an existing field. A field with the specified name must already exist.
	 * 
	 * @param fieldName to remove (required)
	 */
	void removeField(JavaSymbolName fieldName);
	
	/**
	 * Adds a new method. A method with the same name are parameter types must not already exist.
	 * 
	 * @param methodMetadata to add (required)
	 */
	void addMethod(MethodMetadata methodMetadata);
	
}
