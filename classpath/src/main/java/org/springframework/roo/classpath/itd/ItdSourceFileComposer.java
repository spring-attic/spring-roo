package org.springframework.roo.classpath.itd;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.details.AnnotationMetadataUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.DeclaredFieldAnnotationDetails;
import org.springframework.roo.classpath.details.DeclaredMethodAnnotationDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.ImportRegistrationResolverImpl;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * A simple way of producing an inter-type declaration source file.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
public class ItdSourceFileComposer {

    private final JavaType aspect;
    private boolean content;
    private int indentLevel = 0;
    private final JavaType introductionTo;
    private final ItdTypeDetails itdTypeDetails;
    private StringBuilder pw = new StringBuilder();
    private final ImportRegistrationResolver resolver;

    /**
     * Constructs an {@link ItdSourceFileComposer} containing the members that
     * were requested in the passed object.
     * 
     * @param itdTypeDetails to construct (required)
     */
    public ItdSourceFileComposer(final ItdTypeDetails itdTypeDetails) {
        Validate.notNull(itdTypeDetails, "ITD type details required");
        Validate.notNull(itdTypeDetails.getName(),
                "Introduction to is required");

        this.itdTypeDetails = itdTypeDetails;
        introductionTo = itdTypeDetails.getName();
        aspect = itdTypeDetails.getAspect();

        // Create my own resolver, so we can add items to it as we process
        resolver = new ImportRegistrationResolverImpl(itdTypeDetails
                .getAspect().getPackage());
        resolver.addImport(introductionTo); // ROO-2932

        for (final JavaType registeredImport : itdTypeDetails
                .getRegisteredImports()) {
            // Do a sanity check in case the user misused it
            if (resolver.isAdditionLegal(registeredImport)) {
                resolver.addImport(registeredImport);
            }
        }

        appendTypeDeclaration();
        appendExtendsTypes();
        appendImplementsTypes();
        appendTypeAnnotations();
        appendFieldAnnotations();
        appendMethodAnnotations();
        appendFields();
        appendConstructors();
        appendMethods(itdTypeDetails.getGovernor().getPhysicalTypeCategory()
                .equals(PhysicalTypeCategory.INTERFACE));
        appendInnerTypes();
        appendTerminator();

        // Now prepend the package declaration and any imports
        // We need to do this ** at the end ** so we can ensure our compilation
        // unit imports are correct, as they're built as we traverse over the
        // other members
        prependCompilationUnitDetails();
    }

    /**
     * Prints the message, WITHOUT ANY INDENTATION.
     */
    private ItdSourceFileComposer append(final String message) {
        if (message != null && !"".equals(message)) {
            pw.append(message);
            content = true;
        }
        return this;
    }

    private void appendConstructors() {
        final List<? extends ConstructorMetadata> constructors = itdTypeDetails
                .getDeclaredConstructors();
        if (constructors == null || constructors.isEmpty()) {
            return;
        }

        content = true;

        for (final ConstructorMetadata constructor : constructors) {
            Validate.isTrue(
                    constructor.getParameterTypes().size() == constructor
                            .getParameterNames().size(),
                    "Mismatched parameter names against parameter types");

            // Append annotations
            for (final AnnotationMetadata annotation : constructor
                    .getAnnotations()) {
                appendIndent();
                outputAnnotation(annotation);
                this.newLine(false);
            }

            // Append "<modifier> <TargetOfIntroduction>.new" portion
            appendIndent();
            if (constructor.getModifier() != 0) {
                append(Modifier.toString(constructor.getModifier()));
                append(" ");
            }
            append(introductionTo.getSimpleTypeName());
            append(".");
            append("new");

            // Append parameter types and names
            append("(");
            final List<AnnotatedJavaType> parameterTypes = constructor
                    .getParameterTypes();
            final List<JavaSymbolName> parameterNames = constructor
                    .getParameterNames();
            for (int i = 0; i < parameterTypes.size(); i++) {
                final AnnotatedJavaType paramType = parameterTypes.get(i);
                final JavaSymbolName paramName = parameterNames.get(i);
                for (final AnnotationMetadata methodParameterAnnotation : paramType
                        .getAnnotations()) {
                    append(AnnotationMetadataUtils
                            .toSourceForm(methodParameterAnnotation));
                    append(" ");
                }
                append(paramType.getJavaType().getNameIncludingTypeParameters(
                        false, resolver));
                append(" ");
                append(paramName.getSymbolName());
                if (i < parameterTypes.size() - 1) {
                    append(", ");
                }
            }
            append(") {");
            this.newLine(false);
            indent();

            // Add body
            append(constructor.getBody());
            indentRemove();
            appendFormalLine("}");
            this.newLine(false);
        }
    }

