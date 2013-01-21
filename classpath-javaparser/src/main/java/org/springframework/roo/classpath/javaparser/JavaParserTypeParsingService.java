package org.springframework.roo.classpath.javaparser;

import static org.springframework.roo.model.JavaType.OBJECT;
import japa.parser.ASTHelper;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.PackageDeclaration;
import japa.parser.ast.TypeParameter;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.EnumConstantDeclaration;
import japa.parser.ast.body.EnumDeclaration;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.QualifiedNameExpr;
import japa.parser.ast.type.ClassOrInterfaceType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeParsingService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ImportMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.javaparser.details.JavaParserAnnotationMetadataBuilder;
import org.springframework.roo.classpath.javaparser.details.JavaParserClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.javaparser.details.JavaParserConstructorMetadataBuilder;
import org.springframework.roo.classpath.javaparser.details.JavaParserFieldMetadataBuilder;
import org.springframework.roo.classpath.javaparser.details.JavaParserMethodMetadataBuilder;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

@Component(immediate = true)
@Service
public class JavaParserTypeParsingService implements TypeParsingService {

    @Reference MetadataService metadataService;
    @Reference TypeLocationService typeLocationService;

    private void addEnumConstant(final List<EnumConstantDeclaration> constants,
            final JavaSymbolName name) {
        // Determine location to insert
        for (final EnumConstantDeclaration constant : constants) {
            if (constant.getName().equals(name.getSymbolName())) {
                throw new IllegalArgumentException("Enum constant '"
                        + name.getSymbolName() + "' already exists");
            }
        }
        final EnumConstantDeclaration newEntry = new EnumConstantDeclaration(
                name.getSymbolName());
        constants.add(constants.size(), newEntry);
    }

    public final String getCompilationUnitContents(
            final ClassOrInterfaceTypeDetails cid) {
        Validate.notNull(cid, "Class or interface type details are required");
        // Create a compilation unit to store the type to be created
        final CompilationUnit compilationUnit = new CompilationUnit();

        // NB: this import list is replaced at the end of this method by a
        // sorted version
        compilationUnit.setImports(new ArrayList<ImportDeclaration>());

        if (!cid.getName().isDefaultPackage()) {
            compilationUnit.setPackage(new PackageDeclaration(ASTHelper
                    .createNameExpr(cid.getName().getPackage()
                            .getFullyQualifiedPackageName())));
        }

        // Add the class of interface declaration to the compilation unit
        final List<TypeDeclaration> types = new ArrayList<TypeDeclaration>();
        compilationUnit.setTypes(types);

        updateOutput(compilationUnit, null, cid, null);

        return compilationUnit.toString();
    }

    public ClassOrInterfaceTypeDetails getTypeAtLocation(
            final String fileIdentifier, final String declaredByMetadataId,
            final JavaType typeName) {
        Validate.notBlank(fileIdentifier, "Compilation unit path required");
        Validate.notBlank(declaredByMetadataId,
                "Declaring metadata ID required");
        Validate.notNull(typeName, "Java type to locate required");
        final File file = new File(fileIdentifier);
        String typeContents = "";
        try {
            typeContents = FileUtils.readFileToString(file);
        }
        catch (IOException ignored) {
        }
        if (StringUtils.isBlank(typeContents)) {
            return null;
        }
        return getTypeFromString(typeContents, declaredByMetadataId, typeName);
    }

    public ClassOrInterfaceTypeDetails getTypeFromString(
            final String fileContents, final String declaredByMetadataId,
            final JavaType typeName) {
        if (StringUtils.isBlank(fileContents)) {
            return null;
        }

        Validate.notBlank(declaredByMetadataId,
                "Declaring metadata ID required");
        Validate.notNull(typeName, "Java type to locate required");
        try {
            final CompilationUnit compilationUnit = JavaParser
                    .parse(new ByteArrayInputStream(fileContents.getBytes()));
            final TypeDeclaration typeDeclaration = JavaParserUtils
                    .locateTypeDeclaration(compilationUnit, typeName);
            if (typeDeclaration == null) {
                return null;
            }
            return JavaParserClassOrInterfaceTypeDetailsBuilder.getInstance(
                    compilationUnit, null, typeDeclaration,
                    declaredByMetadataId, typeName, metadataService,
                    typeLocationService).build();
        }
        catch (final ParseException e) {
            throw new IllegalStateException("Failed to parse " + typeName
                    + " : " + e.getMessage());
        }
    }

