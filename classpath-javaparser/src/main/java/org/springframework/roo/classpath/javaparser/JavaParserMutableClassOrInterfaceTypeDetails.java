package org.springframework.roo.classpath.javaparser;

import japa.parser.ASTHelper;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.*;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.EnumConstantDeclaration;
import japa.parser.ast.body.EnumDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.expr.*;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.Type;

import java.io.*;
import java.lang.reflect.Modifier;
import java.util.*;

import japa.parser.ast.visitor.VoidVisitor;
import japa.parser.ast.visitor.VoidVisitorAdapter;
import org.springframework.roo.classpath.*;
import org.springframework.roo.classpath.details.*;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.javaparser.details.*;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.*;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;

/**
 * Java Parser implementation of {@link MutableClassOrInterfaceTypeDetails}.
 * 
 * <p>
 * This class is immutable once constructed.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class JavaParserMutableClassOrInterfaceTypeDetails extends AbstractCustomDataAccessorProvider implements MutableClassOrInterfaceTypeDetails, CompilationUnitServices {
	// Passed into constructor
	private FileManager fileManager;

	// Computed from constructor
	private String fileIdentifier;

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
	private ClassOrInterfaceDeclaration clazz;
	private EnumDeclaration enumClazz;
	private CompilationUnit compilationUnit;
	private List<ImportDeclaration> imports;
	private JavaPackage compilationUnitPackage;
	private Set<JavaSymbolName> typeParameterNames;
	private List<TypeDeclaration> innerTypes = new ArrayList<TypeDeclaration>();

	static final String UNSUPPORTED_MESSAGE_PREFIX = "Only enum, class and interface files are supported";

	private int modifier = 0;



	public JavaParserMutableClassOrInterfaceTypeDetails(CompilationUnit compilationUnit, TypeDeclaration typeDeclaration, FileManager fileManager, String declaredByMetadataId, String fileIdentifier, JavaType typeName, MetadataService metadataService, PhysicalTypeMetadataProvider physicalTypeMetadataProvider) throws ParseException, CloneNotSupportedException, IOException {
		super(CustomDataImpl.NONE);
		Assert.notNull(compilationUnit, "Compilation unit required");
		Assert.notNull(typeDeclaration, "Unable to locate the class or interface declaration");
		Assert.notNull(fileManager, "File manager requried");
		Assert.notNull(declaredByMetadataId, "Declared by metadata ID required");
		Assert.notNull(fileIdentifier, "File identifier (canonical path) required");
		Assert.notNull(typeName, "Name required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(physicalTypeMetadataProvider, "Physical type metadata provider required");

		this.name = typeName;
		this.declaredByMetadataId = declaredByMetadataId;
		this.fileManager = fileManager;
		this.fileIdentifier = fileIdentifier;
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

		CompilationUnitServices compilationUnitServices = new CompilationUnitServices() {
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

			public void flush() {
				// We will do this at the very end
			}

            public PhysicalTypeCategory getPhysicalTypeCategory() {

                return physicalTypeCategory;
            }
		};

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
				JavaParserAnnotationMetadata md = new JavaParserAnnotationMetadata(candidate, this);
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
                        FieldMetadata fieldMetadata = new JavaParserFieldMetadata(declaredByMetadataId, castMember, var, this, typeParameterNames);
						declaredFields.add(fieldMetadata);
					}
				}
				if (member instanceof MethodDeclaration) {
					MethodDeclaration castMember = (MethodDeclaration) member;
                    MethodMetadata method = new JavaParserMethodMetadata(declaredByMetadataId, castMember, this, typeParameterNames);
					declaredMethods.add(method);
				}
				if (member instanceof ConstructorDeclaration) {
					ConstructorDeclaration castMember = (ConstructorDeclaration) member;
                    ConstructorMetadata constructorMetadata = new JavaParserConstructorMetadata(declaredByMetadataId, castMember, this, typeParameterNames);
					declaredConstructors.add(constructorMetadata);
				}
                if (member instanceof TypeDeclaration) {
					TypeDeclaration castMember = (TypeDeclaration) member;
                    JavaType innerType = new JavaType(castMember.getName());
                    String innerTypeMetadataId = PhysicalTypeIdentifier.createIdentifier(innerType, Path.SRC_MAIN_JAVA);
                    ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = new JavaParserInnerTypeMetadata(innerTypeMetadataId, castMember, innerType, compilationUnitServices, physicalTypeMetadataProvider, metadataService);
                    declaredInnerTypes.add(classOrInterfaceTypeDetails);
				}
			}
		}
	}

    public JavaParserMutableClassOrInterfaceTypeDetails(String typeContents, String declaredByMetadataId, JavaType typeName, MetadataService metadataService, PhysicalTypeMetadataProvider physicalTypeMetadataProvider) throws ParseException, CloneNotSupportedException, IOException {
		super(CustomDataImpl.NONE);
        Assert.hasText(typeContents, "The type can't be empty");
		Assert.notNull(declaredByMetadataId, "Declared by metadata ID required");
		Assert.notNull(typeName, "Name required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(physicalTypeMetadataProvider, "Physical type metadata provider required");

        CompilationUnit compilationUnit = JavaParser.parse(new ByteArrayInputStream(typeContents.getBytes()));

        TypeDeclaration typeDeclaration = getTypeDeclaration(compilationUnit);

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

		CompilationUnitServices compilationUnitServices = new CompilationUnitServices() {
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

			public void flush() {
				// We will do this at the very end
			}

            public PhysicalTypeCategory getPhysicalTypeCategory() {

                return physicalTypeCategory;
            }
		};

        for (ImportDeclaration importDeclaration : imports) {
            if (importDeclaration.getName() instanceof QualifiedNameExpr) {
                if (importDeclaration.isAsterisk()) {
                    JavaPackage typePackage = new JavaPackage(((QualifiedNameExpr) importDeclaration.getName()).getQualifier().toString() + "." + importDeclaration.getName().getName());
                    ImportMetadataBuilder newImport = new ImportMetadataBuilder(declaredByMetadataId, modifier, typePackage, null, importDeclaration.isStatic(), importDeclaration.isAsterisk());
                    registeredImports.add(newImport.build());
                } else {
                    JavaType type = new JavaType(((QualifiedNameExpr) importDeclaration.getName()).getQualifier().toString() + "." + importDeclaration.getName().getName());
                    JavaPackage typePackage = type.getPackage();

                    ImportMetadataBuilder newImport = new ImportMetadataBuilder(declaredByMetadataId, modifier, typePackage, type, importDeclaration.isStatic(), importDeclaration.isAsterisk());
                    registeredImports.add(newImport.build());
                }
            }
        }

		if (typeDeclaration instanceof ClassOrInterfaceDeclaration) {

			// Determine the type name, adding type parameters if possible
			JavaType newName = JavaParserUtils.getJavaType(compilationUnitServices, this.clazz);

			// Revert back to the original type name (thus avoiding unnecessary inferences about java.lang types; see ROO-244)
			this.name = new JavaType(this.name.getFullyQualifiedTypeName(), newName.getArray(), newName.getDataType(), newName.getArgName(), newName.getParameters());

		}

		Assert.notNull(physicalTypeCategory, UNSUPPORTED_MESSAGE_PREFIX + " (" + typeDeclaration.getClass().getSimpleName() + " for " + name + ")");

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
				typeParameterNames.add(param.getArgName());
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
				JavaParserAnnotationMetadata md = new JavaParserAnnotationMetadata(candidate, this);
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
                        FieldMetadata fieldMetadata = new JavaParserFieldMetadata(declaredByMetadataId, castMember, var, this, typeParameterNames);
						declaredFields.add(fieldMetadata);
					}
				}
				if (member instanceof MethodDeclaration) {
					MethodDeclaration castMember = (MethodDeclaration) member;
                    MethodMetadata method = new JavaParserMethodMetadata(declaredByMetadataId, castMember, this, typeParameterNames);
					declaredMethods.add(method);
				}
				if (member instanceof ConstructorDeclaration) {
					ConstructorDeclaration castMember = (ConstructorDeclaration) member;
                    ConstructorMetadata constructorMetadata = new JavaParserConstructorMetadata(declaredByMetadataId, castMember, this, typeParameterNames);
					declaredConstructors.add(constructorMetadata);
				}
                if (member instanceof TypeDeclaration) {
					TypeDeclaration castMember = (TypeDeclaration) member;
                    JavaType innerType = new JavaType(castMember.getName());
                    String innerTypeMetadataId = PhysicalTypeIdentifier.createIdentifier(innerType, Path.SRC_MAIN_JAVA);
                    ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = new JavaParserInnerTypeMetadata(innerTypeMetadataId, castMember, innerType, compilationUnitServices, physicalTypeMetadataProvider, metadataService);
                    declaredInnerTypes.add(classOrInterfaceTypeDetails);
				}
			}
		}
	}

    private ClassOrInterfaceDeclaration getTypeDeclaration(CompilationUnit compilationUnit) {

        for (TypeDeclaration candidate : compilationUnit.getTypes()) {
            if (candidate instanceof ClassOrInterfaceDeclaration) {
                return (ClassOrInterfaceDeclaration) candidate;
            }
        }

        return null;
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

	public void addTypeAnnotation(AnnotationMetadata annotation) {
		List<AnnotationExpr> annotations = clazz == null ? enumClazz.getAnnotations() : clazz.getAnnotations();
		if (annotations == null) {
			annotations = new ArrayList<AnnotationExpr>();
			if (clazz == null) {
				enumClazz.setAnnotations(annotations);
			} else {
				clazz.setAnnotations(annotations);
			}
		}
		JavaParserAnnotationMetadata.addAnnotationToList(this, annotations, annotation, true);
	}

	public boolean updateTypeAnnotation(AnnotationMetadata annotation, Set<JavaSymbolName> attributesToDeleteIfPresent) {
		boolean writeChangesToDisk = false;

		// We are going to build a replacement AnnotationMetadata.
		// This variable tracks the new attribute values the replacement will hold.
		Map<JavaSymbolName, AnnotationAttributeValue<?>> replacementAttributeValues = new LinkedHashMap<JavaSymbolName, AnnotationAttributeValue<?>>();

		AnnotationMetadata existing = MemberFindingUtils.getTypeAnnotation(this, annotation.getAnnotationType());
		if (existing == null) {
			// Not already present, so just go and add it
			for (JavaSymbolName incomingAttributeName : annotation.getAttributeNames()) {
				// Do not copy incoming attributes which exist in the attributesToDeleteIfPresent Set
				if (attributesToDeleteIfPresent == null || !attributesToDeleteIfPresent.contains(incomingAttributeName)) {
					AnnotationAttributeValue<?> incomingValue = annotation.getAttribute(incomingAttributeName);
					replacementAttributeValues.put(incomingAttributeName, incomingValue);
				}
			}

			AnnotationMetadataBuilder replacement = new AnnotationMetadataBuilder(annotation.getAnnotationType(), new ArrayList<AnnotationAttributeValue<?>>(replacementAttributeValues.values()));
			addTypeAnnotation(replacement.build());
			return true;
		}
		
		// Copy the existing attributes into the new attributes
		for (JavaSymbolName existingAttributeName : existing.getAttributeNames()) {
			if (attributesToDeleteIfPresent != null && attributesToDeleteIfPresent.contains(existingAttributeName)) {
				writeChangesToDisk = true;
			} else {
				AnnotationAttributeValue<?> existingValue = existing.getAttribute(existingAttributeName);
				replacementAttributeValues.put(existingAttributeName, existingValue);
			}
		}

		// Now we ensure every incoming attribute replaces the existing
		for (JavaSymbolName incomingAttributeName : annotation.getAttributeNames()) {
			AnnotationAttributeValue<?> incomingValue = annotation.getAttribute(incomingAttributeName);
			
			// Add this attribute to the end of the list if the attribute is not already present
			if (replacementAttributeValues.keySet().contains(incomingAttributeName)) {
				// There was already an attribute. Need to determine if this new attribute value is materially different
				AnnotationAttributeValue<?> existingValue = replacementAttributeValues.get(incomingAttributeName);
				Assert.notNull(existingValue, "Existing value should have been provided by earlier loop");
				if (!existingValue.equals(incomingValue)) {
					replacementAttributeValues.put(incomingAttributeName, incomingValue);
					writeChangesToDisk = true;
				}
			} else if (attributesToDeleteIfPresent != null && !attributesToDeleteIfPresent.contains(incomingAttributeName)) {
				// This is a new attribute that does not already exist, so add it to the end of the replacement attributes
				replacementAttributeValues.put(incomingAttributeName, incomingValue);
				writeChangesToDisk = true;
			}
		}
		
		// Were there any material changes?
		if (!writeChangesToDisk) {
			return false;
		}
		
		// Make a new AnnotationMetadata representing the replacement
		AnnotationMetadataBuilder replacement = new AnnotationMetadataBuilder(annotation.getAnnotationType(), new ArrayList<AnnotationAttributeValue<?>>(replacementAttributeValues.values()));
		removeTypeAnnotation(replacement.getAnnotationType());
		addTypeAnnotation(replacement.build());
		
		return true;
	}

	public void removeTypeAnnotation(JavaType annotationType) {
		List<AnnotationExpr> annotations = clazz.getAnnotations();
		if (annotations == null) {
			annotations = new ArrayList<AnnotationExpr>();
			clazz.setAnnotations(annotations);
		}
		JavaParserAnnotationMetadata.removeAnnotationFromList(this, annotations, annotationType, true);
	}

	public void addField(FieldMetadata fieldMetadata) {
		List<BodyDeclaration> members = clazz == null ? enumClazz.getMembers() : clazz.getMembers();
		if (members == null) {
			members = new ArrayList<BodyDeclaration>();
			if (clazz == null) {
				enumClazz.setMembers(members);
			} else {
				clazz.setMembers(members);
			}
		}
		JavaParserFieldMetadata.addField(this, members, fieldMetadata, true);
	}

	public void addEnumConstant(JavaSymbolName name) {
		Assert.notNull(name, "Name required");
		Assert.isTrue(this.enumClazz != null, "Enum constants can only be added to an enum class");
		List<EnumConstantDeclaration> constants = this.enumClazz.getEntries();
		if (constants == null) {
			constants = new ArrayList<EnumConstantDeclaration>();
			this.enumClazz.setEntries(constants);
		}
		addEnumConstant(this, constants, name, true);
	}

	private static void addEnumConstant(CompilationUnitServices compilationUnitServices, List<EnumConstantDeclaration> constants, JavaSymbolName name, boolean permitFlush) {
		// Determine location to insert
		EnumConstantDeclaration newEntry = new EnumConstantDeclaration(name.getSymbolName());
		constants.add(constants.size(), newEntry);
		if (permitFlush) {
			compilationUnitServices.flush();
		}
	}

	public void removeField(JavaSymbolName fieldName) {
		List<BodyDeclaration> members = clazz == null ? enumClazz.getMembers() : clazz.getMembers();
		if (members == null) {
			members = new ArrayList<BodyDeclaration>();
			if (clazz == null) {
				enumClazz.setMembers(members);
			} else {
				clazz.setMembers(members);
			}
		}
		JavaParserFieldMetadata.removeField(this, members, fieldName);
	}

	public void addMethod(MethodMetadata methodMetadata) {
		List<BodyDeclaration> members = clazz == null ? enumClazz.getMembers() : clazz.getMembers();
		if (members == null) {
			members = new ArrayList<BodyDeclaration>();
			if (clazz == null) {
				enumClazz.setMembers(members);
			} else {
				clazz.setMembers(members);
			}
		}
		JavaParserMethodMetadata.addMethod(this, members, methodMetadata, true, typeParameterNames);
	}


    private static ArrayList<String> listMembers(TypeDeclaration typeDeclaration) {

        ArrayList<String> referenceTypes = new ArrayList<String>();

        VoidVisitor<ArrayList<String>> visitor = new VoidVisitorAdapter<ArrayList<String>>() {

                @Override
                public void visit(ClassOrInterfaceType n, ArrayList<String> arg) {
                    super.visit(n, arg);
                    arg.add(n.getName());
                }

                @Override
                public void visit(NameExpr n, ArrayList<String> arg) {
                    super.visit(n, arg);
                    arg.add(n.toString());
                }

            };

        typeDeclaration.accept(visitor, referenceTypes);

        return referenceTypes;
    }

	public static final String getOutput(final ClassOrInterfaceTypeDetails cit) {
		// Create a compilation unit to store the type to be created
		final CompilationUnit compilationUnit = new CompilationUnit();


        compilationUnit.setImports(new ArrayList<ImportDeclaration>());

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

		if (!cit.getName().isDefaultPackage()) {
			compilationUnit.setPackage(new PackageDeclaration(ASTHelper.createNameExpr(cit.getName().getPackage().getFullyQualifiedPackageName())));
		}

		// Create a class or interface declaration to represent this actual type
		int javaParserModifier = JavaParserUtils.getJavaParserModifier(cit.getModifier());
		TypeDeclaration typeDeclaration;

		// Implements handling
		List<ClassOrInterfaceType> implementsList = new ArrayList<ClassOrInterfaceType>();
		for (JavaType current : cit.getImplementsTypes()) {
            implementsList.add(getResolvedName(cit.getName(), current, compilationUnit));
		}

		if (cit.getPhysicalTypeCategory() == PhysicalTypeCategory.INTERFACE || cit.getPhysicalTypeCategory() == PhysicalTypeCategory.CLASS) {
			boolean isInterface = cit.getPhysicalTypeCategory() == PhysicalTypeCategory.INTERFACE ? true : false;

			typeDeclaration = new ClassOrInterfaceDeclaration(javaParserModifier, isInterface, cit.getName().getNameIncludingTypeParameters().replaceAll(cit.getName().getPackage().getFullyQualifiedPackageName() + ".", ""));
			ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) typeDeclaration;

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

		// Add the class of interface declaration to the compilation unit
		final List<TypeDeclaration> types = new ArrayList<TypeDeclaration>();
		types.add(typeDeclaration);
		compilationUnit.setTypes(types);

		// Create a compilation unit so that we can use JavaType*Metadata static methods directly
		CompilationUnitServices compilationUnitServices = new CompilationUnitServices() {
			public List<ImportDeclaration> getImports() {
				return compilationUnit.getImports();
			}

			public JavaPackage getCompilationUnitPackage() {
				return cit.getName().getPackage();
			}

			public List<TypeDeclaration> getInnerTypes() {
				return new ArrayList<TypeDeclaration>();
			}

			public JavaType getEnclosingTypeName() {
				return cit.getName();
			}

			public void flush() {
				// We will do this at the very end
			}

            public PhysicalTypeCategory getPhysicalTypeCategory() {
                return cit.getPhysicalTypeCategory();
            }
		};

		// Add type annotations
		List<AnnotationExpr> annotations = new ArrayList<AnnotationExpr>();
		typeDeclaration.setAnnotations(annotations);
		for (AnnotationMetadata candidate : cit.getAnnotations()) {
			JavaParserAnnotationMetadata.addAnnotationToList(compilationUnitServices, annotations, candidate, false);
		}

		// Add enum constants and interfaces
		if (typeDeclaration instanceof EnumDeclaration && cit.getEnumConstants().size() > 0) {
			EnumDeclaration enumDeclaration = (EnumDeclaration) typeDeclaration;

			List<EnumConstantDeclaration> constants = new ArrayList<EnumConstantDeclaration>();
			enumDeclaration.setEntries(constants);

			for (JavaSymbolName constant : cit.getEnumConstants()) {
				addEnumConstant(compilationUnitServices, constants, constant, false);
			}

			// Implements handling
			if (implementsList.size() > 0) {
				enumDeclaration.setImplements(implementsList);
			}
		}

        // Add inner types
		for (ClassOrInterfaceTypeDetails candidate : cit.getDeclaredInnerTypes()) {
			JavaParserInnerTypeMetadata.addInnerType(compilationUnitServices, candidate, typeDeclaration.getMembers());
		}

		// Add fields
		for (FieldMetadata candidate : cit.getDeclaredFields()) {
			JavaParserFieldMetadata.addField(compilationUnitServices, typeDeclaration.getMembers(), candidate, false);
		}

		// Add constructors
		for (ConstructorMetadata candidate : cit.getDeclaredConstructors()) {
			JavaParserConstructorMetadata.addConstructor(compilationUnitServices, typeDeclaration.getMembers(), candidate, false, null);
		}

		// Add methods
		for (MethodMetadata candidate : cit.getDeclaredMethods()) {
			JavaParserMethodMetadata.addMethod(compilationUnitServices, typeDeclaration.getMembers(), candidate, false, null);
		}

        ArrayList<String> referenceTypes = listMembers(typeDeclaration);

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

            if (importType != null && !referenceTypes.contains(importType.getSimpleTypeName())) {
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

		return compilationUnit.toString();
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

	public static final void createType(FileManager fileManager, final ClassOrInterfaceTypeDetails cit, String fileIdentifier) {
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(cit, "Class or interface type details required");
		Assert.hasText(fileIdentifier, "File identifier required");

		final String newContents = getOutput(cit);

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
		tsc.append("customData", getCustomData());
		return tsc.toString();
	}

	public void flush() {
		Reader compilationUnitInputStream = new StringReader(compilationUnit.toString());
		MutableFile mutableFile = fileManager.updateFile(fileIdentifier);
		try {
			FileCopyUtils.copy(compilationUnitInputStream, new OutputStreamWriter(mutableFile.getOutputStream()));
		} catch (IOException ioe) {
			throw new IllegalStateException("Could not update '" + fileIdentifier + "'", ioe);
		}
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
