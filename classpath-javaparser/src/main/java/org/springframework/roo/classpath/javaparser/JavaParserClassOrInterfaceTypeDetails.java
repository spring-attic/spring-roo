package org.springframework.roo.classpath.javaparser;

import japa.parser.ASTHelper;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.PackageDeclaration;
import japa.parser.ast.TypeParameter;
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
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.QualifiedNameExpr;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.Type;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ImportMetadata;
import org.springframework.roo.classpath.details.ImportMetadataBuilder;
import org.springframework.roo.classpath.details.InitializerMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.javaparser.details.JavaParserAnnotationMetadata;
import org.springframework.roo.classpath.javaparser.details.JavaParserConstructorMetadata;
import org.springframework.roo.classpath.javaparser.details.JavaParserFieldMetadata;
import org.springframework.roo.classpath.javaparser.details.JavaParserMethodMetadata;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.AbstractCustomDataAccessorProvider;
import org.springframework.roo.model.CustomDataImpl;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Java Parser implementation of {@link ClassOrInterfaceTypeDetails}.
 * <p/>
 * <p/>
 * This class is immutable once constructed.
 *
 * @author Ben Alex
 * @author James Tyrrell
 * @since 1.0.1
 */
public class JavaParserClassOrInterfaceTypeDetails extends AbstractCustomDataAccessorProvider implements ClassOrInterfaceTypeDetails, CompilationUnitServices {
	private String declaredByMetadataId;

	// To satisfy interface
	private JavaType name;
	private PhysicalTypeCategory physicalTypeCategory;
	private List<ConstructorMetadata> declaredConstructors = new ArrayList<ConstructorMetadata>();
	private List<FieldMetadata> declaredFields = new ArrayList<FieldMetadata>();
	private List<MethodMetadata> declaredMethods = new ArrayList<MethodMetadata>();
	private List<ClassOrInterfaceTypeDetails> declaredInnerTypes = new ArrayList<ClassOrInterfaceTypeDetails>();
	private List<InitializerMetadata> declaredInitializers = new ArrayList<InitializerMetadata>();
	private ClassOrInterfaceTypeDetails superclass = null;
	private List<JavaType> extendsTypes = new ArrayList<JavaType>();
	private List<JavaType> implementsTypes = new ArrayList<JavaType>();
	private Set<ImportMetadata> registeredImports = new HashSet<ImportMetadata>();
	private List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
	private List<JavaSymbolName> enumConstants = new ArrayList<JavaSymbolName>();

	// Internal use
	protected CompilationUnit compilationUnit;
	protected ClassOrInterfaceDeclaration clazz;
	protected EnumDeclaration enumClazz;
	private List<ImportDeclaration> imports;
	private JavaPackage compilationUnitPackage;
	protected Set<JavaSymbolName> typeParameterNames;
	private List<TypeDeclaration> innerTypes = new ArrayList<TypeDeclaration>();

	static final String UNSUPPORTED_MESSAGE_PREFIX = "Only enum, class and interface files are supported";

	private int modifier = 0;

