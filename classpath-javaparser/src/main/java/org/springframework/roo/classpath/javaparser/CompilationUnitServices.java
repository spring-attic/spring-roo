package org.springframework.roo.classpath.javaparser;

import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.body.TypeDeclaration;

import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * An interface that enables Java Parser types to query relevant information about a compilation unit.
 *
 * @author Ben Alex
 * @author James Tyrrell
 * @since 1.0
 *
 */
public interface CompilationUnitServices {

	List<ImportDeclaration> getImports();

	JavaPackage getCompilationUnitPackage();

	/**
	 * @return the enclosing type (never null)
	 */
	JavaType getEnclosingTypeName();
	
	/**
	 * @return the names of each inner type and the enclosing type (never null but may be empty)
	 */
	List<TypeDeclaration> getInnerTypes();

    PhysicalTypeCategory getPhysicalTypeCategory();
}
