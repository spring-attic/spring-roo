package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Default representation of a {@link ClassOrInterfaceTypeDetails}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class DefaultClassOrInterfaceTypeDetails implements ClassOrInterfaceTypeDetails {
	
	private JavaType name;
	private PhysicalTypeCategory physicalTypeCategory;
	private List<ConstructorMetadata> declaredConstructors = new ArrayList<ConstructorMetadata>();
	private List<FieldMetadata> declaredFields = new ArrayList<FieldMetadata>();
	private List<MethodMetadata> declaredMethods = new ArrayList<MethodMetadata>();
	private ClassOrInterfaceTypeDetails superclass;
	private List<JavaType> extendsTypes = new ArrayList<JavaType>();
	private List<JavaType> implementsTypes = new ArrayList<JavaType>();
	private List<AnnotationMetadata> typeAnnotations = new ArrayList<AnnotationMetadata>();
	private int modifier;
	private String declaredByMetadataId;
	
	public DefaultClassOrInterfaceTypeDetails(String declaredByMetadataId, JavaType name, int modifier, PhysicalTypeCategory physicalTypeCategory, List<AnnotationMetadata> typeAnnotations) {
		Assert.hasText(declaredByMetadataId, "Declared by metadata ID required");
		Assert.notNull(name, "Name required");
		Assert.notNull(physicalTypeCategory, "Physical type category required");

		this.declaredByMetadataId = declaredByMetadataId;
		this.modifier = modifier;
		this.name = name;
		this.physicalTypeCategory = physicalTypeCategory;

		if (typeAnnotations != null) {
			this.typeAnnotations = typeAnnotations;
		}
	}

	public DefaultClassOrInterfaceTypeDetails(String declaredByMetadataId, JavaType name, int modifier,
			PhysicalTypeCategory physicalTypeCategory,
			List<ConstructorMetadata> declaredConstructors,
			List<FieldMetadata> declaredFields,
			List<MethodMetadata> declaredMethods,
			ClassOrInterfaceTypeDetails superclass,
			List<JavaType> extendsTypes,
			List<JavaType> implementsTypes,
			List<AnnotationMetadata> typeAnnotations) {
		Assert.hasText(declaredByMetadataId, "Declared by metadata ID required");
		Assert.notNull(name, "Name required");
		Assert.notNull(physicalTypeCategory, "Physical type category required");

		this.declaredByMetadataId = declaredByMetadataId;
		this.modifier = modifier;
		this.name = name;
		this.physicalTypeCategory = physicalTypeCategory;
		this.superclass = superclass;
		
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

	public String getDeclaredByMetadataId() {
		return declaredByMetadataId;
	}

	public int getModifier() {
		return modifier;
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
	
	public ClassOrInterfaceTypeDetails getSuperclass() {
		return superclass;
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

}
