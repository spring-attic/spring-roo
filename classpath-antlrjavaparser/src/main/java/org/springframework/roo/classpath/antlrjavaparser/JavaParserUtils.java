package org.springframework.roo.classpath.antlrjavaparser;

import static org.springframework.roo.model.JavaType.OBJECT;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;

import com.github.antlrjavaparser.api.CompilationUnit;
import com.github.antlrjavaparser.api.ImportDeclaration;
import com.github.antlrjavaparser.api.TypeParameter;
import com.github.antlrjavaparser.api.body.ClassOrInterfaceDeclaration;
import com.github.antlrjavaparser.api.body.ModifierSet;
import com.github.antlrjavaparser.api.body.TypeDeclaration;
import com.github.antlrjavaparser.api.expr.AnnotationExpr;
import com.github.antlrjavaparser.api.expr.ClassExpr;
import com.github.antlrjavaparser.api.expr.Expression;
import com.github.antlrjavaparser.api.expr.FieldAccessExpr;
import com.github.antlrjavaparser.api.expr.MarkerAnnotationExpr;
import com.github.antlrjavaparser.api.expr.NameExpr;
import com.github.antlrjavaparser.api.expr.NormalAnnotationExpr;
import com.github.antlrjavaparser.api.expr.QualifiedNameExpr;
import com.github.antlrjavaparser.api.expr.SingleMemberAnnotationExpr;
import com.github.antlrjavaparser.api.type.ClassOrInterfaceType;
import com.github.antlrjavaparser.api.type.PrimitiveType;
import com.github.antlrjavaparser.api.type.PrimitiveType.Primitive;
import com.github.antlrjavaparser.api.type.ReferenceType;
import com.github.antlrjavaparser.api.type.Type;
import com.github.antlrjavaparser.api.type.VoidType;
import com.github.antlrjavaparser.api.type.WildcardType;

