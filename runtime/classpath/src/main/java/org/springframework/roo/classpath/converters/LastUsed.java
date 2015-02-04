package org.springframework.roo.classpath.converters;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.maven.Pom;

/**
 * Interface for {@link LastUsedImpl}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public interface LastUsed {

    /**
     * @return the package, either explicitly set or via a type set (may also be
     *         null if never set)
     */
    JavaPackage getJavaPackage();

    /**
     * @return the type or null
     */
    JavaType getJavaType();

    JavaPackage getTopLevelPackage();

    /**
     * Sets the package, and clears the type field. Ignores attempts to set to
     * java.*.
     */
    void setPackage(JavaPackage javaPackage);

    void setTopLevelPackage(JavaPackage topLevelPackage);

    /**
     * Sets the type, and also sets the package field. Ignores attempts to set
     * to java.*.
     */
    void setType(JavaType javaType);
    
    /**
     * Sets the type, and also sets the package field. Ignores attempts to set
     * to java.*. But with not verified
     */
    void setTypeNotVerified(JavaType javaType);

    /**
     * Sets the last used type and the module to which it belongs
     * 
     * @param javaType
     * @param module
     */
    void setType(JavaType javaType, Pom module);

    /**
     * Sets the last used type and the module to which it belongs not verified
     * 
     * @param result
     * @param module
     */
	void setTypeNotVerified(JavaType result, Pom module);

}