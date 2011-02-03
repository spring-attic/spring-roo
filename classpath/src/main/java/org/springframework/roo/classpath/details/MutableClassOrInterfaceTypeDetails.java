package org.springframework.roo.classpath.details;

import java.util.Set;

import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Represents the mutable operations available on a {@link ClassOrInterfaceTypeDetails} instance.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface MutableClassOrInterfaceTypeDetails extends ClassOrInterfaceTypeDetails {

	/**
	 * Adds a new type-level annotation. There must not already be an existing annotation of this
	 * {@link JavaType} defined on the type.
	 * 
	 * @param annotation the annotation to add (required)
	 */
	void addTypeAnnotation(AnnotationMetadata annotation);
	
	/**
	 * Creates or updates the type-level annotation indicated. If the annotation does not exist,
	 * it is created automatically. If the annotation already exists, the attributes of that
	 * annotation will be preserved unless the same attribute is presented in the metadata passed
	 * to this method. If an attribute is present in the passed metadata, that attribute will
	 * entirely replace the existing attribute.
	 * 
	 * <p>
	 * For example, if an annotation of @RooFoo(a = 1, b = 2, c = 3) is currently present, and the
	 * annotation metadata passed to this method is for @RooFoo(c = 12, d = 4), the final annotation
	 * after this method completes will be @RooFoo(a = 1, b = 2, c = 12, d = 4).
	 * 
	 * <p>
	 * This method avoids changing the disk if there is no net change to the annotation already
	 * present. For example, if the existing annotation is @RooFoo(a = 1, b = 2, c = 3), and the new
	 * annotation metadata is @RooFoo(b = 2), there is no net change and thus the java source on disk
	 * remains unchanged.
	 * 
	 * <p>
	 * If there are attributes in the presented attributesToDeleteIfPresent Set, these attributes are
	 * removed from the annotation regardless of any attribute values that may have changed.
	 * 
	 * @param annotation the annotation to update (required)
	 * @param attributesToDeleteIfPresent attributes to delete from annotation
	 * @return true if the disk was changed, false otherwise
	 */
	boolean updateTypeAnnotation(AnnotationMetadata annotation, Set<JavaSymbolName> attributesToDeleteIfPresent);

	/**
	 * Removes the type-level annotation of the {@link JavaType} indicated. This annotation must
	 * already exist.
	 * 
	 * @param annotationType the annotation type to remove (required)
	 */
	void removeTypeAnnotation(JavaType annotationType);
	
	/**
	 * Adds a new field. There must not be a field of this name already existing.
	 * 
	 * @param fieldMetadata the field to add (required)
	 */
	void addField(FieldMetadata fieldMetadata);
	
	/**
	 * Removes an existing field. A field with the specified name must already exist.
	 * 
	 * @param fieldName the field name to remove (required)
	 */
	void removeField(JavaSymbolName fieldName);
	
	/**
	 * Inserts a new enum constant into an enum.
	 * 
	 * @param name the enum constant to add.
	 */
	void addEnumConstant(JavaSymbolName name);
	
	/**
	 * Adds a new method. A method with the same name are parameter types must not already exist.
	 * 
	 * @param methodMetadata the method to add (required)
	 */
	void addMethod(MethodMetadata methodMetadata);
}
