package org.springframework.roo.project.layers;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.InitializerMetadataBuilder;
import org.springframework.roo.classpath.details.ItdTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.CustomDataBuilder;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * 
 * @author Stefan Schmidt
 * @since 1.2
 */
public class MemberTypeAdditions {
	
	private ClassOrInterfaceTypeDetailsBuilder classOrInterfaceDetailsBuilder;
	private String methodSignature;
	
	public MemberTypeAdditions(ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder, String methodSignature) {
		super();
		Assert.notNull(classOrInterfaceTypeDetailsBuilder, "Class or member details builder required");
		
		this.classOrInterfaceDetailsBuilder = classOrInterfaceTypeDetailsBuilder;
		this.methodSignature = methodSignature;
	}

	public ClassOrInterfaceTypeDetailsBuilder getClassOrInterfaceTypeDetailsBuilder() {
		return classOrInterfaceDetailsBuilder;
	}

	public String getMethodSignature() {
		return methodSignature;
	}

	@Override
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("memberHoldingTypeDetails", classOrInterfaceDetailsBuilder);
		tsc.append("methodSignature", methodSignature);
		return tsc.toString();
	}
	
	public void copyClassOrInterfaceTypeDetailsIntoTargetTypeBuilder(ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder, ItdTypeDetailsBuilder itdTypeDetailsBuilder) {
		
		// Copy fields
		for (FieldMetadataBuilder field : classOrInterfaceTypeDetailsBuilder.getDeclaredFields()) {
			itdTypeDetailsBuilder.addField(field.build());
		}
		
		// Copy methods
		for (MethodMetadataBuilder method : classOrInterfaceTypeDetailsBuilder.getDeclaredMethods()) {
			itdTypeDetailsBuilder.addMethod(method);
		}
		
		// Copy annotations
		for (AnnotationMetadataBuilder annotation : classOrInterfaceTypeDetailsBuilder.getAnnotations()) {
			itdTypeDetailsBuilder.addAnnotation(annotation);
		}
		
		// Copy custom data
		if (classOrInterfaceTypeDetailsBuilder.getCustomData() != null) {
			CustomDataBuilder customDataBuilder = new CustomDataBuilder(classOrInterfaceTypeDetailsBuilder.getCustomData().build());
			customDataBuilder.append(itdTypeDetailsBuilder.getCustomData().build());
			itdTypeDetailsBuilder.setCustomData(customDataBuilder);
		}
		
		// Copy constructors
		for (ConstructorMetadataBuilder constructor : classOrInterfaceTypeDetailsBuilder.getDeclaredConstructors()) {
			itdTypeDetailsBuilder.addConstructor(constructor);
		}
		
		// Copy initializers
		for (InitializerMetadataBuilder initializer : classOrInterfaceTypeDetailsBuilder.getDeclaredInitializers()) {
			itdTypeDetailsBuilder.addInitializer(initializer);
		}
		
		// Copy inner types
		for (ClassOrInterfaceTypeDetailsBuilder innerType : classOrInterfaceTypeDetailsBuilder.getDeclaredInnerTypes()) {
			itdTypeDetailsBuilder.addInnerType(innerType);
		}
		
		// Copy extends types
		for (JavaType type : classOrInterfaceTypeDetailsBuilder.getExtendsTypes()) {
			itdTypeDetailsBuilder.addExtendsTypes(type);
		}
		
		// Copy implements types
		for (JavaType type : classOrInterfaceTypeDetailsBuilder.getImplementsTypes()) {
			itdTypeDetailsBuilder.addImplementsType(type);
		}
	}
}
