package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.MemberHoldingTypeDetailsMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Utility methods for finding members in {@link MemberHoldingTypeDetails}
 * instances.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.0
 */
public final class MemberFindingUtils {

    /**
     * Locates the metadata for an annotation of the specified type from within
     * the given list.
     * 
     * @param annotations the set of annotations to search (may be
     *            <code>null</code>)
     * @param annotationType the annotation to locate (may be <code>null</code>)
     * @return the annotation, or <code>null</code> if not found
     */
    public static AnnotationMetadata getAnnotationOfType(
            final List<? extends AnnotationMetadata> annotations,
            final JavaType annotationType) {
        if (annotations == null) {
            return null;
        }
        for (final AnnotationMetadata annotation : annotations) {
            if (annotation.getAnnotationType().equals(annotationType)) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * Returns the metadata for the annotation of the given type from within the
     * given metadata
     * 
     * @param metadata the metadata to search; can be <code>null</code>
     * @param annotationType the type of annotation for which to return the
     *            metadata; can be <code>null</code>
     * @return <code>null</code> if not found
     * @since 1.2.0
     */
    public static AnnotationMetadata getAnnotationOfType(
            final MemberHoldingTypeDetailsMetadataItem<?> metadata,
            final JavaType annotationType) {
        if (metadata == null || metadata.getMemberHoldingTypeDetails() == null) {
            return null;
        }
        return getAnnotationOfType(metadata.getMemberHoldingTypeDetails()
                .getAnnotations(), annotationType);
    }

    /**
     * Searches all {@link MemberDetails} and returns all constructors.
     * 
     * @param memberDetails the {@link MemberDetails} to search (required)
     * @return zero or more constructors (never null)
     * @deprecated use {@link MemberDetails#getConstructors()} instead
     */
    @Deprecated
    public static List<ConstructorMetadata> getConstructors(
            final MemberDetails memberDetails) {
        return memberDetails.getConstructors();
    }

    /**
     * Locates the specified constructor.
     * 
     * @param memberHoldingTypeDetails the {@link MemberHoldingTypeDetails} to
     *            search (required)
     * @param parameters to locate (can be null if there are no parameters)
     * @return the constructor, or null if not found
     * @deprecated use
     *             {@link MemberHoldingTypeDetails#getDeclaredConstructor(List)}
     *             instead
     */
    @Deprecated
    public static ConstructorMetadata getDeclaredConstructor(
            final MemberHoldingTypeDetails memberHoldingTypeDetails,
            final List<JavaType> parameters) {
        return memberHoldingTypeDetails.getDeclaredConstructor(parameters);
    }

    /**
     * Locates the specified field of the given {@link MemberHoldingTypeDetails}
     * .
     * 
     * @param memberHoldingTypeDetails the {@link MemberHoldingTypeDetails} to
     *            search (required)
     * @param fieldName to locate (required)
     * @return the field, or null if not found
     * @deprecated call
     *             {@link MemberHoldingTypeDetails#getDeclaredField(JavaSymbolName)}
     *             instead
     */
    @Deprecated
    public static FieldMetadata getDeclaredField(
            final MemberHoldingTypeDetails memberHoldingTypeDetails,
            final JavaSymbolName fieldName) {
        return memberHoldingTypeDetails.getDeclaredField(fieldName);
    }

    /**
     * Locates an inner type with the specified name.
     * 
     * @param MemberDetails to search (required)
     * @param typeName to locate (required)
     * @deprecated use
     *             {@link MemberHoldingTypeDetails#getDeclaredInnerType(JavaType)}
     *             instead
     */
    @Deprecated
    public static ClassOrInterfaceTypeDetails getDeclaredInnerType(
            final MemberHoldingTypeDetails memberHoldingTypeDetails,
            final JavaType typeName) {
        return memberHoldingTypeDetails.getDeclaredInnerType(typeName);
    }

    /**
     * Locates a method on the specified {@link MemberHoldingTypeDetails} based
     * on the method name.
     * 
     * @param memberHoldingTypeDetails the {@link MemberHoldingTypeDetails} to
     *            search; can be <code>null</code>
     * @param methodName to locate; can be <code>null</code>
     * @return the method, or <code>null</code> if the given name was
     *         <code>null</code> or it was simply not found
     */
    public static MethodMetadata getDeclaredMethod(
            final MemberHoldingTypeDetails memberHoldingTypeDetails,
            final JavaSymbolName methodName) {
        if (memberHoldingTypeDetails == null) {
            return null;
        }
        for (final MethodMetadata method : memberHoldingTypeDetails
                .getDeclaredMethods()) {
            if (method.getMethodName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Locates the specified method.
     * 
     * @param memberHoldingTypeDetails the {@link MemberHoldingTypeDetails} to
     *            search; can be <code>null</code>
     * @param methodName to locate; can be <code>null</code>
     * @param parameters to locate (can be null if there are no parameters)
     * @return the method, or <code>null</code> if the given name was
     *         <code>null</code> or it was simply not found
     */
    public static MethodMetadata getDeclaredMethod(
            final MemberHoldingTypeDetails memberHoldingTypeDetails,
            final JavaSymbolName methodName, List<JavaType> parameters) {
        if (memberHoldingTypeDetails == null) {
            return null;
        }
        if (parameters == null) {
            parameters = new ArrayList<JavaType>();
        }
        for (final MethodMetadata method : memberHoldingTypeDetails
                .getDeclaredMethods()) {
            if (method.getMethodName().equals(methodName)) {
                final List<JavaType> parameterTypes = AnnotatedJavaType
                        .convertFromAnnotatedJavaTypes(method
                                .getParameterTypes());
                if (parameterTypes.equals(parameters)) {
                    return method;
                }
            }
        }
        return null;
    }

    /**
     * Locates the specified type-level annotation.
     * 
     * @param builder the {@link MemberHoldingTypeDetails} to search (required)
     * @param type to locate (required)
     * @return the annotation, or null if not found
     * @deprecated use
     *             {@link AbstractIdentifiableAnnotatedJavaStructureBuilder#getDeclaredTypeAnnotation(JavaType)}
     *             instead
     */
    @Deprecated
    public static AnnotationMetadataBuilder getDeclaredTypeAnnotation(
            final AbstractIdentifiableAnnotatedJavaStructureBuilder<? extends IdentifiableAnnotatedJavaStructure> builder,
            final JavaType type) {
        return builder.getDeclaredTypeAnnotation(type);
    }

    /**
     * Locates the specified type-level annotation.
     * 
     * @param memberHoldingTypeDetails the {@link MemberHoldingTypeDetails} to
     *            search (required)
     * @param type to locate (required)
     * @return the annotation, or null if not found
     * @deprecated use
     *             {@link IdentifiableAnnotatedJavaStructure#getAnnotation(JavaType)}
     *             instead
     */
    @Deprecated
    public static AnnotationMetadata getDeclaredTypeAnnotation(
            final IdentifiableAnnotatedJavaStructure memberHoldingTypeDetails,
            final JavaType type) {
        return memberHoldingTypeDetails.getAnnotation(type);
    }

    /**
     * Locates the specified type-level annotation.
     * 
     * @param memberDetails the {@link MemberDetails} to search (required)
     * @param type to locate (required)
     * @return the annotation, or null if not found
     * @deprecated use {@link MemberDetails#getAnnotation(JavaType)} instead
     */
    @Deprecated
    public static AnnotationMetadata getDeclaredTypeAnnotation(
            final MemberDetails memberDetails, final JavaType type) {
        return memberDetails.getAnnotation(type);
    }

    /**
     * Searches up the inheritance hierarchy until the first field with the
     * specified name is located.
     * 
     * @param memberHoldingTypeDetails to search (required)
     * @param fieldName to locate (required)
     * @return the field, or null if not found
     * @deprecated use {@link MemberHoldingTypeDetails#getField(JavaSymbolName)}
     *             instead
     */
    @Deprecated
    public static FieldMetadata getField(
            final MemberHoldingTypeDetails memberHoldingTypeDetails,
            final JavaSymbolName fieldName) {
        return memberHoldingTypeDetails.getField(fieldName);
    }

    /**
     * Searches all {@link MemberDetails} and returns all fields.
     * 
     * @param memberDetails the {@link MemberDetails} to search (required)
     * @return zero or more fields (never null)
     * @deprecated use {@link MemberDetails#getFields()} instead
     */
    @Deprecated
    public static List<FieldMetadata> getFields(
            final MemberDetails memberDetails) {
        return memberDetails.getFields();
    }

    /**
     * Searches up the inheritance hierarchy and locates all declared fields
     * which are annotated with the specified annotation.
     * 
     * @param memberHoldingTypeDetails to search (required)
     * @param annotation to locate (required)
     * @return all the located fields (never null, but may be empty)
     * @deprecated use
     *             {@link MemberHoldingTypeDetails#getFieldsWithAnnotation(JavaType)}
     *             instead
     */
    @Deprecated
    public static List<FieldMetadata> getFieldsWithAnnotation(
            final MemberHoldingTypeDetails memberHoldingTypeDetails,
            final JavaType annotation) {
        return memberHoldingTypeDetails.getFieldsWithAnnotation(annotation);
    }

    /**
     * Returns all fields within the given {@link MemberDetails} that contain
     * the given {@link CustomData} tag.
     * 
     * @param memberDetails the {@link MemberDetails} to search (can be
     *            <code>null</code>)
     * @param tagKey the {@link CustomData} key to search for
     * @return zero or more fields (never <code>null</code>)
     */
    public static List<FieldMetadata> getFieldsWithTag(
            final MemberDetails memberDetails, final Object tagKey) {
        Validate.notNull(tagKey, "Custom data key required");
        final List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
        if (memberDetails != null) {
            for (final MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails
                    .getDetails()) {
                for (final FieldMetadata field : memberHoldingTypeDetails
                        .getDeclaredFields()) {
                    if (field.getCustomData().keySet().contains(tagKey)) {
                        fields.add(field);
                    }
                }
            }
        }
        return fields;
    }

    /**
     * Returns the first of the given types of annotation on the given class or
     * interface
     * 
     * @param cid the class or interface to check (can be <code>null</code>)
     * @param annotationTypes the types of annotation to look for, in order (can
     *            be <code>null</code>)
     * @return <code>null</code> if the given type or array of annotations is
     *         <code>null</code>, or none were found
     */
    public static AnnotationMetadata getFirstAnnotation(
            final ClassOrInterfaceTypeDetails cid,
            final JavaType... annotationTypes) {
        if (cid != null && annotationTypes != null) {
            for (final JavaType annotationType : annotationTypes) {
                final AnnotationMetadata annotation = MemberFindingUtils
                        .getAnnotationOfType(cid.getAnnotations(),
                                annotationType);
                if (annotation != null) {
                    return annotation;
                }
            }
        }
        return null;
    }

    /**
     * Searches all {@link MemberDetails} and returns all
     * {@link MemberHoldingTypeDetails} which contains a given
     * {@link CustomData} tag.
     * 
     * @param memberDetails the {@link MemberDetails} to search (can be
     *            <code>null</code>)
     * @param tagKey the {@link CustomData} key to search for (required)
     * @return zero or more {@link MemberHoldingTypeDetails} (never null)
     */
    public static List<MemberHoldingTypeDetails> getMemberHoldingTypeDetailsWithTag(
            final MemberDetails memberDetails, final Object tagKey) {
        Validate.notNull(tagKey, "Custom data tag required");
        final List<MemberHoldingTypeDetails> result = new ArrayList<MemberHoldingTypeDetails>();
        if (memberDetails != null) {
            for (final MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails
                    .getDetails()) {
                if (memberHoldingTypeDetails.getCustomData().keySet()
                        .contains(tagKey)) {
                    result.add(memberHoldingTypeDetails);
                }
            }
        }
        return result;
    }

    /**
     * Locates a method with the name presented. Searches all
     * {@link MemberDetails} until the first such method is located or none can
     * be found.
     * 
     * @param memberDetails the {@link MemberDetails} to search (required)
     * @param methodName the method name to locate (can be <code>null</code>)
     * @return the first located method, or <code>null</code> if the method name
     *         is <code>null</code> or such a method cannot be found
     * @deprecated use {@link MemberDetails#getMethod(JavaSymbolName)} instead
     */
    @Deprecated
    public static MethodMetadata getMethod(final MemberDetails memberDetails,
            final JavaSymbolName methodName) {
        return memberDetails.getMethod(methodName);
    }

    /**
     * Locates a method with the name and parameter signature presented.
     * Searches all {@link MemberDetails} until the first such method is located
     * or none can be found.
     * 
     * @param memberDetails the {@link MemberDetails} to search (required)
     * @param methodName the method name to locate (can be <code>null</code>)
     * @param parameters the method parameter signature to locate (can be null
     *            if no parameters are required)
     * @return the first located method, or <code>null</code> if the method name
     *         is <code>null</code> or such a method cannot be found
     * @deprecated use {@link MemberDetails#getMethod(JavaSymbolName, List)}
     *             instead
     */
    @Deprecated
    public static MethodMetadata getMethod(final MemberDetails memberDetails,
            final JavaSymbolName methodName, final List<JavaType> parameters) {
        return memberDetails.getMethod(methodName, parameters);
    }

    /**
     * Locates a method with the name and parameter signature presented that is
     * not declared by the presented MID.
     * 
     * @param memberDetails the {@link MemberDetails} to search (required)
     * @param methodName the method name to locate (can be <code>null</code>)
     * @param parameters the method parameter signature to locate (can be null
     *            if no parameters are required)
     * @param excludingMid the MID that a found method cannot be declared by
     * @return the first located method, or <code>null</code> if the method name
     *         is <code>null</code> or such a method cannot be found
     * @deprecated use
     *             {@link MemberDetails#getMethod(JavaSymbolName, List, String)}
     *             instead
     */
    @Deprecated
    public static MethodMetadata getMethod(final MemberDetails memberDetails,
            final JavaSymbolName methodName, final List<JavaType> parameters,
            final String excludingMid) {
        return memberDetails.getMethod(methodName, parameters, excludingMid);
    }

    /**
     * Convenience method which converts a String method name to a
     * {@link JavaSymbolName} for use by the standard
     * {@link #getMethod(MemberDetails, org.springframework.roo.model.JavaSymbolName)}
     * .
     * 
     * @param memberHoldingTypeDetails to search (required)
     * @param methodName to locate (required)
     * @return the method, or null if not found
     */
    public static MethodMetadata getMethod(final MemberDetails memberDetails,
            final String methodName) {
        Validate.notNull(methodName, "Method name required");
        return memberDetails.getMethod(new JavaSymbolName(methodName));
    }

    /**
     * Searches up the inheritance hierarchy until the first method with the
     * specified name is located, method parameters are not taken into account.
     * 
     * @param memberHoldingTypeDetails to search (required)
     * @param methodName to locate (required)
     * @return the method, or null if not found
     * @deprecated use
     *             {@link MemberHoldingTypeDetails#getMethod(JavaSymbolName)}
     *             instead
     */
    @Deprecated
    public static MethodMetadata getMethod(
            final MemberHoldingTypeDetails memberHoldingTypeDetails,
            final JavaSymbolName methodName) {
        return memberHoldingTypeDetails.getMethod(methodName);
    }

    /**
     * Searches up the inheritance hierarchy until the first method with the
     * specified name and parameters is located.
     * 
     * @param memberHoldingTypeDetails to search (required)
     * @param methodName to locate (required)
     * @param parameters to locate (can be null if there are no parameters)
     * @return the method, or null if not found
     * @deprecated use
     *             {@link MemberHoldingTypeDetails#getMethod(JavaSymbolName, List)}
     *             instead
     */
    @Deprecated
    public static MethodMetadata getMethod(
            final MemberHoldingTypeDetails memberHoldingTypeDetails,
            final JavaSymbolName methodName, final List<JavaType> parameters) {
        return memberHoldingTypeDetails.getMethod(methodName, parameters);
    }

    /**
     * Convenience method which converts a String method name to a
     * {@link JavaSymbolName} for use by the standard
     * {@link #getMethod(MemberHoldingTypeDetails, org.springframework.roo.model.JavaSymbolName)}
     * .
     * 
     * @param memberHoldingTypeDetails to search (required)
     * @param methodName to locate (required)
     * @return the method, or null if not found
     */
    public static MethodMetadata getMethod(
            final MemberHoldingTypeDetails memberHoldingTypeDetails,
            final String methodName) {
        return memberHoldingTypeDetails
                .getMethod(new JavaSymbolName(methodName));
    }

    /**
     * Searches all {@link MemberDetails} and returns all methods.
     * 
     * @param memberDetails the {@link MemberDetails} to search (required)
     * @return zero or more methods (never null)
     * @deprecated use {@link MemberDetails#getMethods()} instead
     */
    @Deprecated
    public static List<MethodMetadata> getMethods(
            final MemberDetails memberDetails) {
        return memberDetails.getMethods();
    }

    /**
     * Locates all methods on this class and its superclasses.
     * 
     * @param memberHoldingTypeDetails to search (required)
     * @return zero or more methods (never null)
     * @deprecated use {@link MemberHoldingTypeDetails#getMethods()} instead
     */
    @Deprecated
    public static List<MethodMetadata> getMethods(
            final MemberHoldingTypeDetails memberHoldingTypeDetails) {
        return memberHoldingTypeDetails.getMethods();
    }

    /**
     * Searches all {@link MemberDetails} and returns all methods which contain
     * a given {@link CustomData} tag.
     * 
     * @param memberDetails the {@link MemberDetails} to search (required)
     * @param tagKey the {@link CustomData} key to search for
     * @return zero or more methods (never null)
     * @deprecated use {@link MemberDetails#getMethodsWithTag(Object)} instead
     */
    @Deprecated
    public static List<MethodMetadata> getMethodsWithTag(
            final MemberDetails memberDetails, final Object tagKey) {
        return memberDetails.getMethodsWithTag(tagKey);
    }

    /**
     * Determines the most concrete {@link MemberHoldingTypeDetails} in cases
     * where multiple matches are found for a given tag.
     * 
     * @param memberDetails the {@link MemberDetails} to search (can be
     *            <code>null</code>)
     * @param tag the {@link CustomData} key to search for (required)
     * @return the most concrete tagged type or null if not found
     */
    public static MemberHoldingTypeDetails getMostConcreteMemberHoldingTypeDetailsWithTag(
            final MemberDetails memberDetails, final Object tag) {
        Validate.notNull(tag, "Custom data tag required");
        final List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList = getMemberHoldingTypeDetailsWithTag(
                memberDetails, tag);
        if (memberHoldingTypeDetailsList.isEmpty()) {
            return null;
        }
        return memberHoldingTypeDetailsList.get(memberHoldingTypeDetailsList
                .size() - 1);
    }

    /**
     * Determines the most concrete {@link MemberHoldingTypeDetails} in cases
     * where multiple matches are found for a given tag.
     * 
     * @param memberDetails the {@link MemberDetails} to search (can be
     *            <code>null</code>)
     * @param tagKey the {@link CustomData} key to search for (required)
     * @return the most concrete tagged method or <code>null</code> if not found
     */
    public static MethodMetadata getMostConcreteMethodWithTag(
            final MemberDetails memberDetails, final Object tagKey) {
        if (memberDetails == null) {
            return null;
        }
        return memberDetails.getMostConcreteMethodWithTag(tagKey);
    }

    /**
     * Locates an annotation on this class and its superclasses.
     * 
     * @param memberHoldingTypeDetails to search (required)
     * @param annotationType annotation to locate (required)
     * @return the annotation, or null if not found
     * @deprecated use
     *             {@link IdentifiableAnnotatedJavaStructure#getTypeAnnotation(JavaType)}
     *             instead
     */
    @Deprecated
    public static AnnotationMetadata getTypeAnnotation(
            final IdentifiableAnnotatedJavaStructure memberHoldingTypeDetails,
            final JavaType annotationType) {
        return memberHoldingTypeDetails.getTypeAnnotation(annotationType);
    }

    /**
     * Constructor is private to prevent instantiation
     * 
     * @since 1.2.0
     */
    private MemberFindingUtils() {
    }
}
