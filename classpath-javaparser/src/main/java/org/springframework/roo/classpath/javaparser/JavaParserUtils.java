package org.springframework.roo.classpath.javaparser;

import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.TypeParameter;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ModifierSet;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.ClassExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.FieldAccessExpr;
import japa.parser.ast.expr.MarkerAnnotationExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.NormalAnnotationExpr;
import japa.parser.ast.expr.QualifiedNameExpr;
import japa.parser.ast.expr.SingleMemberAnnotationExpr;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.PrimitiveType;
import japa.parser.ast.type.ReferenceType;
import japa.parser.ast.type.Type;
import japa.parser.ast.type.VoidType;
import japa.parser.ast.type.WildcardType;
import japa.parser.ast.type.PrimitiveType.Primitive;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.roo.model.DataType;
import org.springframework.roo.model.ImportRegistrationResolverImpl;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Assists with the usage of Java Parser.
 * 
 * <p>
 * This class is for internal use by the Java Parser module and should NOT be used by other code.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class JavaParserUtils  {
	
	/**
	 * Converts the presented class name into a name expression (either a {@link NamedExpr} or
	 * {@link QualifiedNameExpr} depending on whether a package was presented).
	 * 
	 * @param className to convert (required; can be fully qualified or simple name only)
	 * @return a compatible expression (never returns null)
	 */
	public static final NameExpr getNameExpr(String className) {
		Assert.hasText(className, "Class name required");
		
		if (className.contains(".")) {
			int offset = className.lastIndexOf(".");
			String packageName = className.substring(0, offset);
			String typeName = className.substring(offset+1);
			return new QualifiedNameExpr(new NameExpr(packageName), typeName);
		}
		return new NameExpr(className);
	}

	/**
	 * Converts a Java Parser modifier integer into a JDK {@link Modifier} integer.
	 * 
	 * @param modifiers the Java Parser int
	 * @return the equivalent JDK int
	 */
	public static final int getJdkModifier(int modifiers) {
		int result = 0;
		if (ModifierSet.isAbstract(modifiers)) {
			result = result |= Modifier.ABSTRACT;
		}
		if (ModifierSet.isFinal(modifiers)) {
			result = result |= Modifier.FINAL;
		}
		if (ModifierSet.isNative(modifiers)) {
			result = result |= Modifier.NATIVE;
		}
		if (ModifierSet.isPrivate(modifiers)) {
			result = result |= Modifier.PRIVATE;
		}
		if (ModifierSet.isProtected(modifiers)) {
			result = result |= Modifier.PROTECTED;
		}
		if (ModifierSet.isPublic(modifiers)) {
			result = result |= Modifier.PUBLIC;
		}
		if (ModifierSet.isStatic(modifiers)) {
			result = result |= Modifier.STATIC;
		}
		if (ModifierSet.isStrictfp(modifiers)) {
			result = result |= Modifier.STRICT;
		}
		if (ModifierSet.isSynchronized(modifiers)) {
			result = result |= Modifier.SYNCHRONIZED;
		}
		if (ModifierSet.isTransient(modifiers)) {
			result = result |= Modifier.TRANSIENT;
		}
		if (ModifierSet.isVolatile(modifiers)) {
			result = result |= Modifier.VOLATILE;
		}
		return result;
	}
	
	/**
	 * Converts a JDK {@link Modifier} integer into the equivalent Java Parser modifier.
	 * 
	 * @param modifiers the JDK int
	 * @return the equivalent Java Parser int
	 */
	public static final int getJavaParserModifier(int modifiers) {
		int result = 0;
		if (Modifier.isAbstract(modifiers)) {
			result = ModifierSet.addModifier(ModifierSet.ABSTRACT, result);
		}
		if (Modifier.isFinal(modifiers)) {
			result = ModifierSet.addModifier(ModifierSet.FINAL, result);
		}
		if (Modifier.isInterface(modifiers)) {
			// unsupported by Java Parser ModifierSet
		}
		if (Modifier.isNative(modifiers)) {
			result = ModifierSet.addModifier(ModifierSet.NATIVE, result);
		}
		if (Modifier.isPrivate(modifiers)) {
			result = ModifierSet.addModifier(ModifierSet.PRIVATE, result);
		}
		if (Modifier.isProtected(modifiers)) {
			result = ModifierSet.addModifier(ModifierSet.PROTECTED, result);
		}
		if (Modifier.isPublic(modifiers)) {
			result = ModifierSet.addModifier(ModifierSet.PUBLIC, result);
		}
		if (Modifier.isStatic(modifiers)) {
			result = ModifierSet.addModifier(ModifierSet.STATIC, result);
		}
		if (Modifier.isStrict(modifiers)) {
			result = ModifierSet.addModifier(ModifierSet.STRICTFP, result);
		}
		if (Modifier.isSynchronized(modifiers)) {
			result = ModifierSet.addModifier(ModifierSet.SYNCHRONIZED, result);
		}
		if (Modifier.isTransient(modifiers)) {
			result = ModifierSet.addModifier(ModifierSet.TRANSIENT, result);
		}
		if (Modifier.isVolatile(modifiers)) {
			result = ModifierSet.addModifier(ModifierSet.VOLATILE, result);
		}
		return result;
	}

	/**
	 * Obtains the name expression ({@link NameExpr}) for the passed {@link AnnotationExpr}, 
	 * which is the annotation's type.
	 * 
	 * @param annotationExpr to retrieve the type name from (required)
	 * @return the name (never null)
	 */
	public static final NameExpr getNameExpr(AnnotationExpr annotationExpr) {
		Assert.notNull(annotationExpr, "Annotation expression required");
		if (annotationExpr instanceof MarkerAnnotationExpr) {
			MarkerAnnotationExpr a = (MarkerAnnotationExpr) annotationExpr;
			NameExpr nameToFind = a.getName();
			Assert.notNull(nameToFind, "Unable to determine annotation name from '" + annotationExpr + "'");
			return nameToFind;
		} else if (annotationExpr instanceof SingleMemberAnnotationExpr) {
			SingleMemberAnnotationExpr a = (SingleMemberAnnotationExpr) annotationExpr;
			NameExpr nameToFind = a.getName();
			Assert.notNull(nameToFind, "Unable to determine annotation name from '" + annotationExpr + "'");
			return nameToFind;
		} else if (annotationExpr instanceof NormalAnnotationExpr) {
			NormalAnnotationExpr a = (NormalAnnotationExpr) annotationExpr;
			NameExpr nameToFind = a.getName();
			Assert.notNull(nameToFind, "Unable to determine annotation name from '" + annotationExpr + "'");
			return nameToFind;
		}
		throw new UnsupportedOperationException("Unknown annotation expression type '" + annotationExpr.getClass().getName() + "'");
	}

	/**
	 * Indicates whether two {@link NameExpr} expressions are equal.
	 * 
	 * <p>
	 * This method is necessary given {@link NameExpr} does not offer an equals method.
	 * 
	 * @param o1 the first entry to compare (null is acceptable)
	 * @param o2 the second entry to compare (null is acceptable)
	 * @return true if and only if both entries are identical
	 */
	private static final boolean isEqual(NameExpr o1, NameExpr o2) {
		if (o1 == null && o2 == null) {
			return true;
		}
		if (o1 == null && o2 != null) {
			return false;
		}
		if (o1 != null && o2 == null) {
			return false;
		}
		if (!o1.getName().equals(o2.getName())) {
			return false;
		}
		return o1.toString().equals(o2.toString());
	}

	/**
	 * Resolves the effective {@link JavaType} a {@link Type} represents. A {@link Type} includes low-level
	 * types such as void, arrays and primitives.
	 * 
	 * @param compilationUnitPackage to use for package resolution (required)
	 * @param imports to use for package resolution (required)
	 * @param type to locate (required)
	 * @param typeParameters names to consider type parameters (can be null if there are none)
	 * @return the {@link JavaType}, with proper indication of primitive and array status (never null)
	 */
	public static JavaType getJavaType(JavaPackage compilationUnitPackage, List<ImportDeclaration> imports, Type type, Set<JavaSymbolName> typeParameters) {
		Assert.notNull(imports, "Imports cannot be null");
		Assert.notNull(compilationUnitPackage, "Compilation unit package required");
		Assert.notNull(type, "The reference type must be provided");
		
		if (type instanceof VoidType) {
			return JavaType.VOID_PRIMITIVE;
		}
		
		int array = 0;

		Type internalType = type;
		if (internalType instanceof ReferenceType) {
			array = ((ReferenceType)internalType).getArrayCount();
			if (array > 0) {
				internalType = ((ReferenceType)internalType).getType();
			}
		}
		
		if (internalType instanceof PrimitiveType) {
			PrimitiveType pt = (PrimitiveType) internalType;
			if (pt.getType().equals(Primitive.Boolean)) {
				return new JavaType(Boolean.class.getName(), array, DataType.PRIMITIVE, null, null);
			}
			if (pt.getType().equals(Primitive.Char)) {
				return new JavaType(Character.class.getName(), array, DataType.PRIMITIVE, null, null);
			}
			if (pt.getType().equals(Primitive.Byte)) {
				return new JavaType(Byte.class.getName(), array, DataType.PRIMITIVE, null, null);
			}
			if (pt.getType().equals(Primitive.Short)) {
				return new JavaType(Short.class.getName(), array, DataType.PRIMITIVE, null, null);
			}
			if (pt.getType().equals(Primitive.Int)) {
				return new JavaType(Integer.class.getName(), array, DataType.PRIMITIVE, null, null);
			}
			if (pt.getType().equals(Primitive.Long)) {
				return new JavaType(Long.class.getName(), array, DataType.PRIMITIVE, null, null);
			}
			if (pt.getType().equals(Primitive.Float)) {
				return new JavaType(Float.class.getName(), array, DataType.PRIMITIVE, null, null);
			}
			if (pt.getType().equals(Primitive.Double)) {
				return new JavaType(Double.class.getName(), array, DataType.PRIMITIVE, null, null);
			}
			throw new IllegalStateException("Unsupported primitive '" + pt.getType() + "'");
		}
		
		if (internalType instanceof WildcardType) {
			// We only provide very primitive support for wildcard types; Roo only needs metadata at the end of the day,
			// not complete binding support from an AST
			WildcardType wt = (WildcardType) internalType;
			if (wt.getSuper() != null) {
				ReferenceType rt = (ReferenceType) wt.getSuper();
				ClassOrInterfaceType cit = (ClassOrInterfaceType) rt.getType();
				JavaType effectiveType = getJavaTypeNow(compilationUnitPackage, imports, cit, typeParameters);
				return new JavaType(effectiveType.getFullyQualifiedTypeName(), rt.getArrayCount(), effectiveType.getDataType(), JavaType.WILDCARD_SUPER, effectiveType.getParameters());
			} else if (wt.getExtends() != null) {
				ReferenceType rt = (ReferenceType) wt.getExtends();
				ClassOrInterfaceType cit = (ClassOrInterfaceType) rt.getType();
				JavaType effectiveType = getJavaTypeNow(compilationUnitPackage, imports, cit, typeParameters);
				return new JavaType(effectiveType.getFullyQualifiedTypeName(), rt.getArrayCount(), effectiveType.getDataType(), JavaType.WILDCARD_EXTENDS, effectiveType.getParameters());
			} else {
				return new JavaType("java.lang.Object", 0, DataType.TYPE, JavaType.WILDCARD_NEITHER, null);
			}
		}
		
		ClassOrInterfaceType cit;
		if (internalType instanceof ClassOrInterfaceType) {
			cit = (ClassOrInterfaceType) internalType;
		} else if (internalType instanceof ReferenceType) {
			cit = (ClassOrInterfaceType) ((ReferenceType)type).getType();
		} else {
			throw new IllegalStateException("The presented type '" + internalType.getClass() + "' with value '" + internalType + "' is unsupported by JavaParserUtils");
		}

		JavaType effectiveType = getJavaTypeNow(compilationUnitPackage, imports, cit, typeParameters);
		if (array > 0) {
			return new JavaType(effectiveType.getFullyQualifiedTypeName(), array, effectiveType.getDataType(), effectiveType.getArgName(), effectiveType.getParameters());
		}
		
		return effectiveType;
	}
	
	/**
	 * Resolves the effective {@link JavaType} a {@link NameExpr} represents.
	 * 
	 * <p>
	 * You should use {@link #getJavaType(JavaPackage, List, ClassOrInterfaceType)} where possible so that
	 * type arguments are preserved (a {@link NameExpr} does not contain type arguments).
	 * 
	 * <p>
	 * A name expression can be either qualified or unqualified. If qualified, that represents the fully-qualified name.
	 * If a name expression is unqualified, the imports are scanned. If the unqualified name expression is found in the
	 * imports, that import declaration represents the fully-qualified name. If the unqualified name expression is not
	 * found in the imports, it indicates the name to find is either in the same package as the qualified name expression,
	 * or the type relates to a member of java.lang. If part of java.lang, the
	 * fully qualified name is treated as part of java.lang. Otherwise the compilation unit package plus unqualified name
	 * expression represents the fully qualified name expression.
	 * 
	 * @param compilationUnitPackage the package that the compilation unit belongs to (required)
	 * @param imports the compilation unit's imports (required)
	 * @param nameToFind to locate (required)
	 * @param typeParameters names to consider type parameters (can be null if there are none)
	 * @return the effective Java type (never null)
	 */
	public static final JavaType getJavaType(JavaPackage compilationUnitPackage, List<ImportDeclaration> imports, NameExpr nameToFind, Set<JavaSymbolName> typeParameters) {
		Assert.notNull(imports, "Compilation unit imports required");
		Assert.notNull(compilationUnitPackage, "Compilation unit package required");
		Assert.notNull(nameToFind, "Name to find is required");
		
		if (nameToFind instanceof QualifiedNameExpr) {
			QualifiedNameExpr qne = (QualifiedNameExpr) nameToFind;
			return new JavaType(qne.toString());
		}
		
		// Unqualified name detected, so check if it's in the type parameter list
		if (typeParameters != null && typeParameters.contains(new JavaSymbolName(nameToFind.getName()))) {
			return new JavaType(nameToFind.getName(), 0, DataType.VARIABLE, null, null);
		}
		
		ImportDeclaration importDeclaration = getImportDeclarationFor(imports, nameToFind);
		if (importDeclaration  == null) {
			if (ImportRegistrationResolverImpl.isPartOfJavaLang(nameToFind.getName())) {
				return new JavaType("java.lang." + nameToFind.getName());
			}
			String name = compilationUnitPackage.getFullyQualifiedPackageName() == "" ? nameToFind.getName() : compilationUnitPackage.getFullyQualifiedPackageName() + "." + nameToFind.getName();
			return new JavaType(name);
		}
		
		return new JavaType(importDeclaration.getName().toString());
	}
	
	/**
	 * Converts the indicated {@link JavaType} into a {@link ReferenceType}.
	 * 
	 * <p>
	 * Note that no effort is made to manage imports etc.
	 * 
	 * @param nameExpr to convert (required)
	 * @return the corresponding {@link ReferenceType} (never null)
	 */
	public static final ReferenceType getReferenceType(NameExpr nameExpr) {
		Assert.notNull(nameExpr, "Java type required");
		return new ReferenceType(getClassOrInterfaceType(nameExpr));
	}
	
	/**
	 * Converts the indicated {@link JavaType} into a {@link ClassOrInterfaceType}.
	 * 
	 * <p>
	 * Note that no effort is made to manage imports etc.
	 * 
	 * @param nameExpr to convert (required)
	 * @return the corresponding {@link ClassOrInterfaceType} (never null)
	 */
	public static final ClassOrInterfaceType getClassOrInterfaceType(NameExpr nameExpr) {
		Assert.notNull(nameExpr, "Java type required");
		if (nameExpr instanceof QualifiedNameExpr) {
			QualifiedNameExpr qne = (QualifiedNameExpr) nameExpr;
			return new ClassOrInterfaceType(qne.getQualifier().getName() + "." + qne.getName());
		}
		return new ClassOrInterfaceType(nameExpr.getName());
	}
	
	/**
	 * Given a primitive type, computes the corresponding Java Parser type.
	 * 
	 * <p>
	 * Presenting a non-primitive type to this method will throw an exception. If you have a
	 * non-primitive type, use {@link #importTypeIfRequired(JavaPackage, List, JavaType)} and
	 * then present the {@link NameExpr} it returns to {@link #getClassOrInterfaceType(NameExpr)}.
	 * 
	 * @param javaType a primitive type (required, and must be primitive)
	 * @return the equivalent Java Parser {@link Type}
	 */
	public static final Type getType(JavaType javaType) {
		Assert.notNull(javaType, "Java type required");
		Assert.isTrue(javaType.isPrimitive(), "Java type must be primitive to be presented to this method");
		if (javaType.equals(JavaType.VOID_PRIMITIVE)) {
			return new VoidType();
		} else if (javaType.equals(JavaType.BOOLEAN_PRIMITIVE)) {
			return new PrimitiveType(Primitive.Boolean);
		} else if (javaType.equals(JavaType.BYTE_PRIMITIVE)) {
			return new PrimitiveType(Primitive.Byte);
		} else if (javaType.equals(JavaType.CHAR_PRIMITIVE)) {
			return new PrimitiveType(Primitive.Char);
		} else if (javaType.equals(JavaType.DOUBLE_PRIMITIVE)) {
			return new PrimitiveType(Primitive.Double);
		} else if (javaType.equals(JavaType.FLOAT_PRIMITIVE)) {
			return new PrimitiveType(Primitive.Float);
		} else if (javaType.equals(JavaType.INT_PRIMITIVE)) {
			return new PrimitiveType(Primitive.Int);
		} else if (javaType.equals(JavaType.LONG_PRIMITIVE)) {
			return new PrimitiveType(Primitive.Long);
		} else if (javaType.equals(JavaType.SHORT_PRIMITIVE)) {
			return new PrimitiveType(Primitive.Short);
		}
		throw new IllegalStateException("Unknown primitive " + javaType);
		
	}

	/**
	 * Resolves the effective {@link JavaType} a {@link ClassOrInterfaceType} represents, including any
	 * type arguments.
	 * 
	 * @param compilationUnitPackage the package that the compilation unit belongs to (required)
	 * @param imports the compilation unit's imports (required)
	 * @param cit the class or interface type to resolve (required)
	 * @return the effective Java type (never null)
	 */
	public static final JavaType getJavaTypeNow(JavaPackage compilationUnitPackage, List<ImportDeclaration> imports, ClassOrInterfaceType cit, Set<JavaSymbolName> typeParameters) {
		Assert.notNull(imports, "Compilation unit imports required");
		Assert.notNull(compilationUnitPackage, "Compilation unit package required");
		Assert.notNull(cit, "ClassOrInterfaceType required");
		String typeName = cit.getName();
		ClassOrInterfaceType scope = cit.getScope();
		while (scope != null) {
			typeName = scope.getName() + "." + typeName;
			scope = scope.getScope();
		}
		NameExpr nameExpr = getNameExpr(typeName);
		
		JavaType effectiveType = getJavaType(compilationUnitPackage, imports, nameExpr, typeParameters);
		
		// Handle any type arguments
		List<JavaType> typeParams = new ArrayList<JavaType>();
		if (cit.getTypeArgs() != null) {
			for (Type ta : cit.getTypeArgs()) {
				typeParams.add(getJavaType(compilationUnitPackage, imports, ta, typeParameters));
			}
		}
		
		return new JavaType(effectiveType.getFullyQualifiedTypeName(), effectiveType.getArray(), effectiveType.getDataType(), null, typeParams);
	}
	
	/**
	 * Resolves the effective {@link JavaType} a {@link ClassOrInterfaceDeclaration} represents, including any
	 * type parameters.
	 * 
	 * @param compilationUnitPackage the package that the compilation unit belongs to (required)
	 * @param imports the compilation unit's imports (required)
	 * @param cid the class or interface declaration to resolve (required)
	 * @return the effective Java type (never null)
	 */
	public static final JavaType getJavaType(JavaPackage compilationUnitPackage, List<ImportDeclaration> imports, ClassOrInterfaceDeclaration cid) {
		Assert.notNull(imports, "Compilation unit imports required");
		Assert.notNull(compilationUnitPackage, "Compilation unit package required");
		Assert.notNull(cid, "ClassOrInterfaceType required");
		
		// Convert the ClassOrInterfaceDeclaration name into a JavaType
		NameExpr nameExpr = getNameExpr(cid.getName());
		JavaType effectiveType = getJavaType(compilationUnitPackage, imports, nameExpr, null);
		
		// Populate JavaType with type parameters
		List<JavaType> typeParams = new ArrayList<JavaType>();
		List<TypeParameter> params = cid.getTypeParameters();
		if (params != null) {
			Set<JavaSymbolName> locatedTypeParams = new HashSet<JavaSymbolName>();
			for (TypeParameter candidate : params) {
				JavaSymbolName currentTypeParam = new JavaSymbolName(candidate.getName());
				locatedTypeParams.add(currentTypeParam);
				JavaType javaType = null;
				if (candidate.getTypeBound() == null) {
					javaType = new JavaType("java.lang.Object", 0, DataType.TYPE, currentTypeParam, null);
				} else {
					ClassOrInterfaceType cit = candidate.getTypeBound().get(0);
					javaType = JavaParserUtils.getJavaTypeNow(compilationUnitPackage, imports, cit, locatedTypeParams);
					javaType = new JavaType(javaType.getFullyQualifiedTypeName(), javaType.getArray(), javaType.getDataType(), currentTypeParam, javaType.getParameters());
				}
				typeParams.add(javaType);
			}
		}
		
		return new JavaType(effectiveType.getFullyQualifiedTypeName(), effectiveType.getArray(), effectiveType.getDataType(), null, typeParams);
	}

	/**
	 * Looks up the import declaration applicable to the presented name expression.
	 * 
	 * <p>
	 * If a fully-qualified name is passed to this method, the corresponding import will be evaluated for a complete match.
	 * If a simple name is passed to this method, the corresponding import will be evaluated if its simple name matches.
	 * This therefore reflects the normal Java semantics for using simple type names that have been imported.
	 * 
	 * @param imports the compilation unit's imports (required)
	 * @param nameExpr the expression to locate an import for (which would generally be a {@link NameExpr} and thus not have a package identifier; required)
	 * @return the relevant import, or null if there is no import for the expression
	 */
	private static final ImportDeclaration getImportDeclarationFor(List<ImportDeclaration> imports, NameExpr nameExpr) {
		Assert.notNull(imports, "Compilation unit imports required");
		Assert.notNull(nameExpr, "Name expression required");
		for (ImportDeclaration candidate : imports) {
			NameExpr candidateNameExpr = candidate.getName();
			Assert.isInstanceOf(QualifiedNameExpr.class, candidateNameExpr, "Expected import '" + candidate + "' to use a fully-qualified type name");
			QualifiedNameExpr candidateQualifiedName = (QualifiedNameExpr) candidateNameExpr;
			if (nameExpr instanceof QualifiedNameExpr) {
				// user is asking for a fully-qualified name; let's see if there is a full match
				if (isEqual(nameExpr, candidateQualifiedName)) {
					return candidate;
				}
			} else {
				// user is not asking for a fully-qualified name, so let's do a simple name comparison that discards the import's qualified-name package
				if (candidateQualifiedName.getName().equals(nameExpr.getName())) {
					return candidate;
				}
			}
		}
		return null;
	}

	/**
	 * Attempts to import the presented {@link JavaType}.
	 * 
	 * <p>
	 * Whether imported or not, the method returns a {@link NameExpr} suitable for subsequent use
	 * when referring to that type.
	 * 
	 * <p>
	 * If an attempt is made to import a java.lang type, it is ignored.
	 * 
	 * <p>
	 * If an attempt is made to import a type without a package, it is ignored.
	 * 
	 * <p>
	 * We import every type usage even if the type usage is within the same package and would
	 * theoretically not require an import. This is undertaken so that there is no requirement
	 * to separately parse every unqualified type usage within the compilation unit so as to
	 * refrain from importing subsequently conflicting types.
	 * 
	 * @param compilationUnitPackage the compilation unit's package (required)
	 * @param imports the compilation unit's imports (required)
	 * @param typeToImport the type to be imported (required)
	 * @return the name expression to be used when referring to that type (never null)
	 */
	public static final NameExpr importTypeIfRequired(JavaPackage compilationUnitPackage, List<ImportDeclaration> imports, JavaType typeToImport) {
		Assert.notNull(compilationUnitPackage, "Compilation unit package is required");
		Assert.notNull(imports, "Compilation unit imports required");
		Assert.notNull(typeToImport, "Java type to import is required");
	
		// Handle if the type doesn't have a package at all
		if (typeToImport.isDefaultPackage()) {
			return new NameExpr(typeToImport.getSimpleTypeName());
		}
		
		NameExpr typeToImportExpr = new QualifiedNameExpr(new NameExpr(typeToImport.getPackage().getFullyQualifiedPackageName()), typeToImport.getSimpleTypeName());
		
		ImportDeclaration newImport = new ImportDeclaration(typeToImportExpr, false, false);
		
		boolean addImport = true;
		boolean useSimpleTypeName = false;
		for (ImportDeclaration existingImport : imports) {
			if (existingImport.getName().getName().equals(newImport.getName().getName())) {
				// do not import, as there is already an import with the simple type name
				addImport = false;
				
				// if this is a complete match, it indicates we can use the simple type name
				if (isEqual(existingImport.getName(), newImport.getName())) {
					useSimpleTypeName = true;
					break;
				}
			}
		}
		
		if (addImport && "java.lang".equals(typeToImport.getPackage().getFullyQualifiedPackageName())) {
			// So we would have imported, but we don't need to
			addImport = false;
			
			// The fact we could have imported means there was no other conflicting simple type names
			useSimpleTypeName = true;
		}
		
		if (addImport && typeToImport.getPackage().equals(compilationUnitPackage)) {
			// It is not theoretically necessary to add an import for something in the same package,
			// but we elect to explicitly perform an import so future conflicting types are not imported
			// addImport = false;
			// useSimpleTypeName = true;
		}
		
		if (addImport) {
			imports.add(newImport);
			useSimpleTypeName = true;
		}
		
		if (useSimpleTypeName) {
			return new NameExpr(typeToImport.getSimpleTypeName());
		} else {
			return new QualifiedNameExpr(new NameExpr(typeToImport.getPackage().getFullyQualifiedPackageName()), typeToImport.getSimpleTypeName());
		}
		
	}

	/**
	 * Recognises {@link Expression}s of type {@link FieldAccessExpr} and {@link ClassExpr} and automatically imports them if required,
	 * returning the correct {@link Expression} that should subsequently be used.
	 * 
	 * <p>
	 * Even if an {@link Expression} is not resolved by this method into a type and/or imported, the method guarantees to always return
	 * an {@link Expression} that the caller can subsequently use in place of the passed {@link Expression}. In practical terms, the
	 * {@link Expression} passed to this method will be returned unless the type was already imported, just imported, or represented
	 * a java.lang type.
	 * 
	 * @param compilationUnitPackage the package name (required)
	 * @param imports the existing imports (required)
	 * @param value that expression, which need not necessarily be resolvable to a type (required)
	 * @return the expression to now use, as appropriately resolved (never returns null)
	 */
	public static final Expression importExpressionIfRequired(JavaPackage compilationUnitPackage, List<ImportDeclaration> imports, Expression value) {
		Assert.notNull(compilationUnitPackage, "Compilation unit package required");
		Assert.notNull(imports, "Imports required");
		Assert.notNull(value, "Expression value required");
		
		if (value instanceof FieldAccessExpr) {
			Expression scope = ((FieldAccessExpr)value).getScope();
			String field = ((FieldAccessExpr)value).getField();
			if (scope instanceof QualifiedNameExpr) {
				String packageName = ((QualifiedNameExpr) scope).getQualifier().getName();
				String simpleName = ((QualifiedNameExpr) scope).getName();
				String fullyQualifiedName = packageName + "." + simpleName;
				JavaType javaType = new JavaType(fullyQualifiedName);
				NameExpr nameToUse = importTypeIfRequired(compilationUnitPackage, imports, javaType);
				if (!(nameToUse instanceof QualifiedNameExpr)) {
					return new FieldAccessExpr(nameToUse, field);
				}
			}
		} else if (value instanceof ClassExpr) {
			Type type = ((ClassExpr)value).getType();
			if (type instanceof ClassOrInterfaceType) {
				JavaType javaType = new JavaType(((ClassOrInterfaceType)type).getName());
				NameExpr nameToUse = importTypeIfRequired(compilationUnitPackage, imports, javaType);
				if (!(nameToUse instanceof QualifiedNameExpr)) {
					return new ClassExpr(new ClassOrInterfaceType(javaType.getSimpleTypeName()));
				}
			} else if (type instanceof ReferenceType && ((ReferenceType)type).getType() instanceof ClassOrInterfaceType) {
				ClassOrInterfaceType cit = (ClassOrInterfaceType) ((ReferenceType)type).getType();
				JavaType javaType = new JavaType(cit.getName());
				NameExpr nameToUse = importTypeIfRequired(compilationUnitPackage, imports, javaType);
				if (!(nameToUse instanceof QualifiedNameExpr)) {
					return new ClassExpr(new ClassOrInterfaceType(javaType.getSimpleTypeName()));
				}
			}
		} 		
		
		// Make no changes
		return value;
	}

}
