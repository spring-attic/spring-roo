package org.springframework.roo.classpath.antlrjavaparser.details;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.antlrjavaparser.CompilationUnitServices;
import org.springframework.roo.classpath.antlrjavaparser.JavaParserUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ImportMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.comments.CommentStructure;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.Builder;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

import com.github.antlrjavaparser.api.CompilationUnit;
import com.github.antlrjavaparser.api.ImportDeclaration;
import com.github.antlrjavaparser.api.body.BodyDeclaration;
import com.github.antlrjavaparser.api.body.ClassOrInterfaceDeclaration;
import com.github.antlrjavaparser.api.body.ConstructorDeclaration;
import com.github.antlrjavaparser.api.body.EnumConstantDeclaration;
import com.github.antlrjavaparser.api.body.EnumDeclaration;
import com.github.antlrjavaparser.api.body.FieldDeclaration;
import com.github.antlrjavaparser.api.body.MethodDeclaration;
import com.github.antlrjavaparser.api.body.TypeDeclaration;
import com.github.antlrjavaparser.api.body.VariableDeclarator;
import com.github.antlrjavaparser.api.expr.AnnotationExpr;
import com.github.antlrjavaparser.api.expr.QualifiedNameExpr;
import com.github.antlrjavaparser.api.type.ClassOrInterfaceType;