/**
 * Assists with the usage of Java Parser.
 * <p>
 * This class is for internal use by the Java Parser module and should NOT be
 * used by other code.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public final class JavaParserUtils {

    /**
     * Constructor is private to prevent instantiation
     */
    private JavaParserUtils() {
    }

    /**
     * Converts the indicated {@link NameExpr} into a
     * {@link ClassOrInterfaceType}.
     * <p>
     * Note that no effort is made to manage imports etc.
     * 
     * @param nameExpr to convert (required)
     * @return the corresponding {@link ClassOrInterfaceType} (never null)
     */
    public static ClassOrInterfaceType getClassOrInterfaceType(
            final NameExpr nameExpr) {
        Validate.notNull(nameExpr, "Java type required");
        if (nameExpr instanceof QualifiedNameExpr) {
            final QualifiedNameExpr qne = (QualifiedNameExpr) nameExpr;
            if (StringUtils.isNotBlank(qne.getQualifier().getName())) {
                return new ClassOrInterfaceType(qne.getQualifier().getName()
                        + "." + qne.getName());
            }
            return new ClassOrInterfaceType(qne.getName());
        }
        return new ClassOrInterfaceType(nameExpr.getName());
    }

    /**
     * Looks up the import declaration applicable to the presented name
     * expression.
     * <p>
     * If a fully-qualified name is passed to this method, the corresponding
     * import will be evaluated for a complete match. If a simple name is passed
     * to this method, the corresponding import will be evaluated if its simple
     * name matches. This therefore reflects the normal Java semantics for using
     * simple type names that have been imported.
     * 
     * @param compilationUnitServices the types in the compilation unit
     *            (required)
     * @param nameExpr the expression to locate an import for (which would
     *            generally be a {@link NameExpr} and thus not have a package
     *            identifier; required)
     * @return the relevant import, or null if there is no import for the
     *         expression
     */
    private static ImportDeclaration getImportDeclarationFor(
            final CompilationUnitServices compilationUnitServices,
            final NameExpr nameExpr) {
        Validate.notNull(compilationUnitServices,
                "Compilation unit services required");
        Validate.notNull(nameExpr, "Name expression required");

        final List<ImportDeclaration> imports = compilationUnitServices
                .getImports();

        for (final ImportDeclaration candidate : imports) {
            final NameExpr candidateNameExpr = candidate.getName();
            if (!candidate.toString().contains("*")) {
                Validate.isInstanceOf(
                        QualifiedNameExpr.class,
                        candidateNameExpr,
                        "Expected import '%s' to use a fully-qualified type name",
                        candidate);
            }
            if (nameExpr instanceof QualifiedNameExpr) {
                // User is asking for a fully-qualified name; let's see if there
                // is a full match
                if (isEqual(nameExpr, candidateNameExpr)) {
                    return candidate;
                }
            }
            else {
                // User is not asking for a fully-qualified name, so let's do a
                // simple name comparison that discards the import's
                // qualified-name package
                if (candidateNameExpr.getName().equals(nameExpr.getName())) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Converts a JDK {@link Modifier} integer into the equivalent Java Parser
     * modifier.
     * 
     * @param modifiers the JDK int
     * @return the equivalent Java Parser int
     */
    public static int getJavaParserModifier(final int modifiers) {
        int result = 0;
        if (Modifier.isAbstract(modifiers)) {
            result = ModifierSet.addModifier(ModifierSet.ABSTRACT, result);
        }
        if (Modifier.isFinal(modifiers)) {
            result = ModifierSet.addModifier(ModifierSet.FINAL, result);
        }
        if (Modifier.isInterface(modifiers)) {
            // Unsupported by Java Parser ModifierSet
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
     * Resolves the effective {@link JavaType} a {@link NameExpr} represents.
     * <p>
     * You should use {@link #getJavaType(CompilationUnitServices, Type, Set)}
     * where possible so that type arguments are preserved (a {@link NameExpr}
     * does not contain type arguments).
     * <p>
     * A name expression can be either qualified or unqualified.
     * <p>
     * If a name expression is qualified and the qualification starts with a
     * lowercase letter, that represents the fully-qualified name. If the
     * qualification starts with an uppercase letter, the package name is
     * prepended to the qualifier.
     * <p>
     * If a name expression is unqualified, the imports are scanned. If the
     * unqualified name expression is found in the imports, that import
     * declaration represents the fully-qualified name. If the unqualified name
     * expression is not found in the imports, it indicates the name to find is
     * either in the same package as the qualified name expression, or the type
     * relates to a member of java.lang. If part of java.lang, the fully
     * qualified name is treated as part of java.lang. Otherwise the compilation
     * unit package plus unqualified name expression represents the fully
     * qualified name expression.
     * 
     * @param compilationUnitServices for package management (required)
     * @param nameToFind to locate (required)
     * @param typeParameters names to consider type parameters (can be null if
     *            there are none)
     * @return the effective Java type (never null)
     */
    public static JavaType getJavaType(
            final CompilationUnitServices compilationUnitServices,
            final NameExpr nameToFind, final Set<JavaSymbolName> typeParameters) {
        Validate.notNull(compilationUnitServices,
                "Compilation unit services required");
        Validate.notNull(nameToFind, "Name to find is required");

        final JavaPackage compilationUnitPackage = compilationUnitServices
                .getCompilationUnitPackage();

        if (nameToFind instanceof QualifiedNameExpr) {
            final QualifiedNameExpr qne = (QualifiedNameExpr) nameToFind;

            // Handle qualified name expressions that are related to inner types
            // (eg Foo.Bar)
            final NameExpr qneQualifier = qne.getQualifier();
            final NameExpr enclosedBy = getNameExpr(compilationUnitServices
                    .getEnclosingTypeName().getSimpleTypeName());
            if (isEqual(qneQualifier, enclosedBy)) {
                // This qualified name expression is simply an inner type
                // reference
                final String name = compilationUnitServices
                        .getEnclosingTypeName().getFullyQualifiedTypeName()
                        + "." + nameToFind.getName();
                return new JavaType(name,
                        compilationUnitServices.getEnclosingTypeName());
            }

            // Refers to a different enclosing type, so calculate the package
            // name based on convention of an uppercase letter denotes same
            // package (ROO-1210)
            if (qne.toString().length() > 1
                    && Character.isUpperCase(qne.toString().charAt(0))) {
                // First letter is uppercase, so this likely requires prepending
                // of some package name
                final ImportDeclaration importDeclaration = getImportDeclarationFor(
                        compilationUnitServices, qne.getQualifier());
                if (importDeclaration == null) {
                    if (!compilationUnitPackage.getFullyQualifiedPackageName()
                            .equals("")) {
                        // It was not imported, so let's assume it's in the same
                        // package
                        return new JavaType(compilationUnitServices
                                .getCompilationUnitPackage()
                                .getFullyQualifiedPackageName()
                                + "." + qne.toString());
                    }
                }
                else {
                    return new JavaType(importDeclaration.getName() + "."
                            + qne.getName());
                }

                // This name expression (which contains a dot) had its qualifier
                // imported, so let's use the import
            }
            else {
                // First letter is lowercase, so the reference already includes
                // a package
                return new JavaType(qne.toString());
            }
        }

        if ("?".equals(nameToFind.getName())) {
            return new JavaType(OBJECT.getFullyQualifiedTypeName(), 0,
                    DataType.TYPE, JavaType.WILDCARD_NEITHER, null);
        }

        // Unqualified name detected, so check if it's in the type parameter
        // list
        if (typeParameters != null
                && typeParameters.contains(new JavaSymbolName(nameToFind
                        .getName()))) {
            return new JavaType(nameToFind.getName(), 0, DataType.VARIABLE,
                    null, null);
        }

        // Check if we are looking for the enclosingType itself
        final NameExpr enclosingTypeName = getNameExpr(compilationUnitServices
                .getEnclosingTypeName().getSimpleTypeName());
        if (isEqual(enclosingTypeName, nameToFind)) {
            return compilationUnitServices.getEnclosingTypeName();
        }

        // We are searching for a non-qualified name expression (nameToFind), so
        // check if the compilation unit itself declares that type
        for (final TypeDeclaration internalType : compilationUnitServices
                .getInnerTypes()) {
            final NameExpr nameExpr = getNameExpr(internalType.getName());
            if (isEqual(nameExpr, nameToFind)) {
                // Found, so now we need to convert the internalType to a proper
                // JavaType
                final String name = compilationUnitServices
                        .getEnclosingTypeName().getFullyQualifiedTypeName()
                        + "." + nameToFind.getName();
                return new JavaType(name);
            }
        }

        final ImportDeclaration importDeclaration = getImportDeclarationFor(
                compilationUnitServices, nameToFind);
        if (importDeclaration == null) {
            if (JdkJavaType.isPartOfJavaLang(nameToFind.getName())) {
                return new JavaType("java.lang." + nameToFind.getName());
            }
            final String name = compilationUnitPackage
                    .getFullyQualifiedPackageName().equals("") ? nameToFind
                    .getName() : compilationUnitPackage
                    .getFullyQualifiedPackageName()
                    + "."
                    + nameToFind.getName();
            return new JavaType(name);
        }

        return new JavaType(importDeclaration.getName().toString());
    }

    /**
     * Resolves the effective {@link JavaType} a {@link Type} represents. A
     * {@link Type} includes low-level types such as void, arrays and
     * primitives.
     * 
     * @param compilationUnitServices to use for package resolution (required)
     * @param type to locate (required)
     * @param typeParameters names to consider type parameters (can be null if
     *            there are none)
     * @return the {@link JavaType}, with proper indication of primitive and
     *         array status (never null)
     */
    public static JavaType getJavaType(
            final CompilationUnitServices compilationUnitServices,
            final Type type, final Set<JavaSymbolName> typeParameters) {
        Validate.notNull(compilationUnitServices,
                "Compilation unit services required");
        Validate.notNull(type, "The reference type must be provided");

        if (type instanceof VoidType) {
            return JavaType.VOID_PRIMITIVE;
        }

        int array = 0;

        Type internalType = type;
        if (internalType instanceof ReferenceType) {
            array = ((ReferenceType) internalType).getArrayCount();
            if (array > 0) {
                internalType = ((ReferenceType) internalType).getType();
            }
        }

        if (internalType instanceof PrimitiveType) {
            final PrimitiveType pt = (PrimitiveType) internalType;
            if (pt.getType().equals(Primitive.Boolean)) {
                return new JavaType(Boolean.class.getName(), array,
                        DataType.PRIMITIVE, null, null);
            }
            if (pt.getType().equals(Primitive.Char)) {
                return new JavaType(Character.class.getName(), array,
                        DataType.PRIMITIVE, null, null);
            }
            if (pt.getType().equals(Primitive.Byte)) {
                return new JavaType(Byte.class.getName(), array,
                        DataType.PRIMITIVE, null, null);
            }
            if (pt.getType().equals(Primitive.Short)) {
                return new JavaType(Short.class.getName(), array,
                        DataType.PRIMITIVE, null, null);
            }
            if (pt.getType().equals(Primitive.Int)) {
                return new JavaType(Integer.class.getName(), array,
                        DataType.PRIMITIVE, null, null);
            }
            if (pt.getType().equals(Primitive.Long)) {
                return new JavaType(Long.class.getName(), array,
                        DataType.PRIMITIVE, null, null);
            }
            if (pt.getType().equals(Primitive.Float)) {
                return new JavaType(Float.class.getName(), array,
                        DataType.PRIMITIVE, null, null);
            }
            if (pt.getType().equals(Primitive.Double)) {
                return new JavaType(Double.class.getName(), array,
                        DataType.PRIMITIVE, null, null);
            }
            throw new IllegalStateException("Unsupported primitive '"
                    + pt.getType() + "'");
        }

        if (internalType instanceof WildcardType) {
            // We only provide very primitive support for wildcard types; Roo
            // only needs metadata at the end of the day,
            // not complete binding support from an AST
            final WildcardType wt = (WildcardType) internalType;
            if (wt.getSuper() != null) {
                final ReferenceType rt = wt.getSuper();
                final ClassOrInterfaceType cit = (ClassOrInterfaceType) rt
                        .getType();
                final JavaType effectiveType = getJavaTypeNow(
                        compilationUnitServices, cit, typeParameters);
                return new JavaType(effectiveType.getFullyQualifiedTypeName(),
                        rt.getArrayCount(), effectiveType.getDataType(),
                        JavaType.WILDCARD_SUPER, effectiveType.getParameters());
            }
            else if (wt.getExtends() != null) {
                final ReferenceType rt = wt.getExtends();
                final ClassOrInterfaceType cit = (ClassOrInterfaceType) rt
                        .getType();
                final JavaType effectiveType = getJavaTypeNow(
                        compilationUnitServices, cit, typeParameters);
                return new JavaType(effectiveType.getFullyQualifiedTypeName(),
                        rt.getArrayCount(), effectiveType.getDataType(),
                        JavaType.WILDCARD_EXTENDS,
                        effectiveType.getParameters());
            }
            else {
                return new JavaType(OBJECT.getFullyQualifiedTypeName(), 0,
                        DataType.TYPE, JavaType.WILDCARD_NEITHER, null);
            }
        }

        ClassOrInterfaceType cit;
        if (internalType instanceof ClassOrInterfaceType) {
            cit = (ClassOrInterfaceType) internalType;
        }
        else if (internalType instanceof ReferenceType) {
            cit = (ClassOrInterfaceType) ((ReferenceType) type).getType();
        }
        else {
            throw new IllegalStateException("The presented type '"
                    + internalType.getClass() + "' with value '" + internalType
                    + "' is unsupported by JavaParserUtils");
        }

        final JavaType effectiveType = getJavaTypeNow(compilationUnitServices,
                cit, typeParameters);
        if (array > 0) {
            return new JavaType(effectiveType.getFullyQualifiedTypeName(),
                    array, effectiveType.getDataType(),
                    effectiveType.getArgName(), effectiveType.getParameters());
        }

        return effectiveType;
    }

    /**
     * Resolves the effective {@link JavaType} a
     * {@link ClassOrInterfaceDeclaration} represents, including any type
     * parameters.
     * 
     * @param compilationUnitServices for package management (required)
     * @param typeDeclaration the type declaration to resolve (required)
     * @return the effective Java type (never null)
     */
    public static JavaType getJavaType(
            final CompilationUnitServices compilationUnitServices,
            final TypeDeclaration typeDeclaration) {
        Validate.notNull(compilationUnitServices,
                "Compilation unit services required");
        Validate.notNull(typeDeclaration, "Type declaration required");

        // Convert the ClassOrInterfaceDeclaration name into a JavaType
        final NameExpr nameExpr = getNameExpr(typeDeclaration.getName());
        final JavaType effectiveType = getJavaType(compilationUnitServices,
                nameExpr, null);

        final List<JavaType> parameterTypes = new ArrayList<JavaType>();
        if (typeDeclaration instanceof ClassOrInterfaceDeclaration) {
            final ClassOrInterfaceDeclaration clazz = (ClassOrInterfaceDeclaration) typeDeclaration;
            // Populate JavaType with type parameters
            final List<TypeParameter> typeParameters = clazz
                    .getTypeParameters();
            if (typeParameters != null) {
                final Set<JavaSymbolName> locatedTypeParameters = new HashSet<JavaSymbolName>();
                for (final TypeParameter candidate : typeParameters) {
                    final JavaSymbolName currentTypeParam = new JavaSymbolName(
                            candidate.getName());
                    locatedTypeParameters.add(currentTypeParam);
                    JavaType javaType = null;
                    if (candidate.getTypeBound() == null) {
                        javaType = new JavaType(
                                OBJECT.getFullyQualifiedTypeName(), 0,
                                DataType.TYPE, currentTypeParam, null);
                    }
                    else {
                        final ClassOrInterfaceType cit = candidate
                                .getTypeBound().get(0);
                        javaType = JavaParserUtils.getJavaTypeNow(
                                compilationUnitServices, cit,
                                locatedTypeParameters);
                        javaType = new JavaType(
                                javaType.getFullyQualifiedTypeName(),
                                javaType.getArray(), javaType.getDataType(),
                                currentTypeParam, javaType.getParameters());
                    }
                    parameterTypes.add(javaType);
                }
            }
        }

        return new JavaType(effectiveType.getFullyQualifiedTypeName(),
                effectiveType.getArray(), effectiveType.getDataType(), null,
                parameterTypes);
    }

    /**
     * Resolves the effective {@link JavaType} a {@link ClassOrInterfaceType}
     * represents, including any type arguments.
     * 
     * @param compilationUnitServices for package management (required)
     * @param cit the class or interface type to resolve (required)
     * @return the effective Java type (never null)
     */
    public static JavaType getJavaTypeNow(
            final CompilationUnitServices compilationUnitServices,
            final ClassOrInterfaceType cit,
            final Set<JavaSymbolName> typeParameters) {
        Validate.notNull(compilationUnitServices,
                "Compilation unit services required");
        Validate.notNull(cit, "ClassOrInterfaceType required");

        final JavaPackage compilationUnitPackage = compilationUnitServices
                .getCompilationUnitPackage();
        Validate.notNull(compilationUnitPackage,
                "Compilation unit package required");

        String typeName = cit.getName();
        ClassOrInterfaceType scope = cit.getScope();
        while (scope != null) {
            typeName = scope.getName() + "." + typeName;
            scope = scope.getScope();
        }
        final NameExpr nameExpr = getNameExpr(typeName);

        final JavaType effectiveType = getJavaType(compilationUnitServices,
                nameExpr, typeParameters);

        // Handle any type arguments
        final List<JavaType> parameterTypes = new ArrayList<JavaType>();
        if (cit.getTypeArgs() != null) {
            for (final Type ta : cit.getTypeArgs()) {
                parameterTypes.add(getJavaType(compilationUnitServices, ta,
                        typeParameters));
            }
        }

        return new JavaType(effectiveType.getFullyQualifiedTypeName(),
                effectiveType.getArray(), effectiveType.getDataType(), null,
                parameterTypes);
    }

    /**
     * Converts a Java Parser modifier integer into a JDK {@link Modifier}
     * integer.
     * 
     * @param modifiers the Java Parser int
     * @return the equivalent JDK int
     */
    public static int getJdkModifier(final int modifiers) {
        int result = 0;
        if (ModifierSet.isAbstract(modifiers)) {
            result |= Modifier.ABSTRACT;
        }
        if (ModifierSet.isFinal(modifiers)) {
            result |= Modifier.FINAL;
        }
        if (ModifierSet.isNative(modifiers)) {
            result |= Modifier.NATIVE;
        }
        if (ModifierSet.isPrivate(modifiers)) {
            result |= Modifier.PRIVATE;
        }
        if (ModifierSet.isProtected(modifiers)) {
            result |= Modifier.PROTECTED;
        }
        if (ModifierSet.isPublic(modifiers)) {
            result |= Modifier.PUBLIC;
        }
        if (ModifierSet.isStatic(modifiers)) {
            result |= Modifier.STATIC;
        }
        if (ModifierSet.isStrictfp(modifiers)) {
            result |= Modifier.STRICT;
        }
        if (ModifierSet.isSynchronized(modifiers)) {
            result |= Modifier.SYNCHRONIZED;
        }
        if (ModifierSet.isTransient(modifiers)) {
            result |= Modifier.TRANSIENT;
        }
        if (ModifierSet.isVolatile(modifiers)) {
            result |= Modifier.VOLATILE;
        }
        return result;
    }

    /**
     * Obtains the name expression ({@link NameExpr}) for the passed
     * {@link AnnotationExpr}, which is the annotation's type.
     * 
     * @param annotationExpr to retrieve the type name from (required)
     * @return the name (never null)
     */
    public static NameExpr getNameExpr(final AnnotationExpr annotationExpr) {
        Validate.notNull(annotationExpr, "Annotation expression required");
        if (annotationExpr instanceof MarkerAnnotationExpr) {
            final MarkerAnnotationExpr a = (MarkerAnnotationExpr) annotationExpr;
            final NameExpr nameToFind = a.getName();
            Validate.notNull(nameToFind,
                    "Unable to determine annotation name from '%s'",
                    annotationExpr);
            return nameToFind;
        }
        else if (annotationExpr instanceof SingleMemberAnnotationExpr) {
            final SingleMemberAnnotationExpr a = (SingleMemberAnnotationExpr) annotationExpr;
            final NameExpr nameToFind = a.getName();
            Validate.notNull(nameToFind,
                    "Unable to determine annotation name from '%s'",
                    annotationExpr);
            return nameToFind;
        }
        else if (annotationExpr instanceof NormalAnnotationExpr) {
            final NormalAnnotationExpr a = (NormalAnnotationExpr) annotationExpr;
            final NameExpr nameToFind = a.getName();
            Validate.notNull(nameToFind,
                    "Unable to determine annotation name from '%s'",
                    annotationExpr);
            return nameToFind;
        }
        throw new UnsupportedOperationException(
                "Unknown annotation expression type '"
                        + annotationExpr.getClass().getName() + "'");
    }

    /**
     * Converts the presented class name into a name expression (either a
     * {@link NameExpr} or {@link QualifiedNameExpr} depending on whether a
     * package was presented).
     * 
     * @param className to convert (required; can be fully qualified or simple
     *            name only)
     * @return a compatible expression (never returns null)
     */
    public static NameExpr getNameExpr(final String className) {
        Validate.notBlank(className, "Class name required");
        if (className.contains(".")) {
            final int offset = className.lastIndexOf(".");
            final String packageName = className.substring(0, offset);
            final String typeName = className.substring(offset + 1);
            return new QualifiedNameExpr(new NameExpr(packageName), typeName);
        }
        return new NameExpr(className);
    }

    /**
     * Converts the indicated {@link JavaType} into a {@link ReferenceType}.
     * <p>
     * Note that no effort is made to manage imports etc.
     * 
     * @param nameExpr to convert (required)
     * @return the corresponding {@link ReferenceType} (never null)
     */
    public static ReferenceType getReferenceType(final NameExpr nameExpr) {
        Validate.notNull(nameExpr, "Java type required");
        return new ReferenceType(getClassOrInterfaceType(nameExpr));
    }

    public static ClassOrInterfaceType getResolvedName(final JavaType target,
            final JavaType current, final CompilationUnit compilationUnit) {
        final NameExpr nameExpr = JavaParserUtils.importTypeIfRequired(target,
                compilationUnit.getImports(), current);
        final ClassOrInterfaceType resolvedName = JavaParserUtils
                .getClassOrInterfaceType(nameExpr);
        if (current.getParameters() != null
                && current.getParameters().size() > 0) {
            resolvedName.setTypeArgs(new ArrayList<Type>());
            for (final JavaType param : current.getParameters()) {
                resolvedName.getTypeArgs().add(
                        getResolvedName(target, param, compilationUnit));
            }
        }

        return resolvedName;
    }

    public static Type getResolvedName(final JavaType target,
            final JavaType current,
            final CompilationUnitServices compilationUnit) {
        final NameExpr nameExpr = JavaParserUtils.importTypeIfRequired(target,
                compilationUnit.getImports(), current);
        final ClassOrInterfaceType resolvedName = JavaParserUtils
                .getClassOrInterfaceType(nameExpr);
        if (current.getParameters() != null
                && current.getParameters().size() > 0) {
            resolvedName.setTypeArgs(new ArrayList<Type>());
            for (final JavaType param : current.getParameters()) {
                resolvedName.getTypeArgs().add(
                        getResolvedName(target, param, compilationUnit));
            }
        }

        if (current.getArray() > 0) {
            // Primitives includes array declaration in resolvedName
            if (!current.isPrimitive()) {
                return new ReferenceType(resolvedName, current.getArray());
            }
        }

        return resolvedName;
    }

    /**
     * Given a primitive type, computes the corresponding Java Parser type.
     * <p>
     * Presenting a non-primitive type to this method will throw an exception.
     * If you have a non-primitive type, use
     * {@link #importTypeIfRequired(JavaType, List, JavaType)} and then present
     * the {@link NameExpr} it returns to
     * {@link #getClassOrInterfaceType(NameExpr)}.
     * 
     * @param javaType a primitive type (required, and must be primitive)
     * @return the equivalent Java Parser {@link Type}
     */
    public static Type getType(final JavaType javaType) {
        Validate.notNull(javaType, "Java type required");
        Validate.isTrue(javaType.isPrimitive(),
                "Java type must be primitive to be presented to this method");
        if (javaType.equals(JavaType.VOID_PRIMITIVE)) {
            return new VoidType();
        }
        else if (javaType.equals(JavaType.BOOLEAN_PRIMITIVE)) {
            return new PrimitiveType(Primitive.Boolean);
        }
        else if (javaType.equals(JavaType.BYTE_PRIMITIVE)) {
            return new PrimitiveType(Primitive.Byte);
        }
        else if (javaType.equals(JavaType.CHAR_PRIMITIVE)) {
            return new PrimitiveType(Primitive.Char);
        }
        else if (javaType.equals(JavaType.DOUBLE_PRIMITIVE)) {
            return new PrimitiveType(Primitive.Double);
        }
        else if (javaType.equals(JavaType.FLOAT_PRIMITIVE)) {
            return new PrimitiveType(Primitive.Float);
        }
        else if (javaType.equals(JavaType.INT_PRIMITIVE)) {
            return new PrimitiveType(Primitive.Int);
        }
        else if (javaType.equals(JavaType.LONG_PRIMITIVE)) {
            return new PrimitiveType(Primitive.Long);
        }
        else if (javaType.equals(JavaType.SHORT_PRIMITIVE)) {
            return new PrimitiveType(Primitive.Short);
        }
        throw new IllegalStateException("Unknown primitive " + javaType);
    }

    /**
     * Recognises {@link Expression}s of type {@link FieldAccessExpr} and
     * {@link ClassExpr} and automatically imports them if required, returning
     * the correct {@link Expression} that should subsequently be used.
     * <p>
     * Even if an {@link Expression} is not resolved by this method into a type
     * and/or imported, the method guarantees to always return an
     * {@link Expression} that the caller can subsequently use in place of the
     * passed {@link Expression}. In practical terms, the {@link Expression}
     * passed to this method will be returned unless the type was already
     * imported, just imported, or represented a java.lang type.
     * 
     * @param targetType the compilation unit target type (required)
     * @param imports the existing imports (required)
     * @param value that expression, which need not necessarily be resolvable to
     *            a type (required)
     * @return the expression to now use, as appropriately resolved (never
     *         returns null)
     */
    public static Expression importExpressionIfRequired(
            final JavaType targetType, final List<ImportDeclaration> imports,
            final Expression value) {
        Validate.notNull(targetType, "Target type required");
        Validate.notNull(imports, "Imports required");
        Validate.notNull(value, "Expression value required");

        if (value instanceof FieldAccessExpr) {
            final Expression scope = ((FieldAccessExpr) value).getScope();
            final String field = ((FieldAccessExpr) value).getField();
            if (scope instanceof QualifiedNameExpr) {
                final String packageName = ((QualifiedNameExpr) scope)
                        .getQualifier().getName();
                final String simpleName = ((QualifiedNameExpr) scope).getName();
                final String fullyQualifiedName = packageName + "."
                        + simpleName;
                final JavaType javaType = new JavaType(fullyQualifiedName);
                final NameExpr nameToUse = importTypeIfRequired(targetType,
                        imports, javaType);
                if (!(nameToUse instanceof QualifiedNameExpr)) {
                    return new FieldAccessExpr(nameToUse, field);
                }
            }
        }
        else if (value instanceof ClassExpr) {
            final Type type = ((ClassExpr) value).getType();
            if (type instanceof ClassOrInterfaceType) {
                final JavaType javaType = new JavaType(
                        ((ClassOrInterfaceType) type).getName());
                final NameExpr nameToUse = importTypeIfRequired(targetType,
                        imports, javaType);
                if (!(nameToUse instanceof QualifiedNameExpr)) {
                    return new ClassExpr(new ClassOrInterfaceType(
                            javaType.getSimpleTypeName()));
                }
            }
            else if (type instanceof ReferenceType
                    && ((ReferenceType) type).getType() instanceof ClassOrInterfaceType) {
                final ClassOrInterfaceType cit = (ClassOrInterfaceType) ((ReferenceType) type)
                        .getType();
                final JavaType javaType = new JavaType(cit.getName());
                final NameExpr nameToUse = importTypeIfRequired(targetType,
                        imports, javaType);
                if (!(nameToUse instanceof QualifiedNameExpr)) {
                    return new ClassExpr(new ClassOrInterfaceType(
                            javaType.getSimpleTypeName()));
                }
            }
        }

        // Make no changes
        return value;
    }

    public static ReferenceType importParametersForType(
            final JavaType targetType, final List<ImportDeclaration> imports,
            final JavaType typeToImport) {
        Validate.notNull(targetType, "Target type is required");
        Validate.notNull(imports, "Compilation unit imports required");
        Validate.notNull(typeToImport, "Java type to import is required");

        final ClassOrInterfaceType cit = getClassOrInterfaceType(importTypeIfRequired(
                targetType, imports, typeToImport));

        // Add any type arguments presented for the return type
        if (typeToImport.getParameters().size() > 0) {
            final List<Type> typeArgs = new ArrayList<Type>();
            cit.setTypeArgs(typeArgs);
            for (final JavaType parameter : typeToImport.getParameters()) {
                typeArgs.add(JavaParserUtils.importParametersForType(
                        targetType, imports, parameter));
            }
        }
        return new ReferenceType(cit);
    }

    /**
     * Attempts to import the presented {@link JavaType}.
     * <p>
     * Whether imported or not, the method returns a {@link NameExpr} suitable
     * for subsequent use when referring to that type.
     * <p>
     * If an attempt is made to import a java.lang type, it is ignored.
     * <p>
     * If an attempt is made to import a type without a package, it is ignored.
     * <p>
     * We import every type usage even if the type usage is within the same
     * package and would theoretically not require an import. This is undertaken
     * so that there is no requirement to separately parse every unqualified
     * type usage within the compilation unit so as to refrain from importing
     * subsequently conflicting types.
     * 
     * @param targetType the compilation unit target type (required)
     * @param imports the compilation unit's imports (required)
     * @param typeToImport the type to be imported (required)
     * @return the name expression to be used when referring to that type (never
     *         null)
     */
    public static NameExpr importTypeIfRequired(final JavaType targetType,
            final List<ImportDeclaration> imports, final JavaType typeToImport) {
        Validate.notNull(targetType, "Target type is required");
        final JavaPackage compilationUnitPackage = targetType.getPackage();
        Validate.notNull(imports, "Compilation unit imports required");
        Validate.notNull(typeToImport, "Java type to import is required");

        // If it's a primitive, it's really easy
        if (typeToImport.isPrimitive()) {
            return new NameExpr(typeToImport.getNameIncludingTypeParameters());
        }

        // Handle if the type doesn't have a package at all
        if (typeToImport.isDefaultPackage()) {
            return new NameExpr(typeToImport.getSimpleTypeName());
        }

        final JavaPackage typeToImportPackage = typeToImport.getPackage();
        if (typeToImportPackage.equals(compilationUnitPackage)) {
            return new NameExpr(typeToImport.getSimpleTypeName());
        }

        NameExpr typeToImportExpr;
        if (typeToImport.getEnclosingType() == null) {
            typeToImportExpr = new QualifiedNameExpr(new NameExpr(typeToImport
                    .getPackage().getFullyQualifiedPackageName()),
                    typeToImport.getSimpleTypeName());
        }
        else {
            typeToImportExpr = new QualifiedNameExpr(new NameExpr(typeToImport
                    .getEnclosingType().getFullyQualifiedTypeName()),
                    typeToImport.getSimpleTypeName());
        }

        final ImportDeclaration newImport = new ImportDeclaration(
                typeToImportExpr, false, false);

        boolean addImport = true;
        boolean useSimpleTypeName = false;
        for (final ImportDeclaration existingImport : imports) {
            if (existingImport.getName().getName()
                    .equals(newImport.getName().getName())) {
                // Do not import, as there is already an import with the simple
                // type name
                addImport = false;

                // If this is a complete match, it indicates we can use the
                // simple type name
                if (isEqual(existingImport.getName(), newImport.getName())) {
                    useSimpleTypeName = true;
                    break;
                }
            }
        }

        if (addImport
                && JdkJavaType.isPartOfJavaLang(typeToImport
                        .getSimpleTypeName())) {
            // This simple type name would be part of java.lang if left as the
            // simple name. We want a fully-qualified name.
            addImport = false;
            useSimpleTypeName = false;
        }

        if (JdkJavaType.isPartOfJavaLang(typeToImport)) {
            // So we would have imported, but we don't need to
            addImport = false;

            // The fact we could have imported means there was no other
            // conflicting simple type names
            useSimpleTypeName = true;
        }

        if (addImport
                && typeToImport.getPackage().equals(compilationUnitPackage)) {
            // It is not theoretically necessary to add an import for something
            // in the same package,
            // but we elect to explicitly perform an import so future
            // conflicting types are not imported
            // addImport = true;
            // useSimpleTypeName = false;
        }

        if (addImport
                && targetType.getSimpleTypeName().equals(
                        typeToImport.getSimpleTypeName())) {
            // So we would have imported it, but then it would conflict with the
            // simple name of the type
            addImport = false;
            useSimpleTypeName = false;
        }

        if (addImport) {
            imports.add(newImport);
            useSimpleTypeName = true;
        }

        // This is pretty crude, but at least it emits source code for people
        // (forget imports, though!)
        if (typeToImport.getArgName() != null) {
            return new NameExpr(typeToImport.toString());
        }

        if (useSimpleTypeName) {
            return new NameExpr(typeToImport.getSimpleTypeName());
        }
        return new QualifiedNameExpr(new NameExpr(typeToImport.getPackage()
                .getFullyQualifiedPackageName()),
                typeToImport.getSimpleTypeName());
    }

    /**
     * Indicates whether two {@link NameExpr} expressions are equal.
     * <p>
     * This method is necessary given {@link NameExpr} does not offer an equals
     * method.
     * 
     * @param o1 the first entry to compare (null is acceptable)
     * @param o2 the second entry to compare (null is acceptable)
     * @return true if and only if both entries are identical
     */
    private static boolean isEqual(final NameExpr o1, final NameExpr o2) {
        if (o1 == null && o2 == null) {
            return true;
        }
        if (o1 == null && o2 != null) {
            return false;
        }
        if (o1 != null && o2 == null) {
            return false;
        }
        if (o1 != null && !o1.getName().equals(o2.getName())) {
            return false;
        }
        return o1 != null && o1.toString().equals(o2.toString());
    }

    /**
     * Searches a compilation unit and locates the declaration with the given
     * type's simple name.
     * 
     * @param compilationUnit to scan (required)
     * @param javaType the target to locate (required)
     * @return the located type declaration or null if it could not be found
     */
    public static TypeDeclaration locateTypeDeclaration(
            final CompilationUnit compilationUnit, final JavaType javaType) {
        Validate.notNull(compilationUnit, "Compilation unit required");
        Validate.notNull(javaType, "Java type to search for required");
        if (compilationUnit.getTypes() == null) {
            return null;
        }
        for (final TypeDeclaration candidate : compilationUnit.getTypes()) {
            if (javaType.getSimpleTypeName().equals(candidate.getName())) {
                // We have the required type declaration
                return candidate;
            }
        }
        return null;
    }

    /**
     * Returns the final {@link ClassOrInterfaceType} from a {@link Type}
     * 
     * @param initType
     * @return the final {@link ClassOrInterfaceType} or null if no
     *         {@link ClassOrInterfaceType} found
     */
    public static ClassOrInterfaceType getClassOrInterfaceType(final Type type) {
        Type tmp = type;
        while (tmp instanceof ReferenceType) {
            tmp = ((ReferenceType) tmp).getType();
        }
        if (tmp instanceof ClassOrInterfaceType) {
            return (ClassOrInterfaceType) tmp;
        }
        return null;
    }
}
