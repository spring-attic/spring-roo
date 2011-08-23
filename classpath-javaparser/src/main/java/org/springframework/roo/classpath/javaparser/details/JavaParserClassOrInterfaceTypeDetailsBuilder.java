package org.springframework.roo.classpath.javaparser.details;

import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.body.*;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.QualifiedNameExpr;
import japa.parser.ast.type.ClassOrInterfaceType;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.*;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.javaparser.CompilationUnitServices;
import org.springframework.roo.classpath.javaparser.JavaParserUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.Builder;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JavaParserClassOrInterfaceTypeDetailsBuilder implements Builder<ClassOrInterfaceTypeDetails>{

	// Constants
	static final String UNSUPPORTED_MESSAGE_PREFIX = "Only enum, class and interface files are supported";

	private MetadataService metadataService;
	private TypeLocationService typeLocationService;

	private String declaredByMetadataId;
	private CompilationUnit compilationUnit;
	private CompilationUnitServices compilationUnitServices;
	private TypeDeclaration typeDeclaration;
	private JavaType name;

	private List<ImportDeclaration> imports = new ArrayList<ImportDeclaration>();
	private JavaPackage compilationUnitPackage;
	private PhysicalTypeCategory physicalTypeCategory;
	private List<TypeDeclaration> innerTypes = new ArrayList<TypeDeclaration>();

	public static JavaParserClassOrInterfaceTypeDetailsBuilder getInstance(CompilationUnit compilationUnit, CompilationUnitServices enclosingCompilationUnitServices, TypeDeclaration typeDeclaration, String declaredByMetadataId, JavaType typeName, MetadataService metadataService, TypeLocationService typeLocationService) {
		return new JavaParserClassOrInterfaceTypeDetailsBuilder(compilationUnit, enclosingCompilationUnitServices, typeDeclaration, declaredByMetadataId, typeName, metadataService, typeLocationService);
	}

	private JavaParserClassOrInterfaceTypeDetailsBuilder(CompilationUnit compilationUnit, CompilationUnitServices enclosingCompilationUnitServices, TypeDeclaration typeDeclaration, String declaredByMetadataId, JavaType typeName, MetadataService metadataService, TypeLocationService typeLocationService) {
		Assert.notNull(compilationUnit, "Compilation unit required");
		Assert.notNull(typeDeclaration, "Unable to locate the class or interface declaration");

		Assert.notNull(declaredByMetadataId, "Declared by metadata ID required");
		Assert.notNull(typeName, "Name required");

		this.metadataService = metadataService;
		this.typeLocationService = typeLocationService;

		this.compilationUnit = compilationUnit;
		this.compilationUnitServices = enclosingCompilationUnitServices;
		this.typeDeclaration = typeDeclaration;
		this.declaredByMetadataId = declaredByMetadataId;
		this.name = typeName;

		if (enclosingCompilationUnitServices == null) {
			compilationUnitServices = getDefaultCompilationUnitServices();
		}
	}

	private CompilationUnitServices getDefaultCompilationUnitServices() {
		return new CompilationUnitServices() {
			public List<ImportDeclaration> getImports() {
				return imports;
			}

			public JavaPackage getCompilationUnitPackage() {
				return compilationUnitPackage;
			}

			public List<TypeDeclaration> getInnerTypes() {
				return innerTypes;
			}

			public JavaType getEnclosingTypeName() {
				return name;
			}

			public PhysicalTypeCategory getPhysicalTypeCategory() {
				return physicalTypeCategory;
			}
		};
	}

	public ClassOrInterfaceTypeDetails build() {
		ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId);

		ClassOrInterfaceDeclaration clazz = null;
		EnumDeclaration enumClazz = null;

		imports = compilationUnit.getImports();
		if (imports == null) {
			imports = new ArrayList<ImportDeclaration>();
			compilationUnit.setImports(imports);
		}

		compilationUnitPackage = name.getPackage();

		Assert.notEmpty(compilationUnit.getTypes(), "No types in compilation unit, so unable to continue parsing");

		physicalTypeCategory = PhysicalTypeCategory.CLASS;
		if (typeDeclaration instanceof ClassOrInterfaceDeclaration) {
			clazz = (ClassOrInterfaceDeclaration) typeDeclaration;
			if (clazz.isInterface()) {
				physicalTypeCategory = PhysicalTypeCategory.INTERFACE;
			}

		} else if (typeDeclaration instanceof EnumDeclaration) {
			enumClazz = (EnumDeclaration) typeDeclaration;
			physicalTypeCategory = PhysicalTypeCategory.ENUMERATION;
		}

		classOrInterfaceTypeDetailsBuilder.setPhysicalTypeCategory(physicalTypeCategory);

		Assert.notNull(physicalTypeCategory, UNSUPPORTED_MESSAGE_PREFIX + " (" + typeDeclaration.getClass().getSimpleName() + " for " + name + ")");

		final PhysicalTypeCategory finalPhysicalTypeCategory = physicalTypeCategory;

		final CompilationUnitServices finalCompilationUnitServices = compilationUnitServices;
		// A hybrid CompilationUnitServices must be provided that references the enclosing types imports and package
		CompilationUnitServices compilationUnitServices = new CompilationUnitServices() {
			public List<ImportDeclaration> getImports() {
				return finalCompilationUnitServices.getImports();
			}

			public JavaPackage getCompilationUnitPackage() {
				return finalCompilationUnitServices.getCompilationUnitPackage();
			}

			public List<TypeDeclaration> getInnerTypes() {
				return innerTypes;
			}

			public JavaType getEnclosingTypeName() {
				return finalCompilationUnitServices.getEnclosingTypeName();
			}

			public PhysicalTypeCategory getPhysicalTypeCategory() {
				return finalPhysicalTypeCategory;
			}
		};

		for (ImportDeclaration importDeclaration : imports) {
			if (importDeclaration.getName() instanceof QualifiedNameExpr) {
				String qualifier = ((QualifiedNameExpr) importDeclaration.getName()).getQualifier().toString();
				String simpleName = importDeclaration.getName().getName();
				String fullName = qualifier + "." + simpleName;
				// We want to calculate these...

				JavaType type = new JavaType(fullName);
				JavaPackage typePackage = type.getPackage();
				ImportMetadataBuilder newImport = new ImportMetadataBuilder(declaredByMetadataId, 0, typePackage, type, importDeclaration.isStatic(), importDeclaration.isAsterisk());
				classOrInterfaceTypeDetailsBuilder.add(newImport.build());
			}
		}

		if (typeDeclaration instanceof ClassOrInterfaceDeclaration) {
			clazz = (ClassOrInterfaceDeclaration) typeDeclaration;

			// Determine the type name, adding type parameters if possible
			JavaType newName = JavaParserUtils.getJavaType(compilationUnitServices, clazz);

			// Revert back to the original type name (thus avoiding unnecessary inferences about java.lang types; see ROO-244)
			name = new JavaType(name.getFullyQualifiedTypeName(), newName.getArray(), newName.getDataType(), newName.getArgName(), newName.getParameters());
		}
		classOrInterfaceTypeDetailsBuilder.setName(name);

		// Verify the package declaration appears to be correct
		Assert.isTrue(compilationUnitPackage.equals(name.getPackage()), "Compilation unit package '" + compilationUnitPackage + "' unexpected for type '" + name.getPackage() + "'");

		// Convert Java Parser modifier into JDK modifier
		classOrInterfaceTypeDetailsBuilder.setModifier(JavaParserUtils.getJdkModifier(typeDeclaration.getModifiers()));

		// Type parameters
		Set<JavaSymbolName> typeParameterNames = new HashSet<JavaSymbolName>();
		for (JavaType param : name.getParameters()) {
			JavaSymbolName arg = param.getArgName();
			// Fortunately type names can only appear at the top-level
			if (arg != null && !JavaType.WILDCARD_NEITHER.equals(arg) && !JavaType.WILDCARD_EXTENDS.equals(arg) && !JavaType.WILDCARD_SUPER.equals(arg)) {
				typeParameterNames.add(arg);
			}
		}

		if (clazz != null) {
			List<ClassOrInterfaceType> extendsList = clazz.getExtends();
			if (extendsList != null) {
				for (ClassOrInterfaceType candidate : extendsList) {
					JavaType javaType = JavaParserUtils.getJavaTypeNow(compilationUnitServices, candidate, typeParameterNames);
					classOrInterfaceTypeDetailsBuilder.addExtendsTypes(javaType);
				}
			}

			List<JavaType> extendsTypes = classOrInterfaceTypeDetailsBuilder.getExtendsTypes();
			// Obtain the superclass, if this is a class and one is available
			if (physicalTypeCategory == PhysicalTypeCategory.CLASS && extendsTypes.size() == 1) {
				JavaType superclass = extendsTypes.get(0);
				String superclassId = typeLocationService.findIdentifier(superclass);
				PhysicalTypeMetadata superPtm = null;
				if (superclassId != null) {
					superPtm = (PhysicalTypeMetadata) metadataService.get(superclassId);
				}
				if (superPtm != null && superPtm.getMemberHoldingTypeDetails() != null && superPtm.getMemberHoldingTypeDetails() instanceof ClassOrInterfaceTypeDetails) {
					classOrInterfaceTypeDetailsBuilder.setSuperclass((ClassOrInterfaceTypeDetails) superPtm.getMemberHoldingTypeDetails());
				}
			}
		}

		if (enumClazz != null) {
			List<EnumConstantDeclaration> constants = enumClazz.getEntries();
			if (constants != null) {
				for (EnumConstantDeclaration enumConstants : constants) {
					classOrInterfaceTypeDetailsBuilder.addEnumConstant(new JavaSymbolName(enumConstants.getName()));
				}
			}
		}

		List<ClassOrInterfaceType> implementsList = clazz == null ? enumClazz.getImplements() : clazz.getImplements();
		if (implementsList != null) {
			for (ClassOrInterfaceType candidate : implementsList) {
				JavaType javaType = JavaParserUtils.getJavaTypeNow(compilationUnitServices, candidate, typeParameterNames);
				classOrInterfaceTypeDetailsBuilder.addImplementsType(javaType);
			}
		}

		List<AnnotationExpr> annotationsList = clazz == null ? enumClazz.getAnnotations() : typeDeclaration.getAnnotations();
		if (annotationsList != null) {
			for (AnnotationExpr candidate : annotationsList) {
				AnnotationMetadata md = JavaParserAnnotationMetadataBuilder.getInstance(candidate, compilationUnitServices).build();
				classOrInterfaceTypeDetailsBuilder.addAnnotation(md);
			}
		}

		List<BodyDeclaration> members = clazz == null ? enumClazz.getMembers() : clazz.getMembers();

		if (members != null) {
			// Now we've finished declaring the type, we should introspect for any inner types that can thus be referred to in other body members
			// We defer this until now because it's illegal to refer to an inner type in the signature of the enclosing type
			for (BodyDeclaration bodyDeclaration : members) {
				if (bodyDeclaration instanceof TypeDeclaration) {
					// Found a type
					innerTypes.add((TypeDeclaration) bodyDeclaration);
				}
			}

			for (BodyDeclaration member : members) {
				if (member instanceof FieldDeclaration) {
					FieldDeclaration castMember = (FieldDeclaration) member;
					for (VariableDeclarator var : castMember.getVariables()) {
						FieldMetadata fieldMetadata = JavaParserFieldMetadataBuilder.getInstance(declaredByMetadataId, castMember, var, compilationUnitServices, typeParameterNames).build();
						classOrInterfaceTypeDetailsBuilder.addField(fieldMetadata);
					}
				}
				if (member instanceof MethodDeclaration) {
					MethodDeclaration castMember = (MethodDeclaration) member;
					MethodMetadata method = JavaParserMethodMetadataBuilder.getInstance(declaredByMetadataId, castMember, compilationUnitServices, typeParameterNames).build();
					classOrInterfaceTypeDetailsBuilder.addMethod(method);
				}
				if (member instanceof ConstructorDeclaration) {
					ConstructorDeclaration castMember = (ConstructorDeclaration) member;
					ConstructorMetadata constructorMetadata = JavaParserConstructorMetadataBuilder.getInstance(declaredByMetadataId, castMember, compilationUnitServices, typeParameterNames).build();
					classOrInterfaceTypeDetailsBuilder.addConstructor(constructorMetadata);
				}
				if (member instanceof TypeDeclaration) {
					TypeDeclaration castMember = (TypeDeclaration) member;
					JavaType innerType = new JavaType(castMember.getName());
					String innerTypeMetadataId = PhysicalTypeIdentifier.createIdentifier(innerType, PhysicalTypeIdentifier.getPath(declaredByMetadataId));
					ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = JavaParserClassOrInterfaceTypeDetailsBuilder.getInstance(compilationUnit, compilationUnitServices, castMember, innerTypeMetadataId, innerType, metadataService, typeLocationService).build();
					classOrInterfaceTypeDetailsBuilder.addInnerType(classOrInterfaceTypeDetails);
				}
			}
		}

		return classOrInterfaceTypeDetailsBuilder.build();
	}
}
