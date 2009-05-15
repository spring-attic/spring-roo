package org.springframework.roo.classpath.javaparser;

import java.util.List;

import org.springframework.roo.model.JavaPackage;

import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.expr.Expression;

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
	 * Forces the implementation to flush any changes.
	 */
	void flush();
}
