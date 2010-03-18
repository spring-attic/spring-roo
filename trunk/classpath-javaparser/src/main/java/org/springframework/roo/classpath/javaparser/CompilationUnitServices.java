package org.springframework.roo.classpath.javaparser;

import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.expr.Expression;

import java.util.List;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

/**
 * An interface that enables Java Parser types to request the compilation unit to be written to disk,
 * as well as find out relevant information about a compilation unit.
 *
 * <p>
 * This is generally useful if a Java Parser type internally stores an {@link Expression} or similar and
 * may support modifying its on-disk representation.
 * 
 * @author Ben Alex
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
	
	/**
	 * Forces the implementation to flush any changes.
	 */
	void flush();
}
