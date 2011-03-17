package org.springframework.roo.classpath.javaparser.details;

import japa.parser.ASTHelper;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.ObjectCreationExpr;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.Type;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.javaparser.CompilationUnitServices;
import org.springframework.roo.classpath.javaparser.JavaParserMutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.javaparser.JavaParserUtils;
import org.springframework.roo.model.AbstractCustomDataAccessorProvider;
import org.springframework.roo.model.CustomDataImpl;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Java Parser implementation of {@link FieldMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class JavaParserFieldMetadata extends AbstractCustomDataAccessorProvider implements FieldMetadata {
	private JavaType fieldType;
	private String fieldInitializer;
	private JavaSymbolName fieldName;
	private List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
	private String declaredByMetadataId;
	private int modifier;

	public JavaParserFieldMetadata(String declaredByMetadataId, FieldDeclaration fieldDeclaration, VariableDeclarator var, CompilationUnitServices compilationUnitServices, Set<JavaSymbolName> typeParameters) {
		super(CustomDataImpl.NONE);
		Assert.notNull(declaredByMetadataId, "Declared by metadata ID required");
		Assert.notNull(fieldDeclaration, "Field declaration is mandatory");
		Assert.notNull(var, "Variable declarator required");
		Assert.isTrue(fieldDeclaration.getVariables().contains(var), "Cannot request a variable not already in the field declaration");
		Assert.notNull(compilationUnitServices, "Compilation unit services are required");
		
		// Convert Java Parser modifier into JDK modifier
		this.modifier = JavaParserUtils.getJdkModifier(fieldDeclaration.getModifiers());
		
		this.declaredByMetadataId = declaredByMetadataId;
		
		Type type = fieldDeclaration.getType();
		this.fieldType = JavaParserUtils.getJavaType(compilationUnitServices, type, typeParameters);
		
		// Convert into an array if this variable ID uses array notation
		if (var.getId().getArrayCount() > 0) {
			this.fieldType = new JavaType(fieldType.getFullyQualifiedTypeName(), var.getId().getArrayCount() + fieldType.getArray(), fieldType.getDataType(), fieldType.getArgName(), fieldType.getParameters());
		}
		
		this.fieldName = new JavaSymbolName(var.getId().getName());
		
		// Lookup initializer, if one was requested and easily determinable
		Expression e = var.getInit();
		if (e != null) {
			this.fieldInitializer = e.toString();
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
	
	public static void addField(CompilationUnitServices compilationUnitServices, List<BodyDeclaration> members, FieldMetadata field) {
		Assert.notNull(compilationUnitServices, "Flushable compilation unit services required");
		Assert.notNull(members, "Members required");
		Assert.notNull(field, "Field required");
		
		JavaParserUtils.importTypeIfRequired(compilationUnitServices.getEnclosingTypeName(), compilationUnitServices.getImports(), field.getFieldType());
		ClassOrInterfaceType initType = JavaParserMutableClassOrInterfaceTypeDetails.getResolvedName(compilationUnitServices.getEnclosingTypeName(), field.getFieldType(), compilationUnitServices);
		
		FieldDeclaration newField = ASTHelper.createFieldDeclaration(JavaParserUtils.getJavaParserModifier(field.getModifier()), initType, field.getFieldName().getSymbolName());
		
		// Add parameterized types for the field type (not initializer)
		if (field.getFieldType().getParameters().size() > 0) {
			List<Type> fieldTypeArgs = new ArrayList<Type>();
			initType.setTypeArgs(fieldTypeArgs);
			for (JavaType parameter : field.getFieldType().getParameters()) {
				NameExpr importedParameterType = JavaParserUtils.importTypeIfRequired(compilationUnitServices.getEnclosingTypeName(), compilationUnitServices.getImports(), parameter);
				fieldTypeArgs.add(JavaParserUtils.getReferenceType(importedParameterType));
			}
		}
		
		List<VariableDeclarator> vars = newField.getVariables();
		Assert.notEmpty(vars, "Expected ASTHelper to have provided a single VariableDeclarator");
		Assert.isTrue(vars.size() == 1, "Expected ASTHelper to have provided a single VariableDeclarator");
		VariableDeclarator vd = vars.iterator().next();

		if (StringUtils.hasText(field.getFieldInitializer())) {
			// There is an initializer.
			// We need to make a fake field that we can have JavaParser parse.
			// Easiest way to do that is to build a simple source class containing the required field and re-parse it.
			StringBuilder sb = new StringBuilder();
			sb.append("class TemporaryClass {\n");
			sb.append("  private " + field.getFieldType()  + " " + field.getFieldName() + " = " + field.getFieldInitializer() + ";\n");
			sb.append("}\n");
			ByteArrayInputStream bais = new ByteArrayInputStream(sb.toString().getBytes());
			CompilationUnit ci;
			try {
				ci = JavaParser.parse(bais);
			} catch (ParseException pe) {
				throw new IllegalStateException("Illegal state: JavaParser did not parse correctly", pe);
			}
			List<TypeDeclaration> types = ci.getTypes();
			if (types == null || types.size() != 1) {
				throw new IllegalArgumentException("Field member invalid");
			}
			TypeDeclaration td = types.get(0);
			List<BodyDeclaration> bodyDeclarations = td.getMembers();
			if (bodyDeclarations == null || bodyDeclarations.size() != 1) {
				throw new IllegalStateException("Illegal state: JavaParser did not return body declarations correctly");
			}
			BodyDeclaration bd = bodyDeclarations.get(0);
			if (!(bd instanceof FieldDeclaration)) {
				throw new IllegalStateException("Illegal state: JavaParser did not return a field declaration correctly");
			}
			FieldDeclaration fd = (FieldDeclaration) bd;
			if (fd.getVariables() == null || fd.getVariables().size() != 1) {
				throw new IllegalStateException("Illegal state: JavaParser did not return a field declaration correctly");
			}
			
			Expression init = fd.getVariables().get(0).getInit();

			// Resolve imports (ROO-1505)
			if (init instanceof ObjectCreationExpr) {
				ObjectCreationExpr ocr = (ObjectCreationExpr) init;
				JavaType typeToImport = JavaParserUtils.getJavaTypeNow(compilationUnitServices, ocr.getType(), null);
				NameExpr nameExpr = JavaParserUtils.importTypeIfRequired(compilationUnitServices.getEnclosingTypeName(), compilationUnitServices.getImports(), typeToImport);
				ClassOrInterfaceType classOrInterfaceType = JavaParserUtils.getClassOrInterfaceType(nameExpr);
				ocr.setType(classOrInterfaceType);
				
				if (typeToImport.getParameters().size() > 0) {
					List<Type> initTypeArgs = new ArrayList<Type>();
					initType.setTypeArgs(initTypeArgs);
					for (JavaType parameter : typeToImport.getParameters()) {
						NameExpr importedParameterType = JavaParserUtils.importTypeIfRequired(compilationUnitServices.getEnclosingTypeName(), compilationUnitServices.getImports(), parameter);
						initTypeArgs.add(JavaParserUtils.getReferenceType(importedParameterType));
					}
					classOrInterfaceType.setTypeArgs(initTypeArgs);
				}
			}

			vd.setInit(init);
		}
		
		// Add annotations
		List<AnnotationExpr> annotations = new ArrayList<AnnotationExpr>();
		newField.setAnnotations(annotations);
		for (AnnotationMetadata annotation : field.getAnnotations()) {
			JavaParserAnnotationMetadata.addAnnotationToList(compilationUnitServices, annotations, annotation);
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
	}
	
	public static void removeField(CompilationUnitServices compilationUnitServices, List<BodyDeclaration> members, JavaSymbolName fieldName) {
		Assert.notNull(compilationUnitServices, "Flushable compilation unit services required");
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
	}

	public String getFieldInitializer() {
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
		tsc.append("customData", getCustomData());
		return tsc.toString();
	}
}