    private void appendExtendsTypes() {
        final List<JavaType> extendsTypes = itdTypeDetails.getExtendsTypes();
        if (extendsTypes == null || extendsTypes.isEmpty()) {
            return;
        }

        content = true;

        for (final JavaType extendsType : extendsTypes) {
            appendIndent();
            append("declare parents: ");
            append(introductionTo.getSimpleTypeName());
            append(" extends ");
            if (resolver
                    .isFullyQualifiedFormRequiredAfterAutoImport(extendsType)) {
                append(extendsType.getNameIncludingTypeParameters());
            }
            else {
                append(extendsType.getNameIncludingTypeParameters(false,
                        resolver));
            }
            append(";");
            this.newLine(false);
            this.newLine();
        }
    }

    private void appendFieldAnnotations() {
        final List<DeclaredFieldAnnotationDetails> fieldAnnotations = itdTypeDetails
                .getFieldAnnotations();
        if (fieldAnnotations == null || fieldAnnotations.isEmpty()) {
            return;
        }

        content = true;

        for (final DeclaredFieldAnnotationDetails fieldDetails : fieldAnnotations) {
            appendIndent();
            append("declare @field: * ");
            append(introductionTo.getSimpleTypeName());
            append(".");
            append(fieldDetails.getField().getFieldName().getSymbolName());
            append(": ");
            if (fieldDetails.isRemoveAnnotation()) {
                append("-");
            }
            outputAnnotation(fieldDetails.getFieldAnnotation());
            append(";");
            this.newLine(false);
            this.newLine();
        }
    }

    private void appendFields() {
        final List<? extends FieldMetadata> fields = itdTypeDetails
                .getDeclaredFields();
        if (fields == null || fields.isEmpty()) {
            return;
        }

        content = true;
        for (final FieldMetadata field : fields) {
            // Append annotations
            for (final AnnotationMetadata annotation : field.getAnnotations()) {
                appendIndent();
                outputAnnotation(annotation);
                this.newLine(false);
            }

            // Append "<modifier> <fieldType> <fieldName>" portion
            appendIndent();
            if (field.getModifier() != 0) {
                append(Modifier.toString(field.getModifier()));
                append(" ");
            }
            append(field.getFieldType().getNameIncludingTypeParameters(false,
                    resolver));
            append(" ");
            append(introductionTo.getSimpleTypeName());
            append(".");
            append(field.getFieldName().getSymbolName());

            // Append initializer, if present
            if (field.getFieldInitializer() != null) {
                append(" = ");
                append(field.getFieldInitializer());
            }

            // Complete the field declaration
            append(";");
            this.newLine(false);
            this.newLine();
        }
    }

    /**
     * Prints the message, after adding indents and returns to a new line. This
     * is the most commonly used method.
     */
    private ItdSourceFileComposer appendFormalLine(final String message) {
        appendIndent();
        if (message != null && !"".equals(message)) {
            pw.append(message);
            content = true;
        }
        return newLine(false);
    }

    private void appendImplementsTypes() {
        final List<JavaType> implementsTypes = itdTypeDetails
                .getImplementsTypes();
        if (implementsTypes == null || implementsTypes.isEmpty()) {
            return;
        }

        content = true;

        for (final JavaType extendsType : implementsTypes) {
            appendIndent();
            append("declare parents: ");
            append(introductionTo.getSimpleTypeName());
            append(" implements ");
            if (resolver
                    .isFullyQualifiedFormRequiredAfterAutoImport(extendsType)) {
                append(extendsType.getNameIncludingTypeParameters());
            }
            else {
                append(extendsType.getNameIncludingTypeParameters(false,
                        resolver));
            }
            append(";");
            this.newLine(false);
            this.newLine();
        }
    }

