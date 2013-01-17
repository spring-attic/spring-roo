package org.springframework.roo.classpath.antlrjavaparser;

import com.github.antlrjavaparser.api.ImportDeclaration;
import com.github.antlrjavaparser.api.body.TypeDeclaration;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

import java.util.List;

/**
 * An interface that enables Java Parser types to query relevant information
 * about a compilation unit.
 * 
 * @author Ben Alex
 * @author James Tyrrell
 * @since 1.0
 */
public interface CompilationUnitServices {

    JavaPackage getCompilationUnitPackage();

    /**
     * @return the enclosing type (never null)
     */
    JavaType getEnclosingTypeName();

    List<ImportDeclaration> getImports();

    /**
     * @return the names of each inner type and the enclosing type (never null
     *         but may be empty)
     */
    List<TypeDeclaration> getInnerTypes();

    PhysicalTypeCategory getPhysicalTypeCategory();
}
