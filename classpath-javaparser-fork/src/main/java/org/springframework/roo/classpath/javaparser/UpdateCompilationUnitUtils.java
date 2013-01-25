package org.springframework.roo.classpath.javaparser;

import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.EnumConstantDeclaration;
import japa.parser.ast.body.EnumDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.PrimitiveType;
import japa.parser.ast.type.Type;
import japa.parser.ast.type.VoidType;
import japa.parser.ast.type.WildcardType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

/**
 * Utilities to update a Java Parser compilation unit from other
 * 
 * @author DiSiD Technologies
 * @since 1.2.2
 */
public class UpdateCompilationUnitUtils {

	/**
	 * Structure to store a {@link VariableDeclarator} and its
	 * {@link FieldDeclaration} together
	 * 
	 * @author DiSiD Technologies
	 */
	private static class FieldEntry {
		private final FieldDeclaration fieldDeclaration;
		private final VariableDeclarator variableDeclarator;

		FieldEntry(FieldDeclaration fieldDeclaration,
				VariableDeclarator variableDeclarator) {
			this.fieldDeclaration = fieldDeclaration;
			this.variableDeclarator = variableDeclarator;
		}
	}

	/**
	 * Compare two {@link ImportDeclaration}
	 * 
	 * @param declaration1
	 * @param declaration2
	 * @return true if are equals
	 */
	public static boolean equals(final ImportDeclaration declaration1,
			final ImportDeclaration declaration2) {
		return (declaration1.isAsterisk() == declaration2.isAsterisk())
				&& (declaration1.isStatic() == declaration2.isStatic())
				&& (declaration1.getName().getName().equals(declaration2
						.getName().getName()));
	}

	/**
	 * Compare two {@link Type}
	 * 
	 * @param type
	 * @param type2
	 * @return
	 */
	private static boolean equals(Type type, Type type2) {
		if (ObjectUtils.equals(type, type2)) {
			return true;
		}
		if (type.getClass() != type2.getClass()) {
			return false;
		}
		if (type instanceof ClassOrInterfaceType) {
			ClassOrInterfaceType cType = (ClassOrInterfaceType) type;
			ClassOrInterfaceType cType2 = (ClassOrInterfaceType) type2;
			return cType.getName().equals(cType2.getName());

		} else if (type instanceof PrimitiveType) {
			PrimitiveType pType = (PrimitiveType) type;
			PrimitiveType pType2 = (PrimitiveType) type2;
			return pType.getType() == pType2.getType();

		} else if (type instanceof VoidType) {
			return true;
		} else if (type instanceof WildcardType) {
			WildcardType wType = (WildcardType) type;
			WildcardType wType2 = (WildcardType) type2;
			return equals(wType.getSuper(), wType2.getSuper())
					&& equals(wType.getExtends(), wType2.getExtends());
		}
		return false;
	}

	/**
	 * Update {@code compilationUnit} imports, annotation, fields, methods...
	 * from {@code cidCompilationUnit} information
	 * 
	 * @param compilationUnit
	 * @param cidCompilationUnit
	 */
	public static void updateCompilationUnitImports(
			CompilationUnit compilationUnit, CompilationUnit cidCompilationUnit) {
		boolean notFound;
		List<ImportDeclaration> cidImports = new ArrayList<ImportDeclaration>();
		if (cidCompilationUnit.getImports() != null) {
			cidImports.addAll(cidCompilationUnit.getImports());
		}
		if (compilationUnit.getImports() != null) {
			for (Iterator<ImportDeclaration> originalImportIter = compilationUnit
					.getImports().iterator(); originalImportIter.hasNext();) {
				ImportDeclaration originalImport = originalImportIter.next();
				notFound = true;
				for (Iterator<ImportDeclaration> newImportIter = cidImports
						.iterator(); newImportIter.hasNext();) {
					ImportDeclaration newImport = newImportIter.next();
					if (equals(originalImport, newImport)) {
						// new Import found in original imports
						// remove from newImports to check
						newImportIter.remove();

						// Mark as found
						notFound = false;
					}
				}
				if (notFound) {
					// If not found in newImports remove from compilation unit
					originalImportIter.remove();
				}
			}
		}

		if (cidImports.isEmpty()) {
			// Done it
			return;
		}

		// Add missing new imports
		compilationUnit.getImports().addAll(cidImports);
	}