    /**
     * Prints the relevant number of indents.
     */
    private ItdSourceFileComposer appendIndent() {
        for (int i = 0; i < indentLevel; i++) {
            pw.append("    ");
        }
        return this;
    }

    /**
     * supports static inner types with static field definitions only at this
     * point
     */
    private void appendInnerTypes() {
        final List<ClassOrInterfaceTypeDetails> innerTypes = itdTypeDetails
                .getInnerTypes();

        for (final ClassOrInterfaceTypeDetails innerType : innerTypes) {
            content = true;
            appendIndent();
            if (innerType.getModifier() != 0) {
                append(Modifier.toString(innerType.getModifier()));
                append(" ");
            }
            append("class ");
            append(introductionTo.getNameIncludingTypeParameters());
            append(".");
            append(innerType.getName().getSimpleTypeName());
            if (innerType.getExtendsTypes().size() > 0) {
                append(" extends ");
                // There should only be one extends type for inner classes
                final JavaType extendsType = innerType.getExtendsTypes().get(0);
                if (resolver
                        .isFullyQualifiedFormRequiredAfterAutoImport(extendsType)) {
                    append(extendsType.getNameIncludingTypeParameters());
                }
                else {
                    append(extendsType.getNameIncludingTypeParameters(false,
                            resolver));
                }
                append(" ");
            }
            final List<JavaType> implementsTypes = innerType
                    .getImplementsTypes();
            if (implementsTypes.size() > 0) {
                append(" implements ");
                for (int i = 0; i < implementsTypes.size(); i++) {
                    final JavaType implementsType = implementsTypes.get(i);
                    if (resolver
                            .isFullyQualifiedFormRequiredAfterAutoImport(implementsType)) {
                        append(implementsType.getNameIncludingTypeParameters());
                    }
                    else {
                        append(implementsType.getNameIncludingTypeParameters(
                                false, resolver));
                    }
                    if (i != implementsTypes.size() - 1) {
                        append(", ");
                    }
                    else {
                        append(" ");
                    }
                }
            }
            append("{");
            this.newLine(false);

            // Write out fields
            for (final FieldMetadata field : innerType.getDeclaredFields()) {
                indent();
                this.newLine(false);

                // Append annotations
                for (final AnnotationMetadata annotation : field
                        .getAnnotations()) {
                    appendIndent();
                    outputAnnotation(annotation);
                    this.newLine(false);
                }
                appendIndent();
                if (field.getModifier() != 0) {
                    append(Modifier.toString(field.getModifier()));
                    append(" ");
                }
                append(field.getFieldType().getNameIncludingTypeParameters(
                        false, resolver));
                append(" ");
                append(field.getFieldName().getSymbolName());

                // Append initializer, if present
                if (field.getFieldInitializer() != null) {
                    append(" = ");
                    append(field.getFieldInitializer());
                }

                // Complete the field declaration
                append(";");
                this.newLine(false);
                indentRemove();
            }
            this.newLine(false);

            // Write out methods
            indent();
            writeMethods(innerType.getDeclaredMethods(), false, false);
            indentRemove();

            appendIndent();
            append("}");
            this.newLine(false);
            this.newLine();
        }
    }

