package org.springframework.roo.classpath.converters;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * Interface for {@link LastUsedImpl}.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
public interface LastUsed {

	/**
	 * Sets the package, and clears the type field. Ignores attempts to set to java.*.
	 */
	void setPackage(JavaPackage javaPackage);

	/**
	 * Sets the type, and also sets the package field. Ignores attempts to set to java.*.
	 */
	void setType(JavaType javaType);

	JavaPackage getTopLevelPackage();

	void setTopLevelPackage(JavaPackage topLevelPackage);

	/**
	 * @return the type or null
	 */
	JavaType getJavaType();

	/**
	 * @return the package, either explicitly set or via a type set (may also be null if never set)
	 */
	JavaPackage getJavaPackage();

}