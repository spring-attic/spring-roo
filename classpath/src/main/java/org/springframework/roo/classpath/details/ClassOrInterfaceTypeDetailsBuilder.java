package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.CustomDataBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Builder for {@link ClassOrInterfaceTypeDetails}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public class ClassOrInterfaceTypeDetailsBuilder extends AbstractMemberHoldingTypeDetailsBuilder<ClassOrInterfaceTypeDetails> {
	
	// Fields
	private JavaType name;
	private PhysicalTypeCategory physicalTypeCategory;
	private ClassOrInterfaceTypeDetailsBuilder superclass;
	private List<JavaSymbolName> enumConstants = new ArrayList<JavaSymbolName>();
	private Set<ImportMetadata> registeredImports = new HashSet<ImportMetadata>();

	/**
	 * Constructor
	 *
	 * @param declaredbyMetadataId
	 */
	public ClassOrInterfaceTypeDetailsBuilder(String declaredbyMetadataId) {
		super(declaredbyMetadataId);
	}

	/**
	 * Constructor
	 *
	 * @param existing
	 */
	public ClassOrInterfaceTypeDetailsBuilder(ClassOrInterfaceTypeDetails existing) {
		super(existing);
		init(existing);
	}

	/**
	 * Constructor
	 *
	 * @param declaredbyMetadataId
	 * @param existing
	 */
	public ClassOrInterfaceTypeDetailsBuilder(String declaredbyMetadataId, ClassOrInterfaceTypeDetails existing) {
		super(declaredbyMetadataId, existing);
		init(existing);
	}

	/**
	 * Constructor
	 *
	 * @param declaredbyMetadataId
	 * @param modifier
	 * @param name
	 * @param physicalTypeCategory
	 */
	public ClassOrInterfaceTypeDetailsBuilder(String declaredbyMetadataId, int modifier, JavaType name, PhysicalTypeCategory physicalTypeCategory) {
		this(declaredbyMetadataId);
		setModifier(modifier);
		this.name = name;
		this.physicalTypeCategory = physicalTypeCategory;
	}
	
	private void init(ClassOrInterfaceTypeDetails existing) {
		this.name = existing.getName();
		this.physicalTypeCategory = existing.getPhysicalTypeCategory();
		if (existing.getSuperclass() != null) {
			superclass = new ClassOrInterfaceTypeDetailsBuilder(existing.getSuperclass());
		}
		enumConstants.addAll(existing.getEnumConstants());
		registeredImports = existing.getRegisteredImports();
	}

	public JavaType getName() {
		return name;
	}

	public void setName(JavaType name) {
		this.name = name;
	}

	public PhysicalTypeCategory getPhysicalTypeCategory() {
		return physicalTypeCategory;
	}

	public void setPhysicalTypeCategory(PhysicalTypeCategory physicalTypeCategory) {
		this.physicalTypeCategory = physicalTypeCategory;
	}

	public ClassOrInterfaceTypeDetailsBuilder getSuperclass() {
		return superclass;
	}

	public void setSuperclass(ClassOrInterfaceTypeDetailsBuilder superclass) {
		this.superclass = superclass;
	}

	public List<JavaSymbolName> getEnumConstants() {
		return enumConstants;
	}

	public void setEnumConstants(List<JavaSymbolName> enumConstants) {
		this.enumConstants = enumConstants;
	}

	public boolean addEnumConstant(JavaSymbolName javaSymbolName) {
		return enumConstants.add(javaSymbolName);
	}

	public ClassOrInterfaceTypeDetails build() {
		ClassOrInterfaceTypeDetails superclass = null;
		if (this.superclass != null) {
			superclass = this.superclass.build();
		}
		return new DefaultClassOrInterfaceTypeDetails(getCustomData().build(), getDeclaredByMetadataId(), getModifier(), buildAnnotations(), getName(), getPhysicalTypeCategory(), buildConstructors(), buildFields(), buildMethods(), buildInnerTypes(), buildInitializers(), superclass, getExtendsTypes(), getImplementsTypes(), getEnumConstants(), getRegisteredImports());
	}

	public Set<ImportMetadata> getRegisteredImports() {
		return registeredImports;
	}

	public void setRegisteredImports(Set<ImportMetadata> registeredImports) {
		this.registeredImports = registeredImports;
	}
	
	/**
	 * Copies this builder's modifications into the given ITD builder
	 * 
	 * @param targetBuilder the ITD builder to receive the additions (required)
	 */
	public void copyTo(final AbstractMemberHoldingTypeDetailsBuilder<?> targetBuilder) {
		
		// Copy fields
		for (FieldMetadataBuilder field : getDeclaredFields()) {
			boolean fieldExists = false;
			for (FieldMetadataBuilder targetField : targetBuilder.getDeclaredFields()) {
				if (targetField.getFieldType().equals(field.getFieldType()) && targetField.getFieldName().equals(field.getFieldName())) {
					fieldExists = true;
				}
			}
			if (!fieldExists) {
				targetBuilder.addField(field.build());
			}
		}
		
		// Copy methods
		for (MethodMetadataBuilder method : getDeclaredMethods()) {
			targetBuilder.addMethod(method);
		}
		
		// Copy annotations
		for (AnnotationMetadataBuilder annotation : getAnnotations()) {
			targetBuilder.addAnnotation(annotation);
		}
		
		// Copy custom data
		if (getCustomData() != null) {
			CustomDataBuilder customDataBuilder = new CustomDataBuilder(getCustomData().build());
			customDataBuilder.append(targetBuilder.getCustomData().build());
			targetBuilder.setCustomData(customDataBuilder);
		}
		
		// Copy constructors
		for (ConstructorMetadataBuilder constructor : getDeclaredConstructors()) {
			targetBuilder.addConstructor(constructor);
		}
		
		// Copy initializers
		for (InitializerMetadataBuilder initializer : getDeclaredInitializers()) {
			targetBuilder.addInitializer(initializer);
		}
		
		// Copy inner types
		for (ClassOrInterfaceTypeDetailsBuilder innerType : getDeclaredInnerTypes()) {
			targetBuilder.addInnerType(innerType);
		}
		
		// Copy extends types
		for (JavaType type : getExtendsTypes()) {
			targetBuilder.addExtendsTypes(type);
		}
		
		// Copy implements types
		for (JavaType type : getImplementsTypes()) {
			targetBuilder.addImplementsType(type);
		}
	}
}