    private void appendMethodAnnotations() {
        final List<DeclaredMethodAnnotationDetails> methodAnnotations = itdTypeDetails
                .getMethodAnnotations();
        if (methodAnnotations == null || methodAnnotations.isEmpty()) {
            return;
        }

        content = true;

        for (final DeclaredMethodAnnotationDetails methodDetails : methodAnnotations) {
            appendIndent();
            append("declare @method: ");
            append(Modifier.toString(methodDetails.getMethodMetadata()
                    .getModifier()));
            append(" ");
            append(methodDetails.getMethodMetadata().getReturnType()
                    .getNameIncludingTypeParameters());
            append(" ");
            append(introductionTo.getSimpleTypeName());
            append(".");
            append(methodDetails.getMethodMetadata().getMethodName()
                    .getSymbolName());
            append("(");
            for (int i = 0; i < methodDetails.getMethodMetadata()
                    .getParameterTypes().size(); i++) {
                append(methodDetails.getMethodMetadata().getParameterTypes()
                        .get(i).getJavaType()
                        .getNameIncludingTypeParameters(false, resolver));
                if (i != methodDetails.getMethodMetadata().getParameterTypes()
                        .size() - 1) {
                    append(",");
                }
            }
            append("): ");
            outputAnnotation(methodDetails.getMethodAnnotation());
            append(";");
            this.newLine(false);
            this.newLine();
        }
    }

    private void appendMethods(final boolean interfaceMethod) {
        final List<? extends MethodMetadata> methods = itdTypeDetails
                .getDeclaredMethods();
        if (methods == null || methods.isEmpty()) {
            return;
        }

        content = true;
        writeMethods(methods, true, interfaceMethod);
    }

    private void appendTerminator() {
        Validate.isTrue(indentLevel == 1,
                "Indent level must be 1 (not %d) to conclude", indentLevel);
        indentRemove();

        // Ensure we present the content flag, as it will be set true during the
        // formal line append
        final boolean contentBefore = content;
        appendFormalLine("}");
        content = contentBefore;

    }

    private void appendTypeAnnotations() {
        final List<? extends AnnotationMetadata> typeAnnotations = itdTypeDetails
                .getAnnotations();
        if (typeAnnotations == null || typeAnnotations.isEmpty()) {
            return;
        }

        content = true;

        for (final AnnotationMetadata typeAnnotation : typeAnnotations) {
            appendIndent();
            append("declare @type: ");
            append(introductionTo.getSimpleTypeName());
            append(": ");
            outputAnnotation(typeAnnotation);
            append(";");
            this.newLine(false);
            this.newLine();
        }
    }

    private void appendTypeDeclaration() {
        Validate.isTrue(
                introductionTo.getPackage().equals(aspect.getPackage()),
                "Aspect and introduction must be in identical packages");

        appendIndent();
        if (itdTypeDetails.isPrivilegedAspect()) {
            append("privileged ");
        }
        append("aspect " + aspect.getSimpleTypeName() + " {");
        this.newLine(false);
        indent();
        this.newLine();

        // Set to false, as it was set true during the above operations
        content = false;
    }

    private String getNewLine() {
        // We use \n for consistency with JavaParser's DumpVisitor, which always
        // uses \n
        return "\n";
    }

    public String getOutput() {
        return pw.toString();
    }

    /**
     * Increases the indent by one level.
     */
    private ItdSourceFileComposer indent() {
        indentLevel++;
        return this;
    }

    /**
     * Decreases the indent by one level.
     */
    private ItdSourceFileComposer indentRemove() {
        indentLevel--;
        return this;
    }

    /**
     * Indicates whether any content was added to the ITD, aside from the formal
     * ITD declaration.
     * 
     * @return true if there is actual content in the ITD, false otherwise
     */
    public boolean isContent() {
        return content;
    }

    /**
     * Prints a blank line, ensuring any indent is included before doing so.
     */
    private ItdSourceFileComposer newLine() {
        return newLine(true);
    }

    /**
     * Prints a blank line, ensuring any indent is included before doing so.
     */
    private ItdSourceFileComposer newLine(final boolean indent) {
        if (indent) {
            appendIndent();
        }
        // We use \n for consistency with JavaParser's DumpVisitor, which always
        // uses \n
        pw.append(getNewLine());
        // pw.append(StringUtils.LINE_SEPARATOR);
        return this;
    }

    private void outputAnnotation(final AnnotationMetadata annotation) {
        append(AnnotationMetadataUtils.toSourceForm(annotation, resolver));
    }

