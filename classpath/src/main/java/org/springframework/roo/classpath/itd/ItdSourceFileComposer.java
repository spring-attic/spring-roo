package org.springframework.roo.classpath.itd;

import java.lang.reflect.Modifier;
import java.util.List;

import org.springframework.roo.classpath.details.AnnotationMetadataUtils;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * A simple way of producing an inter-type declaration source file.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class ItdSourceFileComposer {
	
	private int indentLevel = 0;
	
	private JavaType introductionTo;
	private StringBuilder pw = new StringBuilder();
	private boolean content;

	/**
	 * Constructs an {@link ItdSourceFileComposer} containing the members that were requested in
	 * the passed object. If no members were requested, the {@link #isContent()} will be false.
	 * The type is {@link #appendTerminator()} if the append terminator is true.
	 * 
	 * @param itdTypeDetails to construct (required)
	 */
	public ItdSourceFileComposer(ItdTypeDetails itdTypeDetails, boolean appendTerminator) {
		appendItdTypeDetails(itdTypeDetails);
		if (appendTerminator) {
			appendTerminator();
		}
	}
	
	/**
	 * Constructs an {@link ItdSourceFileComposer} and automatically appends the terminator.
	 * 
	 * @param itdTypeDetails to construct (required)
	 */
	public ItdSourceFileComposer(ItdTypeDetails itdTypeDetails) {
		appendItdTypeDetails(itdTypeDetails);
		appendTerminator();
	}
	
	private void appendItdTypeDetails(ItdTypeDetails itdTypeDetails) {
		Assert.notNull(itdTypeDetails, "ITD type details required");
		appendDeclaration(itdTypeDetails.isPrivilegedAspect(), itdTypeDetails.getAspect(), itdTypeDetails.getName());
		appendExtendsTypes(itdTypeDetails.getExtendsTypes());
		appendImplementsTypes(itdTypeDetails.getImplementsTypes());
		appendTypeAnnotations(itdTypeDetails.getTypeAnnotations());
		appendFields(itdTypeDetails.getDeclaredFields());
		appendConstructors(itdTypeDetails.getDeclaredConstructors());
		appendMethods(itdTypeDetails.getDeclaredMethods());
	}
	
	/**
	 * Used to create an {@link ItdSourceFileComposer} by hand. It is recommended instead that
	 * one of the alternate constructors be used, as they are more automatic.
	 * 
	 * @param privilegedAspect whether the aspect is declared as privileged or not
	 * @param aspect the type of the aspect itself (required)
	 * @param introductionTo the type that will receive introductions (required)
	 */
	public ItdSourceFileComposer(boolean privilegedAspect, JavaType aspect, JavaType introductionTo) {
		appendDeclaration(privilegedAspect, aspect, introductionTo);
	}
	
	private void appendDeclaration(boolean privilegedAspect, JavaType aspect, JavaType introductionTo) {
		Assert.notNull(aspect, "Aspect is required");
		Assert.notNull(introductionTo, "Introduction to is required");

		this.introductionTo = introductionTo;
		
		Assert.isTrue(introductionTo.getPackage().equals(aspect.getPackage()), "Aspect and introduction must be in identical packages");
		
		if (!aspect.isDefaultPackage()) {
			this.appendFormalLine("package " + aspect.getPackage().getFullyQualifiedPackageName() + ";");
		}
		this.newLine();
		this.appendIndent();
		if (privilegedAspect) {
			this.append("privileged ");
		}
		this.append("aspect " + aspect.getSimpleTypeName() + " {");
		this.newLine();
		this.indent();
		this.newLine();

		// Set to false, as it was set true during the above operations
		content = false;
	}

	private void outputAnnotation(AnnotationMetadata annotation) {
		this.append(AnnotationMetadataUtils.toSourceForm(annotation));
	}
	
	private String getIntroductionTo() {
		// Workaround to simpify type name, as per AspectJ bug # 280380 and ROO-94
		if (introductionTo.isDefaultPackage()) {
			return introductionTo.getFullyQualifiedTypeName();
			//return introductionTo.getFullyQualifiedTypeNameIncludingTypeParameterNames();
		}
		return introductionTo.getFullyQualifiedTypeName().substring(introductionTo.getPackage().getFullyQualifiedPackageName().length()+1);
		//return introductionTo.getFullyQualifiedTypeNameIncludingTypeParameterNames().substring(introductionTo.getPackage().getFullyQualifiedPackageName().length()+1);
	}
	
	public void appendTypeAnnotations(List<? extends AnnotationMetadata> typeAnnotations) {
		if (typeAnnotations == null || typeAnnotations.size() == 0) {
			return;
		}
		
		content = true;
		
		for (AnnotationMetadata typeAnnotation : typeAnnotations) {
			this.appendIndent();
			this.append("declare @type: ");
			this.append(getIntroductionTo());
			this.append(": ");
			outputAnnotation(typeAnnotation);
			this.append(";");
			this.newLine();
			this.newLine();
		}
	}

	public void appendExtendsTypes(List<JavaType> extendsTypes) {
		if (extendsTypes == null || extendsTypes.size() == 0) {
			return;
		}
		
		content = true;
		
		for (JavaType extendsType : extendsTypes) {
			this.appendIndent();
			this.append("declare parents: ");
			this.append(getIntroductionTo());
			this.append(" extends ");
			this.append(extendsType.getFullyQualifiedTypeNameIncludingTypeParameters());
			this.append(";");
			this.newLine();
			this.newLine();
		}
	}

	public void appendImplementsTypes(List<JavaType> implementsTypes) {
		if (implementsTypes == null || implementsTypes.size() == 0) {
			return;
		}
		
		content = true;
		
		for (JavaType extendsType : implementsTypes) {
			this.appendIndent();
			this.append("declare parents: ");
			this.append(getIntroductionTo());
			this.append(" implements ");
			this.append(extendsType.getFullyQualifiedTypeNameIncludingTypeParameters());
			this.append(";");
			this.newLine();
			this.newLine();
		}
	}

	public void appendConstructors(List<? extends ConstructorMetadata> constructors) {
		if (constructors == null || constructors.size() == 0) {
			return;
		}
		content = true;
		for (ConstructorMetadata constructor : constructors) {
			Assert.isTrue(constructor.getParameterTypes().size() == constructor.getParameterNames().size(), "Mismatched parameter names against parameter types");
			
			// Append annotations
			for (AnnotationMetadata annotation : constructor.getAnnotations()) {
				this.appendIndent();
				outputAnnotation(annotation);
				this.newLine();
			}
			
			// Append "<modifier> <TargetOfIntroduction>.new" portion
			this.appendIndent();
			if (constructor.getModifier() != 0) {
				this.append(Modifier.toString(constructor.getModifier()));
				this.append(" ");
			}
			this.append(getIntroductionTo());
			this.append(".");
			this.append("new");

			// Append parameter types and names
			this.append("(");
			List<AnnotatedJavaType> paramTypes = constructor.getParameterTypes();
			List<JavaSymbolName> paramNames = constructor.getParameterNames();
			for (int i = 0 ; i < paramTypes.size(); i++) {
				AnnotatedJavaType paramType = paramTypes.get(i);
				JavaSymbolName paramName = paramNames.get(i);
				for (AnnotationMetadata methodParameterAnnotation : paramType.getAnnotations()) {
					this.append(AnnotationMetadataUtils.toSourceForm(methodParameterAnnotation));
					this.append(" ");
				}
				this.append(paramType.getJavaType().getFullyQualifiedTypeNameIncludingTypeParameters());
				this.append(" ");
				this.append(paramName.getSymbolName());
				if (i < paramTypes.size() - 1) {
					this.append(", ");
				}
			}
			this.append(") {");
			this.newLine();
			this.indent();

			// Add body
			this.append(constructor.getBody());
			this.indentRemove();
			this.appendFormalLine("}");
			this.newLine();
		}
	}
	
	public void appendMethods(List<? extends MethodMetadata> methods) {
		if (methods == null || methods.size() == 0) {
			return;
		}
		content = true;
		for (MethodMetadata method : methods) {
			Assert.isTrue(method.getParameterTypes().size() == method.getParameterNames().size(), "Mismatched parameter names against parameter types");
			
			// Append annotations
			for (AnnotationMetadata annotation : method.getAnnotations()) {
				this.appendIndent();
				outputAnnotation(annotation);
				this.newLine();
			}
			
			// Append "<modifier> <returntype> <methodName>" portion
			this.appendIndent();
			if (method.getModifier() != 0) {
				this.append(Modifier.toString(method.getModifier()));
				this.append(" ");
			}
//			this.append(method.getReturnType().getFullyQualifiedTypeNameIncludingTypeParameterNames());
			this.append(method.getReturnType().getFullyQualifiedTypeNameIncludingTypeParameters());
			this.append(" ");
			this.append(getIntroductionTo());
			this.append(".");
			this.append(method.getMethodName().getSymbolName());

			// Append parameter types and names
			this.append("(");
			List<AnnotatedJavaType> paramTypes = method.getParameterTypes();
			List<JavaSymbolName> paramNames = method.getParameterNames();
			for (int i = 0 ; i < paramTypes.size(); i++) {
				AnnotatedJavaType paramType = paramTypes.get(i);
				JavaSymbolName paramName = paramNames.get(i);
				for (AnnotationMetadata methodParameterAnnotation : paramType.getAnnotations()) {
					this.append(AnnotationMetadataUtils.toSourceForm(methodParameterAnnotation));
					this.append(" ");
				}
				this.append(paramType.getJavaType().getFullyQualifiedTypeNameIncludingTypeParameters());
				this.append(" ");
				this.append(paramName.getSymbolName());
				if (i < paramTypes.size() - 1) {
					this.append(", ");
				}
			}
			this.append(") {");
			this.newLine();
			this.indent();

			// Add body
			this.append(method.getBody());
			this.indentRemove();
			this.appendFormalLine("}");
			this.newLine();
		}
	}
	
	public void appendFields(List<? extends FieldMetadata> fields) {
		if (fields == null || fields.size() == 0) {
			return;
		}
		content = true;
		for (FieldMetadata field : fields) {
			
			// Append annotations
			for (AnnotationMetadata annotation : field.getAnnotations()) {
				this.appendIndent();
				outputAnnotation(annotation);
				this.newLine();
			}
			
			// Append "<modifier> <fieldtype> <fieldName>" portion
			this.appendIndent();
			if (field.getModifier() != 0) {
				this.append(Modifier.toString(field.getModifier()));
				this.append(" ");
			}
			this.append(field.getFieldType().getFullyQualifiedTypeNameIncludingTypeParameters());
			this.append(" ");
			this.append(getIntroductionTo());
			this.append(".");
			this.append(field.getFieldName().getSymbolName());

			// Append initializer, if present
			if (field.getFieldInitializer() != null) {
				this.append(" = new ");
				this.append(field.getFieldInitializer().getFullyQualifiedTypeNameIncludingTypeParameters());
				this.append("()");
			}
			
			// Complete the field declaration
			this.append(";");
			this.newLine();
			this.newLine();
		}
	}

	/**
	 * Increases the indent by one level.
	 */
	public ItdSourceFileComposer indent() {
		indentLevel++;
		return this;
	}
	
	/**
	 * Resets the indent to zero.
	 */
	public ItdSourceFileComposer reset() {
		indentLevel = 0;
		return this;
	}

	/**
	 * Decreases the indent by one level.
	 */
	public ItdSourceFileComposer indentRemove() {
		indentLevel--;
		return this;
	}

	/**
	 * Prints a blank line, ensuring any indent is included before doing so.
	 */
	public ItdSourceFileComposer newLine() {
		appendIndent();
        // We use \n for consistency with JavaParser's DumpVisitor, which always uses \n
		pw.append("\n");
		//pw.append(System.getProperty("line.separator"));
		return this;
	}
	
	/**
	 * Prints the message, WITHOUT ANY INDENTATION.
	 */
	public ItdSourceFileComposer append(String message) {
		if (message != null && !"".equals(message)) {
			pw.append(message);
			content = true;
		}
		return this;
	}

	/**
	 * Prints the message, after adding indents and returns to a new line. This is the most commonly used method.
	 */
	public ItdSourceFileComposer appendFormalLine(String message) {
		appendIndent();
		if (message != null && !"".equals(message)) {
			pw.append(message);
			content = true;
		}
		return newLine();
	}

	/**
	 * Prints the relevant number of indents.
	 */
	public ItdSourceFileComposer appendIndent() {
		for (int i = 0 ; i < indentLevel; i++) {
			pw.append("    ");
		}
		return this;
	}
	
	public void appendTerminator() {
		Assert.isTrue(this.indentLevel == 1, "Indent level must be 1 (not " + indentLevel + ") to conclude!");
		this.indentRemove();
		
		// Ensure we present the content flag, as it will be set true during the formal line append
		boolean contentBefore = content;
		this.appendFormalLine("}");
		content = contentBefore;
		
	}
	
	public String getOutput() {
		return pw.toString();
	}

	/**
	 * Indicates whether any content was added to the ITD, aside from the formal ITD declaration.
	 * 
	 * @return true if there is actual content in the ITD, false otherwise
	 */
	public boolean isContent() {
		return content;
	}
}
