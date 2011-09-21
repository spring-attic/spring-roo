package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.MemberHoldingTypeDetailsMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Utility methods for finding members in {@link MemberHoldingTypeDetails} instances.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.0
 */
public final class MemberFindingUtils {

	/**
	 * Locates the specified field.
	 * 
	 * @param memberHoldingTypeDetails the {@link MemberHoldingTypeDetails} to search (required)
	 * @param fieldName to locate (required)
	 * @return the field, or null if not found
	 */
	public static FieldMetadata getDeclaredField(MemberHoldingTypeDetails memberHoldingTypeDetails, JavaSymbolName fieldName) {
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
	 * @param memberHoldingTypeDetails the {@link MemberHoldingTypeDetails} to search; can be <code>null</code>
	 * @param methodName to locate; can be <code>null</code>
	 * @param parameters to locate (can be null if there are no parameters)
	 * @return the method, or <code>null</code> if the given name was
	 * <code>null</code> or it was simply not found
	 */
	public static MethodMetadata getDeclaredMethod(final MemberHoldingTypeDetails memberHoldingTypeDetails, final JavaSymbolName methodName, List<JavaType> parameters) {
		if (memberHoldingTypeDetails == null) {
			return null;
		}
		if (parameters == null) {
			parameters = new ArrayList<JavaType>();
		}
		for (MethodMetadata method : memberHoldingTypeDetails.getDeclaredMethods()) {
			if (method.getMethodName().equals(methodName)) {
				List<JavaType> parameterTypes = AnnotatedJavaType.convertFromAnnotatedJavaTypes(method.getParameterTypes());
				if (parameterTypes.equals(parameters)) {
					return method;
				}
			}
		}
		return null;
	}

	/**
	 * Locates a method on the specified {@link MemberHoldingTypeDetails} based on the method name.
	 *
	 * @param memberHoldingTypeDetails the {@link MemberHoldingTypeDetails} to search; can be <code>null</code>
	 * @param methodName to locate; can be <code>null</code>
	 * @return the method, or <code>null</code> if the given name was
	 * <code>null</code> or it was simply not found
	 */
	public static MethodMetadata getDeclaredMethod(final MemberHoldingTypeDetails memberHoldingTypeDetails, final JavaSymbolName methodName) {
		if (memberHoldingTypeDetails == null) {
			return null;
		}
		for (MethodMetadata method : memberHoldingTypeDetails.getDeclaredMethods()) {
			if (method.getMethodName().equals(methodName)) {
				return method;
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
	public static ConstructorMetadata getDeclaredConstructor(MemberHoldingTypeDetails memberHoldingTypeDetails, List<JavaType> parameters) {
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
	public static AnnotationMetadata getDeclaredTypeAnnotation(IdentifiableAnnotatedJavaStructure memberHoldingTypeDetails, JavaType type) {
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
	 * @param memberHoldingTypeDetails the {@link MemberHoldingTypeDetails} to search (required)
	 * @param type to locate (required)
	 * @return the annotation, or null if not found
	 */
	public static AnnotationMetadataBuilder getDeclaredTypeAnnotation(AbstractIdentifiableAnnotatedJavaStructureBuilder<? extends IdentifiableAnnotatedJavaStructure> memberHoldingTypeDetails, JavaType type) {
		Assert.notNull(memberHoldingTypeDetails, "Member holding type details required");
		Assert.notNull(type, "Annotation type to locate required");
		for (AnnotationMetadataBuilder md : memberHoldingTypeDetails.getAnnotations()) {
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
	public static AnnotationMetadata getDeclaredTypeAnnotation(MemberDetails memberDetails, JavaType type) {
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
	 * @param methodName the method name to locate (can be <code>null</code>)
	 * @param parameters the method parameter signature to locate (can be null
	 * if no parameters are required)
	 * @return the first located method, or <code>null</code> if the method name
	 * is <code>null</code> or such a method cannot be found
	 */
	public static MethodMetadata getMethod(final MemberDetails memberDetails, final JavaSymbolName methodName, final List<JavaType> parameters) {
		Assert.notNull(memberDetails, "Member details required");
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails.getDetails()) {
			MethodMetadata md = getDeclaredMethod(memberHoldingTypeDetails, methodName, parameters);
			if (md != null) {
				return md;
			}
		}
		return null;
	}

	/**
	 * Locates a method with the name presented. Searches all {@link MemberDetails} until the first such method is located
	 * or none can be found.
	 *
	 * @param memberDetails the {@link MemberDetails} to search (required)
	 * @param methodName the method name to locate (can be <code>null</code>)
	 * @return the first located method, or <code>null</code> if the method name
	 * is <code>null</code> or such a method cannot be found
	 */
	public static MethodMetadata getMethod(final MemberDetails memberDetails, final JavaSymbolName methodName) {
		Assert.notNull(memberDetails, "Member details required");
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails.getDetails()) {
			MethodMetadata md = getDeclaredMethod(memberHoldingTypeDetails, methodName);
			if (md != null) {
				return md;
			}
		}
		return null;
	}

	/**
	 * Convenience method which converts a String method name to a {@link JavaSymbolName} for use by the standard
	 * {@link #getMethod(MemberDetails, org.springframework.roo.model.JavaSymbolName)}.
	 *
	 * @param memberHoldingTypeDetails to search (required)
	 * @param methodName to locate (required)
	 * @return the method, or null if not found
	 */
	public static MethodMetadata getMethod(final MemberDetails memberDetails, final String methodName) {
		Assert.notNull(methodName, "Method name required");
		return getMethod(memberDetails, new JavaSymbolName(methodName));
	}

	/**
	 * Locates a method with the name and parameter signature presented that is not declared by the presented MID.
	 * @param memberDetails the {@link MemberDetails} to search (required)
	 * @param methodName the method name to locate (can be <code>null</code>)
	 * @param parameters the method parameter signature to locate (can be null
	 * if no parameters are required)
	 * @param excludingMid the MID that a found method cannot be declared by
	 * @return the first located method, or <code>null</code> if the method name
	 * is <code>null</code> or such a method cannot be found
	 */
	public static MethodMetadata getMethod(final MemberDetails memberDetails, final JavaSymbolName methodName, final List<JavaType> parameters, String excludingMid) {
		Assert.notNull(memberDetails, "Member details required");
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails.getDetails()) {
			MethodMetadata md = getDeclaredMethod(memberHoldingTypeDetails, methodName, parameters);
			if (md != null && !md.getDeclaredByMetadataId().equals(excludingMid)) {
				return md;
			}
		}
		return null;
	}

	/**
	 * Locates the metadata for an annotation of the specified type from within
	 * the given list.
	 * 
	 * @param annotations the set of annotations to search (may be <code>null</code>)
	 * @param annotationType the annotation to locate (may be <code>null</code>)
	 * @return the annotation, or <code>null</code> if not found
	 */
	public static AnnotationMetadata getAnnotationOfType(final List<? extends AnnotationMetadata> annotations, final JavaType annotationType) {
		if (annotations == null) {
			return null;
		}
		for (final AnnotationMetadata md : annotations) {
			if (md.getAnnotationType().equals(annotationType)) {
				return md;
			}
		}
		return null;
	}
	
	/**
	 * Returns the metadata for the annotation of the given type from within the
	 * given metadata
	 * 
	 * @param metadata the metadata to search; can be <code>null</code>
	 * @param annotationType the type of annotation for which to return the metadata; can
	 * be <code>null</code>
	 * @return <code>null</code> if not found
	 * @since 1.2.0
	 */
	public static AnnotationMetadata getAnnotationOfType(final MemberHoldingTypeDetailsMetadataItem<?> metadata, final JavaType annotationType) {
		if (metadata == null || metadata.getMemberHoldingTypeDetails() == null) {
			return null;
		}
		return getAnnotationOfType(metadata.getMemberHoldingTypeDetails().getAnnotations(), annotationType);
	}

	/**
	 * Locates an inner type with the specified name.
	 * 
	 * @param MemberDetails to search (required)
	 * @param typeName to locate (required)
	 */
	public static ClassOrInterfaceTypeDetails getDeclaredInnerType(MemberHoldingTypeDetails memberHoldingTypeDetails, JavaType typeName) {
		Assert.notNull(memberHoldingTypeDetails, "Member holding type details required");
		Assert.notNull(typeName, "Name of inner type required");
		for (ClassOrInterfaceTypeDetails coitd : memberHoldingTypeDetails.getDeclaredInnerTypes()) {
			if (coitd.getName().getSimpleTypeName().equals(typeName.getSimpleTypeName())) {
				return coitd;
			}
		}
		return null;
	}

	/**
	 * Locates an annotation on this class and its superclasses.
	 * 
	 * @param memberHoldingTypeDetails to search (required)
	 * @param annotationType annotation to locate (required)
	 * @return the annotation, or null if not found
	 */
	public static AnnotationMetadata getTypeAnnotation(IdentifiableAnnotatedJavaStructure memberHoldingTypeDetails, JavaType annotationType) {
		Assert.notNull(memberHoldingTypeDetails, "Class or interface type details required");
		Assert.notNull(annotationType, "Annotation type required");
		IdentifiableAnnotatedJavaStructure current = memberHoldingTypeDetails;
		while (current != null) {
			AnnotationMetadata result = getDeclaredTypeAnnotation(current, annotationType);
			if (result != null) {
				return result;
			}
			if (current instanceof ClassOrInterfaceTypeDetails) {
				current = ((ClassOrInterfaceTypeDetails)current).getSuperclass();
			} else {
				current = null;
			}
		}
		return null;
	}

	/**
	 * Locates all methods on this class and its superclasses.
	 * 
	 * @param memberHoldingTypeDetails to search (required)
	 * @return zero or more methods (never null)
	 */
	public static List<MethodMetadata> getMethods(MemberHoldingTypeDetails memberHoldingTypeDetails) {
		List<MethodMetadata> result = new ArrayList<MethodMetadata>();
		MemberHoldingTypeDetails current = memberHoldingTypeDetails;
		while (current != null) {
			for (MethodMetadata methods : current.getDeclaredMethods()) {
				result.add(methods);
			}
			if (current instanceof ClassOrInterfaceTypeDetails) {
				current = ((ClassOrInterfaceTypeDetails)current).getSuperclass();
			} else {
				current = null;
			}
		}
		return result;
	}

	/**
	 * Searches up the inheritance hierarchy until the first field with the specified name is located.
	 * 
	 * @param memberHoldingTypeDetails to search (required)
	 * @param fieldName to locate (required)
	 * @return the field, or null if not found
	 */
	public static FieldMetadata getField(MemberHoldingTypeDetails memberHoldingTypeDetails, JavaSymbolName fieldName) {
		Assert.notNull(memberHoldingTypeDetails, "Member holding type details required");
		Assert.notNull(fieldName, "Field name required");
		MemberHoldingTypeDetails current = memberHoldingTypeDetails;
		while (current != null) {
			FieldMetadata result = getDeclaredField(current, fieldName);
			if (result != null) {
				return result;
			}
			if (current instanceof ClassOrInterfaceTypeDetails) {
				current = ((ClassOrInterfaceTypeDetails)current).getSuperclass();
			} else {
				current = null;
			}
		}
		return null;
	}

	/**
	 * Searches up the inheritance hierarchy and locates all declared fields which are annotated with the specified annotation.
	 * 
	 * @param memberHoldingTypeDetails to search (required)
	 * @param annotation to locate (required)
	 * @return all the located fields (never null, but may be empty)
	 */
	public static List<FieldMetadata> getFieldsWithAnnotation(MemberHoldingTypeDetails memberHoldingTypeDetails, JavaType annotation) {
		Assert.notNull(memberHoldingTypeDetails, "Member holding type details required");
		Assert.notNull(annotation, "Annotation required");
		List<FieldMetadata> result = new ArrayList<FieldMetadata>();
		MemberHoldingTypeDetails current = memberHoldingTypeDetails;
		while (current != null) {
			for (FieldMetadata field : current.getDeclaredFields()) {
				if (getAnnotationOfType(field.getAnnotations(), annotation) != null) {
					// Found the annotation on this field
					result.add(field);
				}
			}
			if (current instanceof ClassOrInterfaceTypeDetails) {
				current = ((ClassOrInterfaceTypeDetails)current).getSuperclass();
			} else {
				current = null;
			}
		}
		return result;
	}

	/**
	 * Searches up the inheritance hierarchy until the first method with the specified name and parameters is located.
	 * 
	 * @param memberHoldingTypeDetails to search (required)
	 * @param methodName to locate (required)
	 * @param parameters to locate (can be null if there are no parameters)
	 * @return the method, or null if not found
	 */
	public static MethodMetadata getMethod(MemberHoldingTypeDetails memberHoldingTypeDetails, JavaSymbolName methodName, List<JavaType> parameters) {
		Assert.notNull(memberHoldingTypeDetails, "Class or interface type details required");
		Assert.notNull(methodName, "Method name required");
		if (parameters == null) {
			parameters = new ArrayList<JavaType>();
		}

		MemberHoldingTypeDetails current = memberHoldingTypeDetails;
		while (current != null) {
			MethodMetadata result = getDeclaredMethod(current, methodName, parameters);
			if (result != null) {
				return result;
			}
			if (current instanceof ClassOrInterfaceTypeDetails) {
				current = ((ClassOrInterfaceTypeDetails)current).getSuperclass();
			} else {
				current = null;
			}
		}
		return null;
	}

	/**
	 * Searches up the inheritance hierarchy until the first method with the specified name is located, method parameters
	 * are not taken into account.
	 *
	 * @param memberHoldingTypeDetails to search (required)
	 * @param methodName to locate (required)
	 * @return the method, or null if not found
	 */
	public static MethodMetadata getMethod(MemberHoldingTypeDetails memberHoldingTypeDetails, JavaSymbolName methodName) {
		Assert.notNull(memberHoldingTypeDetails, "Class or interface type details required");
		Assert.notNull(methodName, "Method name required");

		MemberHoldingTypeDetails current = memberHoldingTypeDetails;
		while (current != null) {
			MethodMetadata result = getDeclaredMethod(current, methodName);
			if (result != null) {
				return result;
			}
			if (current instanceof ClassOrInterfaceTypeDetails) {
				current = ((ClassOrInterfaceTypeDetails)current).getSuperclass();
			} else {
				current = null;
			}
		}
		return null;
	}

	/**
	 * Convenience method which converts a String method name to a {@link JavaSymbolName} for use by the standard
	 * {@link #getMethod(MemberHoldingTypeDetails, org.springframework.roo.model.JavaSymbolName)}.
	 *
	 * @param memberHoldingTypeDetails to search (required)
	 * @param methodName to locate (required)
	 * @return the method, or null if not found
	 */
	public static MethodMetadata getMethod(MemberHoldingTypeDetails memberHoldingTypeDetails, String methodName) {
		Assert.notNull(methodName, "Method name required");
		return getMethod(memberHoldingTypeDetails, new JavaSymbolName(methodName));
	}
	
	/**
	 * Searches all {@link MemberDetails} and returns all constructors.
	 * 
	 * @param memberDetails the {@link MemberDetails} to search (required)
	 * @return zero or more constructors (never null)
	 */
	public static List<ConstructorMetadata> getConstructors(MemberDetails memberDetails) {
		Assert.notNull(memberDetails, "Member details required");
		List<ConstructorMetadata> result = new ArrayList<ConstructorMetadata>();
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails.getDetails()) {
			result.addAll(memberHoldingTypeDetails.getDeclaredConstructors());
		}
		return result;
	}

	/**
	 * Searches all {@link MemberDetails} and returns all methods.
	 * 
	 * @param memberDetails the {@link MemberDetails} to search (required)
	 * @return zero or more methods (never null)
	 */
	public static List<MethodMetadata> getMethods(MemberDetails memberDetails) {
		Assert.notNull(memberDetails, "Member details required");
		List<MethodMetadata> result = new ArrayList<MethodMetadata>();
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails.getDetails()) {
			result.addAll(memberHoldingTypeDetails.getDeclaredMethods());
		}
		return result;
	}
	
	/**
	 * Searches all {@link MemberDetails} and returns all fields.
	 * 
	 * @param memberDetails the {@link MemberDetails} to search (required)
	 * @return zero or more fields (never null)
	 */
	public static List<FieldMetadata> getFields(MemberDetails memberDetails) {
		Assert.notNull(memberDetails, "Member details required");
		List<FieldMetadata> result = new ArrayList<FieldMetadata>();
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails.getDetails()) {
			result.addAll(memberHoldingTypeDetails.getDeclaredFields());
		}
		return result;
	}
	
	/**
	 * Searches all {@link MemberDetails} and returns all methods which contain a given
	 * {@link CustomData} tag.
	 * 
	 * @param memberDetails the {@link MemberDetails} to search (required)
	 * @param tagKey the {@link CustomData} key to search for
	 * @return zero or more methods (never null)
	 */
	public static List<MethodMetadata> getMethodsWithTag(MemberDetails memberDetails, Object tagKey) {
		Assert.notNull(memberDetails, "Member details required");
		Assert.notNull(tagKey, "Custom data key required");
		List<MethodMetadata> result = new ArrayList<MethodMetadata>();
		for (MethodMetadata method: getMethods(memberDetails)) {
			if (method.getCustomData().keySet().contains(tagKey)) {
				result.add(method);
			}
		}
		return result;
	}
	
	/**
	 * Determines the most concrete {@link MemberHoldingTypeDetails} in cases where multiple matches are found for a given tag.
	 * 
	 * @param memberDetails the {@link MemberDetails} to search (can be <code>null</code>)
	 * @param tagKey the {@link CustomData} key to search for (required)
	 * @return the most concrete tagged method or <code>null</code> if not found
	 */
	public static MethodMetadata getMostConcreteMethodWithTag(final MemberDetails memberDetails, final Object tagKey) {
		if (memberDetails == null) {
			return null;
		}
		final List<MethodMetadata> taggedMethods = getMethodsWithTag(memberDetails, tagKey);
		if (taggedMethods.isEmpty()) {
			return null;
		} 
		return taggedMethods.get(0);
	}
	
	/**
	 * Returns all fields within the given {@link MemberDetails} that contain
	 * the given {@link CustomData} tag.
	 * 
	 * @param memberDetails the {@link MemberDetails} to search (can be <code>null</code>)
	 * @param tagKey the {@link CustomData} key to search for
	 * @return zero or more fields (never <code>null</code>)
	 */
	public static List<FieldMetadata> getFieldsWithTag(final MemberDetails memberDetails, final Object tagKey) {
		Assert.notNull(tagKey, "Custom data key required");
		final List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
		if (memberDetails != null) {
			for (final MemberHoldingTypeDetails mhtd: memberDetails.getDetails()) {
				for (final FieldMetadata field: mhtd.getDeclaredFields()) {
					if (field.getCustomData().keySet().contains(tagKey)) {
						fields.add(field);
					}
				}
			}
		}
		return fields;
	}
	
	/**
	 * Searches all {@link MemberDetails} and returns all {@link MemberHoldingTypeDetails} which contains a given
	 * {@link CustomData} tag.
	 * 
	 * @param memberDetails the {@link MemberDetails} to search (can be <code>null</code>)
	 * @param tagKey the {@link CustomData} key to search for (required)
	 * @return zero or more {@link MemberHoldingTypeDetails} (never null)
	 */
	public static List<MemberHoldingTypeDetails> getMemberHoldingTypeDetailsWithTag(final MemberDetails memberDetails, final Object tagKey) {
		Assert.notNull(tagKey, "Custom data tag required");
		final List<MemberHoldingTypeDetails> result = new ArrayList<MemberHoldingTypeDetails>();
		if (memberDetails != null) {
			for (final MemberHoldingTypeDetails mhtd: memberDetails.getDetails()) {
				if (mhtd.getCustomData().keySet().contains(tagKey)) {
					result.add(mhtd);
				}
			}
		}
		return result;
	}
	
	/**
	 * Determines the most concrete {@link MemberHoldingTypeDetails} in cases where multiple matches are found for a given tag.
	 * 
	 * @param memberDetails the {@link MemberDetails} to search (can be <code>null</code>)
	 * @param tag the {@link CustomData} key to search for (required)
	 * @return the most concrete tagged type or null if not found
	 */
	public static MemberHoldingTypeDetails getMostConcreteMemberHoldingTypeDetailsWithTag(final MemberDetails memberDetails, final Object tag) {
		Assert.notNull(tag, "Custom data tag required");
		final List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList = getMemberHoldingTypeDetailsWithTag(memberDetails, tag);
		if (memberHoldingTypeDetailsList.isEmpty()) {
			return null;
		}
		return memberHoldingTypeDetailsList.get(memberHoldingTypeDetailsList.size() - 1);
	}

	/**
	 * Indicates whether the given method has the same name (case-sensitive) as
	 * any of the other given methods
	 * 
	 * @param method the method to check against the others (required)
	 * @param otherMethods the methods to check against; can be empty or
	 * contain <code>null</code> elements, which will be ignored
	 * @return see above
	 * @since 1.2.0
	 */
	public static boolean hasSameName(final MethodMetadata method, final MethodMetadata... otherMethods) {
		Assert.notNull(method, "Method metadata required");
		for (final MethodMetadata otherMethod : otherMethods) {
			if (otherMethod != null && method.getMethodName().equals(otherMethod.getMethodName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Indicates whether the requesting MID is annotated with the specified annotation.
	 *
	 * @param memberDetails the {@link MemberDetails} to search (required)
	 * @param annotationMetadata the annotation to look for
	 * @param requestingMid the MID interested in
	 * @return see above
	 * @since 1.2.0
	 */
	public static boolean isRequestingAnnotatedWith(MemberDetails memberDetails, AnnotationMetadata annotationMetadata, String requestingMid) {
		Assert.notNull(memberDetails, "Member details required");
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails.getDetails()) {
			if (getAnnotationOfType(memberHoldingTypeDetails.getAnnotations(), annotationMetadata.getAnnotationType()) != null) {
				if (memberHoldingTypeDetails.getDeclaredByMetadataId().equals(requestingMid)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Indicates whether a method specified by the method attributes is present and isn't declared by the passed in MID.
	 *
	 * @param memberDetails the {@link MemberDetails} to search (required)
	 * @param methodName the name of the method being searched for
	 * @param parameterTypes the parameters of the method being searched for
	 * @param declaredByMetadataId the MID to be used to see if a found method is declared by the MID
	 * @return see above
	 * @since 1.2.0
	 */
	public static boolean isMethodDeclaredByAnother(MemberDetails memberDetails,  JavaSymbolName methodName, List<JavaType> parameterTypes, String declaredByMetadataId) {
		Assert.notNull(memberDetails, "Member details required");
		MethodMetadata methodMetadata = MemberFindingUtils.getMethod(memberDetails, methodName, parameterTypes);
		return methodMetadata != null && !methodMetadata.getDeclaredByMetadataId().equals(declaredByMetadataId);
	}

	public static AnnotationMetadata getFirstAnnotation(ClassOrInterfaceTypeDetails cid, JavaType... annotationTypes) {
		for (JavaType annotationType : annotationTypes) {
			AnnotationMetadata annotationMetadata = MemberFindingUtils.getAnnotationOfType(cid.getAnnotations(), annotationType);
			if (annotationMetadata != null) {
				return annotationMetadata;
			}
		}
		return null;
	}
	
	/**
	 * Constructor is private to prevent instantiation
	 * 
	 * @since 1.2.0
	 */
	private MemberFindingUtils() {}
}