    private void prependCompilationUnitDetails() {
        final StringBuilder topOfFile = new StringBuilder();

        topOfFile
                .append("// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.")
                .append(getNewLine());
        topOfFile
                .append("// You may push code into the target .java compilation unit if you wish to edit any member(s).")
                .append(getNewLine()).append(getNewLine());

        // Note we're directly interacting with the top of file string builder
        if (!aspect.isDefaultPackage()) {
            topOfFile.append("package ")
                    .append(aspect.getPackage().getFullyQualifiedPackageName())
                    .append(";").append(getNewLine());
            topOfFile.append(getNewLine());
        }

        // Ordered to ensure consistency of output
        final SortedSet<JavaType> types = new TreeSet<JavaType>();
        types.addAll(resolver.getRegisteredImports());
        if (!types.isEmpty()) {
            for (final JavaType importType : types) {
                if (introductionTo.equals(importType.getEnclosingType())) {
                    // We don't "import" types defined within governor, as they
                    // already have scope and this causes AJDT warnings (see
                    // ROO-1686)
                    continue;
                }
                topOfFile.append("import ")
                        .append(importType.getFullyQualifiedTypeName())
                        .append(";").append(getNewLine());
            }

            topOfFile.append(getNewLine());
        }

        // Now append the normal file to the bottom
        topOfFile.append(pw.toString());

        // Replace the old writer with out new writer
        pw = topOfFile;
    }

    private void writeMethods(final List<? extends MethodMetadata> methods,
            final boolean defineTarget, final boolean isInterfaceMethod) {
        for (final MethodMetadata method : methods) {
            Validate.isTrue(
                    method.getParameterTypes().size() == method
                            .getParameterNames().size(),
                    "Method %s has mismatched parameter names against parameter types",
                    method.getMethodName().getSymbolName());

            // Append annotations
            for (final AnnotationMetadata annotation : method.getAnnotations()) {
                appendIndent();
                outputAnnotation(annotation);
                this.newLine(false);
            }

            // Append "<modifier> <returnType> <methodName>" portion
            appendIndent();
            if (method.getModifier() != 0) {
                append(Modifier.toString(method.getModifier()));
                append(" ");
            }

            // return type
            final boolean staticMethod = Modifier
                    .isStatic(method.getModifier());
            append(method.getReturnType().getNameIncludingTypeParameters(
                    staticMethod, resolver));
            append(" ");
            if (defineTarget) {
                append(introductionTo.getSimpleTypeName());
                append(".");
            }
            append(method.getMethodName().getSymbolName());

            // Append parameter types and names
            append("(");
            final List<AnnotatedJavaType> parameterTypes = method
                    .getParameterTypes();
            final List<JavaSymbolName> parameterNames = method
                    .getParameterNames();
            for (int i = 0; i < parameterTypes.size(); i++) {
                final AnnotatedJavaType paramType = parameterTypes.get(i);
                final JavaSymbolName paramName = parameterNames.get(i);
                for (final AnnotationMetadata methodParameterAnnotation : paramType
                        .getAnnotations()) {
                    outputAnnotation(methodParameterAnnotation);
                    append(" ");
                }
                append(paramType.getJavaType().getNameIncludingTypeParameters(
                        false, resolver));
                append(" ");
                append(paramName.getSymbolName());
                if (i < parameterTypes.size() - 1) {
                    append(", ");
                }
            }

            // Add exceptions to be thrown
            final List<JavaType> throwsTypes = method.getThrowsTypes();
            if (throwsTypes.size() > 0) {
                append(") throws ");
                for (int i = 0; i < throwsTypes.size(); i++) {
                    append(throwsTypes.get(i).getNameIncludingTypeParameters(
                            false, resolver));
                    if (throwsTypes.size() > i + 1) {
                        append(", ");
                    }
                }
            }
            else {
                append(")");
            }

            if (isInterfaceMethod) {
                append(";");
            }
            else {
                append(" {");
                this.newLine(false);

                // Add body
                indent();
                append(method.getBody());
                indentRemove();

                appendFormalLine("}");
            }
            this.newLine();
        }
    }
}
