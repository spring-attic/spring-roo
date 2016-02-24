package org.springframework.roo.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.Validate;

/**
 * Implementation of {@link ImportRegistrationResolver}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class ImportRegistrationResolverImpl implements
        ImportRegistrationResolver {

    private final JavaPackage compilationUnitPackage;
    private final SortedSet<JavaType> registeredImports = new TreeSet<JavaType>(
            new Comparator<JavaType>() {
                public int compare(final JavaType o1, final JavaType o2) {
                    return o1.getFullyQualifiedTypeName().compareTo(
                            o2.getFullyQualifiedTypeName());
                }
            });

    public ImportRegistrationResolverImpl(
            final JavaPackage compilationUnitPackage) {
        Validate.notNull(compilationUnitPackage,
                "Compilation unit package required");
        this.compilationUnitPackage = compilationUnitPackage;
    }

    public void addImport(final JavaType javaType) {
        if (javaType != null) {
            if (!JdkJavaType.isPartOfJavaLang(javaType)) {
                registeredImports.add(javaType);
            }
        }
    }

    public void addImports(final JavaType... typesToImport) {
        for (final JavaType typeToImport : typesToImport) {
            addImport(typeToImport);
        }
    }

    public void addImports(final List<JavaType> typesToImport) {
        if (typesToImport != null) {
            for (final JavaType typeToImport : typesToImport) {
                addImport(typeToImport);
            }
        }
    }

    public JavaPackage getCompilationUnitPackage() {
        return compilationUnitPackage;
    }

    public Set<JavaType> getRegisteredImports() {
        return Collections.unmodifiableSet(registeredImports);
    }

    public boolean isAdditionLegal(final JavaType javaType) {
        Validate.notNull(javaType, "Java type required");

        if (javaType.getDataType() != DataType.TYPE) {
            // It's a type variable or primitive
            return false;
        }

        if (javaType.isDefaultPackage()) {
            // Cannot import types from the default package
            return false;
        }

        // Must be a class, so it's legal if there isn't an existing
        // registration that conflicts
        for (final JavaType candidate : registeredImports) {
            if (candidate.getSimpleTypeName().equals(
                    javaType.getSimpleTypeName())) {
                // Conflict detected
                return false;
            }
        }

        return true;
    }

    public boolean isFullyQualifiedFormRequired(final JavaType javaType) {
        Validate.notNull(javaType, "Java type required");

        if (javaType.getDataType() == DataType.PRIMITIVE
                || javaType.getDataType() == DataType.VARIABLE) {
            // Primitives and type variables do not need to be used in
            // fully-qualified form
            return false;
        }

        if (registeredImports.contains(javaType)) {
            // Already know about this one
            return false;
        }

        if (compilationUnitPackage.equals(javaType.getPackage())) {
            // No need for an explicit registration, given it's in the same
            // package
            return false;
        }

        if (JdkJavaType.isPartOfJavaLang(javaType)) {
            return false;
        }

        // To get this far, it must need a fully-qualified name
        return true;
    }

    public boolean isFullyQualifiedFormRequiredAfterAutoImport(
            final JavaType javaType) {
        Validate.notNull(javaType, "Java type required");

        // Try to add import if possible
        if (isAdditionLegal(javaType)) {
            addImport(javaType);
        }

        // Indicate whether we can use in a simple or need a fully-qualified
        // form
        return isFullyQualifiedFormRequired(javaType);
    }
}
