package org.springframework.roo.classpath.details;


import java.util.List;

import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Metadata concerning a particular method.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public interface MethodMetadata extends InvocableMemberMetadata {
	
	/**
	 * @return the return type (never null, even if void)
	 */
	JavaType getReturnType();
	
	/**
	 * @return the name of the method (never null)
	 */
	JavaSymbolName getMethodName();
	
	/**
	 * @return a list of types the method can throw (never null)
	 */
	List<JavaType> getThrowsTypes();

}
