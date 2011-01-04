package org.springframework.roo.classpath.javaparser.details;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.TypeParameter;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.body.VariableDeclaratorId;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.ReferenceType;
import japa.parser.ast.type.Type;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
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
 * Java Parser implementation of {@link MethodMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class JavaParserMethodMetadata extends AbstractCustomDataAccessorProvider implements MethodMetadata {
	private List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
	private List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
	private List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
	private List<JavaType> throwsTypes = new ArrayList<JavaType>();
	private JavaType returnType;
	private JavaSymbolName methodName;
	private String body;
	private String declaredByMetadataId;
	private int modifier;
	
	public JavaParserMethodMetadata(String declaredByMetadataId, MethodDeclaration methodDeclaration, CompilationUnitServices compilationUnitServices, Set<JavaSymbolName> typeParameters) {
		super(CustomDataImpl.NONE);
		Assert.hasText(declaredByMetadataId, "Declared by metadata ID required");
		Assert.notNull(methodDeclaration, "Method declaration is mandatory");
		Assert.notNull(compilationUnitServices, "Compilation unit services are required");

        this.declaredByMetadataId = declaredByMetadataId;

		// Convert Java Parser modifier into JDK modifier
		this.modifier = JavaParserUtils.getJdkModifier(methodDeclaration.getModifiers());
		
		// Add method-declared type parameters (if any) to the list of type parameters
		Set<JavaSymbolName> fullTypeParameters = new HashSet<JavaSymbolName>();
		fullTypeParameters.addAll(typeParameters);
		List<TypeParameter> params = methodDeclaration.getTypeParameters();
		if (params != null) {
			for (TypeParameter candidate : params) {
				JavaSymbolName currentTypeParam = new JavaSymbolName(candidate.getName());
				fullTypeParameters.add(currentTypeParam);
			}
		}
		
		// Compute the return type
		Type rt = methodDeclaration.getType();
		this.returnType = JavaParserUtils.getJavaType(compilationUnitServices, rt, fullTypeParameters);
		
		// Compute the method name
		this.methodName = new JavaSymbolName(methodDeclaration.getName());
		
		// Get the body
		this.body = methodDeclaration.getBody() == null ? null : methodDeclaration.getBody().toString();

        if (this.body != null) {
            this.body = this.body.replaceFirst("\\{", "");
            this.body = this.body.substring(0, this.body.lastIndexOf("}"));
        }

		// Lookup the parameters and their names
		if (methodDeclaration.getParameters() != null) {
			for (Parameter p : methodDeclaration.getParameters()) {
				Type pt = p.getType();
				JavaType parameterType = JavaParserUtils.getJavaType(compilationUnitServices, pt, fullTypeParameters);
				
				List<AnnotationExpr> annotationsList = p.getAnnotations();
				List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
				if (annotationsList != null) {
					for (AnnotationExpr candidate : annotationsList) {
						JavaParserAnnotationMetadata md = new JavaParserAnnotationMetadata(candidate, compilationUnitServices);
						annotations.add(md);
					}
				}
				AnnotatedJavaType param = new AnnotatedJavaType(parameterType, annotations);
                param.setVarArgs(p.isVarArgs());
				parameterTypes.add(param);
				parameterNames.add(new JavaSymbolName(p.getId().getName()));
			}
		}
		
		if (methodDeclaration.getThrows() != null) {
			for (NameExpr throwsType: methodDeclaration.getThrows()) {
				JavaType throwing = JavaParserUtils.getJavaType(compilationUnitServices, throwsType, fullTypeParameters);
				throwsTypes.add(throwing);
			}
		}
		
		if (methodDeclaration.getAnnotations() != null) {
			for (AnnotationExpr annotation : methodDeclaration.getAnnotations()) {
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

	public JavaType getReturnType() {
		return returnType;
	}

	public JavaSymbolName getMethodName() {
		return methodName;
	}

	public List<JavaType> getThrowsTypes() {
		return throwsTypes;
	}

	public String getBody() {
		return body;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("declaredByMetadataId", declaredByMetadataId);
		tsc.append("modifier", Modifier.toString(modifier));
		tsc.append("methodName", methodName);
		tsc.append("parameterTypes", parameterTypes);
		tsc.append("parameterNames", parameterNames);
		tsc.append("returnType", returnType);
		tsc.append("throwsTypes", throwsTypes);
		tsc.append("annotations", annotations);
		tsc.append("customData", getCustomData());
		tsc.append("body", body);
		return tsc.toString();
	}

	public static void addMethod(CompilationUnitServices compilationUnitServices, List<BodyDeclaration> members, MethodMetadata method, Set<JavaSymbolName> typeParameters) {
		Assert.notNull(compilationUnitServices, "Flushable compilation unit services required");
		Assert.notNull(members, "Members required");
		Assert.notNull(method, "Method required");
		
		if (typeParameters == null) {
			typeParameters = new HashSet<JavaSymbolName>();
		}
		
		// Create the return type we should use
		Type returnType = null;
		if (method.getReturnType().isPrimitive()) {
			returnType = JavaParserUtils.getType(method.getReturnType());
		} else {
			NameExpr importedType = JavaParserUtils.importTypeIfRequired(compilationUnitServices.getEnclosingTypeName(), compilationUnitServices.getImports(), method.getReturnType());
			ClassOrInterfaceType cit = JavaParserUtils.getClassOrInterfaceType(importedType);
			
			// Add any type arguments presented for the return type
			if (method.getReturnType().getParameters().size() > 0) {
				List<Type> typeArgs = new ArrayList<Type>();
				cit.setTypeArgs(typeArgs);
				for (JavaType parameter : method.getReturnType().getParameters()) {
					typeArgs.add(JavaParserUtils.importParametersForType(compilationUnitServices.getEnclosingTypeName(), compilationUnitServices.getImports(), parameter));
				}
			}
			
			// Handle arrays
			if (method.getReturnType().isArray()) {
				ReferenceType rt = new ReferenceType();
				rt.setArrayCount(method.getReturnType().getArray());
				rt.setType(cit);
				returnType = rt;
			} else {
				returnType = cit;
			}
		}
		
		// Start with the basic method
		MethodDeclaration d = new MethodDeclaration();
		d.setModifiers(JavaParserUtils.getJavaParserModifier(method.getModifier()));
		d.setName(method.getMethodName().getSymbolName());
		d.setType(returnType);
		
		// Add any method-level annotations (not parameter annotations)
		List<AnnotationExpr> annotations = new ArrayList<AnnotationExpr>();
		d.setAnnotations(annotations);
		for (AnnotationMetadata annotation : method.getAnnotations()) {
			JavaParserAnnotationMetadata.addAnnotationToList(compilationUnitServices, annotations, annotation);
		}
	
		// Add any method parameters, including their individual annotations and type parameters
		List<Parameter> parameters = new ArrayList<Parameter>();
		d.setParameters(parameters);

		int index = -1;
		for (AnnotatedJavaType methodParameter : method.getParameterTypes()) {
			index++;

			// Add the parameter annotations applicable for this parameter type
			List<AnnotationExpr> parameterAnnotations = new ArrayList<AnnotationExpr>();
	
			for (AnnotationMetadata parameterAnnotation : methodParameter.getAnnotations()) {
				JavaParserAnnotationMetadata.addAnnotationToList(compilationUnitServices, parameterAnnotations, parameterAnnotation);
			}
			
			// Compute the parameter name
			String parameterName = method.getParameterNames().get(index).getSymbolName();
			
			// Compute the parameter type
			Type parameterType = null;
			if (methodParameter.getJavaType().isPrimitive()) {
				parameterType = JavaParserUtils.getType(methodParameter.getJavaType());
			} else {
				parameterType = JavaParserMutableClassOrInterfaceTypeDetails.getResolvedName(compilationUnitServices.getEnclosingTypeName(), AnnotatedJavaType.convertFromAnnotatedJavaTypes(Collections.singletonList(methodParameter)).get(0), compilationUnitServices);
			}

			// Create a Java Parser method parameter and add it to the list of parameters
			Parameter p = new Parameter(parameterType, new VariableDeclaratorId(parameterName));
            p.setVarArgs(methodParameter.isVarArgs());
			p.setAnnotations(parameterAnnotations);
			parameters.add(p);
		}
		
		// Add exceptions which the method my throw
		if (method.getThrowsTypes().size() > 0) {
			List<NameExpr> throwsTypes = new ArrayList<NameExpr>();
			for (JavaType javaType: method.getThrowsTypes()) {
				NameExpr importedType = JavaParserUtils.importTypeIfRequired(compilationUnitServices.getEnclosingTypeName(), compilationUnitServices.getImports(), javaType);
				throwsTypes.add(importedType);
			}
			d.setThrows(throwsTypes);
		}
		
		// Set the body
		if (!StringUtils.hasText(method.getBody())) {
			// Never set the body if an abstract method
			if (!Modifier.isAbstract(method.getModifier()) && !PhysicalTypeCategory.INTERFACE.equals(compilationUnitServices.getPhysicalTypeCategory())) {
				d.setBody(new BlockStmt());
			}
		} else {
			// There is a body.
			// We need to make a fake method that we can have JavaParser parse.
			// Easiest way to do that is to build a simple source class containing the required method and re-parse it.
			StringBuilder sb = new StringBuilder();
			sb.append("class TemporaryClass {\n");
			sb.append("  public void temporaryMethod() {\n");
			sb.append(method.getBody());
			sb.append("\n");
			sb.append("  }\n");
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
				throw new IllegalArgumentException("Method body invalid");
			}
			TypeDeclaration td = types.get(0);
			List<BodyDeclaration> bodyDeclarations = td.getMembers();
			if (bodyDeclarations == null || bodyDeclarations.size() != 1) {
				throw new IllegalStateException("Illegal state: JavaParser did not return body declarations correctly");
			}
			BodyDeclaration bd = bodyDeclarations.get(0);
			if (!(bd instanceof MethodDeclaration)) {
				throw new IllegalStateException("Illegal state: JavaParser did not return a method declaration correctly");
			}
			MethodDeclaration md = (MethodDeclaration) bd;
			d.setBody(md.getBody());
		}
	
		// Locate where to add this method; also verify if this method already exists
		for (BodyDeclaration bd : members) {
			if (bd instanceof MethodDeclaration) {
				// Next method should appear after this current method
				MethodDeclaration md = (MethodDeclaration) bd;
				if (md.getName().equals(d.getName())) {
					if ((md.getParameters() == null || md.getParameters().isEmpty()) && (d.getParameters() == null || d.getParameters().isEmpty())) {
						throw new IllegalStateException("Method '" + method.getMethodName().getSymbolName() + "' already exists");
					} else if (md.getParameters() != null && md.getParameters().size() == d.getParameters().size()) {
						// Possible match, we need to consider parameter types as well now
						JavaParserMethodMetadata jpmm = new JavaParserMethodMetadata(method.getDeclaredByMetadataId(), md, compilationUnitServices, typeParameters);
						boolean matchesFully = true;
						index = -1;
						for (AnnotatedJavaType existingParameter : jpmm.getParameterTypes()) {
							index++;
							AnnotatedJavaType parameterType = method.getParameterTypes().get(index);
							if (!existingParameter.getJavaType().equals(parameterType.getJavaType())) {
								matchesFully = false;
								break;
							}
						}
						if (matchesFully) {
							throw new IllegalStateException("Method '" + method.getMethodName().getSymbolName() + "' already exists with identical parameters");
						}
					}
				}
			}
		}

		// Add the method to the end of the compilation unit
		members.add(d);
	}
}