	/**
	 * Updates {@code compilationUnit} types from {@code cidCompilationUnit}
	 * information
	 * 
	 * @param compilationUnit
	 * @param cidCompilationUnit
	 */
	public static void updateCompilationUnitTypes(
			CompilationUnit compilationUnit, CompilationUnit cidCompilationUnit) {
		boolean notFound;
		List<TypeDeclaration> cidTypes = new ArrayList<TypeDeclaration>(
				cidCompilationUnit.getTypes());

		for (Iterator<TypeDeclaration> originalTypestIter = compilationUnit
				.getTypes().iterator(); originalTypestIter.hasNext();) {
			TypeDeclaration originalType = originalTypestIter.next();
			notFound = true;
			for (Iterator<TypeDeclaration> newTypeIter = cidTypes.iterator(); newTypeIter
					.hasNext();) {
				TypeDeclaration newType = newTypeIter.next();
				if (originalType.getName().equals(newType.getName())
						&& originalType.getClass() == newType.getClass()) {
					// new Type found in original imports
					if (originalType instanceof EnumDeclaration) {
						updateCompilationUnitEnumeration(
								(EnumDeclaration) originalType,
								(EnumDeclaration) newType);
					} else {
						updateCompilationUnitType(originalType, newType);
					}

					// remove from newImports to check
					newTypeIter.remove();

					// Mark as found
					notFound = false;
				}
			}

			if (notFound) {
				// If not found in newTypes so remove from compilation unit
				originalTypestIter.remove();
			}
		}

		if (cidTypes.isEmpty()) {
			// Done it
			return;
		}

		// Add missing new imports
		compilationUnit.getTypes().addAll(cidTypes);
	}

	/**
	 * Update {@code originalType} annotation, fields, methods... from
	 * {@code cidCompilationUnit} information
	 * 
	 * @param originalType
	 * @param newType
	 */
	public static void updateCompilationUnitType(TypeDeclaration originalType,
			TypeDeclaration newType) {

		if (originalType.getModifiers() != newType.getModifiers()) {
			originalType.setModifiers(newType.getModifiers());
		}

		if (originalType.getAnnotations() == null
				&& newType.getAnnotations() != null) {
			originalType.setAnnotations(new ArrayList<AnnotationExpr>());
		}
		updateAnnotations(originalType.getAnnotations(),
				newType.getAnnotations());

		updateFields(originalType, newType);

		updateConstructors(originalType, newType);

		updateMethods(originalType, newType);

		updateInnerTypes(originalType, newType);
	}

	/**
	 * Update {@code originalType} constructors from {@code cidCompilationUnit}
	 * information
	 * 
	 * @param originalType
	 * @param newType
	 */
	private static void updateConstructors(TypeDeclaration originalType,
			TypeDeclaration newType) {
		// Get a list of all constructors
		List<ConstructorDeclaration> cidConstructor = new ArrayList<ConstructorDeclaration>();
		if (newType.getMembers() != null) {
			for (BodyDeclaration element : newType.getMembers()) {
				if (element instanceof ConstructorDeclaration) {
					cidConstructor.add((ConstructorDeclaration) element);
				}
			}
		}

		ConstructorDeclaration originalConstructor, newConstructor;
		boolean notFound;
		// Iterate over every method definition
		if (originalType.getMembers() != null) {
			for (Iterator<BodyDeclaration> originalMemberstIter = originalType
					.getMembers().iterator(); originalMemberstIter.hasNext();) {
				BodyDeclaration originalMember = originalMemberstIter.next();
				if (!(originalMember instanceof ConstructorDeclaration)) {
					// this is not a method definition
					continue;
				}
				originalConstructor = (ConstructorDeclaration) originalMember;

				notFound = true;

				// look at cidConstructor for originalConstructor
				for (Iterator<ConstructorDeclaration> newConstructorIter = cidConstructor
						.iterator(); newConstructorIter.hasNext();) {
					newConstructor = newConstructorIter.next();
					// Check if is the same constructor (comparing its
					// parameters)
					if (equalsDeclaration(originalConstructor, newConstructor)) {
						notFound = false;

						// Remove from cid methods to check
						newConstructorIter.remove();

						// Update modifier if is changed
						if (originalConstructor.getModifiers() != newConstructor
								.getModifiers()) {
							originalConstructor.setModifiers(newConstructor
									.getModifiers());
						}

						// Update annotations if are changed
						if (!equalsAnnotations(
								originalConstructor.getAnnotations(),
								newConstructor.getAnnotations())) {
							originalConstructor.setAnnotations(newConstructor
									.getAnnotations());
						}

						// Update body if is changed
						if (!equals(originalConstructor.getBlock(),
								newConstructor.getBlock())) {
							originalConstructor.setBlock(newConstructor
									.getBlock());
						}
						break;

					}
				}
				if (notFound) {
					originalMemberstIter.remove();
				}
			}
		}

		if (cidConstructor.isEmpty()) {
			// Done it
			return;
		}

		// add new constructors
		if (originalType.getMembers() == null) {
			originalType.setMembers(new ArrayList<BodyDeclaration>());
		}
		originalType.getMembers().addAll(cidConstructor);
	}

