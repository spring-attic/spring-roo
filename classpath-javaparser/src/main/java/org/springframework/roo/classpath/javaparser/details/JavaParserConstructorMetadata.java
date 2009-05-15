package org.springframework.roo.classpath.javaparser.details;

import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.type.Type;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.javaparser.CompilationUnitServices;
import org.springframework.roo.classpath.javaparser.JavaParserUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Java Parser implementation of {@link ConstructorMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class JavaParserConstructorMetadata implements ConstructorMetadata {

	private List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
	private List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
	private List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
	private String body;
	private String declaredByMetadataId;
	private int modifier;
	
	public JavaParserConstructorMetadata(String declaredByMetadataId, ConstructorDeclaration constructorDeclaration, CompilationUnitServices compilationUnitServices) {
		Assert.hasText(declaredByMetadataId, "Declared by metadata ID required");
		Assert.notNull(constructorDeclaration, "Constructor declaration is mandatory");
		Assert.notNull(compilationUnitServices, "Compilation unit services are required");
		
		// Convert Java Parser modifier into JDK modifier
		this.modifier = JavaParserUtils.getJdkModifier(constructorDeclaration.getModifiers());
		
		this.declaredByMetadataId = declaredByMetadataId;
		
		// Get the body
		this.body = constructorDeclaration.getBlock().toString();
		
		// Lookup the parameters and their names
		if (constructorDeclaration.getParameters() != null) {
			for (Parameter p : constructorDeclaration.getParameters()) {
				Type pt = p.getType();
				JavaType parameterType = JavaParserUtils.getJavaType(compilationUnitServices.getCompilationUnitPackage(), compilationUnitServices.getImports(), pt);
				
				List<AnnotationExpr> annotationsList = p.getAnnotations();
				List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
				if (annotationsList != null) {
					for (AnnotationExpr candidate : annotationsList) {
						JavaParserAnnotationMetadata md = new JavaParserAnnotationMetadata(candidate, compilationUnitServices);
						annotations.add(md);
					}
				}
				
				parameterTypes.add(new AnnotatedJavaType(parameterType, annotations));
				parameterNames.add(new JavaSymbolName(p.getId().getName()));
			}
		}
		
		if (constructorDeclaration.getAnnotations() != null) {
			for (AnnotationExpr annotation : constructorDeclaration.getAnnotations()) {
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
	
	public List<JavaSymbolName> getParameterNames() {
		return Collections.unmodifiableList(parameterNames);
	}

	public List<AnnotatedJavaType> getParameterTypes() {
		return Collections.unmodifiableList(parameterTypes);
	}
	
	public String getBody() {
		return body;
	}
	
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("declaredByMetadataId", declaredByMetadataId);
		tsc.append("modifier", Modifier.toString(modifier));
		tsc.append("parameterTypes", parameterTypes);
		tsc.append("parameterNames", parameterNames);
		tsc.append("annotations", annotations);
		tsc.append("body", body);
		return tsc.toString();
	}
}
