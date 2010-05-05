package org.springframework.roo.classpath.javaparser;

import japa.parser.ASTHelper;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.PackageDeclaration;
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
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.Type;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.javaparser.details.JavaParserAnnotationMetadata;
import org.springframework.roo.classpath.javaparser.details.JavaParserConstructorMetadata;
import org.springframework.roo.classpath.javaparser.details.JavaParserFieldMetadata;
import org.springframework.roo.classpath.javaparser.details.JavaParserMethodMetadata;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
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
 * 
 */
public class JavaParserMutableClassOrInterfaceTypeDetails implements MutableClassOrInterfaceTypeDetails, CompilationUnitServices {
	// passed into constructor
	private FileManager fileManager;
	
	// computed from constructor
	private String fileIdentifier;
	
	private String declaredByMetadataId;
	
	// to satisfy interface
	private JavaType name;
	private PhysicalTypeCategory physicalTypeCategory;
	private List<ConstructorMetadata> declaredConstructors = new ArrayList<ConstructorMetadata>();
	private List<FieldMetadata> declaredFields = new ArrayList<FieldMetadata>();
	private List<MethodMetadata> declaredMethods = new ArrayList<MethodMetadata>();
	private ClassOrInterfaceTypeDetails superclass = null;
	private List<JavaType> extendsTypes = new ArrayList<JavaType>();
	private List<JavaType> implementsTypes = new ArrayList<JavaType>();
	private List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
	private List<JavaSymbolName> enumConstants = new ArrayList<JavaSymbolName>();
	