	/**
	 * Compares {@code originalConstructor} declaration to
	 * {@code newConstructor} <br>
	 * This compares constructor parameters and its types
	 * 
	 * @param originalConstructor
	 * @param newConstructor
	 * @return true if declaration are equals
	 */
	private static boolean equalsDeclaration(
			ConstructorDeclaration originalConstructor,
			ConstructorDeclaration newConstructor) {
		if (!equalsParameters(originalConstructor.getParameters(),
				newConstructor.getParameters())) {
			return false;
		}
		return true;
	}

	/**
	 * Updates all subclasses of {@code originalType} from {@code newType}
	 * information
	 * 
	 * @param originalType
	 * @param newType
	 */
	private static void updateInnerTypes(TypeDeclaration originalType,
			TypeDeclaration newType) {

		// Get a list of all types
		List<TypeDeclaration> cidTypes = new ArrayList<TypeDeclaration>();
		if (newType.getMembers() != null) {
			for (BodyDeclaration element : newType.getMembers()) {
				if (element instanceof TypeDeclaration) {
					cidTypes.add((TypeDeclaration) element);
				}
			}
		}

		TypeDeclaration originalInner, newInner;
		boolean notFound;
		// Iterate over every type definition
		if (originalType.getMembers() != null) {
			for (Iterator<BodyDeclaration> originalMemberstIter = originalType
					.getMembers().iterator(); originalMemberstIter.hasNext();) {
				BodyDeclaration originalMember = originalMemberstIter.next();
				if (!(originalMember instanceof TypeDeclaration)) {
					// this is not a method definition
					continue;
				}
				originalInner = (TypeDeclaration) originalMember;

				notFound = true;
				// look at ciMethods for method
				for (Iterator<TypeDeclaration> newInnerIter = cidTypes
						.iterator(); newInnerIter.hasNext();) {
					newInner = newInnerIter.next();

					if (originalInner.getName().equals(newInner.getName())
							&& originalInner.getClass() == newInner.getClass()) {
						notFound = false;

						if (originalInner instanceof EnumDeclaration) {
							updateCompilationUnitEnumeration(
									(EnumDeclaration) originalInner,
									(EnumDeclaration) newInner);
						} else {
							updateCompilationUnitType(originalInner, newInner);
						}

						newInnerIter.remove();
						break;
					}

				}

				if (notFound) {
					originalMemberstIter.remove();
				}

			}
		}

		if (cidTypes.isEmpty()) {
			// Done it
			return;
		}

		// add new methods
		if (originalType.getMembers() == null) {
			originalType.setMembers(new ArrayList<BodyDeclaration>());
		}
		originalType.getMembers().addAll(cidTypes);

	}

