package org.springframework.roo.classpath.javaparser.details;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.TypeParameter;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.body.VariableDeclaratorId;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.Type;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.javaparser.CompilationUnitServices;
import org.springframework.roo.classpath.javaparser.JavaParserUtils;
import org.springframework.roo.model.Builder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Java Parser implementation of {@link ConstructorMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class JavaParserConstructorMetadataBuilder implements
        Builder<ConstructorMetadata> {

    // TODO: Should parse the throws types from JavaParser source

    public static void addConstructor(
            final CompilationUnitServices compilationUnitServices,
            final List<BodyDeclaration> members,
            final ConstructorMetadata constructor,
            final Set<JavaSymbolName> typeParameters) {
        Validate.notNull(compilationUnitServices,
                "Compilation unit services required");
        Validate.notNull(members, "Members required");
        Validate.notNull(constructor, "Method required");

        // Start with the basic constructor
        final ConstructorDeclaration d = new ConstructorDeclaration();
        d.setModifiers(JavaParserUtils.getJavaParserModifier(constructor
                .getModifier()));
        d.setName(PhysicalTypeIdentifier.getJavaType(
                constructor.getDeclaredByMetadataId()).getSimpleTypeName());

        // Add any constructor-level annotations (not parameter annotations)
        final List<AnnotationExpr> annotations = new ArrayList<AnnotationExpr>();
        d.setAnnotations(annotations);
        for (final AnnotationMetadata annotation : constructor.getAnnotations()) {
            JavaParserAnnotationMetadataBuilder.addAnnotationToList(
                    compilationUnitServices, annotations, annotation);
        }

        // Add any constructor parameters, including their individual
        // annotations and type parameters
        final List<Parameter> parameters = new ArrayList<Parameter>();
        d.setParameters(parameters);
        int index = -1;
        for (final AnnotatedJavaType constructorParameter : constructor
                .getParameterTypes()) {
            index++;

            // Add the parameter annotations applicable for this parameter type
            final List<AnnotationExpr> parameterAnnotations = new ArrayList<AnnotationExpr>();

            for (final AnnotationMetadata parameterAnnotation : constructorParameter
                    .getAnnotations()) {
                JavaParserAnnotationMetadataBuilder.addAnnotationToList(
                        compilationUnitServices, parameterAnnotations,
                        parameterAnnotation);
            }

            // Compute the parameter name
            final String parameterName = constructor.getParameterNames()
                    .get(index).getSymbolName();

            // Compute the parameter type
            Type parameterType = null;
            if (constructorParameter.getJavaType().isPrimitive()) {
                parameterType = JavaParserUtils.getType(constructorParameter
                        .getJavaType());
            }
            else {
                final NameExpr importedType = JavaParserUtils
                        .importTypeIfRequired(
                                compilationUnitServices.getEnclosingTypeName(),
                                compilationUnitServices.getImports(),
                                constructorParameter.getJavaType());
                final ClassOrInterfaceType cit = JavaParserUtils
                        .getClassOrInterfaceType(importedType);

                // Add any type arguments presented for the return type
                if (constructorParameter.getJavaType().getParameters().size() > 0) {
                    final List<Type> typeArgs = new ArrayList<Type>();
                    cit.setTypeArgs(typeArgs);
                    for (final JavaType parameter : constructorParameter
                            .getJavaType().getParameters()) {
                        // NameExpr importedParameterType =
                        // JavaParserUtils.importTypeIfRequired(compilationUnitServices.getEnclosingTypeName(),
                        // compilationUnitServices.getImports(), parameter);
                        // typeArgs.add(JavaParserUtils.getReferenceType(importedParameterType));
                        typeArgs.add(JavaParserUtils.importParametersForType(
                                compilationUnitServices.getEnclosingTypeName(),
                                compilationUnitServices.getImports(), parameter));
                    }

                }
                parameterType = cit;
            }

            // Create a Java Parser constructor parameter and add it to the list
            // of parameters
            final Parameter p = new Parameter(parameterType,
                    new VariableDeclaratorId(parameterName));
            p.setAnnotations(parameterAnnotations);
            parameters.add(p);
        }

        // Set the body
        if (constructor.getBody() == null
                || constructor.getBody().length() == 0) {
            d.setBlock(new BlockStmt());
        }
        else {
            // There is a body.
            // We need to make a fake constructor that we can have JavaParser
            // parse.
            // Easiest way to do that is to build a simple source class
            // containing the required method and re-parse it.
            final StringBuilder sb = new StringBuilder();
            sb.append("class TemporaryClass {\n");
            sb.append("  TemporaryClass() {\n");
            sb.append(constructor.getBody());
            sb.append("\n");
            sb.append("  }\n");
            sb.append("}\n");
            final ByteArrayInputStream bais = new ByteArrayInputStream(sb
                    .toString().getBytes());
            CompilationUnit ci;
            try {
                ci = JavaParser.parse(bais);
            }
            catch (final ParseException pe) {
                throw new IllegalStateException(
                        "Illegal state: JavaParser did not parse correctly", pe);
            }
            final List<TypeDeclaration> types = ci.getTypes();
            if (types == null || types.size() != 1) {
                throw new IllegalArgumentException("Method body invalid");
            }
            final TypeDeclaration td = types.get(0);
            final List<BodyDeclaration> bodyDeclarations = td.getMembers();
            if (bodyDeclarations == null || bodyDeclarations.size() != 1) {
                throw new IllegalStateException(
                        "Illegal state: JavaParser did not return body declarations correctly");
            }
            final BodyDeclaration bd = bodyDeclarations.get(0);
            if (!(bd instanceof ConstructorDeclaration)) {
                throw new IllegalStateException(
                        "Illegal state: JavaParser did not return a method declaration correctly");
            }
            final ConstructorDeclaration cd = (ConstructorDeclaration) bd;
            d.setBlock(cd.getBlock());
        }

        // Locate where to add this constructor; also verify if this method
        // already exists
        for (final BodyDeclaration bd : members) {
            if (bd instanceof ConstructorDeclaration) {
                // Next constructor should appear after this current constructor
                final ConstructorDeclaration cd = (ConstructorDeclaration) bd;
                if (cd.getParameters().size() == d.getParameters().size()) {
                    // Possible match, we need to consider parameter types as
                    // well now
                    final ConstructorMetadata constructorMetadata = new JavaParserConstructorMetadataBuilder(
                            constructor.getDeclaredByMetadataId(), cd,
                            compilationUnitServices, typeParameters).build();
                    boolean matchesFully = true;
                    for (final AnnotatedJavaType existingParameter : constructorMetadata
                            .getParameterTypes()) {
                        if (!existingParameter.getJavaType().equals(
                                constructor.getParameterTypes().get(index))) {
                            matchesFully = false;
                            break;
                        }
                    }
                    if (matchesFully) {
                        throw new IllegalStateException("Constructor '"
                                + constructor.getParameterNames()
                                + "' already exists with identical parameters");
                    }
                }
            }
        }

        // Add the constructor to the end of the compilation unit
        members.add(d);
    }

    public static JavaParserConstructorMetadataBuilder getInstance(
            final String declaredByMetadataId,
            final ConstructorDeclaration constructorDeclaration,
            final CompilationUnitServices compilationUnitServices,
            final Set<JavaSymbolName> typeParameterNames) {
        return new JavaParserConstructorMetadataBuilder(declaredByMetadataId,
                constructorDeclaration, compilationUnitServices,
                typeParameterNames);
    }

    private final List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
    private String body;
    private final String declaredByMetadataId;
    private final int modifier;
    private final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    private final List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    private final List<JavaType> throwsTypes = new ArrayList<JavaType>();

    private JavaParserConstructorMetadataBuilder(
            final String declaredByMetadataId,
            final ConstructorDeclaration constructorDeclaration,
            final CompilationUnitServices compilationUnitServices,
            Set<JavaSymbolName> typeParameterNames) {
        Validate.notBlank(declaredByMetadataId,
                "Declared by metadata ID required");
        Validate.notNull(constructorDeclaration,
                "Constructor declaration is mandatory");
        Validate.notNull(compilationUnitServices,
                "Compilation unit services are required");

        // Convert Java Parser modifier into JDK modifier
        modifier = JavaParserUtils.getJdkModifier(constructorDeclaration
                .getModifiers());

        this.declaredByMetadataId = declaredByMetadataId;

        if (typeParameterNames == null) {
            typeParameterNames = new HashSet<JavaSymbolName>();
        }

        // Add method-declared type parameters (if any) to the list of type
        // parameters
        final Set<JavaSymbolName> fullTypeParameters = new HashSet<JavaSymbolName>();
        fullTypeParameters.addAll(typeParameterNames);
        final List<TypeParameter> params = constructorDeclaration
                .getTypeParameters();
        if (params != null) {
            for (final TypeParameter candidate : params) {
                final JavaSymbolName currentTypeParam = new JavaSymbolName(
                        candidate.getName());
                fullTypeParameters.add(currentTypeParam);
            }
        }

        // Get the body
        body = constructorDeclaration.getBlock().toString();
        body = StringUtils.replace(body, "{", "", 1);
        body = body.substring(0, body.lastIndexOf("}"));

        // Lookup the parameters and their names
        if (constructorDeclaration.getParameters() != null) {
            for (final Parameter p : constructorDeclaration.getParameters()) {
                final Type pt = p.getType();
                final JavaType parameterType = JavaParserUtils.getJavaType(
                        compilationUnitServices, pt, fullTypeParameters);

                final List<AnnotationExpr> annotationsList = p.getAnnotations();
                final List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
                if (annotationsList != null) {
                    for (final AnnotationExpr candidate : annotationsList) {
                        final JavaParserAnnotationMetadataBuilder md = JavaParserAnnotationMetadataBuilder
                                .getInstance(candidate, compilationUnitServices);
                        annotations.add(md.build());
                    }
                }

                parameterTypes.add(new AnnotatedJavaType(parameterType,
                        annotations));
                parameterNames.add(new JavaSymbolName(p.getId().getName()));
            }
        }

        if (constructorDeclaration.getAnnotations() != null) {
            for (final AnnotationExpr annotation : constructorDeclaration
                    .getAnnotations()) {
                annotations.add(JavaParserAnnotationMetadataBuilder
                        .getInstance(annotation, compilationUnitServices)
                        .build());
            }
        }
    }

    public ConstructorMetadata build() {
        final ConstructorMetadataBuilder constructorBuilder = new ConstructorMetadataBuilder(
                declaredByMetadataId);
        constructorBuilder.setAnnotations(annotations);
        constructorBuilder.setBodyBuilder(InvocableMemberBodyBuilder
                .getInstance().append(body));
        constructorBuilder.setModifier(modifier);
        constructorBuilder.setParameterNames(parameterNames);
        constructorBuilder.setParameterTypes(parameterTypes);
        constructorBuilder.setThrowsTypes(throwsTypes);
        return constructorBuilder.build();
    }
}
