package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Default representation of an {@link ItdTypeDetails}.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class DefaultItdTypeDetails implements ItdTypeDetails {
	
	private ClassOrInterfaceTypeDetails governor;
	private JavaType aspect;
	private boolean privilegedAspect;
	
	private PhysicalTypeCategory physicalTypeCategory = PhysicalTypeCategory.ITD;
	private List<ConstructorMetadata> declaredConstructors = new ArrayList<ConstructorMetadata>();
	private List<FieldMetadata> declaredFields = new ArrayList<FieldMetadata>();
	private List<MethodMetadata> declaredMethods = new ArrayList<MethodMetadata>();
	private List<JavaType> extendsTypes = new ArrayList<JavaType>();
	private List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
	private List<JavaType> implementsTypes = new ArrayList<JavaType>();
	private Set<JavaType> registeredImports = new HashSet<JavaType>();
	private List<DeclaredFieldAnnotationDetails> fieldAnnotations = new ArrayList<DeclaredFieldAnnotationDetails>();
	private List<DeclaredMethodAnnotationDetails> methodAnnotations = new ArrayList<DeclaredMethodAnnotationDetails>();
	private List<ClassOrInterfaceTypeDetails> innerTypes = new ArrayList<ClassOrInterfaceTypeDetails>();
	
	public DefaultItdTypeDetails(ClassOrInterfaceTypeDetails governor, JavaType aspect,
			boolean privilegedAspect, Set<JavaType> registeredImports,
			List<ConstructorMetadata> declaredConstructors,
			List<FieldMetadata> declaredFields,
			List<MethodMetadata> declaredMethods,
			List<JavaType> extendsTypes,
			List<JavaType> implementsTypes,
			List<AnnotationMetadata> typeAnnotations,
			List<DeclaredFieldAnnotationDetails> fieldAnnotations,
			List<DeclaredMethodAnnotationDetails> methodAnnotations,
			List<ClassOrInterfaceTypeDetails> innerTypes) {
		Assert.notNull(governor, "Governor (to receive the introductions) required");
		Assert.notNull(aspect, "Aspect required");
		
		this.governor = governor;
		this.aspect = aspect;
		this.privilegedAspect = privilegedAspect;
		
		if (registeredImports != null) {
			this.registeredImports = registeredImports;
		}

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
		
		if (fieldAnnotations != null) {
			this.fieldAnnotations = fieldAnnotations;
		}
		
		if (methodAnnotations != null) {
			this.methodAnnotations = methodAnnotations;
		}
		
		if (innerTypes != null) {
			this.innerTypes = innerTypes;
		}
	}
	
	public static DefaultItdTypeDetailsBuilder getBuilder(String declaredByMetadataId, ClassOrInterfaceTypeDetails governor, JavaType aspect, boolean privilegedAspect) {
		return new DefaultItdTypeDetailsBuilder(declaredByMetadataId, governor, aspect, privilegedAspect);
	}

	public Set<JavaType> getRegisteredImports() {
		return Collections.unmodifiableSet(registeredImports);
	}

	public List<JavaType> getImplementsTypes() {
		return Collections.unmodifiableList(implementsTypes);
	}

	public PhysicalTypeCategory getPhysicalTypeCategory() {
		return physicalTypeCategory;
	}

	public JavaType getName() {
		return governor.getName();
	}
	
	public ClassOrInterfaceTypeDetails getGovernor() {
		return governor;
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
	
	public List<DeclaredFieldAnnotationDetails> getFieldAnnotations() {
		return Collections.unmodifiableList(fieldAnnotations);
	}
	
	public List<DeclaredMethodAnnotationDetails> getMethodAnnotations() {
		return Collections.unmodifiableList(methodAnnotations);
	}
	
	public List<ClassOrInterfaceTypeDetails> getInnerTypes() {
		return Collections.unmodifiableList(innerTypes);
	}
	
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("name", governor);
		tsc.append("aspect", aspect);
		tsc.append("physicalTypeCategory", physicalTypeCategory);
		tsc.append("privilegedAspect", privilegedAspect);
		tsc.append("registeredImports", registeredImports);
		tsc.append("declaredConstructors", declaredConstructors);
		tsc.append("declaredFields", declaredFields);
		tsc.append("declaredMethods", declaredMethods);
		tsc.append("extendsTypes", extendsTypes);
		tsc.append("fieldAnnotations", fieldAnnotations);
		tsc.append("methodAnnotations", methodAnnotations);
		tsc.append("typeAnnotations", typeAnnotations);
		tsc.append("innerTypes", innerTypes);
		return tsc.toString();
	}
}