	/**
	 * Updates {@code originalType} enumeration from {@code newType} information
	 * 
	 * @param originalType
	 * @param newType
	 */
	private static void updateCompilationUnitEnumeration(
			EnumDeclaration originalType, EnumDeclaration newType) {

		if (originalType.getModifiers() != newType.getModifiers()) {
			originalType.setModifiers(newType.getModifiers());
		}

		if (originalType.getAnnotations() == null
				&& newType.getAnnotations() != null) {
			originalType.setAnnotations(new ArrayList<AnnotationExpr>());
		}
		if (!equalsEnumConstants(originalType.getEntries(),newType.getEntries())){
			originalType.setEntries(newType.getEntries());
		}
		updateAnnotations(originalType.getAnnotations(),
				newType.getAnnotations());

		updateFields(originalType, newType);

		updateConstructors(originalType, newType);

		updateMethods(originalType, newType);

	}

	/** 
	 * Compares two {@link EnumConstantDeclaration} list
	 * @param entries
	 * @param entries2
	 * @return
	 */
	private static boolean equalsEnumConstants(
			List<EnumConstantDeclaration> entries,
			List<EnumConstantDeclaration> entries2) {
		if (ObjectUtils.equals(entries, entries2)){
			return true;
		}
		if (entries == null || entries2 == null){
			return false;
		}
		if (entries.size() != entries2.size()){
			return false;
		}
		for (int i = 0; i < entries.size(); i++) {
			EnumConstantDeclaration constant = entries.get(i);
			EnumConstantDeclaration constant2 = entries2.get(i);
			
			if (!equals(constant, constant2)){
				return false;
			}
		}
		return true;
	}

	/**
	 * Compares to {@link EnumConstantDeclaration}
	 * 
	 * @param constant
	 * @param constant2
	 * @return
	 */
	private static boolean equals(EnumConstantDeclaration constant,
			EnumConstantDeclaration constant2) {
		if (!constant.getName().equals(constant2.getName())){
			return false;
		}
		if (!equalsAnnotations(constant.getAnnotations(), constant2.getAnnotations())){
			return false;
		}
		if (ObjectUtils.equals(constant.getClassBody(), constant2.getClassBody())){
			return true;
		}
		if (constant.getClassBody() == null || constant2.getClassBody() == null){
			return false;
		}
		final List<BodyDeclaration> body = constant.getClassBody();
		final List<BodyDeclaration> body2 = constant2.getClassBody();
		if (body.size() != body2.size()){
			return false;
		}
		for (int i = 0; i < body.size(); i++) {
			BodyDeclaration item = body.get(i);
			BodyDeclaration item2 = body2.get(i);
			
			if (item.getClass() != item2.getClass()){
				return false;
			}
			// Compares contents
			if (!item.toString().equals(item2.toString())){
				return false;
			}
		}
		return true;
	}

	/**
	 * Updates {@code originalType} methods from {@code newType} information
	 * 
	 * @param originalType
	 * @param newType
	 */
	private static void updateMethods(TypeDeclaration originalType,
			TypeDeclaration newType) {
		// Get a list of all methods
		List<MethodDeclaration> cidMethods = new ArrayList<MethodDeclaration>();
		if (newType.getMembers() != null) {
			for (BodyDeclaration element : newType.getMembers()) {
				if (element instanceof MethodDeclaration) {
					cidMethods.add((MethodDeclaration) element);
				}
			}
		}

		MethodDeclaration originalMethod, newMethod;
		boolean notFound;
		// Iterate over every method definition
		if (originalType.getMembers() != null) {
			for (Iterator<BodyDeclaration> originalMemberstIter = originalType
					.getMembers().iterator(); originalMemberstIter.hasNext();) {
				BodyDeclaration originalMember = originalMemberstIter.next();
				if (!(originalMember instanceof MethodDeclaration)) {
					// this is not a method definition
					continue;
				}
				originalMethod = (MethodDeclaration) originalMember;

				notFound = true;

				// look at ciMethos for method
				for (Iterator<MethodDeclaration> newMethodsIter = cidMethods
						.iterator(); newMethodsIter.hasNext();) {
					newMethod = newMethodsIter.next();
					if (equals(originalMethod, newMethod)) {
						notFound = false;

						// Remove from cid methods to check
						newMethodsIter.remove();
						break;
					}
				}
				if (notFound) {
					originalMemberstIter.remove();
				}
			}
		}

		if (cidMethods.isEmpty()) {
			// Done it
			return;
		}

		// add new methods
		if (originalType.getMembers() == null) {
			originalType.setMembers(new ArrayList<BodyDeclaration>());
		}
		originalType.getMembers().addAll(cidMethods);
	}

