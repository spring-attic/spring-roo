package org.springframework.roo.classpath.details;

import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.model.JavaType;

/**
 * Immutable representation of the members in a class, interface, enum or aspect.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface MemberHoldingTypeDetails extends PhysicalTypeDetails, IdentifiableAnnotatedJavaStructure {
	
	List<? extends MethodMetadata> getDeclaredMethods();
	
	List<? extends ConstructorMetadata> getDeclaredConstructors();
	
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

}