    /**
     * Appends the presented class to the end of the presented body
     * declarations. The body declarations appear within the presented
     * compilation unit. This is used to progressively build inner types.
     * 
     * @param compilationUnit the work-in-progress compilation unit (required)
     * @param enclosingCompilationUnitServices
     * @param cid the new class to add (required)
     * @param parent the class body declarations a subclass should be added to
     *            (may be null, which denotes a top-level type within the
     *            compilation unit)
     */
    private void updateOutput(final CompilationUnit compilationUnit,
            CompilationUnitServices enclosingCompilationUnitServices,
            final ClassOrInterfaceTypeDetails cid,
            final List<BodyDeclaration> parent) {
        // Append the new imports this class declares
        Validate.notNull(compilationUnit.getImports(),
                "Compilation unit imports should be non-null when producing type '"
                        + cid.getName() + "'");
        for (final ImportMetadata importType : cid.getRegisteredImports()) {
            if (!importType.isAsterisk()) {
                NameExpr typeToImportExpr;
                if (importType.getImportType().getEnclosingType() == null) {
                    typeToImportExpr = new QualifiedNameExpr(new NameExpr(
                            importType.getImportType().getPackage()
                                    .getFullyQualifiedPackageName()),
                            importType.getImportType().getSimpleTypeName());
                }
                else {
                    typeToImportExpr = new QualifiedNameExpr(new NameExpr(
                            importType.getImportType().getEnclosingType()
                                    .getFullyQualifiedTypeName()), importType
                            .getImportType().getSimpleTypeName());
                }
                compilationUnit.getImports().add(
                        new ImportDeclaration(typeToImportExpr, importType
                                .isStatic(), false));
            }
            else {
                compilationUnit.getImports().add(
                        new ImportDeclaration(new NameExpr(importType
                                .getImportPackage()
                                .getFullyQualifiedPackageName()), importType
                                .isStatic(), importType.isAsterisk()));
            }
        }

        // Create a class or interface declaration to represent this actual type
        final int javaParserModifier = JavaParserUtils
                .getJavaParserModifier(cid.getModifier());
        TypeDeclaration typeDeclaration;
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration;

        // Implements handling
        final List<ClassOrInterfaceType> implementsList = new ArrayList<ClassOrInterfaceType>();
        for (final JavaType current : cid.getImplementsTypes()) {
            implementsList.add(JavaParserUtils.getResolvedName(cid.getName(),
                    current, compilationUnit));
        }

        if (cid.getPhysicalTypeCategory() == PhysicalTypeCategory.INTERFACE
                || cid.getPhysicalTypeCategory() == PhysicalTypeCategory.CLASS) {
            final boolean isInterface = cid.getPhysicalTypeCategory() == PhysicalTypeCategory.INTERFACE;

            if (parent == null) {
                // Top level type
                typeDeclaration = new ClassOrInterfaceDeclaration(
                        javaParserModifier, isInterface, cid
                                .getName()
                                .getNameIncludingTypeParameters()
                                .replace(
                                        cid.getName().getPackage()
                                                .getFullyQualifiedPackageName()
                                                + ".", ""));
                classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) typeDeclaration;
            }
            else {
                // Inner type
                typeDeclaration = new ClassOrInterfaceDeclaration(
                        javaParserModifier, isInterface, cid.getName()
                                .getSimpleTypeName());
                classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) typeDeclaration;

                if (cid.getName().getParameters().size() > 0) {
                    classOrInterfaceDeclaration
                            .setTypeParameters(new ArrayList<TypeParameter>());

                    for (final JavaType param : cid.getName().getParameters()) {
                        NameExpr pNameExpr = JavaParserUtils
                                .importTypeIfRequired(cid.getName(),
                                        compilationUnit.getImports(), param);
                        final String tempName = StringUtils.replace(
                                pNameExpr.toString(), param.getArgName()
                                        + " extends ", "", 1);
                        pNameExpr = new NameExpr(tempName);
                        final ClassOrInterfaceType pResolvedName = JavaParserUtils
                                .getClassOrInterfaceType(pNameExpr);
                        classOrInterfaceDeclaration.getTypeParameters().add(
                                new TypeParameter(param.getArgName()
                                        .getSymbolName(), Collections
                                        .singletonList(pResolvedName)));
                    }
                }
            }

            // Superclass handling
            final List<ClassOrInterfaceType> extendsList = new ArrayList<ClassOrInterfaceType>();
            for (final JavaType current : cid.getExtendsTypes()) {
                if (!OBJECT.equals(current)) {
                    extendsList.add(JavaParserUtils.getResolvedName(
                            cid.getName(), current, compilationUnit));
                }
            }
            if (extendsList.size() > 0) {
                classOrInterfaceDeclaration.setExtends(extendsList);
            }

