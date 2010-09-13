package org.springframework.roo.classpath.details;

import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Metadata concerning an invocable member, namely a method or constructor.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface InvocableMemberMetadata extends IdentifiableAnnotatedJavaStructure {
	
	/**
	 * @return the parameter types (never null, but may be an empty)
	 */
	List<AnnotatedJavaType> getParameterTypes();

	/**
	 * @return the parameter names, if available (never null, but may be an empty)
	 */
	List<JavaSymbolName> getParameterNames();

	/**
	 * @return a list of types that the invocable member can throw (never null)
	 */
	List<JavaType> getThrowsTypes();

	 /**
	  * @return the body of the method, if available (can be null if unavailable)
	  */
	 String getBody();

}