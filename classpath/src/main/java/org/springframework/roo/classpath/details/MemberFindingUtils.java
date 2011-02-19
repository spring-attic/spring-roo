package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Utility methods for finding members in {@link MemberHoldingTypeDetails} instances.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public abstract class MemberFindingUtils {

	/**
	 * Locates the specified field.
	 * 
	 * @param memberHoldingTypeDetails the {@link MemberHoldingTypeDetails} to search (required)
	 * @param fieldName to locate (required)
	 * @return the field, or null if not found
	 */
	public static final FieldMetadata getDeclaredField(MemberHoldingTypeDetails memberHoldingTypeDetails, JavaSymbolName fieldName) {
		Assert.notNull(memberHoldingTypeDetails, "Member holding type details required");
		Assert.notNull(fieldName, "Field name required");
		for (FieldMetadata field : memberHoldingTypeDetails.getDeclaredFields()) {
			if (field.getFieldName().equals(fieldName)) {
				return field;
			}
		}
		return null;
	}

	/**
	 * Locates the specified method.
	 * 
	 * @param memberHoldingTypeDetails the {@link MemberHoldingTypeDetails} to search (required)
	 * @param methodName to locate (required)
	 * @param parameters to locate (can be null if there are no parameters)
	 * @return the method, or null if not found
	 */
	public static final MethodMetadata getDeclaredMethod(MemberHoldingTypeDetails memberHoldingTypeDetails, JavaSymbolName methodName, List<JavaType> parameters) {
		Assert.notNull(memberHoldingTypeDetails, "Member holding type details required");
		Assert.notNull(methodName, "Method name required");
		if (parameters == null) {
			parameters = new ArrayList<JavaType>();
		}
		for (MethodMetadata method : memberHoldingTypeDetails.getDeclaredMethods()) {
			if (method.getMethodName().equals(methodName)) {
				if (parameters.equals(AnnotatedJavaType.convertFromAnnotatedJavaTypes(method.getParameterTypes()))) {
					return method;
				}
			}
		}
		return null;
	}

	/**
	 * Locates the specified constructor.
	 * 
	 * @param memberHoldingTypeDetails the {@link MemberHoldingTypeDetails} to search (required)
	 * @param parameters to locate (can be null if there are no parameters)
	 * @return the constructor, or null if not found
	 */
	public static final ConstructorMetadata getDeclaredConstructor(MemberHoldingTypeDetails memberHoldingTypeDetails, List<JavaType> parameters) {
		Assert.notNull(memberHoldingTypeDetails, "Member holding type details required");
		if (parameters == null) {
			parameters = new ArrayList<JavaType>();
		}
		for (ConstructorMetadata constructor : memberHoldingTypeDetails.getDeclaredConstructors()) {
			if (parameters.equals(AnnotatedJavaType.convertFromAnnotatedJavaTypes(constructor.getParameterTypes()))) {
				return constructor;
			}
		}
		return null;
	}

	/**
	 * Locates the specified type-level annotation.
	 * 
	 * @param memberHoldingTypeDetails the {@link MemberHoldingTypeDetails} to search (required)
	 * @param type to locate (required)
	 * @return the annotation, or null if not found
	 */
	public static final AnnotationMetadata getDeclaredTypeAnnotation(MemberHoldingTypeDetails memberHoldingTypeDetails, JavaType type) {
		Assert.notNull(memberHoldingTypeDetails, "Member holding type details required");
		Assert.notNull(type, "Annotation type to locate required");
		for (AnnotationMetadata md : memberHoldingTypeDetails.getAnnotations()) {
			if (md.getAnnotationType().equals(type)) {
				return md;
			}
		}
		return null;
	}

	/**
	 * Locates the specified type-level annotation.
	 * 
	 * @param memberDetails the {@link MemberDetails} to search (required)
	 * @param type to locate (required)
	 * @return the annotation, or null if not found
	 */
	public static final AnnotationMetadata getDeclaredTypeAnnotation(MemberDetails memberDetails, JavaType type) {
		Assert.notNull(memberDetails, "Member details required");
		Assert.notNull(type, "Annotation type to locate required");
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails.getDetails()) {
			AnnotationMetadata md = getDeclaredTypeAnnotation(memberHoldingTypeDetails, type);
			if (md != null) {
				return md;
			}
		}
		return null;
	}
	
	/**
	 * Locates a method with the name and parameter signature presented. Searches all {@link MemberDetails} until the first such method is located
	 * or none can be found.
	 * 
	 * @param memberDetails the {@link MemberDetails} to search (required)
	 * @param methodName the method name to locate (required)
	 * @param parameters the method parameter signature to locate (can be null if no parameters are required)
	 * @return the first located method, or null if such a method cannot be found
	 */
	public static final MethodMetadata getMethod(MemberDetails memberDetails, JavaSymbolName methodName, List<JavaType> parameters) {
		Assert.notNull(memberDetails, "Member details required");
		Assert.notNull(methodName, "Method name required");
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails.getDetails()) {
			MethodMetadata md = getDeclaredMethod(memberHoldingTypeDetails, methodName, parameters);
			if (md != null) {
				return md;
			}
		}
		return null;
	}

	/**
	 * Locates an annotation with the specified type from a list of annotations.
	 * 
	 * @param annotations to search (required)
	 * @param type to locate (required)
	 * @return the annotation, or null if not found
	 */
	public static final AnnotationMetadata getAnnotationOfType(List<? extends AnnotationMetadata> annotations, JavaType type) {
		Assert.notNull(annotations, "Annotations to search required");
		Assert.notNull(type, "Annotation type to locate required");
		for (AnnotationMetadata md : annotations) {
			if (md.getAnnotationType().equals(type)) {
				return md;
			}
		}
		return null;
	}

	/**
	 * Locates an annotation on this class and its superclasses.
	 * 
	 * @param classOrInterfaceTypeDetails to search (required)
	 * @param annotationType annotation to locate (required)
	 * @return the annotation, or null if not found
	 */
	public static final AnnotationMetadata getTypeAnnotation(ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails, JavaType annotationType) {
		Assert.notNull(classOrInterfaceTypeDetails, "Class or interface type details required");
		Assert.notNull(annotationType, "Annotation type required");
		ClassOrInterfaceTypeDetails current = classOrInterfaceTypeDetails;
		while (current != null) {
			AnnotationMetadata result = getDeclaredTypeAnnotation(current, annotationType);
			if (result != null) {
				return result;
			}
			current = current.getSuperclass();
		}
		return null;
	}

	/**
	 * Locates all methods on this class and its superclasses.
	 * 
	 * @param classOrInterfaceTypeDetails to search (required)
	 * @return zero or more methods (never null)
	 */
	public static final List<MethodMetadata> getMethods(ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails) {
		List<MethodMetadata> result = new ArrayList<MethodMetadata>();
		ClassOrInterfaceTypeDetails current = classOrInterfaceTypeDetails;
		while (current != null) {
			for (MethodMetadata methods : current.getDeclaredMethods()) {
				result.add(methods);
			}
			current = current.getSuperclass();
		}
		return result;
	}

	/**
	 * Searches up the inheritance hierarchy until the first field with the specified name is located.
	 * 
	 * @param classOrInterfaceTypeDetails to search (required)
	 * @param fieldName to locate (required)
	 * @return the field, or null if not found
	 */
	public static final FieldMetadata getField(ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails, JavaSymbolName fieldName) {
		Assert.notNull(classOrInterfaceTypeDetails, "Class or interface type details required");
		Assert.notNull(fieldName, "Field name required");
		ClassOrInterfaceTypeDetails current = classOrInterfaceTypeDetails;
		while (current != null) {
			FieldMetadata result = getDeclaredField(current, fieldName);
			if (result != null) {
				return result;
			}
			current = current.getSuperclass();
		}
		return null;
	}

	/**
	 * Searches up the inheritance hierarchy and locates all declared fields which are annotated with the specified annotation.
	 * 
	 * @param classOrInterfaceTypeDetails to search (required)
	 * @param annotation to locate (required)
	 * @return all the located fields (never null, but may be empty)
	 */
	public static final List<FieldMetadata> getFieldsWithAnnotation(ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails, JavaType annotation) {
		List<FieldMetadata> result = new ArrayList<FieldMetadata>();
		ClassOrInterfaceTypeDetails current = classOrInterfaceTypeDetails;
		while (current != null) {
			for (FieldMetadata field : current.getDeclaredFields()) {
				if (getAnnotationOfType(field.getAnnotations(), annotation) != null) {
					// Found the annotation on this field
					result.add(field);
				}
			}
			current = current.getSuperclass();
		}
		return result;
	}

	/**
	 * Searches up the inheritance hierarchy until the first method with the specified name and parameters is located.
	 * 
	 * @param classOrInterfaceTypeDetails to search (required)
	 * @param methodName to locate (required)
	 * @param parameters to locate (can be null if there are no parameters)
	 * @return the method, or null if not found
	 */
	public static final MethodMetadata getMethod(ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails, JavaSymbolName methodName, List<JavaType> parameters) {
		Assert.notNull(classOrInterfaceTypeDetails, "Class or interface type details required");
		Assert.notNull(methodName, "Method name required");
		if (parameters == null) {
			parameters = new ArrayList<JavaType>();
		}

		ClassOrInterfaceTypeDetails current = classOrInterfaceTypeDetails;
		while (current != null) {
			MethodMetadata result = getDeclaredMethod(current, methodName, parameters);
			if (result != null) {
				return result;
			}
			current = current.getSuperclass();
		}
		return null;
	}
	
	/**
	 * Searches all {@link MemberDetails} and returns all methods.
	 * 
	 * @param memberDetails the {@link MemberDetails} to search (required)
	 * @return zero or more methods (never null)
	 */
	public static final List<MethodMetadata> getMethods(MemberDetails memberDetails) {
		Assert.notNull(memberDetails, "Member details required");
		List<MethodMetadata> result = new ArrayList<MethodMetadata>();
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails.getDetails()) {
			result.addAll(memberHoldingTypeDetails.getDeclaredMethods());
		}
		return result;
	}
}
