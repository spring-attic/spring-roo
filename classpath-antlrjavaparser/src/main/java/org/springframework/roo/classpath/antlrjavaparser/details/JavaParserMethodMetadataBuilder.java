package org.springframework.roo.classpath.antlrjavaparser.details;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.antlrjavaparser.CompilationUnitServices;
import org.springframework.roo.classpath.antlrjavaparser.JavaParserUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.model.Builder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

import com.github.antlrjavaparser.JavaParser;
import com.github.antlrjavaparser.ParseException;
import com.github.antlrjavaparser.api.CompilationUnit;
import com.github.antlrjavaparser.api.TypeParameter;
import com.github.antlrjavaparser.api.body.BodyDeclaration;
import com.github.antlrjavaparser.api.body.MethodDeclaration;
import com.github.antlrjavaparser.api.body.Parameter;
import com.github.antlrjavaparser.api.body.TypeDeclaration;
import com.github.antlrjavaparser.api.body.VariableDeclaratorId;
import com.github.antlrjavaparser.api.expr.AnnotationExpr;
import com.github.antlrjavaparser.api.expr.NameExpr;
import com.github.antlrjavaparser.api.stmt.BlockStmt;
import com.github.antlrjavaparser.api.type.ClassOrInterfaceType;
import com.github.antlrjavaparser.api.type.ReferenceType;
import com.github.antlrjavaparser.api.type.Type;

