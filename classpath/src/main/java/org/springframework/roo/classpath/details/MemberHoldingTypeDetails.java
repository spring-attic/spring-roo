package org.springframework.roo.classpath.details;

import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Immutable representation of the members in a class, interface, enum or aspect.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface MemberHoldingTypeDetails extends PhysicalTypeDetails, IdentifiableAnnotatedJavaStructure {
	
	/**
	 * Indicates whether this type extends the given type. Equivalent to
	 * calling {@link #getExtendsTypes()} and checking whether the given type
	 * is in the returned list.
	 * 
	 * @param type the supertype being checked for (required)
	 * @return see above
	 */
	boolean extendsType(JavaType type);

	/**
	 * Indicates whether this type implements the given types. Equivalent to
	 * calling {@link #getImplementsTypes()} and checking whether the given types
	 * are in the returned list.
	 * 
	 * @param type the interfaces being checked for (required)
	 * @return see above
	 */
	boolean implementsAny(JavaType... type);

	List<? extends MethodMetadata> getDeclaredMethods();

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
	 * @param fieldName to locate (required)
	 * @return the field, or <code>null</code> if not found
	 * @since 1.2.0
	 */
	FieldMetadata getDeclaredField(JavaSymbolName fieldName);

	List<? extends FieldMetadata> getDeclaredFields();

	List<? extends InitializerMetadata> getDeclaredInitializers();

	List<? extends ClassOrInterfaceTypeDetails> getDeclaredInnerTypes();
	
	/**
	 * Lists the classes this type extends. This may be empty. Always empty in the case of an enum.
	 * 
	 * <p>
	 * While a {@link List} is used, normally in Java a class will only extend a single other class.
	 * A {@link List} is used to support interfaces, as well as support 
	 * the special "declare parents: DestinationType extends SuperclassType"
	 * feature of ITDs which permits effectively multiple inheritance.
	 * 
	 * @return an unmodifiable representation of classes this type extends (may be empty, but never null)
	 */
	List<JavaType> getExtendsTypes();
	
	/**
	 * Lists the classes this type implements. Always empty in the case of an interface.
	 * 
	 * <p>
	 * A {@link List} is used to support interfaces, as well as support 
	 * the special "declare parents: DestinationType implements SomeInterfaceType"
	 * feature of ITDs.
	 * 
	 * @return an unmodifiable representation of classes this type implements (may be empty, but never null)
	 */
	List<JavaType> getImplementsTypes();
	
	/**
	 * Generates a unique name for a field, starting from the given
	 * proposed name and adding underscores until it's unique.
	 * 
	 * @param proposedName the proposed field name (required)
	 * @param prepend whether to prepend (<code>true</code>) or append
	 * (<code>false</code>) the underscores, if any
	 * TODO is it a bug or a feature that this parameter is necessary? Why
	 * wouldn't all callers generate unique names in the same way?
	 * @return a non-<code>null</code> name that's unique within the governor
	 * @see MemberFindingUtils#getField(org.springframework.roo.classpath.details.MemberHoldingTypeDetails, JavaSymbolName)
	 * @since 1.2.0
	 */
	JavaSymbolName getUniqueFieldName(final String proposedName, final boolean prepend);
}