	/**
	 * Compares two {@link MethodDeclaration}
	 * 
	 * @param originalMethod
	 * @param newMethod
	 * @return
	 */
	private static boolean equals(MethodDeclaration originalMethod,
			MethodDeclaration newMethod) {

		return originalMethod.getModifiers() == newMethod.getModifiers()
				&& originalMethod.getName().equals(newMethod.getName())
				&& equalsParameters(originalMethod.getParameters(),
						newMethod.getParameters())
				&& equals(originalMethod.getBody(), newMethod.getBody());
	}

	/**
	 * Compares two {@link BlockStmt}
	 * 
	 * @param body
	 * @param body2
	 * @return
	 */
	private static boolean equals(BlockStmt body, BlockStmt body2) {
		if (ObjectUtils.equals(body, body2)) {
			return true;
		}
		if (body == null || body2 == null) {
			return false;
		}
		if (body.getStmts().size() != body2.getStmts().size()) {
			return false;
		}
		List<Statement> statements = body.getStmts();
		List<Statement> statements2 = body2.getStmts();
		for (int i = 0; i < statements.size(); i++) {
			if (!equals(statements.get(i), statements2.get(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Compares tow {@link Statement}
	 * 
	 * @param statement
	 * @param statement2
	 * @return
	 */
	private static boolean equals(Statement statement, Statement statement2) {
		if (statement.getClass() != statement2.getClass()) {
			return false;
		}
		// TODO As Roo doesn't make statement changes we can ignore it
		return true;
	}

	/**
	 * Compares two {@link Parameter} list
	 * 
	 * @param parameters
	 * @param parameters2
	 * @return
	 */
	private static boolean equalsParameters(List<Parameter> parameters,
			List<Parameter> parameters2) {
		if (parameters == parameters2) {
			return true;
		}
		if (parameters == null || parameters2 == null) {
			return false;
		}
		if (parameters.size() != parameters2.size()) {
			return false;
		}

		Parameter parameter, parameter2;
		for (int i = 0; i < parameters.size(); i++) {
			parameter = parameters.get(i);
			parameter2 = parameters2.get(i);
			if (!parameter.getId().getName()
					.equals(parameter2.getId().getName())) {
				return false;
			}
			if (!equals(parameter.getType(), parameter2.getType())) {
				return false;
			}
			if (!equalsAnnotations(parameter.getAnnotations(),
					parameter2.getAnnotations())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Update {@code originalType} fields from {@code newType} information
	 * 
	 * @param originalType
	 * @param newType
	 */
	private static void updateFields(TypeDeclaration originalType,
			TypeDeclaration newType) {

		// Get a map of all fields (as FieldDeclaration could contain more than
		// one field)
		Map<String, FieldEntry> cidFields = new HashMap<String, FieldEntry>();
		String fieldName;
		FieldDeclaration field;
		if (newType.getMembers() != null) {
			for (BodyDeclaration element : newType.getMembers()) {
				if (element instanceof FieldDeclaration) {
					field = (FieldDeclaration) element;
					for (VariableDeclarator variable : field.getVariables()) {
						fieldName = variable.getId().getName();
						cidFields.put(fieldName,
								new FieldEntry(field, variable));
					}
				}
			}
		}

		// Iterate over every field definition
		if (originalType.getMembers() != null) {
			for (Iterator<BodyDeclaration> originalMemberstIter = originalType
					.getMembers().iterator(); originalMemberstIter.hasNext();) {
				BodyDeclaration originalMember = originalMemberstIter.next();
				if (!(originalMember instanceof FieldDeclaration)) {
					// this is not a field definition
					continue;
				}
				field = (FieldDeclaration) originalMember;

				// Check every variable declared in definition
				for (Iterator<VariableDeclarator> variablesIter = field
						.getVariables().iterator(); variablesIter.hasNext();) {
					VariableDeclarator originalVariable = variablesIter.next();
					fieldName = originalVariable.getId().getName();

					// look for field name in cid
					FieldEntry entry = cidFields.get(fieldName);
					if (entry == null) {
						// Not found: remove field from original compilation
						// unit
						variablesIter.remove();
						continue;
					}

					// Check modifiers, type and annotations
					if (equalFieldTypeModifiersAnnotations(field,
							entry.fieldDeclaration)) {
						// Variable declaration is equals:
						// remove from cid map as already exists
						cidFields.remove(fieldName);
					} else {
						// as there are more variable definition remove it
						// from original. At the end, process will create it
						// again
						// using new modifiers and type
						variablesIter.remove();
						// Modifiers changed
						if (field.getVariables().size() == 1) {
							// if no more variables update all field definition
							field.setModifiers(entry.fieldDeclaration
									.getModifiers());
							field.setType(entry.fieldDeclaration.getType());
							if (field.getAnnotations() == null
									&& entry.fieldDeclaration.getAnnotations() != null) {
								field.setAnnotations(new ArrayList<AnnotationExpr>());
							}
							updateAnnotations(field.getAnnotations(),
									entry.fieldDeclaration.getAnnotations());

							// remove processed field of cid
							cidFields.remove(fieldName);
							continue;
						}
					}

				}
				if (field.getVariables().isEmpty()) {
					originalMemberstIter.remove();
				}
			}
		}

		if (cidFields.isEmpty()) {
			// Done it
			return;
		}

		if (originalType.getMembers() == null) {
			originalType.setMembers(new ArrayList<BodyDeclaration>());
		}

		// Add new fields
		List<VariableDeclarator> variables;
		for (FieldEntry entry : cidFields.values()) {
			variables = new ArrayList<VariableDeclarator>(1);
			variables.add(entry.variableDeclarator);
			field = new FieldDeclaration(entry.fieldDeclaration.getModifiers(),
					entry.fieldDeclaration.getType(), variables);
			field.setAnnotations(entry.fieldDeclaration.getAnnotations());
			originalType.getMembers().add(field);
		}
	}

	/**
	 * Compares Type, modifies and annotation of two {@link FieldDeclaration}
	 * 
	 * @param fieldDeclaration
	 * @param fieldDeclaration2
	 * @return
	 */
	public static boolean equalFieldTypeModifiersAnnotations(
			FieldDeclaration fieldDeclaration,
			FieldDeclaration fieldDeclaration2) {
		return fieldDeclaration.getModifiers() == fieldDeclaration2
				.getModifiers()
				&& equals(fieldDeclaration.getType(),
						fieldDeclaration2.getType())
				&& equalsAnnotations(fieldDeclaration.getAnnotations(),
						fieldDeclaration2.getAnnotations());
	}

	/**
	 * Compares two {@link AnnotationExpr} list
	 * 
	 * @param annotations
	 * @param annotations2
	 * @return
	 */
	private static boolean equalsAnnotations(List<AnnotationExpr> annotations,
			List<AnnotationExpr> annotations2) {
		if (annotations == annotations2) {
			return true;
		} else if (annotations == null || annotations2 == null) {
			return false;
		}
		if (annotations.size() != annotations2.size()) {
			return false;
		}
		boolean found;
		for (AnnotationExpr annotation1 : annotations) {
			found = false;
			for (AnnotationExpr annotation2 : annotations2) {
				if (equals(annotation1, annotation2)) {
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Compares two annotation expression
	 * 
	 * @param annotation1
	 * @param annotation2
	 * @return
	 */
	public static boolean equals(AnnotationExpr annotation1,
			AnnotationExpr annotation2) {
		
		if (annotation1 == annotation2){
			return true;
		}
		if (annotation1 == null || annotation2 == null){
			return false;
		}
		if (!annotation1.getName().getName()
				.equals(annotation2.getName().getName())){
			return false;
		}
		// As we has no way (API) to make additional checks
		// check string representation (to check annotation parameters)
		return annotation1.toString().equals(annotation2.toString());
	}

	/**
	 * Update {@code annotations} with {@code }
	 * 
	 * @param annotations
	 * @param annotations2
	 */
	public static void updateAnnotations(List<AnnotationExpr> annotations,
			List<AnnotationExpr> annotations2) {
		if (!equalsAnnotations(annotations, annotations2)) {
			// XXX japa version (1.0.7) has no way to manage
			// AnnotationExpr
			annotations.clear();
			annotations.addAll(annotations2);
		}
	}
	

}