	public JavaParserClassOrInterfaceTypeDetails(CompilationUnit compilationUnit, TypeDeclaration typeDeclaration, String declaredByMetadataId, JavaType typeName, MetadataService metadataService, PhysicalTypeMetadataProvider physicalTypeMetadataProvider) throws ParseException, CloneNotSupportedException, IOException {
		this(compilationUnit, null, typeDeclaration, declaredByMetadataId, typeName, metadataService, physicalTypeMetadataProvider);
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

	public JavaParserClassOrInterfaceTypeDetails(CompilationUnit compilationUnit, CompilationUnitServices enclosingCompilationUnitServices, TypeDeclaration typeDeclaration, String declaredByMetadataId, JavaType typeName, MetadataService metadataService, PhysicalTypeMetadataProvider physicalTypeMetadataProvider) throws ParseException, CloneNotSupportedException, IOException {
		super(CustomDataImpl.NONE);
		Assert.notNull(compilationUnit, "Compilation unit required");
		Assert.notNull(typeDeclaration, "Unable to locate the class or interface declaration");

		Assert.notNull(declaredByMetadataId, "Declared by metadata ID required");
		Assert.notNull(typeName, "Name required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(physicalTypeMetadataProvider, "Physical type metadata provider required");

		this.name = typeName;
		this.declaredByMetadataId = declaredByMetadataId;
		this.compilationUnit = compilationUnit;

		imports = compilationUnit.getImports();
		if (imports == null) {
			imports = new ArrayList<ImportDeclaration>();
			compilationUnit.setImports(imports);
		}

		compilationUnitPackage = typeName.getPackage();

		Assert.notEmpty(compilationUnit.getTypes(), "No types in compilation unit, so unable to continue parsing");

		if (typeDeclaration instanceof ClassOrInterfaceDeclaration) {
			this.clazz = (ClassOrInterfaceDeclaration) typeDeclaration;

			if (this.clazz.isInterface()) {
				physicalTypeCategory = PhysicalTypeCategory.INTERFACE;
			} else {
				physicalTypeCategory = PhysicalTypeCategory.CLASS;
			}

		} else if (typeDeclaration instanceof EnumDeclaration) {
			this.enumClazz = (EnumDeclaration) typeDeclaration;
			this.physicalTypeCategory = PhysicalTypeCategory.ENUMERATION;
		}

		Assert.notNull(physicalTypeCategory, UNSUPPORTED_MESSAGE_PREFIX + " (" + typeDeclaration.getClass().getSimpleName() + " for " + name + ")");

		if (enclosingCompilationUnitServices == null) {
			enclosingCompilationUnitServices = getDefaultCompilationUnitServices();
		}

		final CompilationUnitServices finalCompilationUnitServices = enclosingCompilationUnitServices;
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
				return physicalTypeCategory;
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
				ImportMetadataBuilder newImport = new ImportMetadataBuilder(declaredByMetadataId, modifier, typePackage, type, importDeclaration.isStatic(), importDeclaration.isAsterisk());
				registeredImports.add(newImport.build());
			}
		}

		if (typeDeclaration instanceof ClassOrInterfaceDeclaration) {
			this.clazz = (ClassOrInterfaceDeclaration) typeDeclaration;

			// Determine the type name, adding type parameters if possible
			JavaType newName = JavaParserUtils.getJavaType(compilationUnitServices, this.clazz);

			// Revert back to the original type name (thus avoiding unnecessary inferences about java.lang types; see ROO-244)
			this.name = new JavaType(this.name.getFullyQualifiedTypeName(), newName.getArray(), newName.getDataType(), newName.getArgName(), newName.getParameters());

		}

		// Verify the package declaration appears to be correct
		Assert.isTrue(compilationUnitPackage.equals(name.getPackage()), "Compilation unit package '" + compilationUnitPackage + "' unexpected for type '" + name.getPackage() + "'");

		// Convert Java Parser modifier into JDK modifier
		this.modifier = JavaParserUtils.getJdkModifier(typeDeclaration.getModifiers());

		// Type parameters
		typeParameterNames = new HashSet<JavaSymbolName>();
		for (JavaType param : this.name.getParameters()) {
			JavaSymbolName arg = param.getArgName();
			// Fortunately type names can only appear at the top-level
			if (arg != null && !JavaType.WILDCARD_NEITHER.equals(arg) && !JavaType.WILDCARD_EXTENDS.equals(arg) && !JavaType.WILDCARD_SUPER.equals(arg)) {
				typeParameterNames.add(arg);
			}
		}

		if (this.clazz != null) {
			List<ClassOrInterfaceType> extendsList = this.clazz.getExtends();
			if (extendsList != null) {
				for (ClassOrInterfaceType candidate : extendsList) {
					JavaType javaType = JavaParserUtils.getJavaTypeNow(compilationUnitServices, candidate, typeParameterNames);
					extendsTypes.add(javaType);
				}
			}

			// Obtain the superclass, if this is a class and one is available
			if (physicalTypeCategory == PhysicalTypeCategory.CLASS && extendsTypes.size() == 1) {
				JavaType superclass = extendsTypes.get(0);
				String superclassId = physicalTypeMetadataProvider.findIdentifier(superclass);
				PhysicalTypeMetadata superPtm = null;
				if (superclassId != null) {
					superPtm = (PhysicalTypeMetadata) metadataService.get(superclassId);
				}
				if (superPtm != null && superPtm.getMemberHoldingTypeDetails() != null && superPtm.getMemberHoldingTypeDetails() instanceof ClassOrInterfaceTypeDetails) {
					this.superclass = (ClassOrInterfaceTypeDetails) superPtm.getMemberHoldingTypeDetails();
				}
			}
		}

		if (this.enumClazz != null) {
			List<EnumConstantDeclaration> constants = this.enumClazz.getEntries();
			if (constants != null) {
				for (EnumConstantDeclaration enumConstants : constants) {
					this.enumConstants.add(new JavaSymbolName(enumConstants.getName()));
				}
			}
		}

		List<ClassOrInterfaceType> implementsList = this.clazz == null ? this.enumClazz.getImplements() : this.clazz.getImplements();
		if (implementsList != null) {
			for (ClassOrInterfaceType candidate : implementsList) {
				JavaType javaType = JavaParserUtils.getJavaTypeNow(compilationUnitServices, candidate, typeParameterNames);
				implementsTypes.add(javaType);
			}
		}

		List<AnnotationExpr> annotationsList = this.clazz == null ? this.enumClazz.getAnnotations() : typeDeclaration.getAnnotations();
		if (annotationsList != null) {
			for (AnnotationExpr candidate : annotationsList) {
				JavaParserAnnotationMetadata md = new JavaParserAnnotationMetadata(candidate, compilationUnitServices);
				annotations.add(md);
			}
		}

		List<BodyDeclaration> members = this.clazz == null ? this.enumClazz.getMembers() : this.clazz.getMembers();

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
						FieldMetadata fieldMetadata = new JavaParserFieldMetadata(declaredByMetadataId, castMember, var, compilationUnitServices, typeParameterNames);
						declaredFields.add(fieldMetadata);
					}
				}
				if (member instanceof MethodDeclaration) {
					MethodDeclaration castMember = (MethodDeclaration) member;
					MethodMetadata method = new JavaParserMethodMetadata(declaredByMetadataId, castMember, compilationUnitServices, typeParameterNames);
					declaredMethods.add(method);
				}
				if (member instanceof ConstructorDeclaration) {
					ConstructorDeclaration castMember = (ConstructorDeclaration) member;
					ConstructorMetadata constructorMetadata = new JavaParserConstructorMetadata(declaredByMetadataId, castMember, compilationUnitServices, typeParameterNames);
					declaredConstructors.add(constructorMetadata);
				}
				if (member instanceof TypeDeclaration) {
					TypeDeclaration castMember = (TypeDeclaration) member;
					JavaType innerType = new JavaType(castMember.getName());
					String innerTypeMetadataId = PhysicalTypeIdentifier.createIdentifier(innerType, PhysicalTypeIdentifier.getPath(declaredByMetadataId));
					ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = new JavaParserClassOrInterfaceTypeDetails(compilationUnit, compilationUnitServices, castMember, innerTypeMetadataId, innerType, metadataService, physicalTypeMetadataProvider);
					declaredInnerTypes.add(classOrInterfaceTypeDetails);
				}
			}
		}
	}

	JavaParserClassOrInterfaceTypeDetails(String typeContents, String declaredByMetadataId, JavaType typeName, MetadataService metadataService, PhysicalTypeMetadataProvider physicalTypeMetadataProvider) throws ParseException, CloneNotSupportedException, IOException {
		this(JavaParser.parse(new ByteArrayInputStream(typeContents.getBytes())), JavaParserUtils.locateTypeDeclaration(JavaParser.parse(new ByteArrayInputStream(typeContents.getBytes())), typeName), declaredByMetadataId, typeName, metadataService, physicalTypeMetadataProvider);
	}

	public String getDeclaredByMetadataId() {
		return declaredByMetadataId;
	}

	public int getModifier() {
		return modifier;
	}

	public ClassOrInterfaceTypeDetails getSuperclass() {
		return superclass;
	}

	public PhysicalTypeCategory getPhysicalTypeCategory() {
		return physicalTypeCategory;
	}

	public JavaType getName() {
		return name;
	}

	public List<? extends ConstructorMetadata> getDeclaredConstructors() {
		return Collections.unmodifiableList(declaredConstructors);
	}

	public List<JavaSymbolName> getEnumConstants() {
		return Collections.unmodifiableList(enumConstants);
	}

	public List<? extends FieldMetadata> getDeclaredFields() {
		return Collections.unmodifiableList(declaredFields);
	}

	public List<? extends MethodMetadata> getDeclaredMethods() {
		return Collections.unmodifiableList(declaredMethods);
	}

	public List<ClassOrInterfaceTypeDetails> getDeclaredInnerTypes() {
		return Collections.unmodifiableList(declaredInnerTypes);
	}

	public List<InitializerMetadata> getDeclaredInitializers() {
		return Collections.unmodifiableList(declaredInitializers);
	}

	public List<JavaType> getExtendsTypes() {
		return Collections.unmodifiableList(extendsTypes);
	}

	public List<JavaType> getImplementsTypes() {
		return Collections.unmodifiableList(implementsTypes);
	}

	public Set<ImportMetadata> getRegisteredImports() {
		return Collections.unmodifiableSet(registeredImports);
	}

	public List<AnnotationMetadata> getAnnotations() {
		return Collections.unmodifiableList(annotations);
	}

	/**
	 * Appends the presented class to the end of the presented body declarations. The body declarations appear within the presented compilation unit. This is used to progressively build inner types.
	 *
	 * @param compilationUnit the work-in-progress compilation unit (required)
	 * @param cit             the new class to add (required)
	 * @param parent          the class body declarations a subclass should be added to (may be null, which denotes a top-level type within the compilation unit)
	 */
	private static final void updateOutput(final CompilationUnit compilationUnit, CompilationUnitServices enclosingCompilationUnitServices, final ClassOrInterfaceTypeDetails cit, List<BodyDeclaration> parent) {
		// Append the new imports this class declares
		Assert.notNull(compilationUnit.getImports(), "Compilation unit imports should be non-null when producing type '" + cit.getName() + "'");
		for (ImportMetadata importType : cit.getRegisteredImports()) {
			if (!importType.isAsterisk()) {
				NameExpr typeToImportExpr;
				if (importType.getImportType().getEnclosingType() == null) {
					typeToImportExpr = new QualifiedNameExpr(new NameExpr(importType.getImportType().getPackage().getFullyQualifiedPackageName()), importType.getImportType().getSimpleTypeName());
				} else {
					typeToImportExpr = new QualifiedNameExpr(new NameExpr(importType.getImportType().getEnclosingType().getFullyQualifiedTypeName()), importType.getImportType().getSimpleTypeName());
				}
				compilationUnit.getImports().add(new ImportDeclaration(typeToImportExpr, false, false));
			} else {
				compilationUnit.getImports().add(new ImportDeclaration(new NameExpr(importType.getImportPackage().getFullyQualifiedPackageName()), importType.isStatic(), importType.isAsterisk()));
			}
		}

		// Create a class or interface declaration to represent this actual type
		int javaParserModifier = JavaParserUtils.getJavaParserModifier(cit.getModifier());
		TypeDeclaration typeDeclaration;
		ClassOrInterfaceDeclaration classOrInterfaceDeclaration;

		// Implements handling
		List<ClassOrInterfaceType> implementsList = new ArrayList<ClassOrInterfaceType>();
		for (JavaType current : cit.getImplementsTypes()) {
			implementsList.add(getResolvedName(cit.getName(), current, compilationUnit));
		}

		if (cit.getPhysicalTypeCategory() == PhysicalTypeCategory.INTERFACE || cit.getPhysicalTypeCategory() == PhysicalTypeCategory.CLASS) {
			boolean isInterface = cit.getPhysicalTypeCategory() == PhysicalTypeCategory.INTERFACE;

			if (parent == null) {
				// Top level type
				typeDeclaration = new ClassOrInterfaceDeclaration(javaParserModifier, isInterface, cit.getName().getNameIncludingTypeParameters().replaceAll(cit.getName().getPackage().getFullyQualifiedPackageName() + ".", ""));
				classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) typeDeclaration;
			} else {
				// Inner type
				typeDeclaration = new ClassOrInterfaceDeclaration(javaParserModifier, isInterface, cit.getName().getSimpleTypeName());
				classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) typeDeclaration;

				if (cit.getName().getParameters().size() > 0) {
					classOrInterfaceDeclaration.setTypeParameters(new ArrayList<TypeParameter>());

					for (JavaType param : cit.getName().getParameters()) {
						NameExpr pNameExpr = JavaParserUtils.importTypeIfRequired(cit.getName(), compilationUnit.getImports(), param);
						String tempName = pNameExpr.toString().replaceFirst(param.getArgName() + " extends ", "");
						pNameExpr = new NameExpr(tempName);
						ClassOrInterfaceType pResolvedName = JavaParserUtils.getClassOrInterfaceType(pNameExpr);
						classOrInterfaceDeclaration.getTypeParameters().add(new TypeParameter(param.getArgName().getSymbolName(), Collections.singletonList(pResolvedName)));
					}
				}
			}

			// Superclass handling
			List<ClassOrInterfaceType> extendsList = new ArrayList<ClassOrInterfaceType>();
			for (JavaType current : cit.getExtendsTypes()) {
				if (!"java.lang.Object".equals(current.getFullyQualifiedTypeName())) {
					extendsList.add(getResolvedName(cit.getName(), current, compilationUnit));
				}
			}
			if (extendsList.size() > 0) {
				classOrInterfaceDeclaration.setExtends(extendsList);
			}

			// Implements handling
			if (implementsList.size() > 0) {
				classOrInterfaceDeclaration.setImplements(implementsList);
			}
		} else {
			typeDeclaration = new EnumDeclaration(javaParserModifier, cit.getName().getSimpleTypeName());
		}
		typeDeclaration.setMembers(new ArrayList<BodyDeclaration>());

		Assert.notNull(typeDeclaration.getName(), "Missing type declaration name for '" + cit.getName() + "'");

		// If adding a new top-level type, must add it to the compilation unit types
		Assert.notNull(compilationUnit.getTypes(), "Compilation unit types must not be null when attempting to add '" + cit.getName() + "'");

		if (parent == null) {
			// Top-level class
			compilationUnit.getTypes().add(typeDeclaration);
		} else {
			// Inner class
			parent.add(typeDeclaration);
		}

		// If the enclosing CompilationUnitServices was not provided a default CompilationUnitServices needs to be created
		if (enclosingCompilationUnitServices == null) {
			// Create a compilation unit so that we can use JavaType*Metadata static methods directly
			enclosingCompilationUnitServices = new CompilationUnitServices() {
				public List<ImportDeclaration> getImports() {
					return compilationUnit.getImports();
				}

				public JavaPackage getCompilationUnitPackage() {
					return cit.getName().getPackage();
				}

				public List<TypeDeclaration> getInnerTypes() {
					return compilationUnit.getTypes();
				}

				public JavaType getEnclosingTypeName() {
					return cit.getName();
				}

				public PhysicalTypeCategory getPhysicalTypeCategory() {
					return cit.getPhysicalTypeCategory();
				}
			};
		}

		final CompilationUnitServices finalCompilationUnitServices = enclosingCompilationUnitServices;
		// A hybrid CompilationUnitServices must be provided that references the enclosing types imports and package
		CompilationUnitServices compilationUnitServices = new CompilationUnitServices() {
			public List<ImportDeclaration> getImports() {
				return finalCompilationUnitServices.getImports();
			}

			public JavaPackage getCompilationUnitPackage() {
				return finalCompilationUnitServices.getCompilationUnitPackage();
			}

			public List<TypeDeclaration> getInnerTypes() {
				return compilationUnit.getTypes();
			}

			public JavaType getEnclosingTypeName() {
				return cit.getName();
			}

			public PhysicalTypeCategory getPhysicalTypeCategory() {
				return cit.getPhysicalTypeCategory();
			}
		};

		// Add type annotations
		List<AnnotationExpr> annotations = new ArrayList<AnnotationExpr>();
		typeDeclaration.setAnnotations(annotations);
		for (AnnotationMetadata candidate : cit.getAnnotations()) {
			JavaParserAnnotationMetadata.addAnnotationToList(compilationUnitServices, annotations, candidate);
		}

		// Add enum constants and interfaces
		if (typeDeclaration instanceof EnumDeclaration && cit.getEnumConstants().size() > 0) {
			EnumDeclaration enumDeclaration = (EnumDeclaration) typeDeclaration;

			List<EnumConstantDeclaration> constants = new ArrayList<EnumConstantDeclaration>();
			enumDeclaration.setEntries(constants);

			for (JavaSymbolName constant : cit.getEnumConstants()) {
				addEnumConstant(compilationUnitServices, constants, constant);
			}

			// Implements handling
			if (implementsList.size() > 0) {
				enumDeclaration.setImplements(implementsList);
			}
		}

		// Add fields
		for (FieldMetadata candidate : cit.getDeclaredFields()) {
			JavaParserFieldMetadata.addField(compilationUnitServices, typeDeclaration.getMembers(), candidate);
		}

		// Add constructors
		for (ConstructorMetadata candidate : cit.getDeclaredConstructors()) {
			JavaParserConstructorMetadata.addConstructor(compilationUnitServices, typeDeclaration.getMembers(), candidate, null);
		}

		// Add methods
		for (MethodMetadata candidate : cit.getDeclaredMethods()) {
			JavaParserMethodMetadata.addMethod(compilationUnitServices, typeDeclaration.getMembers(), candidate, null);
		}

		// Add inner types
		for (ClassOrInterfaceTypeDetails candidate : cit.getDeclaredInnerTypes()) {
			updateOutput(compilationUnit, compilationUnitServices, candidate, typeDeclaration.getMembers());
		}

		HashSet<String> imported = new HashSet<String>();
		ArrayList<ImportDeclaration> imports = new ArrayList<ImportDeclaration>();
		for (ImportDeclaration importDeclaration : compilationUnit.getImports()) {
			JavaPackage importPackage = null;
			JavaType importType = null;
			if (importDeclaration.isAsterisk()) {
				importPackage = new JavaPackage(importDeclaration.getName().toString());
			} else {
				importType = new JavaType(importDeclaration.getName().toString());
				importPackage = importType.getPackage();
			}

			if (importPackage.equals(cit.getName().getPackage()) && importDeclaration.isAsterisk()) {
				continue;
			}

			if (importPackage.equals(cit.getName().getPackage()) && importType != null && importType.getEnclosingType() == null) {
				continue;
			}

			if (importType != null && importType.equals(cit.getName())) {
				continue;
			}

			if (!imported.contains(importDeclaration.getName().toString())) {
				imports.add(importDeclaration);
				imported.add(importDeclaration.getName().toString());
			}
		}

		Collections.sort(imports, new Comparator<ImportDeclaration>() {
			public int compare(ImportDeclaration importDeclaration, ImportDeclaration importDeclaration1) {
				return importDeclaration.getName().toString().compareTo(importDeclaration1.getName().toString());
			}
		});

		compilationUnit.setImports(imports);
	}

	static final String getCompilationUnitContents(final ClassOrInterfaceTypeDetails cit) {
		Assert.notNull(cit, "Class or interface type details are required");
		// Create a compilation unit to store the type to be created
		final CompilationUnit compilationUnit = new CompilationUnit();

		// NB: this import list is replaced at the end of this method by a sorted version
		compilationUnit.setImports(new ArrayList<ImportDeclaration>());

		if (!cit.getName().isDefaultPackage()) {
			compilationUnit.setPackage(new PackageDeclaration(ASTHelper.createNameExpr(cit.getName().getPackage().getFullyQualifiedPackageName())));
		}

		// Add the class of interface declaration to the compilation unit
		final List<TypeDeclaration> types = new ArrayList<TypeDeclaration>();
		compilationUnit.setTypes(types);

		// We pass in null as the 3rd argument as this denotes we're working with a top-level type
		updateOutput(compilationUnit, null, cit, null);

		return compilationUnit.toString();
	}

	protected static void addEnumConstant(CompilationUnitServices compilationUnitServices, List<EnumConstantDeclaration> constants, JavaSymbolName name) {
		// Determine location to insert
		for (EnumConstantDeclaration constant : constants) {
			if (constant.getName().equals(name.getSymbolName())) {
				throw new IllegalArgumentException("Enum constant '" + name.getSymbolName() + "' already exists");
			}
		}
		EnumConstantDeclaration newEntry = new EnumConstantDeclaration(name.getSymbolName());
		constants.add(constants.size(), newEntry);
	}

	public static ClassOrInterfaceType getResolvedName(JavaType target, JavaType current, CompilationUnit compilationUnit) {
		NameExpr nameExpr = JavaParserUtils.importTypeIfRequired(target, compilationUnit.getImports(), current);
		ClassOrInterfaceType resolvedName = JavaParserUtils.getClassOrInterfaceType(nameExpr);
		if (current.getParameters() != null && current.getParameters().size() > 0) {
			resolvedName.setTypeArgs(new ArrayList<Type>());
			for (JavaType param : current.getParameters()) {
				resolvedName.getTypeArgs().add(getResolvedName(target, param, compilationUnit));
			}
		}

		return resolvedName;
	}

	public static ClassOrInterfaceType getResolvedName(JavaType target, JavaType current, CompilationUnitServices compilationUnit) {
		NameExpr nameExpr = JavaParserUtils.importTypeIfRequired(target, compilationUnit.getImports(), current);
		ClassOrInterfaceType resolvedName = JavaParserUtils.getClassOrInterfaceType(nameExpr);
		if (current.getParameters() != null && current.getParameters().size() > 0) {
			resolvedName.setTypeArgs(new ArrayList<Type>());
			for (JavaType param : current.getParameters()) {
				resolvedName.getTypeArgs().add(getResolvedName(target, param, compilationUnit));
			}
		}

		return resolvedName;
	}

	static final void createOrUpdateTypeOnDisk(FileManager fileManager, final ClassOrInterfaceTypeDetails cit, String fileIdentifier) {
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(cit, "Class or interface type details required");
		Assert.hasText(fileIdentifier, "File identifier required");

		final String newContents = getCompilationUnitContents(cit);

		fileManager.createOrUpdateTextFileIfRequired(fileIdentifier, newContents, true);
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("name", name);
		tsc.append("modifier", Modifier.toString(modifier));
		tsc.append("physicalTypeCategory", physicalTypeCategory);
		tsc.append("declaredByMetadataId", declaredByMetadataId);
		tsc.append("declaredConstructors", declaredConstructors);
		tsc.append("declaredFields", declaredFields);
		tsc.append("declaredMethods", declaredMethods);
		tsc.append("enumConstants", enumConstants);
		tsc.append("superclass", superclass);
		tsc.append("extendsTypes", extendsTypes);
		tsc.append("implementsTypes", implementsTypes);
		tsc.append("typeAnnotations", annotations);
		tsc.append("declaredInnerTypes", declaredInnerTypes);
		tsc.append("customData", getCustomData());
		return tsc.toString();
	}

	public JavaPackage getCompilationUnitPackage() {
		return compilationUnitPackage;
	}

	public List<ImportDeclaration> getImports() {
		return imports;
	}

	public List<TypeDeclaration> getInnerTypes() {
		return innerTypes;
	}

	public JavaType getEnclosingTypeName() {
		return name;
	}
}