            // Implements handling
            if (implementsList.size() > 0) {
                classOrInterfaceDeclaration.setImplements(implementsList);
            }
        }
        else {
            typeDeclaration = new EnumDeclaration(javaParserModifier, cid
                    .getName().getSimpleTypeName());
        }
        typeDeclaration.setMembers(new ArrayList<BodyDeclaration>());

        Validate.notNull(typeDeclaration.getName(),
                "Missing type declaration name for '" + cid.getName() + "'");

        // If adding a new top-level type, must add it to the compilation unit
        // types
        Validate.notNull(compilationUnit.getTypes(),
                "Compilation unit types must not be null when attempting to add '"
                        + cid.getName() + "'");

        if (parent == null) {
            // Top-level class
            compilationUnit.getTypes().add(typeDeclaration);
        }
        else {
            // Inner class
            parent.add(typeDeclaration);
        }

        // If the enclosing CompilationUnitServices was not provided a default
        // CompilationUnitServices needs to be created
        if (enclosingCompilationUnitServices == null) {
            // Create a compilation unit so that we can use JavaType*Metadata
            // static methods directly
            enclosingCompilationUnitServices = new CompilationUnitServices() {
                public JavaPackage getCompilationUnitPackage() {
                    return cid.getName().getPackage();
                }

                public JavaType getEnclosingTypeName() {
                    return cid.getName();
                }

                public List<ImportDeclaration> getImports() {
                    return compilationUnit.getImports();
                }

                public List<TypeDeclaration> getInnerTypes() {
                    return compilationUnit.getTypes();
                }

                public PhysicalTypeCategory getPhysicalTypeCategory() {
                    return cid.getPhysicalTypeCategory();
                }
            };
        }

        final CompilationUnitServices finalCompilationUnitServices = enclosingCompilationUnitServices;
        // A hybrid CompilationUnitServices must be provided that references the
        // enclosing types imports and package
        final CompilationUnitServices compilationUnitServices = new CompilationUnitServices() {
            public JavaPackage getCompilationUnitPackage() {
                return finalCompilationUnitServices.getCompilationUnitPackage();
            }

            public JavaType getEnclosingTypeName() {
                return cid.getName();
            }

            public List<ImportDeclaration> getImports() {
                return finalCompilationUnitServices.getImports();
            }

            public List<TypeDeclaration> getInnerTypes() {
                return compilationUnit.getTypes();
            }

            public PhysicalTypeCategory getPhysicalTypeCategory() {
                return cid.getPhysicalTypeCategory();
            }
        };

        // Add type annotations
        final List<AnnotationExpr> annotations = new ArrayList<AnnotationExpr>();
        typeDeclaration.setAnnotations(annotations);
        for (final AnnotationMetadata candidate : cid.getAnnotations()) {
            JavaParserAnnotationMetadataBuilder.addAnnotationToList(
                    compilationUnitServices, annotations, candidate);
        }

        // Add enum constants and interfaces
        if (typeDeclaration instanceof EnumDeclaration
                && cid.getEnumConstants().size() > 0) {
            final EnumDeclaration enumDeclaration = (EnumDeclaration) typeDeclaration;

            final List<EnumConstantDeclaration> constants = new ArrayList<EnumConstantDeclaration>();
            enumDeclaration.setEntries(constants);

            for (final JavaSymbolName constant : cid.getEnumConstants()) {
                addEnumConstant(constants, constant);
            }

            // Implements handling
            if (implementsList.size() > 0) {
                enumDeclaration.setImplements(implementsList);
            }
        }

        // Add fields
        for (final FieldMetadata candidate : cid.getDeclaredFields()) {
            JavaParserFieldMetadataBuilder.addField(compilationUnitServices,
                    typeDeclaration.getMembers(), candidate);
        }

        // Add constructors
        for (final ConstructorMetadata candidate : cid
                .getDeclaredConstructors()) {
            JavaParserConstructorMetadataBuilder.addConstructor(
                    compilationUnitServices, typeDeclaration.getMembers(),
                    candidate, null);
        }

        // Add methods
        for (final MethodMetadata candidate : cid.getDeclaredMethods()) {
            JavaParserMethodMetadataBuilder.addMethod(compilationUnitServices,
                    typeDeclaration.getMembers(), candidate, null);
        }

        // Add inner types
        for (final ClassOrInterfaceTypeDetails candidate : cid
                .getDeclaredInnerTypes()) {
            updateOutput(compilationUnit, compilationUnitServices, candidate,
                    typeDeclaration.getMembers());
        }

        final HashSet<String> imported = new HashSet<String>();
        final ArrayList<ImportDeclaration> imports = new ArrayList<ImportDeclaration>();
        for (final ImportDeclaration importDeclaration : compilationUnit
                .getImports()) {
            JavaPackage importPackage = null;
            JavaType importType = null;
            if (importDeclaration.isAsterisk()) {
                importPackage = new JavaPackage(importDeclaration.getName()
                        .toString());
            }
            else {
                importType = new JavaType(importDeclaration.getName()
                        .toString());
                importPackage = importType.getPackage();
            }

            if (importPackage.equals(cid.getName().getPackage())
                    && importDeclaration.isAsterisk()) {
                continue;
            }

            if (importPackage.equals(cid.getName().getPackage())
                    && importType != null
                    && importType.getEnclosingType() == null) {
                continue;
            }

            if (importType != null && importType.equals(cid.getName())) {
                continue;
            }

            if (!imported.contains(importDeclaration.getName().toString())) {
                imports.add(importDeclaration);
                imported.add(importDeclaration.getName().toString());
            }
        }

        Collections.sort(imports, new Comparator<ImportDeclaration>() {
            public int compare(final ImportDeclaration importDeclaration,
                    final ImportDeclaration importDeclaration1) {
                return importDeclaration.getName().toString()
                        .compareTo(importDeclaration1.getName().toString());
            }
        });

        compilationUnit.setImports(imports);
    }
}
