package org.springframework.roo.classpath.antlrjavaparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import com.github.antlrjavaparser.api.CompilationUnit;
import com.github.antlrjavaparser.api.ImportDeclaration;
import com.github.antlrjavaparser.api.body.BodyDeclaration;
import com.github.antlrjavaparser.api.body.ConstructorDeclaration;
import com.github.antlrjavaparser.api.body.EnumConstantDeclaration;
import com.github.antlrjavaparser.api.body.EnumDeclaration;
import com.github.antlrjavaparser.api.body.FieldDeclaration;
import com.github.antlrjavaparser.api.body.MethodDeclaration;
import com.github.antlrjavaparser.api.body.Parameter;
import com.github.antlrjavaparser.api.body.TypeDeclaration;
import com.github.antlrjavaparser.api.body.VariableDeclarator;
import com.github.antlrjavaparser.api.expr.AnnotationExpr;
import com.github.antlrjavaparser.api.expr.MarkerAnnotationExpr;
import com.github.antlrjavaparser.api.expr.MemberValuePair;
import com.github.antlrjavaparser.api.expr.NormalAnnotationExpr;
import com.github.antlrjavaparser.api.expr.SingleMemberAnnotationExpr;
import com.github.antlrjavaparser.api.stmt.BlockStmt;
import com.github.antlrjavaparser.api.stmt.Statement;
import com.github.antlrjavaparser.api.type.ClassOrInterfaceType;
import com.github.antlrjavaparser.api.type.PrimitiveType;
import com.github.antlrjavaparser.api.type.Type;
import com.github.antlrjavaparser.api.type.VoidType;
import com.github.antlrjavaparser.api.type.WildcardType;

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

        FieldEntry(final FieldDeclaration fieldDeclaration,
                final VariableDeclarator variableDeclarator) {
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
        return declaration1.isAsterisk() == declaration2.isAsterisk()
                && declaration1.isStatic() == declaration2.isStatic()
                && declaration1.getName().getName()
                        .equals(declaration2.getName().getName());
    }

    /**
     * Compare two {@link Type}
     * 
     * @param type
     * @param type2
     * @return
     */
    private static boolean equals(final Type type, final Type type2) {
        if (ObjectUtils.equals(type, type2)) {
            return true;
        }
        if (type.getClass() != type2.getClass()) {
            return false;
        }
        if (type instanceof ClassOrInterfaceType) {
            final ClassOrInterfaceType cType = (ClassOrInterfaceType) type;
            final ClassOrInterfaceType cType2 = (ClassOrInterfaceType) type2;
            return cType.getName().equals(cType2.getName());

        }
        else if (type instanceof PrimitiveType) {
            final PrimitiveType pType = (PrimitiveType) type;
            final PrimitiveType pType2 = (PrimitiveType) type2;
            return pType.getType() == pType2.getType();

        }
        else if (type instanceof VoidType) {
            return true;
        }
        else if (type instanceof WildcardType) {
            final WildcardType wType = (WildcardType) type;
            final WildcardType wType2 = (WildcardType) type2;
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
            final CompilationUnit compilationUnit,
            final CompilationUnit cidCompilationUnit) {
        boolean notFound;
        final List<ImportDeclaration> cidImports = new ArrayList<ImportDeclaration>();
        if (cidCompilationUnit.getImports() != null) {
            cidImports.addAll(cidCompilationUnit.getImports());
        }
        if (compilationUnit.getImports() != null) {
            for (final Iterator<ImportDeclaration> originalImportIter = compilationUnit
                    .getImports().iterator(); originalImportIter.hasNext();) {
                final ImportDeclaration originalImport = originalImportIter
                        .next();
                notFound = true;
                for (final Iterator<ImportDeclaration> newImportIter = cidImports
                        .iterator(); newImportIter.hasNext();) {
                    final ImportDeclaration newImport = newImportIter.next();
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
            final CompilationUnit compilationUnit,
            final CompilationUnit cidCompilationUnit) {
        boolean notFound;
        final List<TypeDeclaration> cidTypes = new ArrayList<TypeDeclaration>(
                cidCompilationUnit.getTypes());

        for (final Iterator<TypeDeclaration> originalTypestIter = compilationUnit
                .getTypes().iterator(); originalTypestIter.hasNext();) {
            final TypeDeclaration originalType = originalTypestIter.next();
            notFound = true;
            for (final Iterator<TypeDeclaration> newTypeIter = cidTypes
                    .iterator(); newTypeIter.hasNext();) {
                final TypeDeclaration newType = newTypeIter.next();
                if (originalType.getName().equals(newType.getName())
                        && originalType.getClass() == newType.getClass()) {
                    // new Type found in original imports
                    if (originalType instanceof EnumDeclaration) {
                        updateCompilationUnitEnumeration(
                                (EnumDeclaration) originalType,
                                (EnumDeclaration) newType);
                    }
                    else {
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
    public static void updateCompilationUnitType(
            final TypeDeclaration originalType, final TypeDeclaration newType) {

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
    private static void updateConstructors(final TypeDeclaration originalType,
            final TypeDeclaration newType) {
        // Get a list of all constructors
        final List<ConstructorDeclaration> cidConstructor = new ArrayList<ConstructorDeclaration>();
        if (newType.getMembers() != null) {
            for (final BodyDeclaration element : newType.getMembers()) {
                if (element instanceof ConstructorDeclaration) {
                    cidConstructor.add((ConstructorDeclaration) element);
                }
            }
        }

        ConstructorDeclaration originalConstructor, newConstructor;
        boolean notFound;
        // Iterate over every method definition
        if (originalType.getMembers() != null) {
            for (final Iterator<BodyDeclaration> originalMemberstIter = originalType
                    .getMembers().iterator(); originalMemberstIter.hasNext();) {
                final BodyDeclaration originalMember = originalMemberstIter
                        .next();
                if (!(originalMember instanceof ConstructorDeclaration)) {
                    // this is not a method definition
                    continue;
                }
                originalConstructor = (ConstructorDeclaration) originalMember;

                notFound = true;

                // look at cidConstructor for originalConstructor
                for (final Iterator<ConstructorDeclaration> newConstructorIter = cidConstructor
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
            final ConstructorDeclaration originalConstructor,
            final ConstructorDeclaration newConstructor) {
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
    private static void updateInnerTypes(final TypeDeclaration originalType,
            final TypeDeclaration newType) {

        // Get a list of all types
        final List<TypeDeclaration> cidTypes = new ArrayList<TypeDeclaration>();
        if (newType.getMembers() != null) {
            for (final BodyDeclaration element : newType.getMembers()) {
                if (element instanceof TypeDeclaration) {
                    cidTypes.add((TypeDeclaration) element);
                }
            }
        }

        TypeDeclaration originalInner, newInner;
        boolean notFound;
        // Iterate over every type definition
        if (originalType.getMembers() != null) {
            for (final Iterator<BodyDeclaration> originalMemberstIter = originalType
                    .getMembers().iterator(); originalMemberstIter.hasNext();) {
                final BodyDeclaration originalMember = originalMemberstIter
                        .next();
                if (!(originalMember instanceof TypeDeclaration)) {
                    // this is not a method definition
                    continue;
                }
                originalInner = (TypeDeclaration) originalMember;

                notFound = true;
                // look at ciMethods for method
                for (final Iterator<TypeDeclaration> newInnerIter = cidTypes
                        .iterator(); newInnerIter.hasNext();) {
                    newInner = newInnerIter.next();

                    if (originalInner.getName().equals(newInner.getName())
                            && originalInner.getClass() == newInner.getClass()) {
                        notFound = false;

                        if (originalInner instanceof EnumDeclaration) {
                            updateCompilationUnitEnumeration(
                                    (EnumDeclaration) originalInner,
                                    (EnumDeclaration) newInner);
                        }
                        else {
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

        // Add new methods
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
            final EnumDeclaration originalType, final EnumDeclaration newType) {
        if (originalType.getModifiers() != newType.getModifiers()) {
            originalType.setModifiers(newType.getModifiers());
        }

        if (originalType.getAnnotations() == null
                && newType.getAnnotations() != null) {
            originalType.setAnnotations(new ArrayList<AnnotationExpr>());
        }
        if (!equalsEnumConstants(originalType.getEntries(),
                newType.getEntries())) {
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
     * 
     * @param entries
     * @param entries2
     * @return
     */
    private static boolean equalsEnumConstants(
            final List<EnumConstantDeclaration> entries,
            final List<EnumConstantDeclaration> entries2) {
        if (ObjectUtils.equals(entries, entries2)) {
            return true;
        }
        if (entries == null || entries2 == null) {
            return false;
        }
        if (entries.size() != entries2.size()) {
            return false;
        }
        for (int i = 0; i < entries.size(); i++) {
            final EnumConstantDeclaration constant = entries.get(i);
            final EnumConstantDeclaration constant2 = entries2.get(i);

            if (!equals(constant, constant2)) {
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
    private static boolean equals(final EnumConstantDeclaration constant,
            final EnumConstantDeclaration constant2) {
        if (!constant.getName().equals(constant2.getName())) {
            return false;
        }
        if (!equalsAnnotations(constant.getAnnotations(),
                constant2.getAnnotations())) {
            return false;
        }
        if (ObjectUtils.equals(constant.getClassBody(),
                constant2.getClassBody())) {
            return true;
        }
        if (constant.getClassBody() == null || constant2.getClassBody() == null) {
            return false;
        }
        final List<BodyDeclaration> body = constant.getClassBody();
        final List<BodyDeclaration> body2 = constant2.getClassBody();
        if (body.size() != body2.size()) {
            return false;
        }
        for (int i = 0; i < body.size(); i++) {
            final BodyDeclaration item = body.get(i);
            final BodyDeclaration item2 = body2.get(i);

            if (item.getClass() != item2.getClass()) {
                return false;
            }
            // Compares contents
            if (!item.toString().equals(item2.toString())) {
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
    private static void updateMethods(final TypeDeclaration originalType,
            final TypeDeclaration newType) {
        // Get a list of all methods
        final List<MethodDeclaration> cidMethods = new ArrayList<MethodDeclaration>();
        if (newType.getMembers() != null) {
            for (final BodyDeclaration element : newType.getMembers()) {
                if (element instanceof MethodDeclaration) {
                    cidMethods.add((MethodDeclaration) element);
                }
            }
        }

        MethodDeclaration originalMethod, newMethod;
        boolean notFound;
        // Iterate over every method definition
        if (originalType.getMembers() != null) {
            for (final Iterator<BodyDeclaration> originalMemberstIter = originalType
                    .getMembers().iterator(); originalMemberstIter.hasNext();) {
                final BodyDeclaration originalMember = originalMemberstIter
                        .next();
                if (!(originalMember instanceof MethodDeclaration)) {
                    // this is not a method definition
                    continue;
                }
                originalMethod = (MethodDeclaration) originalMember;

                notFound = true;

                // look at ciMethos for method
                for (final Iterator<MethodDeclaration> newMethodsIter = cidMethods
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

        // Add new methods
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
    private static boolean equals(final MethodDeclaration originalMethod,
            final MethodDeclaration newMethod) {

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
    private static boolean equals(final BlockStmt body, final BlockStmt body2) {
        if (ObjectUtils.equals(body, body2)) {
            return true;
        }
        if (body == null || body2 == null) {
            return false;
        }
        if (body.getStmts().size() != body2.getStmts().size()) {
            return false;
        }
        final List<Statement> statements = body.getStmts();
        final List<Statement> statements2 = body2.getStmts();
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
    private static boolean equals(final Statement statement,
            final Statement statement2) {
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
    private static boolean equalsParameters(final List<Parameter> parameters,
            final List<Parameter> parameters2) {
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
    private static void updateFields(final TypeDeclaration originalType,
            final TypeDeclaration newType) {

        // Get a map of all fields (as FieldDeclaration could contain more than
        // one field)
        final Map<String, FieldEntry> cidFields = new HashMap<String, FieldEntry>();
        String fieldName;
        FieldDeclaration field;
        if (newType.getMembers() != null) {
            for (final BodyDeclaration element : newType.getMembers()) {
                if (element instanceof FieldDeclaration) {
                    field = (FieldDeclaration) element;
                    for (final VariableDeclarator variable : field
                            .getVariables()) {
                        fieldName = variable.getId().getName();
                        cidFields.put(fieldName,
                                new FieldEntry(field, variable));
                    }
                }
            }
        }

        // Iterate over every field definition
        if (originalType.getMembers() != null) {
            for (final Iterator<BodyDeclaration> originalMemberstIter = originalType
                    .getMembers().iterator(); originalMemberstIter.hasNext();) {
                final BodyDeclaration originalMember = originalMemberstIter
                        .next();
                if (!(originalMember instanceof FieldDeclaration)) {
                    // this is not a field definition
                    continue;
                }
                field = (FieldDeclaration) originalMember;

                // Check every variable declared in definition
                for (final Iterator<VariableDeclarator> variablesIter = field
                        .getVariables().iterator(); variablesIter.hasNext();) {
                    final VariableDeclarator originalVariable = variablesIter
                            .next();
                    fieldName = originalVariable.getId().getName();

                    // look for field name in cid
                    final FieldEntry entry = cidFields.get(fieldName);
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
                    }
                    else {
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
        for (final FieldEntry entry : cidFields.values()) {
            variables = new ArrayList<VariableDeclarator>(1);
            variables.add(entry.variableDeclarator);
            field = new FieldDeclaration(entry.fieldDeclaration.getModifiers(),
                    entry.fieldDeclaration.getType(), variables);
            field.setAnnotations(entry.fieldDeclaration.getAnnotations());
            field.setBeginComments(entry.fieldDeclaration.getBeginComments());
            field.setInternalComments(entry.fieldDeclaration
                    .getInternalComments());
            field.setEndComments(entry.fieldDeclaration.getEndComments());
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
            final FieldDeclaration fieldDeclaration,
            final FieldDeclaration fieldDeclaration2) {
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
    private static boolean equalsAnnotations(
            final List<AnnotationExpr> annotations,
            final List<AnnotationExpr> annotations2) {
        if (annotations == annotations2) {
            return true;
        }
        else if (annotations == null || annotations2 == null) {
            return false;
        }
        if (annotations.size() != annotations2.size()) {
            return false;
        }
        boolean found;
        for (final AnnotationExpr annotation1 : annotations) {
            found = false;
            for (final AnnotationExpr annotation2 : annotations2) {
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
    public static boolean equals(final AnnotationExpr annotation1,
            final AnnotationExpr annotation2) {

        if (annotation1 == annotation2) {
            return true;
        }
        if (annotation1 == null || annotation2 == null) {
            return false;
        }
        if (!annotation1.getName().getName()
                .equals(annotation2.getName().getName())) {
            return false;
        }
        if (!annotation1.getName().equals(annotation2.getName())){
            return false;
        }
        if (!annotation1.getClass().equals(annotation2.getClass())){
            return false;
        }
        if (annotation1 instanceof SingleMemberAnnotationExpr){
            // Compare expression
            String expression1 = ((SingleMemberAnnotationExpr)annotation1).getMemberValue().toString();
            String expression2 = ((SingleMemberAnnotationExpr)annotation2).getMemberValue().toString();
            return expression1.equals(expression2);
        } else if (annotation1 instanceof NormalAnnotationExpr) {
            // Compare pairs
            List<MemberValuePair> pairs1 = ((NormalAnnotationExpr)annotation1).getPairs();
            List<MemberValuePair> pairs2 = ((NormalAnnotationExpr)annotation2).getPairs();
            
            return equals(pairs1,pairs2);
            
        } else if (annotation1 instanceof MarkerAnnotationExpr) {
            // just compare name (and already done it)
            return true;
        } else {
            // No other way to check are equals but toString output
            return annotation1.toString().equals(annotation2.toString());
        }
    }

    /**
     * Compares to {@link MemberValuePair} list
     * 
     * @param pairs1
     * @param pairs2
     * @return
     */
    private static boolean equals(List<MemberValuePair> pairs1,
            List<MemberValuePair> pairs2) {
        if (ObjectUtils.equals(pairs1, pairs2)){
            return true;
        }
        if (pairs1 == null || pairs2 == null){
            return false;
        }
        if (pairs1.size() != pairs2.size()) {
            return false;
        }
        
        // Clone pair2 to better performance
        List<MemberValuePair> pairs2Cloned = new ArrayList<MemberValuePair>(pairs2);
        
        MemberValuePair pair2;
        Iterator<MemberValuePair> pairIterator;
        boolean found;
        // For every pair in 1
        for (MemberValuePair pair1 : pairs1) {
            found = false;
            pairIterator = pairs2Cloned.iterator();
            // Iterate over remaining pair2 elements
            while (pairIterator.hasNext()){
                pair2 = pairIterator.next();
                if (pair1.getName().equals(pair2.getName())){
                    // Found name
                    found = true;
                    // Remove from remaining pair2 elements
                    pairIterator.remove();
                    // compare value
                    if (ObjectUtils.equals(pair1.getValue(), pair2.getValue())){
                        // Equals: check for pair1 finished
                        break;
                    } else {
                        String value1 = ObjectUtils.defaultIfNull(pair1.getValue(), "").toString();
                        String value2 = ObjectUtils.defaultIfNull(pair2.getValue(), "").toString();
                        if (value1.equals(value2)){
                            // Equals: check for pair1 finished
                            break;
                        } else {
                            // Not equals: return false
                            return false;
                        }
                    }
                }
            }
            if (!found) {
                // Pair1 not found: return false
                return false;
            }
        }
        return true;
    }

    /**
     * Update {@code annotations} with {@code }
     * 
     * @param annotations
     * @param annotations2
     */
    public static void updateAnnotations(
            final List<AnnotationExpr> annotations,
            final List<AnnotationExpr> annotations2) {
        if (!equalsAnnotations(annotations, annotations2)) {
            // XXX japa version (1.0.7) has no way to manage
            // AnnotationExpr
            annotations.clear();
            annotations.addAll(annotations2);
        }
    }
}
