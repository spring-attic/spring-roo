package org.springframework.roo.classpath.details;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * Metadata concerning a particular import.
 * 
 * @author Ben Alex
 * @since 1.1.1
 */
public interface ImportMetadata extends IdentifiableJavaStructure {

	/**
	 * @return the import package (null if type import)
	 */
	JavaPackage getImportPackage();

    /**
	 * @return the import type (null if package import)
	 */
    JavaType getImportType();

    boolean isStatic();

	boolean isAsterisk();
	
}