/**
 * Java Parser implementation of {@link MethodMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class JavaParserMethodMetadataBuilder implements Builder<MethodMetadata> {

    public static void addMethod(
            final CompilationUnitServices compilationUnitServices,
            final List<BodyDeclaration> members, final MethodMetadata method,
            Set<JavaSymbolName> typeParameters) {
        Validate.notNull(compilationUnitServices,
                "Flushable compilation unit services required");
        Validate.notNull(members, "Members required");
        Validate.notNull(method, "Method required");

        if (typeParameters == null) {
            typeParameters = new HashSet<JavaSymbolName>();
        }

        // Create the return type we should use
        Type returnType = null;
        if (method.getReturnType().isPrimitive()) {
            returnType = JavaParserUtils.getType(method.getReturnType());
        }
        else {
            final NameExpr importedType = JavaParserUtils.importTypeIfRequired(
                    compilationUnitServices.getEnclosingTypeName(),
                    compilationUnitServices.getImports(),
                    method.getReturnType());
            final ClassOrInterfaceType cit = JavaParserUtils
                    .getClassOrInterfaceType(importedType);

            // Add any type arguments presented for the return type
            if (method.getReturnType().getParameters().size() > 0) {
                final List<Type> typeArgs = new ArrayList<Type>();
                cit.setTypeArgs(typeArgs);
                for (final JavaType parameter : method.getReturnType()
                        .getParameters()) {
                    typeArgs.add(JavaParserUtils.importParametersForType(
                            compilationUnitServices.getEnclosingTypeName(),
                            compilationUnitServices.getImports(), parameter));
                }
            }

            // Handle arrays
            if (method.getReturnType().isArray()) {
                final ReferenceType rt = new ReferenceType();
                rt.setArrayCount(method.getReturnType().getArray());
                rt.setType(cit);
                returnType = rt;
            }
            else {
                returnType = cit;
            }
        }

        // Start with the basic method
        final MethodDeclaration d = new MethodDeclaration();
        d.setModifiers(JavaParserUtils.getJavaParserModifier(method
                .getModifier()));
        d.setName(method.getMethodName().getSymbolName());
        d.setType(returnType);

        // Add any method-level annotations (not parameter annotations)
        final List<AnnotationExpr> annotations = new ArrayList<AnnotationExpr>();
        d.setAnnotations(annotations);
        for (final AnnotationMetadata annotation : method.getAnnotations()) {
            JavaParserAnnotationMetadataBuilder.addAnnotationToList(
                    compilationUnitServices, annotations, annotation);
        }

        // Add any method parameters, including their individual annotations and
        // type parameters
        final List<Parameter> parameters = new ArrayList<Parameter>();
        d.setParameters(parameters);

        int index = -1;
        for (final AnnotatedJavaType methodParameter : method
                .getParameterTypes()) {
            index++;

            // Add the parameter annotations applicable for this parameter type
            final List<AnnotationExpr> parameterAnnotations = new ArrayList<AnnotationExpr>();

            for (final AnnotationMetadata parameterAnnotation : methodParameter
                    .getAnnotations()) {
                JavaParserAnnotationMetadataBuilder.addAnnotationToList(
                        compilationUnitServices, parameterAnnotations,
                        parameterAnnotation);
            }

            // Compute the parameter name
            final String parameterName = method.getParameterNames().get(index)
                    .getSymbolName();

            // Compute the parameter type
            Type parameterType = null;
            if (methodParameter.getJavaType().isPrimitive()) {
                parameterType = JavaParserUtils.getType(methodParameter
                        .getJavaType());
            }
            else {
                final NameExpr type = JavaParserUtils.importTypeIfRequired(
                        compilationUnitServices.getEnclosingTypeName(),
                        compilationUnitServices.getImports(),
                        methodParameter.getJavaType());
                final ClassOrInterfaceType cit = JavaParserUtils
                        .getClassOrInterfaceType(type);

                // Add any type arguments presented for the return type
                if (methodParameter.getJavaType().getParameters().size() > 0) {
                    final List<Type> typeArgs = new ArrayList<Type>();
                    cit.setTypeArgs(typeArgs);
                    for (final JavaType parameter : methodParameter
                            .getJavaType().getParameters()) {
                        typeArgs.add(JavaParserUtils.importParametersForType(
                                compilationUnitServices.getEnclosingTypeName(),
                                compilationUnitServices.getImports(), parameter));
                    }
                }

                // Handle arrays
                if (methodParameter.getJavaType().isArray()) {
                    final ReferenceType rt = new ReferenceType();
                    rt.setArrayCount(methodParameter.getJavaType().getArray());
                    rt.setType(cit);
                    parameterType = rt;
                }
                else {
                    parameterType = cit;
                }
            }

            // Create a Java Parser method parameter and add it to the list of
            // parameters
            final Parameter p = new Parameter(parameterType,
                    new VariableDeclaratorId(parameterName));
            p.setVarArgs(methodParameter.isVarArgs());
            p.setAnnotations(parameterAnnotations);
            parameters.add(p);
        }

        // Add exceptions which the method my throw
        if (method.getThrowsTypes().size() > 0) {
            final List<NameExpr> throwsTypes = new ArrayList<NameExpr>();
            for (final JavaType javaType : method.getThrowsTypes()) {
                final NameExpr importedType = JavaParserUtils
                        .importTypeIfRequired(
                                compilationUnitServices.getEnclosingTypeName(),
                                compilationUnitServices.getImports(), javaType);
                throwsTypes.add(importedType);
            }
            d.setThrows(throwsTypes);
        }

        // Set the body
        if (StringUtils.isBlank(method.getBody())) {
            // Never set the body if an abstract method
            if (!Modifier.isAbstract(method.getModifier())
                    && !PhysicalTypeCategory.INTERFACE
                            .equals(compilationUnitServices
                                    .getPhysicalTypeCategory())) {
                d.setBody(new BlockStmt());
            }
        }
        else {
            // There is a body.
            // We need to make a fake method that we can have JavaParser parse.
            // Easiest way to do that is to build a simple source class
            // containing the required method and re-parse it.
            final StringBuilder sb = new StringBuilder();
            sb.append("class TemporaryClass {\n");
            sb.append("  public void temporaryMethod() {\n");
            sb.append(method.getBody());
            sb.append("\n");
            sb.append("  }\n");
            sb.append("}\n");
            final ByteArrayInputStream bais = new ByteArrayInputStream(sb
                    .toString().getBytes());
            CompilationUnit ci;
            try {
                ci = JavaParser.parse(bais);
            }
            catch (final IOException e) {
                throw new IllegalStateException(
                        "Illegal state: Unable to parse input stream", e);
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
            if (!(bd instanceof MethodDeclaration)) {
                throw new IllegalStateException(
                        "Illegal state: JavaParser did not return a method declaration correctly");
            }
            final MethodDeclaration md = (MethodDeclaration) bd;
            d.setBody(md.getBody());
        }

        // Locate where to add this method; also verify if this method already
        // exists
        for (final BodyDeclaration bd : members) {
            if (bd instanceof MethodDeclaration) {
                // Next method should appear after this current method
                final MethodDeclaration md = (MethodDeclaration) bd;
                if (md.getName().equals(d.getName())) {
                    if ((md.getParameters() == null || md.getParameters()
                            .isEmpty())
                            && (d.getParameters() == null || d.getParameters()
                                    .isEmpty())) {
                        throw new IllegalStateException("Method '"
                                + method.getMethodName().getSymbolName()
                                + "' already exists");
                    }
                    else if (md.getParameters() != null
                            && md.getParameters().size() == d.getParameters()
                                    .size()) {
                        // Possible match, we need to consider parameter types
                        // as well now
                        final MethodMetadata methodMetadata = JavaParserMethodMetadataBuilder
                                .getInstance(method.getDeclaredByMetadataId(),
                                        md, compilationUnitServices,
                                        typeParameters).build();
                        boolean matchesFully = true;
                        index = -1;
                        for (final AnnotatedJavaType existingParameter : methodMetadata
                                .getParameterTypes()) {
                            index++;
                            final AnnotatedJavaType parameterType = method
                                    .getParameterTypes().get(index);
                            if (!existingParameter.getJavaType().equals(
                                    parameterType.getJavaType())) {
                                matchesFully = false;
                                break;
                            }
                        }
                        if (matchesFully) {
                            throw new IllegalStateException(
                                    "Method '"
                                            + method.getMethodName()
                                                    .getSymbolName()
                                            + "' already exists with identical parameters");
                        }
                    }
                }
            }
        }

        // Add the method to the end of the compilation unit
        members.add(d);
    }

    public static JavaParserMethodMetadataBuilder getInstance(
            final String declaredByMetadataId,
            final MethodDeclaration methodDeclaration,
            final CompilationUnitServices compilationUnitServices,
            final Set<JavaSymbolName> typeParameters) {
        return new JavaParserMethodMetadataBuilder(declaredByMetadataId,
                methodDeclaration, compilationUnitServices, typeParameters);
    }

    private final List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
    private String body;
    private final String declaredByMetadataId;
    private final JavaSymbolName methodName;
    private final int modifier;
    private final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    private final List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    private final JavaType returnType;

    private final List<JavaType> throwsTypes = new ArrayList<JavaType>();

    private JavaParserMethodMetadataBuilder(final String declaredByMetadataId,
            final MethodDeclaration methodDeclaration,
            final CompilationUnitServices compilationUnitServices,
            final Set<JavaSymbolName> typeParameters) {
        Validate.notBlank(declaredByMetadataId,
                "Declared by metadata ID required");
        Validate.notNull(methodDeclaration, "Method declaration is mandatory");
        Validate.notNull(compilationUnitServices,
                "Compilation unit services are required");

        this.declaredByMetadataId = declaredByMetadataId;

        // Convert Java Parser modifier into JDK modifier
        modifier = JavaParserUtils.getJdkModifier(methodDeclaration
                .getModifiers());

        // Add method-declared type parameters (if any) to the list of type
        // parameters
        final Set<JavaSymbolName> fullTypeParameters = new HashSet<JavaSymbolName>();
        fullTypeParameters.addAll(typeParameters);
        final List<TypeParameter> params = methodDeclaration
                .getTypeParameters();
        if (params != null) {
            for (final TypeParameter candidate : params) {
                final JavaSymbolName currentTypeParam = new JavaSymbolName(
                        candidate.getName());
                fullTypeParameters.add(currentTypeParam);
            }
        }

        // Compute the return type
        final Type rt = methodDeclaration.getType();
        returnType = JavaParserUtils.getJavaType(compilationUnitServices, rt,
                fullTypeParameters);

        // Compute the method name
        methodName = new JavaSymbolName(methodDeclaration.getName());

        // Get the body
        body = methodDeclaration.getBody() == null ? null : methodDeclaration
                .getBody().toString();
        if (body != null) {
            body = StringUtils.replace(body, "{", "", 1);
            body = body.substring(0, body.lastIndexOf("}"));
        }

        // Lookup the parameters and their names
        if (methodDeclaration.getParameters() != null) {
            for (final Parameter p : methodDeclaration.getParameters()) {
                final Type pt = p.getType();
                final JavaType parameterType = JavaParserUtils.getJavaType(
                        compilationUnitServices, pt, fullTypeParameters);
                final List<AnnotationExpr> annotationsList = p.getAnnotations();
                final List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
                if (annotationsList != null) {
                    for (final AnnotationExpr candidate : annotationsList) {
                        final AnnotationMetadata annotationMetadata = JavaParserAnnotationMetadataBuilder
                                .getInstance(candidate, compilationUnitServices)
                                .build();
                        annotations.add(annotationMetadata);
                    }
                }
                final AnnotatedJavaType param = new AnnotatedJavaType(
                        parameterType, annotations);
                param.setVarArgs(p.isVarArgs());
                parameterTypes.add(param);
                parameterNames.add(new JavaSymbolName(p.getId().getName()));
            }
        }

        if (methodDeclaration.getThrows() != null) {
            for (final NameExpr throwsType : methodDeclaration.getThrows()) {
                final JavaType throwing = JavaParserUtils
                        .getJavaType(compilationUnitServices, throwsType,
                                fullTypeParameters);
                throwsTypes.add(throwing);
            }
        }

        if (methodDeclaration.getAnnotations() != null) {
            for (final AnnotationExpr annotation : methodDeclaration
                    .getAnnotations()) {
                annotations.add(JavaParserAnnotationMetadataBuilder
                        .getInstance(annotation, compilationUnitServices)
                        .build());
            }
        }
    }

    @Override
    public MethodMetadata build() {
        final MethodMetadataBuilder methodMetadataBuilder = new MethodMetadataBuilder(
                declaredByMetadataId);
        methodMetadataBuilder.setMethodName(methodName);
        methodMetadataBuilder.setReturnType(returnType);
        methodMetadataBuilder.setAnnotations(annotations);
        methodMetadataBuilder.setBodyBuilder(InvocableMemberBodyBuilder
                .getInstance().append(body));
        methodMetadataBuilder.setModifier(modifier);
        methodMetadataBuilder.setParameterNames(parameterNames);
        methodMetadataBuilder.setParameterTypes(parameterTypes);
        methodMetadataBuilder.setThrowsTypes(throwsTypes);
        return methodMetadataBuilder.build();
    }
}
