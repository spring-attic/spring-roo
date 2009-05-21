package org.springframework.roo.classpath.javaparser;

import japa.parser.ASTHelper;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.PackageDeclaration;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.EnumDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.type.ClassOrInterfaceType;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
	
	// internal use
	private ClassOrInterfaceDeclaration clazz;
	private EnumDeclaration enumClazz;
	private CompilationUnit compilationUnit;
	private List<ImportDeclaration> imports;
	private JavaPackage compilationUnitPackage;
	
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
		
		if (typeDeclaration instanceof ClassOrInterfaceDeclaration) {
			this.clazz = (ClassOrInterfaceDeclaration) typeDeclaration;

			// Determine the type name, adding type parameters if possible
			this.name = JavaParserUtils.getJavaType(compilationUnitPackage, imports, this.clazz);
			
			if (this.clazz.isInterface()) {
				physicalTypeCategory = PhysicalTypeCategory.INTERFACE;
			} else {
				physicalTypeCategory = PhysicalTypeCategory.CLASS;
			}
			
		} else if (typeDeclaration instanceof EnumDeclaration) {
			this.enumClazz = (EnumDeclaration) typeDeclaration;
			this.physicalTypeCategory = PhysicalTypeCategory.ENUMERATION;
			// NB: JavaParser does not parse the actual enum constants, so this is not available
		}
		
		Assert.notNull(physicalTypeCategory, "Only enum, class and interface files are supported");
		
		
		// Verify the package declaration appears to be correct
		Assert.isTrue(compilationUnitPackage.equals(name.getPackage()), "Compilation unit package '" + compilationUnitPackage + "' unexpected for type '" + name.getPackage() + "'");
		
		// Convert Java Parser modifier into JDK modifier
		this.modifier = JavaParserUtils.getJdkModifier(typeDeclaration.getModifiers());
		
		if (this.clazz != null) {
			List<ClassOrInterfaceType> extendsList = this.clazz.getExtends();
			if (extendsList != null) {
				for (ClassOrInterfaceType candidate : extendsList) {
					JavaType javaType = JavaParserUtils.getJavaType(compilationUnitPackage, imports, candidate);
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
		
		List<ClassOrInterfaceType> implementsList = this.clazz == null ? this.enumClazz.getImplements() : this.clazz.getImplements();
		if (implementsList != null) {
			for (ClassOrInterfaceType candidate : implementsList) {
				JavaType javaType = JavaParserUtils.getJavaType(compilationUnitPackage, imports, candidate);
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
                for (BodyDeclaration member : members) {
                        if (member instanceof FieldDeclaration) {
                                FieldDeclaration castMember = (FieldDeclaration) member;
                                for (VariableDeclarator var : castMember.getVariables()) {
                                        declaredFields.add(new JavaParserFieldMetadata(declaredByMetadataId, castMember, var, this));
                                }
                        }
                        if (member instanceof MethodDeclaration) {
                                MethodDeclaration castMember = (MethodDeclaration) member;
                                declaredMethods.add(new JavaParserMethodMetadata(declaredByMetadataId, castMember, this));
                        }
                        if (member instanceof ConstructorDeclaration) {
                                ConstructorDeclaration castMember = (ConstructorDeclaration) member;
                                declaredConstructors.add(new JavaParserConstructorMetadata(declaredByMetadataId, castMember, this));
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
		JavaParserMethodMetadata.addMethod(this, members, methodMetadata, true);
	}

	public static final void createType(FileManager fileManager, final ClassOrInterfaceTypeDetails cit, String fileIdentifier) {
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(cit, "Class or interface type details required");
		Assert.hasText(fileIdentifier, "File identifier required");

		// Create a compilation unit to store the type to be created
		final CompilationUnit compilationUnit = new CompilationUnit();
		
		compilationUnit.setImports(new ArrayList<ImportDeclaration>());
		
		if (!cit.getName().isDefaultPackage()) {
            compilationUnit.setPackage(new PackageDeclaration(ASTHelper.createNameExpr(cit.getName().getPackage().getFullyQualifiedPackageName())));
        }
		
		boolean isInterface = cit.getPhysicalTypeCategory() == PhysicalTypeCategory.INTERFACE ? true : false;
		
		// Create a class or interface declaration to represent this actual type
		int javaParserModifier = JavaParserUtils.getJavaParserModifier(cit.getModifier());
		ClassOrInterfaceDeclaration cid = new ClassOrInterfaceDeclaration(javaParserModifier, isInterface, cit.getName().getSimpleTypeName());
		cid.setMembers(new ArrayList<BodyDeclaration>());
		
		// Add the class of interface declaration to the compilation unit
		List<TypeDeclaration> types = new ArrayList<TypeDeclaration>();
		types.add(cid);
		compilationUnit.setTypes(types);
		
        // Superclass handling
    	List<ClassOrInterfaceType> extendsList = new ArrayList<ClassOrInterfaceType>();
		for (JavaType current : cit.getExtendsTypes()) {
			if (!"java.lang.Object".equals(current.getFullyQualifiedTypeName())) {
				NameExpr nameExpr = JavaParserUtils.importTypeIfRequired(cit.getName().getPackage(), compilationUnit.getImports(), current);
	        	ClassOrInterfaceType resolvedName = JavaParserUtils.getClassOrInterfaceType(nameExpr);
	        	extendsList.add(resolvedName);
			}
		}
    	if (extendsList.size() > 0) {
    		cid.setExtends(extendsList);
    	}
		
        // Implements handling
    	List<ClassOrInterfaceType> implementsList = new ArrayList<ClassOrInterfaceType>();
		for (JavaType current : cit.getImplementsTypes()) {
			NameExpr nameExpr = JavaParserUtils.importTypeIfRequired(cit.getName().getPackage(), compilationUnit.getImports(), current);
        	ClassOrInterfaceType resolvedName = JavaParserUtils.getClassOrInterfaceType(nameExpr);
        	extendsList.add(resolvedName);
		}
    	if (implementsList.size() > 0) {
    		cid.setImplements(implementsList);
    	}
        
        // Create a compilation unit so that we can use JavaType*Metadata static methods directly
        CompilationUnitServices compilationUnitServices = new CompilationUnitServices() {
			public List<ImportDeclaration> getImports() {
				return compilationUnit.getImports();
			}
			public JavaPackage getCompilationUnitPackage() {
				return cit.getName().getPackage();
			}
			public void flush() {
				// We will do this at the very end
			}
		};
        
        // Add type annotations
        List<AnnotationExpr> annotations = new ArrayList<AnnotationExpr>();
        cid.setAnnotations(annotations);
        for (AnnotationMetadata candidate : cit.getTypeAnnotations()) {
        	JavaParserAnnotationMetadata.addAnnotationToList(compilationUnitServices, annotations, candidate, false);
        }
        
        // Add fields
        for (FieldMetadata candidate : cit.getDeclaredFields()) {
        	JavaParserFieldMetadata.addField(compilationUnitServices, cid.getMembers(), candidate, false);
        }

        // Add methods
        for (MethodMetadata candidate : cit.getDeclaredMethods()) {
        	JavaParserMethodMetadata.addMethod(compilationUnitServices, cid.getMembers(), candidate, false);
        }

        // Write to disk
		MutableFile mutableFile = fileManager.createFile(fileIdentifier);
		try {
			FileCopyUtils.copy(compilationUnit.toString(), new OutputStreamWriter(mutableFile.getOutputStream()));
		} catch (IOException ioe) {
			throw new IllegalStateException("Could not create '" + fileIdentifier + "'", ioe);
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
}