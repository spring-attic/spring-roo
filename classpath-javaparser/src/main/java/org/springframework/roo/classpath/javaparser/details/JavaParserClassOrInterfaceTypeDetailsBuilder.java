package org.springframework.roo.classpath.javaparser.details;

import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.EnumConstantDeclaration;
import japa.parser.ast.body.EnumDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.QualifiedNameExpr;
import japa.parser.ast.type.ClassOrInterfaceType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ImportMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.javaparser.CompilationUnitServices;
import org.springframework.roo.classpath.javaparser.JavaParserUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.Builder;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

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

        Validate.notNull(physicalTypeCategory, UNSUPPORTED_MESSAGE_PREFIX
                + " (" + typeDeclaration.getClass().getSimpleName() + " for "
                + name + ")");

        cidBuilder.setName(name);
        cidBuilder.setPhysicalTypeCategory(physicalTypeCategory);

        imports = compilationUnit.getImports();
        if (imports == null) {
            imports = new ArrayList<ImportDeclaration>();
            compilationUnit.setImports(imports);
        }

        // Verify the package declaration appears to be correct
        Validate.isTrue(compilationUnitPackage.equals(name.getPackage()),
                "Compilation unit package '" + compilationUnitPackage
                        + "' unexpected for type '" + name.getPackage() + "'");

        for (final ImportDeclaration importDeclaration : imports) {
            if (importDeclaration.getName() instanceof QualifiedNameExpr) {
                final String qualifier = ((QualifiedNameExpr) importDeclaration
                        .getName()).getQualifier().toString();
                final String simpleName = importDeclaration.getName().getName();
                final String fullName = qualifier + "." + simpleName;
                // We want to calculate these...

                final JavaType type = new JavaType(fullName);
                final JavaPackage typePackage = type.getPackage();
                final ImportMetadataBuilder newImport = new ImportMetadataBuilder(
                        declaredByMetadataId, 0, typePackage, type,
                        importDeclaration.isStatic(),
                        importDeclaration.isAsterisk());
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
                        cidBuilder.addField(field);
                    }
                }
                if (member instanceof MethodDeclaration) {
                    final MethodDeclaration castMember = (MethodDeclaration) member;
                    final MethodMetadata method = JavaParserMethodMetadataBuilder
                            .getInstance(declaredByMetadataId, castMember,
                                    compilationUnitServices, typeParameterNames)
                            .build();
                    cidBuilder.addMethod(method);
                }
                if (member instanceof ConstructorDeclaration) {
                    final ConstructorDeclaration castMember = (ConstructorDeclaration) member;
                    final ConstructorMetadata constructor = JavaParserConstructorMetadataBuilder
                            .getInstance(declaredByMetadataId, castMember,
                                    compilationUnitServices, typeParameterNames)
                            .build();
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
            public JavaPackage getCompilationUnitPackage() {
                return compilationUnitPackage;
            }

            public JavaType getEnclosingTypeName() {
                return name;
            }

            public List<ImportDeclaration> getImports() {
                return imports;
            }

            public List<TypeDeclaration> getInnerTypes() {
                return innerTypes;
            }

            public PhysicalTypeCategory getPhysicalTypeCategory() {
                return physicalTypeCategory;
            }
        };
    }
}
