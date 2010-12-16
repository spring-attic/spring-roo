package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;
import java.util.*;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Default representation of a {@link ClassOrInterfaceTypeDetails}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class DefaultClassOrInterfaceTypeDetails extends AbstractIdentifiableAnnotatedJavaStructureProvider implements ClassOrInterfaceTypeDetails {
	private JavaType name;
	private PhysicalTypeCategory physicalTypeCategory;
	private List<ConstructorMetadata> declaredConstructors = new ArrayList<ConstructorMetadata>();
	private List<FieldMetadata> declaredFields = new ArrayList<FieldMetadata>();
	private List<MethodMetadata> declaredMethods = new ArrayList<MethodMetadata>();
    private List<ClassOrInterfaceTypeDetails> declaredInnerTypes = new ArrayList<ClassOrInterfaceTypeDetails>();
    private List<InitializerMetadata> declaredInitializers = new ArrayList<InitializerMetadata>();
	private ClassOrInterfaceTypeDetails superclass;
	private List<JavaType> extendsTypes = new ArrayList<JavaType>();
	private List<JavaType> implementsTypes = new ArrayList<JavaType>();
	private List<JavaSymbolName> enumConstants = new ArrayList<JavaSymbolName>();
    private Set<ImportMetadata> registeredImports = new HashSet<ImportMetadata>();
	
	// Package protected to mandate the use of ClassOrInterfaceTypeDetailsBuilder
    DefaultClassOrInterfaceTypeDetails(CustomData customData,
			String declaredByMetadataId,
			int modifier,
			List<AnnotationMetadata> annotations,
			JavaType name,
			PhysicalTypeCategory physicalTypeCategory,
			List<ConstructorMetadata> declaredConstructors,
			List<FieldMetadata> declaredFields,
			List<MethodMetadata> declaredMethods,
            List<ClassOrInterfaceTypeDetails> declaredInnerTypes,
            List<InitializerMetadata> declaredInitializers,
			ClassOrInterfaceTypeDetails superclass,
			List<JavaType> extendsTypes,
			List<JavaType> implementsTypes,
			List<JavaSymbolName> enumConstants,
            Set<ImportMetadata> registeredImports) {
		super(customData, declaredByMetadataId, modifier, annotations);
		Assert.notNull(name, "Name required");
		Assert.notNull(physicalTypeCategory, "Physical type category required");

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

        if (declaredInnerTypes != null) {
            this.declaredInnerTypes = declaredInnerTypes;
        }

        if (declaredInitializers != null) {
			this.declaredInitializers = declaredInitializers;
		}

		if (extendsTypes != null) {
			this.extendsTypes = extendsTypes;
		}

		if (implementsTypes != null) {
			this.implementsTypes = implementsTypes;
		}

		if (enumConstants != null && physicalTypeCategory == PhysicalTypeCategory.ENUMERATION) {
			this.enumConstants = enumConstants;
		}

        if (registeredImports != null) {
            this.registeredImports = registeredImports;
        }
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
	
	public List<JavaSymbolName> getEnumConstants() {
		return Collections.unmodifiableList(enumConstants);
	}

	public List<? extends FieldMetadata> getDeclaredFields() {
		return Collections.unmodifiableList(declaredFields);
	}
	
	public List<? extends MethodMetadata> getDeclaredMethods() {
		return Collections.unmodifiableList(declaredMethods);
	}

    public List<ClassOrInterfaceTypeDetails> getDeclaredInnerTypes() {
        return Collections.unmodifiableList(declaredInnerTypes);
    }

    public List<InitializerMetadata> getDeclaredInitializers() {
        return Collections.unmodifiableList(declaredInitializers);
    }
	
	public List<JavaType> getExtendsTypes() {
		return Collections.unmodifiableList(extendsTypes);
	}
	
	public List<JavaType> getImplementsTypes() {
		return Collections.unmodifiableList(implementsTypes);
	}
	
	public ClassOrInterfaceTypeDetails getSuperclass() {
		return superclass;
	}

    public Set<ImportMetadata> getRegisteredImports() {
        return Collections.unmodifiableSet(registeredImports);
    }
	
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("name", name);
		tsc.append("modifier", Modifier.toString(getModifier()));
		tsc.append("physicalTypeCategory", physicalTypeCategory);
		tsc.append("declaredByMetadataId", getDeclaredByMetadataId());
		tsc.append("declaredConstructors", declaredConstructors);
		tsc.append("declaredFields", declaredFields);
		tsc.append("declaredMethods", declaredMethods);
		tsc.append("enumConstants", enumConstants);
		tsc.append("superclass", superclass);
		tsc.append("extendsTypes", extendsTypes);
		tsc.append("implementsTypes", implementsTypes);
		tsc.append("annotations", getAnnotations());
		tsc.append("customData", getCustomData());
		return tsc.toString();
	}
}
