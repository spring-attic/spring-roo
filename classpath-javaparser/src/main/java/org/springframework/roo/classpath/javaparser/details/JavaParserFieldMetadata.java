package org.springframework.roo.classpath.javaparser.details;

import japa.parser.ASTHelper;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.ObjectCreationExpr;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.Type;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.javaparser.CompilationUnitServices;
import org.springframework.roo.classpath.javaparser.JavaParserUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Java Parser implementation of {@link FieldMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class JavaParserFieldMetadata implements FieldMetadata {
	private JavaType fieldType;
	private JavaType fieldInitializer;
	private JavaSymbolName fieldName;
	private List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
	private String declaredByMetadataId;
	private int modifier;

	public JavaParserFieldMetadata(String declaredByMetadataId, FieldDeclaration fieldDeclaration, VariableDeclarator var, CompilationUnitServices compilationUnitServices, Set<JavaSymbolName> typeParameters) {
		Assert.notNull(declaredByMetadataId, "Declared by metadata ID required");
		Assert.notNull(fieldDeclaration, "Field declaration is mandatory");
		Assert.notNull(var, "Variable declarator required");
		Assert.isTrue(fieldDeclaration.getVariables().contains(var), "Cannot request a variable not already in the field declaration");
		Assert.notNull(compilationUnitServices, "Compilation unit services are required");
		
		// Convert Java Parser modifier into JDK modifier
		this.modifier = JavaParserUtils.getJdkModifier(fieldDeclaration.getModifiers());
		
		this.declaredByMetadataId = declaredByMetadataId;
		
		Type type = fieldDeclaration.getType();
		this.fieldType = JavaParserUtils.getJavaType(compilationUnitServices.getCompilationUnitPackage(), compilationUnitServices.getImports(), type, typeParameters);
		this.fieldName = new JavaSymbolName(var.getId().getName());
		
		// Lookup initializer, if one was requested and easily determinable
		Expression e = var.getInit();
		if (e != null) {
			if (e instanceof ObjectCreationExpr) {
				ObjectCreationExpr initializer = (ObjectCreationExpr) e;
				ClassOrInterfaceType initializerType = initializer.getType();
				this.fieldInitializer = JavaParserUtils.getJavaTypeNow(compilationUnitServices.getCompilationUnitPackage(), compilationUnitServices.getImports(), initializerType, typeParameters);
			} else {
				// TODO: Support other initializers (eg japa.parser.ast.expr.IntegerLiteralExpr)
			}
		}
		
		List<AnnotationExpr> annotations = fieldDeclaration.getAnnotations();
		if (annotations != null) {
			for (AnnotationExpr annotation : annotations) {
				this.annotations.add(new JavaParserAnnotationMetadata(annotation, compilationUnitServices));
			}
		}
	}

	public int getModifier() {
		return modifier;
	}

	public String getDeclaredByMetadataId() {
		return declaredByMetadataId;
	}

	public List<AnnotationMetadata> getAnnotations() {
		return Collections.unmodifiableList(annotations);
	}

	public JavaSymbolName getFieldName() {
		return fieldName;
	}

	public JavaType getFieldType() {
		return fieldType;
	}
	
	public static void addField(CompilationUnitServices compilationUnitServices, List<BodyDeclaration> members, FieldMetadata field, boolean permitFlush) {
		Assert.notNull(compilationUnitServices, "Compilation unit services required");
		Assert.notNull(members, "Members required");
		Assert.notNull(field, "Field required");
		
		// Import the field type into the compilation unit
		NameExpr importedType = JavaParserUtils.importTypeIfRequired(compilationUnitServices.getCompilationUnitPackage(), compilationUnitServices.getImports(), field.getFieldType());
		ClassOrInterfaceType fieldType = JavaParserUtils.getClassOrInterfaceType(importedType);
		
		FieldDeclaration newField = ASTHelper.createFieldDeclaration(JavaParserUtils.getJavaParserModifier(field.getModifier()), fieldType, field.getFieldName().getSymbolName());
		
		// Add parameterized types for the field type (not initializer)
		if (field.getFieldType().getParameters().size() > 0) {
			List<Type> fieldTypeArgs = new ArrayList<Type>();
			fieldType.setTypeArgs(fieldTypeArgs);
			for (JavaType parameter : field.getFieldType().getParameters()) {
				NameExpr importedParameterType = JavaParserUtils.importTypeIfRequired(compilationUnitServices.getCompilationUnitPackage(), compilationUnitServices.getImports(), parameter);
				fieldTypeArgs.add(JavaParserUtils.getReferenceType(importedParameterType));
			}
		}
		
		// Deal with initializers, if one has been requested
		JavaType initializer = field.getFieldInitializer();
		if (initializer != null) {
			if (initializer.isArray() || initializer.isPrimitive()) {
				// TODO: Support arrays and primitives as initializers
				throw new UnsupportedOperationException("Array or primitive initializers are currently unsupported");
			}
			
			List<VariableDeclarator> vars = newField.getVariables();
			Assert.notEmpty(vars, "Expected ASTHelper to have provided a single VariableDeclarator");
			Assert.isTrue(vars.size() == 1, "Expected ASTHelper to have provided a single VariableDeclarator");
			VariableDeclarator vd = vars.iterator().next();
			
			NameExpr importedInitialzierType = JavaParserUtils.importTypeIfRequired(compilationUnitServices.getCompilationUnitPackage(), compilationUnitServices.getImports(), initializer);
			ClassOrInterfaceType initializerType = JavaParserUtils.getClassOrInterfaceType(importedInitialzierType);
			
			// Add parameterized types for the initializer
			List<Type> initTypeArgs = new ArrayList<Type>();
			initializerType.setTypeArgs(initTypeArgs);
			for (JavaType parameter : initializer.getParameters()) {
				NameExpr importedParameterType = JavaParserUtils.importTypeIfRequired(compilationUnitServices.getCompilationUnitPackage(), compilationUnitServices.getImports(), parameter);
				initTypeArgs.add(JavaParserUtils.getReferenceType(importedParameterType));
			}

			vd.setInit(new ObjectCreationExpr(null, initializerType, null));
		}
		
		// Add annotations
		List<AnnotationExpr> annotations = new ArrayList<AnnotationExpr>();
		newField.setAnnotations(annotations);
		for (AnnotationMetadata annotation : field.getAnnotations()) {
			JavaParserAnnotationMetadata.addAnnotationToList(compilationUnitServices, annotations, annotation, false);
		}
		

		// Locate where to add this field; also verify if this field already exists
		int nextFieldIndex = 0;
		int i = -1;
		for (BodyDeclaration bd : members) {
			i++;
			if (bd instanceof FieldDeclaration) {
				// Next field should appear after this current field
				nextFieldIndex = i + 1;
				FieldDeclaration bdf = (FieldDeclaration) bd;
				for (VariableDeclarator v : bdf.getVariables()) {
					Assert.isTrue(!field.getFieldName().getSymbolName().equals(v.getId().getName()), "A field with name '" + field.getFieldName().getSymbolName() + "' already exists");
				}
			}
		}

		// Add the field to the compilation unit
		members.add(nextFieldIndex, newField);
		
		if (permitFlush) {
			compilationUnitServices.flush();
		}
	}
	
	public static void removeField(CompilationUnitServices compilationUnitServices, List<BodyDeclaration> members, JavaSymbolName fieldName) {
		Assert.notNull(compilationUnitServices, "Compilation unit services required");
		Assert.notNull(members, "Members required");
		Assert.notNull(fieldName, "Field name to remove is required");
		
		// Locate the field
		int i = -1;
		int toDelete = -1;
		for (BodyDeclaration bd : members) {
			i++;
			if (bd instanceof FieldDeclaration) {
				FieldDeclaration fieldDeclaration = (FieldDeclaration) bd;
				for (VariableDeclarator var : fieldDeclaration.getVariables()) {
					if (var.getId().getName().equals(fieldName.getSymbolName())) {
						toDelete = i;
						break;
					}
				}
			}
		}
		
		Assert.isTrue(toDelete > -1, "Could not locate field '" + fieldName + "' to delete");
		
		// Do removal outside iteration of body declaration members, to avoid concurrent modification exceptions
		members.remove(toDelete);

		compilationUnitServices.flush();
	}

	public JavaType getFieldInitializer() {
		return this.fieldInitializer;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("declaredByMetadataId", declaredByMetadataId);
		tsc.append("modifier", Modifier.toString(modifier));
		tsc.append("fieldType", fieldType);
		tsc.append("fieldName", fieldName);
		tsc.append("fieldInitializer", fieldInitializer);
		tsc.append("annotations", annotations);
		return tsc.toString();
	}

}
