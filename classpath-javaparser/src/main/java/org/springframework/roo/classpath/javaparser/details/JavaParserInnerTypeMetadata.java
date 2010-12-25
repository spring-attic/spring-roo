package org.springframework.roo.classpath.javaparser.details;

import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.TypeParameter;
import japa.parser.ast.body.*;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.QualifiedNameExpr;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.Type;

import java.lang.reflect.Modifier;
import java.util.*;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.details.*;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.javaparser.CompilationUnitServices;
import org.springframework.roo.classpath.javaparser.JavaParserMutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.javaparser.JavaParserUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.*;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Java Parser implementation of {@link MethodMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class JavaParserInnerTypeMetadata extends AbstractCustomDataAccessorProvider implements MutableClassOrInterfaceTypeDetails {
	private List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
	private List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
	private List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
	private List<JavaType> throwsTypes = new ArrayList<JavaType>();
	private String declaredByMetadataId;
	private int modifier;

    static final String UNSUPPORTED_MESSAGE_PREFIX = "Only enum, class and interface files are supported";

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
	private List<JavaSymbolName> enumConstants = new ArrayList<JavaSymbolName>();
    private Set<ClassOrInterfaceTypeDetails> innerTypes = new HashSet<ClassOrInterfaceTypeDetails>();
    private List<IdentifiableAnnotatedJavaStructure> members = new ArrayList<IdentifiableAnnotatedJavaStructure>();

    // Internal use
    private CompilationUnitServices compilationUnitServices;
    private ClassOrInterfaceDeclaration clazz;
    private EnumDeclaration enumClazz;
    private JavaPackage compilationUnitPackage;
    private Set<JavaSymbolName> typeParameterNames;
    private List<TypeDeclaration> innerTypeDeclarations;


    public JavaParserInnerTypeMetadata(String declaredByMetadataId, TypeDeclaration typeDeclaration, final JavaType typeName, final CompilationUnitServices compilationUnitServices, PhysicalTypeMetadataProvider physicalTypeMetadataProvider, MetadataService metadataService) {
        super(CustomDataImpl.NONE);

        CompilationUnitServices innerTypeCompilationUnitServices = new CompilationUnitServices() {
			public List<ImportDeclaration> getImports() {
				return compilationUnitServices.getImports();
			}

			public JavaPackage getCompilationUnitPackage() {
				return compilationUnitServices.getCompilationUnitPackage();
			}

			public List<TypeDeclaration> getInnerTypes() {
				return compilationUnitServices.getInnerTypes();
			}

			public JavaType getEnclosingTypeName() {
				return typeName;
			}

			public void flush() {
				// We will do this at the very end
			}

            public PhysicalTypeCategory getPhysicalTypeCategory() {

                return compilationUnitServices.getPhysicalTypeCategory();
            }
		};

        this.name = typeName;
        this.compilationUnitServices = innerTypeCompilationUnitServices;
        this.declaredByMetadataId = declaredByMetadataId;

        compilationUnitPackage = typeName.getPackage();

        if (typeDeclaration instanceof ClassOrInterfaceDeclaration) {
			this.clazz = (ClassOrInterfaceDeclaration) typeDeclaration;

			// Determine the type name, adding type parameters if possible
			JavaType newName = JavaParserUtils.getJavaType(innerTypeCompilationUnitServices, this.clazz);

			// Revert back to the original type name (thus avoiding unnecessary inferences about java.lang types; see ROO-244)
			this.name = new JavaType(this.name.getFullyQualifiedTypeName(), newName.getArray(), newName.getDataType(), newName.getArgName(), newName.getParameters());

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
					JavaType javaType = JavaParserUtils.getJavaTypeNow(innerTypeCompilationUnitServices, candidate, typeParameterNames);
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
				JavaType javaType = JavaParserUtils.getJavaTypeNow(innerTypeCompilationUnitServices, candidate, typeParameterNames);
				implementsTypes.add(javaType);
			}
		}

		List<AnnotationExpr> annotationsList = this.clazz == null ? this.enumClazz.getAnnotations() : typeDeclaration.getAnnotations();
		if (annotationsList != null) {
			for (AnnotationExpr candidate : annotationsList) {
				JavaParserAnnotationMetadata md = new JavaParserAnnotationMetadata(candidate, innerTypeCompilationUnitServices);
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
					innerTypeDeclarations.add((TypeDeclaration) bodyDeclaration);
				}
			}

			for (BodyDeclaration member : members) {
				if (member instanceof FieldDeclaration) {
					FieldDeclaration castMember = (FieldDeclaration) member;
					for (VariableDeclarator var : castMember.getVariables()) {
						declaredFields.add(new JavaParserFieldMetadata(declaredByMetadataId, castMember, var, innerTypeCompilationUnitServices, typeParameterNames));
					}
				}
				if (member instanceof MethodDeclaration) {
					MethodDeclaration castMember = (MethodDeclaration) member;
					declaredMethods.add(new JavaParserMethodMetadata(declaredByMetadataId, castMember, innerTypeCompilationUnitServices, typeParameterNames));
				}
				if (member instanceof ConstructorDeclaration) {
					ConstructorDeclaration castMember = (ConstructorDeclaration) member;
					declaredConstructors.add(new JavaParserConstructorMetadata(declaredByMetadataId, castMember, innerTypeCompilationUnitServices, typeParameterNames));
				}

                if (member instanceof TypeDeclaration) {
                    TypeDeclaration castMember = (TypeDeclaration) member;
                    declaredInnerTypes.add(new JavaParserInnerTypeMetadata(declaredByMetadataId, castMember, typeName, innerTypeCompilationUnitServices, physicalTypeMetadataProvider, metadataService));
                }
			}
		}
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
		JavaParserAnnotationMetadata.addAnnotationToList(compilationUnitServices, annotations, annotation, true);
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
		JavaParserAnnotationMetadata.removeAnnotationFromList(compilationUnitServices, annotations, annotationType, true);
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
		JavaParserFieldMetadata.addField(compilationUnitServices, members, fieldMetadata, true);
	}

	public void addEnumConstant(JavaSymbolName name) {
		Assert.notNull(name, "Name required");
		Assert.isTrue(this.enumClazz != null, "Enum constants can only be added to an enum class");
		List<EnumConstantDeclaration> constants = this.enumClazz.getEntries();
		if (constants == null) {
			constants = new ArrayList<EnumConstantDeclaration>();
			this.enumClazz.setEntries(constants);
		}
		addEnumConstant(compilationUnitServices, constants, name, true);
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
		JavaParserFieldMetadata.removeField(compilationUnitServices, members, fieldName);
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
		JavaParserMethodMetadata.addMethod(compilationUnitServices, members, methodMetadata, true, typeParameterNames);
	}

    public Set<ImportMetadata> getRegisteredImports() {
		return Collections.unmodifiableSet(new HashSet<ImportMetadata>());
	}

    public Set<ClassOrInterfaceTypeDetails> getInnerTypes() {
        return Collections.unmodifiableSet(innerTypes);
    }

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("declaredByMetadataId", declaredByMetadataId);
		tsc.append("modifier", Modifier.toString(modifier));
		tsc.append("parameterTypes", parameterTypes);
		tsc.append("parameterNames", parameterNames);
		tsc.append("throwsTypes", throwsTypes);
		tsc.append("annotations", annotations);
		tsc.append("customData", getCustomData());
		return tsc.toString();
	}

	public static void addInnerType(final CompilationUnitServices compilationUnitServices, final ClassOrInterfaceTypeDetails cit, List<BodyDeclaration> members) {

        final CompilationUnit compilationUnit = new CompilationUnit();

        CompilationUnitServices innerTypeCompilationUnitServices = new CompilationUnitServices() {
			public List<ImportDeclaration> getImports() {
				return compilationUnitServices.getImports();
			}

			public JavaPackage getCompilationUnitPackage() {
				return compilationUnitServices.getCompilationUnitPackage();
			}

			public List<TypeDeclaration> getInnerTypes() {
				return compilationUnitServices.getInnerTypes();
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

		compilationUnit.setImports(new ArrayList<ImportDeclaration>());

		// Create a class or interface declaration to represent this actual type
		int javaParserModifier = JavaParserUtils.getJavaParserModifier(cit.getModifier());
		TypeDeclaration typeDeclaration;

		// Implements handling
		List<ClassOrInterfaceType> implementsList = new ArrayList<ClassOrInterfaceType>();
		for (JavaType current : cit.getImplementsTypes()) {
            implementsList.add(JavaParserMutableClassOrInterfaceTypeDetails.getResolvedName(cit.getName(), current, compilationUnit));
		}

		if (cit.getPhysicalTypeCategory() == PhysicalTypeCategory.INTERFACE || cit.getPhysicalTypeCategory() == PhysicalTypeCategory.CLASS) {
			boolean isInterface = cit.getPhysicalTypeCategory() == PhysicalTypeCategory.INTERFACE ? true : false;
			NameExpr nameExpression = JavaParserUtils.getNameExpr(cit.getName().getSimpleTypeName());
            ClassOrInterfaceType typeName = JavaParserUtils.getClassOrInterfaceType(nameExpression);;
            if (cit.getName().getParameters() != null && cit.getName().getParameters().size() > 0) {
                typeName.setTypeArgs(new ArrayList<Type>());
                for (JavaType param : cit.getName().getParameters()) {
                    NameExpr pNameExpr = JavaParserUtils.importTypeIfRequired(cit.getName(), compilationUnit.getImports(), param);
                    ClassOrInterfaceType pResolvedName = JavaParserUtils.getClassOrInterfaceType(pNameExpr);
                    if (pNameExpr instanceof QualifiedNameExpr) {
                        if (((QualifiedNameExpr) pNameExpr).getQualifier().getName().equals(cit.getName().getPackage().getFullyQualifiedPackageName())) {
                            pResolvedName =  new ClassOrInterfaceType(pNameExpr.getName());
                        }
                    }
                    typeName.getTypeArgs().add(pResolvedName);
                }
            }

            typeDeclaration = new ClassOrInterfaceDeclaration(javaParserModifier, isInterface, cit.getName().getFullyQualifiedTypeName());
			ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) typeDeclaration;
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

			// Superclass handling
			List<ClassOrInterfaceType> extendsList = new ArrayList<ClassOrInterfaceType>();
			for (JavaType current : cit.getExtendsTypes()) {
				if (!"java.lang.Object".equals(current.getFullyQualifiedTypeName())) {
                    extendsList.add(JavaParserMutableClassOrInterfaceTypeDetails.getResolvedName(cit.getName(), current, compilationUnit));
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

		// Add type annotations
		List<AnnotationExpr> annotations = new ArrayList<AnnotationExpr>();
		typeDeclaration.setAnnotations(annotations);
		for (AnnotationMetadata candidate : cit.getAnnotations()) {
			JavaParserAnnotationMetadata.addAnnotationToList(innerTypeCompilationUnitServices, annotations, candidate, false);
		}

		// Add enum constants and interfaces
		if (typeDeclaration instanceof EnumDeclaration && cit.getEnumConstants().size() > 0) {
			EnumDeclaration enumDeclaration = (EnumDeclaration) typeDeclaration;

			List<EnumConstantDeclaration> constants = new ArrayList<EnumConstantDeclaration>();
			enumDeclaration.setEntries(constants);

			for (JavaSymbolName constant : cit.getEnumConstants()) {
				addEnumConstant(innerTypeCompilationUnitServices, constants, constant, false);
			}

			// Implements handling
			if (implementsList.size() > 0) {
				enumDeclaration.setImplements(implementsList);
			}
		}

		// Add fields
		for (FieldMetadata candidate : cit.getDeclaredFields()) {
			JavaParserFieldMetadata.addField(innerTypeCompilationUnitServices, typeDeclaration.getMembers(), candidate, false);
		}

		// Add constructors
		for (ConstructorMetadata candidate : cit.getDeclaredConstructors()) {
			JavaParserConstructorMetadata.addConstructor(innerTypeCompilationUnitServices, typeDeclaration.getMembers(), candidate, false, null);
		}

		// Add methods
		for (MethodMetadata candidate : cit.getDeclaredMethods()) {
			JavaParserMethodMetadata.addMethod(innerTypeCompilationUnitServices, typeDeclaration.getMembers(), candidate, false, null);
		}

        // Add inner types
        for (ClassOrInterfaceTypeDetails innerType : cit.getDeclaredInnerTypes()) {
            JavaParserInnerTypeMetadata.addInnerType(innerTypeCompilationUnitServices, innerType, typeDeclaration.getMembers());
        }

        innerTypeCompilationUnitServices.getImports().addAll(compilationUnit.getImports());

        members.add(typeDeclaration);
    }
    
    public List<IdentifiableAnnotatedJavaStructure> getMembers() {
        return members;
    }
}
