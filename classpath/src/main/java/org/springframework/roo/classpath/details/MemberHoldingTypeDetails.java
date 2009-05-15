package org.springframework.roo.classpath.details;

import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaType;

/**
 * Provides information about the different members in a class, interface or aspect.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface MemberHoldingTypeDetails extends PhysicalTypeDetails {
	
	List<? extends MethodMetadata> getDeclaredMethods();
	
	List<? extends ConstructorMetadata> getDeclaredConstructors();
	
	List<? extends FieldMetadata> getDeclaredFields();
	
	/**
	 * Lists the type-level annotations.
	 * 
	 * <p>
	 * This includes those annotations declared on the type, together with those defined via the ITD
	 * "declare @type: DestinationType: @Annotation" feature.
	 * 
	 * @return an unmodifiable representation of annotations declared on this type (may be empty, but never null)
	 */
	List<? extends AnnotationMetadata> getTypeAnnotations();
	
	/**
	 * Lists the classes this type extends. This may be empty for a class or an interface.
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
