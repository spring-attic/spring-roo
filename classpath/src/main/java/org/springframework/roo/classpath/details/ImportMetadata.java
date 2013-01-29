package org.springframework.roo.classpath.details;

import org.springframework.roo.classpath.details.comments.CommentedJavaStructure;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * Metadata concerning a particular import.
 * <p>
 * As always with metadata types, instances of this class are immutable once
 * constructed.
 * 
 * @author James Tyrrell
 * @since 1.1.1
 */
public interface ImportMetadata extends IdentifiableJavaStructure, CommentedJavaStructure {

    /**
     * @return the import package (null if type import)
     */
    JavaPackage getImportPackage();

    /**
     * @return the import type (null if package import)
     */
    JavaType getImportType();

    /**
     * @return true if the import was a wildcard (eg "import com.foo.*;")
     */
    boolean isAsterisk();

    /**
     * @return true if the import used the "static" keyword
     */
    boolean isStatic();
}
