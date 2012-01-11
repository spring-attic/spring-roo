package org.springframework.roo.classpath.details;

import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Immutable representation of the members of a class, interface, enum or
 * aspect.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface MemberHoldingTypeDetails extends PhysicalTypeDetails,
        IdentifiableAnnotatedJavaStructure {

    /**
     * Indicates whether this type extends the given type. Equivalent to calling
     * {@link #getExtendsTypes()} and checking whether the given type is in the
     * returned list.
     * 
     * @param type the supertype being checked for (required)
     * @return see above
     */
    boolean extendsType(JavaType type);

    /**
     * Locates the constructor with the specified parameter types.
     * 
     * @param parameters to locate (can be null if there are no parameters)
     * @return the constructor, or <code>null</code> if not found
     * @since 1.2.0
     */
    ConstructorMetadata getDeclaredConstructor(List<JavaType> parameters);

    List<? extends ConstructorMetadata> getDeclaredConstructors();

    /**
     * Locates the specified field.
     * 
     * @param fieldName to locate (can be <code>null</code>)
     * @return the field, or <code>null</code> if not found
     * @since 1.2.0
     */
    FieldMetadata getDeclaredField(JavaSymbolName fieldName);

    List<? extends FieldMetadata> getDeclaredFields();

    List<? extends InitializerMetadata> getDeclaredInitializers();

    /**
     * Locates an inner type with the specified name.
     * 
     * @param typeName to locate (required)
     * @since 1.2.0
     */
    ClassOrInterfaceTypeDetails getDeclaredInnerType(JavaType typeName);

    List<ClassOrInterfaceTypeDetails> getDeclaredInnerTypes();

    List<? extends MethodMetadata> getDeclaredMethods();

    /**
     * Returns the names of any dynamic finders
     * 
     * @return a non-<code>null</code> list
     * @since 1.2.0
     */
    List<String> getDynamicFinderNames();

    /**
     * Lists the classes this type extends. This may be empty. Always empty in
     * the case of an enum.
     * <p>
     * While a {@link List} is used, normally in Java a class will only extend a
     * single other class. A {@link List} is used to support interfaces, as well
     * as support the special
     * "declare parents: DestinationType extends SuperclassType" feature of ITDs
     * which permits effectively multiple inheritance.
     * 
     * @return an unmodifiable representation of classes this type extends (may
     *         be empty, but never null)
     */
    List<JavaType> getExtendsTypes();

    /**
     * Searches up the inheritance hierarchy until the first field with the
     * specified name is located.
     * 
     * @param fieldName to locate (required)
     * @return the field, or null if not found
     * @since 1.2.0
     */
    FieldMetadata getField(JavaSymbolName fieldName);

    /**
     * Searches up the inheritance hierarchy and locates all declared fields
     * which are annotated with the specified annotation.
     * 
     * @param annotation to locate (required)
     * @return all the located fields (never null, but may be empty)
     * @since 1.2.0
     */
    List<FieldMetadata> getFieldsWithAnnotation(JavaType annotation);

    /**
     * Lists the classes this type implements. Always empty in the case of an
     * interface.
     * <p>
     * A {@link List} is used to support interfaces, as well as support the
     * special "declare parents: DestinationType implements SomeInterfaceType"
     * feature of ITDs.
     * 
     * @return an unmodifiable representation of classes this type implements
     *         (may be empty, but never null)
     */
    List<JavaType> getImplementsTypes();

    /**
     * If this is a layering component, for example a service or repository,
     * returns the domain entities managed by this component, otherwise returns
     * an empty list.
     * 
     * @return a non-<code>null</code> list (may be empty)
     * @since 1.2.0
     */
    List<JavaType> getLayerEntities();

    /**
     * Searches up the inheritance hierarchy until the first method with the
     * specified name is located; method parameters are not taken into account.
     * 
     * @param methodName to locate (required)
     * @return the method, or null if not found
     * @since 1.2.0
     */
    MethodMetadata getMethod(JavaSymbolName methodName);

    /**
     * Searches up the inheritance hierarchy until the first method with the
     * specified name and parameters is located.
     * 
     * @param methodName to locate (required)
     * @param parameters to locate (can be null if there are no parameters)
     * @return the method, or <code>null</code> if not found
     * @since 1.2.0
     */
    MethodMetadata getMethod(JavaSymbolName methodName,
            List<JavaType> parameters);

    /**
     * Locates all methods on this class and its superclasses.
     * 
     * @return zero or more methods (never null)
     */
    List<MethodMetadata> getMethods();

    /**
     * Generates a unique name for a field, starting from the given proposed
     * name and adding underscores until it's unique.
     * 
     * @param proposedName the proposed field name (required)
     * @return a non-<code>null</code> name that's unique within the governor
     * @see MemberFindingUtils#getField(org.springframework.roo.classpath.details.MemberHoldingTypeDetails,
     *      JavaSymbolName)
     * @since 1.2.0
     */
    JavaSymbolName getUniqueFieldName(final String proposedName);

    /**
     * Indicates whether this type implements the given types. Equivalent to
     * calling {@link #getImplementsTypes()} and checking whether the given
     * types are in the returned list.
     * 
     * @param type the interfaces being checked for (required)
     * @return see above
     */
    boolean implementsAny(JavaType... type);

    /**
     * Indicates whether this type implements the given interface. Equivalent to
     * calling {@link #getImplementsTypes()} and checking whether the given type
     * is in the returned list.
     * 
     * @param type the interface being checked for (required)
     * @return see above
     */
    boolean implementsType(JavaType interfaceType);
}
