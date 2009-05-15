package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Default representation of an {@link ItdTypeDetails}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class DefaultItdTypeDetails implements ItdTypeDetails {
	
	private JavaType name;
	private JavaType aspect;
	private boolean privilegedAspect;
	
	private PhysicalTypeCategory physicalTypeCategory = PhysicalTypeCategory.ITD;
	private List<ConstructorMetadata> declaredConstructors = new ArrayList<ConstructorMetadata>();
	private List<FieldMetadata> declaredFields = new ArrayList<FieldMetadata>();
	private List<MethodMetadata> declaredMethods = new ArrayList<MethodMetadata>();
	private List<JavaType> extendsTypes = new ArrayList<JavaType>();
	private List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
	private List<JavaType> implementsTypes = new ArrayList<JavaType>();
	
	public DefaultItdTypeDetails(JavaType name, JavaType aspect,
			boolean privilegedAspect,
			List<ConstructorMetadata> declaredConstructors,
			List<FieldMetadata> declaredFields,
			List<MethodMetadata> declaredMethods,
			List<JavaType> extendsTypes,
			List<JavaType> implementsTypes,
			List<AnnotationMetadata> typeAnnotations) {
		Assert.notNull(name, "Name (to receive the introductions) required");
		Assert.notNull(aspect, "Aspect required");

		this.name = name;
		this.aspect = aspect;
		this.privilegedAspect = privilegedAspect;
		
		if (declaredConstructors != null) {
			this.declaredConstructors = declaredConstructors;
		}
		
		if (declaredFields != null) {
			this.declaredFields = declaredFields;
		}
		
		if (declaredMethods != null) {
			this.declaredMethods = declaredMethods;
		}
		
		if (extendsTypes != null) {
			this.extendsTypes = extendsTypes;
		}
		
		if (implementsTypes != null) {
			this.implementsTypes = implementsTypes;
		}

		if (typeAnnotations != null) {
			this.typeAnnotations = typeAnnotations;
		}
	}
	
	public static DefaultItdTypeDetailsBuilder getBuilder(String declaredByMetadataId, JavaType name, JavaType aspect, boolean privilegedAspect) {
		return new DefaultItdTypeDetailsBuilder(declaredByMetadataId, name, aspect, privilegedAspect);
	}

	public List<JavaType> getImplementsTypes() {
		return Collections.unmodifiableList(implementsTypes);
	}

	public PhysicalTypeCategory getPhysicalTypeCategory() {
		return physicalTypeCategory;
	}

	public JavaType getName() {
		return name;
	}
	
	public JavaType getAspect() {
		return aspect;
	}

	public boolean isPrivilegedAspect() {
		return privilegedAspect;
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
	
	public List<? extends AnnotationMetadata> getTypeAnnotations() {
		return Collections.unmodifiableList(typeAnnotations);
	}
	
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("name", name);
		tsc.append("aspect", aspect);
		tsc.append("physicalTypeCategory", physicalTypeCategory);
		tsc.append("privilegedAspect", privilegedAspect);
		tsc.append("declaredConstructors", declaredConstructors);
		tsc.append("declaredFields", declaredFields);
		tsc.append("declaredMethods", declaredMethods);
		tsc.append("extendsTypes", extendsTypes);
		tsc.append("typeAnnotations", typeAnnotations);
		return tsc.toString();
	}
}