	// internal use
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
		};

		if (typeDeclaration instanceof ClassOrInterfaceDeclaration) {
			this.clazz = (ClassOrInterfaceDeclaration) typeDeclaration;

			// Determine the type name, adding type parameters if possible
			JavaType newName = JavaParserUtils.getJavaType(compilationUnitServices, this.clazz);
			
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
				if (superPtm != null && superPtm.getPhysicalTypeDetails() != null && superPtm.getPhysicalTypeDetails() instanceof ClassOrInterfaceTypeDetails) {
					this.superclass = (ClassOrInterfaceTypeDetails) superPtm.getPhysicalTypeDetails();
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
				typeAnnotations.add(md);
			}
		}

        List<BodyDeclaration> members = this.clazz == null ? this.enumClazz.getMembers() : this.clazz.getMembers();
		
        if (members != null) {
    		// Now we've finished declaring the type, we should introspect for any inner types that can thus be referred to in other body members
    		// We defer this until now because it's illegal to refer to an inner type in the signature of the enclosing type
    		for (BodyDeclaration bodyDeclaration : members) {
    			if (bodyDeclaration instanceof TypeDeclaration) {
    				// found a type
    				innerTypes.add((TypeDeclaration) bodyDeclaration);
    			}
    		}

    		for (BodyDeclaration member : members) {
                  if (member instanceof FieldDeclaration) {
                                FieldDeclaration castMember = (FieldDeclaration) member;
                                for (VariableDeclarator var : castMember.getVariables()) {
                                        declaredFields.add(new JavaParserFieldMetadata(declaredByMetadataId, castMember, var, this, typeParameterNames));
                                }
                  }
                  if (member instanceof MethodDeclaration) {
                	  MethodDeclaration castMember = (MethodDeclaration) member;
                      declaredMethods.add(new JavaParserMethodMetadata(declaredByMetadataId, castMember, this, typeParameterNames));
                  }
                  if (member instanceof ConstructorDeclaration) {
                      ConstructorDeclaration castMember = (ConstructorDeclaration) member;
                      declaredConstructors.add(new JavaParserConstructorMetadata(declaredByMetadataId, castMember, this, typeParameterNames));
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
	
	public List<JavaType> getExtendsTypes() {
		return Collections.unmodifiableList(extendsTypes);
	}
	
	public List<JavaType> getImplementsTypes() {
		return Collections.unmodifiableList(implementsTypes);
	}
	
	public List<? extends AnnotationMetadata> getTypeAnnotations() {
		return Collections.unmodifiableList(typeAnnotations);
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
	
	public static final String getOutput(final ClassOrInterfaceTypeDetails cit) {
		// Create a compilation unit to store the type to be created
		final CompilationUnit compilationUnit = new CompilationUnit();
		
		compilationUnit.setImports(new ArrayList<ImportDeclaration>());
		
		if (!cit.getName().isDefaultPackage()) {
            compilationUnit.setPackage(new PackageDeclaration(ASTHelper.createNameExpr(cit.getName().getPackage().getFullyQualifiedPackageName())));
        }
		
		// Create a class or interface declaration to represent this actual type
		int javaParserModifier = JavaParserUtils.getJavaParserModifier(cit.getModifier());
		TypeDeclaration typeDeclaration;
		
        // Implements handling
    	List<ClassOrInterfaceType> implementsList = new ArrayList<ClassOrInterfaceType>();
		for (JavaType current : cit.getImplementsTypes()) {
			NameExpr nameExpr = JavaParserUtils.importTypeIfRequired(cit.getName(), compilationUnit.getImports(), current);
        	ClassOrInterfaceType resolvedName = JavaParserUtils.getClassOrInterfaceType(nameExpr);
        	if (current.getParameters() != null && current.getParameters().size() > 0) {
        		resolvedName.setTypeArgs(new ArrayList<Type>());
        		for (JavaType param : current.getParameters()) {
					NameExpr pNameExpr = JavaParserUtils.importTypeIfRequired(cit.getName(), compilationUnit.getImports(), param);
		        	ClassOrInterfaceType pResolvedName = JavaParserUtils.getClassOrInterfaceType(pNameExpr);
        			resolvedName.getTypeArgs().add(pResolvedName);
        		}
        	}
        	implementsList.add(resolvedName);
		}

		if (cit.getPhysicalTypeCategory() == PhysicalTypeCategory.INTERFACE || cit.getPhysicalTypeCategory() == PhysicalTypeCategory.CLASS) {
			boolean isInterface = cit.getPhysicalTypeCategory() == PhysicalTypeCategory.INTERFACE ? true : false;
			typeDeclaration = new ClassOrInterfaceDeclaration(javaParserModifier, isInterface, cit.getName().getSimpleTypeName());
			ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) typeDeclaration;
			
	        // Superclass handling
	    	List<ClassOrInterfaceType> extendsList = new ArrayList<ClassOrInterfaceType>();
			for (JavaType current : cit.getExtendsTypes()) {
				if (!"java.lang.Object".equals(current.getFullyQualifiedTypeName())) {
					NameExpr nameExpr = JavaParserUtils.importTypeIfRequired(cit.getName(), compilationUnit.getImports(), current);
		        	ClassOrInterfaceType resolvedName = JavaParserUtils.getClassOrInterfaceType(nameExpr);
		        	if (current.getParameters() != null && current.getParameters().size() > 0) {
		        		resolvedName.setTypeArgs(new ArrayList<Type>());
		        		for (JavaType param : current.getParameters()) {
							NameExpr pNameExpr = JavaParserUtils.importTypeIfRequired(cit.getName(), compilationUnit.getImports(), param);
				        	ClassOrInterfaceType pResolvedName = JavaParserUtils.getClassOrInterfaceType(pNameExpr);
		        			resolvedName.getTypeArgs().add(pResolvedName);
		        		}
		        	}
		        	extendsList.add(resolvedName);
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
		};
        
        // Add type annotations
        List<AnnotationExpr> annotations = new ArrayList<AnnotationExpr>();
        typeDeclaration.setAnnotations(annotations);
        for (AnnotationMetadata candidate : cit.getTypeAnnotations()) {
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
        
		return compilationUnit.toString();
	}
	
	public static final void createType(FileManager fileManager, final ClassOrInterfaceTypeDetails cit, String fileIdentifier) {
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(cit, "Class or interface type details required");
		Assert.hasText(fileIdentifier, "File identifier required");
		
		final String newContents = getOutput(cit);

		MutableFile mutableFile = null;
		if (fileManager.exists(fileIdentifier)) {
			// First verify if the file has even changed
			File f = new File(fileIdentifier);
			String existing = null;
			try {
				existing = FileCopyUtils.copyToString(new FileReader(f));
			} catch (IOException ignoreAndJustOverwriteIt) {}
			
			if (!newContents.equals(existing)) {
				mutableFile = fileManager.updateFile(fileIdentifier);
			}
			
		} else {
			mutableFile = fileManager.createFile(fileIdentifier);
			Assert.notNull(mutableFile, "Could not create Java output file '" + fileIdentifier + "'");
		}
		
		try {
			if (mutableFile != null) {
				// If mutableFile was null, that means the source == destination content
				FileCopyUtils.copy(newContents.getBytes(), mutableFile.getOutputStream());
			}
		} catch (IOException ioe) {
			throw new IllegalStateException("Could not output '" + mutableFile.getCanonicalPath() + "'", ioe);
		}
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
		tsc.append("typeAnnotations", typeAnnotations);
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