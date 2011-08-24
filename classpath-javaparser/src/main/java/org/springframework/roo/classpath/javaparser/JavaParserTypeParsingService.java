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
import japa.parser.ast.body.EnumConstantDeclaration;
import japa.parser.ast.body.EnumDeclaration;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.QualifiedNameExpr;
import japa.parser.ast.type.ClassOrInterfaceType;
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
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;


@Component(immediate = true) 
@Service
public class JavaParserTypeParsingService implements TypeParsingService {

	@Reference private MetadataService metadataService;
	@Reference private TypeLocationService typeLocationService;

	public final String getCompilationUnitContents(final ClassOrInterfaceTypeDetails cit) {
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

		updateOutput(compilationUnit, null, cit, null);

		return compilationUnit.toString();
	}

	public ClassOrInterfaceTypeDetails getTypeAtLocation(String fileIdentifier, String declaredByMetadataId, JavaType typeName) {
		Assert.hasText(fileIdentifier, "Compilation unit path required");
		Assert.hasText(declaredByMetadataId, "Declaring metadata ID required");
		Assert.notNull(typeName, "Java type to locate required");
		try {
			File file = new File(fileIdentifier);
			String typeContents = FileCopyUtils.copyToString(file);
			return getTypeFromString(typeContents, declaredByMetadataId, typeName);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public ClassOrInterfaceTypeDetails getTypeFromString(String fileContents, String declaredByMetadataId, JavaType typeName) {
		Assert.hasText(fileContents, "Compilation unit contents required");
		Assert.hasText(declaredByMetadataId, "Declaring metadata ID required");
		Assert.notNull(typeName, "Java type to locate required");
		try {
			CompilationUnit compilationUnit = JavaParser.parse(new ByteArrayInputStream(fileContents.getBytes()));
			TypeDeclaration typeDeclaration = JavaParserUtils.locateTypeDeclaration(compilationUnit, typeName);
			return JavaParserClassOrInterfaceTypeDetailsBuilder.getInstance(compilationUnit, null, typeDeclaration, declaredByMetadataId, typeName, metadataService, typeLocationService).build();
		} catch (ParseException e) {
			throw new IllegalStateException(e);
		}
	}


	/**
	 * Appends the presented class to the end of the presented body declarations. The body declarations appear within the presented compilation unit. This is used to progressively build inner types.
	 *
	 * @param compilationUnit the work-in-progress compilation unit (required)
	 * @param enclosingCompilationUnitServices
	 * @param cit             the new class to add (required)
	 * @param parent          the class body declarations a subclass should be added to (may be null, which denotes a top-level type within the compilation unit)
	 */
	private void updateOutput(final CompilationUnit compilationUnit, CompilationUnitServices enclosingCompilationUnitServices, final ClassOrInterfaceTypeDetails cit, List<BodyDeclaration> parent) {
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
			implementsList.add(JavaParserUtils.getResolvedName(cit.getName(), current, compilationUnit));
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
					extendsList.add(JavaParserUtils.getResolvedName(cit.getName(), current, compilationUnit));
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
			JavaParserAnnotationMetadataBuilder.addAnnotationToList(compilationUnitServices, annotations, candidate);
		}

		// Add enum constants and interfaces
		if (typeDeclaration instanceof EnumDeclaration && cit.getEnumConstants().size() > 0) {
			EnumDeclaration enumDeclaration = (EnumDeclaration) typeDeclaration;

			List<EnumConstantDeclaration> constants = new ArrayList<EnumConstantDeclaration>();
			enumDeclaration.setEntries(constants);

			for (JavaSymbolName constant : cit.getEnumConstants()) {
				addEnumConstant(constants, constant);
			}

			// Implements handling
			if (implementsList.size() > 0) {
				enumDeclaration.setImplements(implementsList);
			}
		}

		// Add fields
		for (FieldMetadata candidate : cit.getDeclaredFields()) {
			JavaParserFieldMetadataBuilder.addField(compilationUnitServices, typeDeclaration.getMembers(), candidate);
		}

		// Add constructors
		for (ConstructorMetadata candidate : cit.getDeclaredConstructors()) {
			JavaParserConstructorMetadataBuilder.addConstructor(compilationUnitServices, typeDeclaration.getMembers(), candidate, null);
		}

		// Add methods
		for (MethodMetadata candidate : cit.getDeclaredMethods()) {
			JavaParserMethodMetadataBuilder.addMethod(compilationUnitServices, typeDeclaration.getMembers(), candidate, null);
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

	private void addEnumConstant(List<EnumConstantDeclaration> constants, JavaSymbolName name) {
		// Determine location to insert
		for (EnumConstantDeclaration constant : constants) {
			if (constant.getName().equals(name.getSymbolName())) {
				throw new IllegalArgumentException("Enum constant '" + name.getSymbolName() + "' already exists");
			}
		}
		EnumConstantDeclaration newEntry = new EnumConstantDeclaration(name.getSymbolName());
		constants.add(constants.size(), newEntry);
	}
}