public class JavaParserClassOrInterfaceTypeDetailsBuilder implements
        Builder<ClassOrInterfaceTypeDetails> {

    static final String UNSUPPORTED_MESSAGE_PREFIX = "Only enum, class and interface files are supported";

    /**
     * Factory method for this builder class
     * 
     * @param compilationUnit
     * @param enclosingCompilationUnitServices
     * @param typeDeclaration
     * @param declaredByMetadataId
     * @param typeName
     * @param metadataService
     * @param typeLocationService
     * @return a non-<code>null</code> builder
     */
    public static JavaParserClassOrInterfaceTypeDetailsBuilder getInstance(
            final CompilationUnit compilationUnit,
            final CompilationUnitServices enclosingCompilationUnitServices,
            final TypeDeclaration typeDeclaration,
            final String declaredByMetadataId, final JavaType typeName,
            final MetadataService metadataService,
            final TypeLocationService typeLocationService) {
        return new JavaParserClassOrInterfaceTypeDetailsBuilder(
                compilationUnit, enclosingCompilationUnitServices,
                typeDeclaration, declaredByMetadataId, typeName,
                metadataService, typeLocationService);
    }

    private final CompilationUnit compilationUnit;
    private JavaPackage compilationUnitPackage;
    private final CompilationUnitServices compilationUnitServices;
    private final String declaredByMetadataId;
    private List<ImportDeclaration> imports = new ArrayList<ImportDeclaration>();
    private final List<TypeDeclaration> innerTypes = new ArrayList<TypeDeclaration>();
    private final MetadataService metadataService;

    private JavaType name;
    private PhysicalTypeCategory physicalTypeCategory;
    private final TypeDeclaration typeDeclaration;
    private final TypeLocationService typeLocationService;

    /**
     * Constructor
     * 
     * @param compilationUnit
     * @param enclosingCompilationUnitServices
     * @param typeDeclaration
     * @param declaredByMetadataId
     * @param typeName
     * @param metadataService
     * @param typeLocationService
     */
    private JavaParserClassOrInterfaceTypeDetailsBuilder(
            final CompilationUnit compilationUnit,
            final CompilationUnitServices enclosingCompilationUnitServices,
            final TypeDeclaration typeDeclaration,
            final String declaredByMetadataId, final JavaType typeName,
            final MetadataService metadataService,
            final TypeLocationService typeLocationService) {
        // Check
        Validate.notNull(compilationUnit, "Compilation unit required");
        Validate.notBlank(declaredByMetadataId,
                "Declared by metadata ID required");
        Validate.notNull(typeDeclaration,
                "Unable to locate the class or interface declaration");
        Validate.notNull(typeName, "Name required");

        // Assign
        this.compilationUnit = compilationUnit;
        compilationUnitServices = enclosingCompilationUnitServices == null ? getDefaultCompilationUnitServices()
                : enclosingCompilationUnitServices;
        this.declaredByMetadataId = declaredByMetadataId;
        this.metadataService = metadataService;
        name = typeName;
        this.typeDeclaration = typeDeclaration;
        this.typeLocationService = typeLocationService;
    }

    @Override
    public ClassOrInterfaceTypeDetails build() {
        Validate.notEmpty(compilationUnit.getTypes(),
                "No types in compilation unit, so unable to continue parsing");

        ClassOrInterfaceDeclaration clazz = null;
        EnumDeclaration enumClazz = null;

        final StringBuilder sb = new StringBuilder(compilationUnit.getPackage()
                .getName().toString());
        if (name.getEnclosingType() != null) {
            sb.append(".").append(name.getEnclosingType().getSimpleTypeName());
        }
        compilationUnitPackage = new JavaPackage(sb.toString());

        // Determine the type name, adding type parameters if possible
        final JavaType newName = JavaParserUtils.getJavaType(
                compilationUnitServices, typeDeclaration);

        // Revert back to the original type name (thus avoiding unnecessary
        // inferences about java.lang types; see ROO-244)
        name = new JavaType(newName.getFullyQualifiedTypeName(),
                newName.getEnclosingType(), newName.getArray(),
                newName.getDataType(), newName.getArgName(),
                newName.getParameters());

        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                declaredByMetadataId);

        physicalTypeCategory = PhysicalTypeCategory.CLASS;
        if (typeDeclaration instanceof ClassOrInterfaceDeclaration) {
            clazz = (ClassOrInterfaceDeclaration) typeDeclaration;
            if (clazz.isInterface()) {
                physicalTypeCategory = PhysicalTypeCategory.INTERFACE;
            }

        }
        else if (typeDeclaration instanceof EnumDeclaration) {
            enumClazz = (EnumDeclaration) typeDeclaration;
            physicalTypeCategory = PhysicalTypeCategory.ENUMERATION;
        }

        Validate.notNull(physicalTypeCategory, "%s (%s for %s)",
                UNSUPPORTED_MESSAGE_PREFIX, typeDeclaration.getClass()
                        .getSimpleName(), name);

        cidBuilder.setName(name);
        cidBuilder.setPhysicalTypeCategory(physicalTypeCategory);

        imports = compilationUnit.getImports();
        if (imports == null) {
            imports = new ArrayList<ImportDeclaration>();
            compilationUnit.setImports(imports);
        }

        // Verify the package declaration appears to be correct
        Validate.isTrue(compilationUnitPackage.equals(name.getPackage()),
                "Compilation unit package '%s' unexpected for type '%s'",
                compilationUnitPackage, name.getPackage());

        for (final ImportDeclaration importDeclaration : imports) {
            if (importDeclaration.getName() instanceof QualifiedNameExpr) {
                final String qualifier = ((QualifiedNameExpr) importDeclaration
                        .getName()).getQualifier().toString();
                final String simpleName = importDeclaration.getName().getName();
                final String fullName = qualifier + "." + simpleName;
                // We want to calculate these...

                final JavaType type = new JavaType(fullName);
                final JavaPackage typePackage = importDeclaration.isAsterisk() ? new JavaPackage(
                        fullName) : type.getPackage();

                // Process any comments for the import
                final CommentStructure commentStructure = new CommentStructure();
                JavaParserCommentMetadataBuilder.updateCommentsToRoo(
                        commentStructure, importDeclaration);

                final ImportMetadataBuilder newImport = new ImportMetadataBuilder(
                        declaredByMetadataId, 0, typePackage, type,
                        importDeclaration.isStatic(),
                        importDeclaration.isAsterisk());

                newImport.setCommentStructure(commentStructure);

                cidBuilder.add(newImport.build());
            }
        }

        // Convert Java Parser modifier into JDK modifier
        cidBuilder.setModifier(JavaParserUtils.getJdkModifier(typeDeclaration
                .getModifiers()));

        // Type parameters
        final Set<JavaSymbolName> typeParameterNames = new HashSet<JavaSymbolName>();
        for (final JavaType param : name.getParameters()) {
            final JavaSymbolName arg = param.getArgName();
            // Fortunately type names can only appear at the top-level
            if (arg != null && !JavaType.WILDCARD_NEITHER.equals(arg)
                    && !JavaType.WILDCARD_EXTENDS.equals(arg)
                    && !JavaType.WILDCARD_SUPER.equals(arg)) {
                typeParameterNames.add(arg);
            }
        }

        List<ClassOrInterfaceType> implementsList;
        List<AnnotationExpr> annotationsList = null;
        List<BodyDeclaration> members = null;

        if (clazz != null) {
            final List<ClassOrInterfaceType> extendsList = clazz.getExtends();
            if (extendsList != null) {
                for (final ClassOrInterfaceType candidate : extendsList) {
                    final JavaType javaType = JavaParserUtils.getJavaTypeNow(
                            compilationUnitServices, candidate,
                            typeParameterNames);
                    cidBuilder.addExtendsTypes(javaType);
                }
            }

            final List<JavaType> extendsTypes = cidBuilder.getExtendsTypes();
            // Obtain the superclass, if this is a class and one is available
            if (physicalTypeCategory == PhysicalTypeCategory.CLASS
                    && extendsTypes.size() == 1) {
                final JavaType superclass = extendsTypes.get(0);
                final String superclassId = typeLocationService
                        .getPhysicalTypeIdentifier(superclass);
                PhysicalTypeMetadata superPtm = null;
                if (superclassId != null) {
                    superPtm = (PhysicalTypeMetadata) metadataService
                            .get(superclassId);
                }
                if (superPtm != null
                        && superPtm.getMemberHoldingTypeDetails() != null) {
                    cidBuilder.setSuperclass(superPtm
                            .getMemberHoldingTypeDetails());
                }
            }

            implementsList = clazz.getImplements();
            if (implementsList != null) {
                for (final ClassOrInterfaceType candidate : implementsList) {
                    final JavaType javaType = JavaParserUtils.getJavaTypeNow(
                            compilationUnitServices, candidate,
                            typeParameterNames);
                    cidBuilder.addImplementsType(javaType);
                }
            }

            annotationsList = typeDeclaration.getAnnotations();
            members = clazz.getMembers();
        }

        if (enumClazz != null) {
            final List<EnumConstantDeclaration> constants = enumClazz
                    .getEntries();
            if (constants != null) {
                for (final EnumConstantDeclaration enumConstants : constants) {
                    cidBuilder.addEnumConstant(new JavaSymbolName(enumConstants
                            .getName()));
                }
            }

            implementsList = enumClazz.getImplements();
            annotationsList = enumClazz.getAnnotations();
            members = enumClazz.getMembers();
        }

        if (annotationsList != null) {
            for (final AnnotationExpr candidate : annotationsList) {
                final AnnotationMetadata md = JavaParserAnnotationMetadataBuilder
                        .getInstance(candidate, compilationUnitServices)
                        .build();

                final CommentStructure commentStructure = new CommentStructure();
                JavaParserCommentMetadataBuilder.updateCommentsToRoo(
                        commentStructure, candidate);
                md.setCommentStructure(commentStructure);

                cidBuilder.addAnnotation(md);
            }
        }

        if (members != null) {
            // Now we've finished declaring the type, we should introspect for
            // any inner types that can thus be referred to in other body
            // members
            // We defer this until now because it's illegal to refer to an inner
            // type in the signature of the enclosing type
            for (final BodyDeclaration bodyDeclaration : members) {
                if (bodyDeclaration instanceof TypeDeclaration) {
                    // Found a type
                    innerTypes.add((TypeDeclaration) bodyDeclaration);
                }
            }

            for (final BodyDeclaration member : members) {
                if (member instanceof FieldDeclaration) {
                    final FieldDeclaration castMember = (FieldDeclaration) member;
                    for (final VariableDeclarator var : castMember
                            .getVariables()) {
                        final FieldMetadata field = JavaParserFieldMetadataBuilder
                                .getInstance(declaredByMetadataId, castMember,
                                        var, compilationUnitServices,
                                        typeParameterNames).build();

                        final CommentStructure commentStructure = new CommentStructure();
                        JavaParserCommentMetadataBuilder.updateCommentsToRoo(
                                commentStructure, member);
                        field.setCommentStructure(commentStructure);

                        cidBuilder.addField(field);
                    }
                }
                if (member instanceof MethodDeclaration) {
                    final MethodDeclaration castMember = (MethodDeclaration) member;
                    final MethodMetadata method = JavaParserMethodMetadataBuilder
                            .getInstance(declaredByMetadataId, castMember,
                                    compilationUnitServices, typeParameterNames)
                            .build();

                    final CommentStructure commentStructure = new CommentStructure();
                    JavaParserCommentMetadataBuilder.updateCommentsToRoo(
                            commentStructure, member);
                    method.setCommentStructure(commentStructure);

                    cidBuilder.addMethod(method);
                }
                if (member instanceof ConstructorDeclaration) {
                    final ConstructorDeclaration castMember = (ConstructorDeclaration) member;
                    final ConstructorMetadata constructor = JavaParserConstructorMetadataBuilder
                            .getInstance(declaredByMetadataId, castMember,
                                    compilationUnitServices, typeParameterNames)
                            .build();

                    final CommentStructure commentStructure = new CommentStructure();
                    JavaParserCommentMetadataBuilder.updateCommentsToRoo(
                            commentStructure, member);
                    constructor.setCommentStructure(commentStructure);

                    cidBuilder.addConstructor(constructor);
                }
                if (member instanceof TypeDeclaration) {
                    final TypeDeclaration castMember = (TypeDeclaration) member;
                    final JavaType innerType = new JavaType(
                            castMember.getName(), name);
                    final String innerTypeMetadataId = PhysicalTypeIdentifier
                            .createIdentifier(innerType, PhysicalTypeIdentifier
                                    .getPath(declaredByMetadataId));
                    final ClassOrInterfaceTypeDetails cid = new JavaParserClassOrInterfaceTypeDetailsBuilder(
                            compilationUnit, compilationUnitServices,
                            castMember, innerTypeMetadataId, innerType,
                            metadataService, typeLocationService).build();
                    cidBuilder.addInnerType(cid);
                }
            }
        }

        return cidBuilder.build();
    }

    private CompilationUnitServices getDefaultCompilationUnitServices() {
        return new CompilationUnitServices() {
            @Override
            public JavaPackage getCompilationUnitPackage() {
                return compilationUnitPackage;
            }

            @Override
            public JavaType getEnclosingTypeName() {
                return name;
            }

            @Override
            public List<ImportDeclaration> getImports() {
                return imports;
            }

            @Override
            public List<TypeDeclaration> getInnerTypes() {
                return innerTypes;
            }

            @Override
            public PhysicalTypeCategory getPhysicalTypeCategory() {
                return physicalTypeCategory;
            }
        };
    }
}
