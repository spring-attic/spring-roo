package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdSourceFileComposer;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.CustomDataAccessor;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Default representation of an {@link ItdTypeDetails}.
 * 
 * <p>
 * Provides a basic {@link #hashCode()} that is used for detecting significant changes in {@link AbstractItdMetadataProvider} and avoiding
 * downstream notifications accordingly.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class DefaultItdTypeDetails extends AbstractIdentifiableAnnotatedJavaStructureProvider implements ItdTypeDetails {
	
	private ClassOrInterfaceTypeDetails governor;
	private JavaType aspect;
	private boolean privilegedAspect;
	
	private PhysicalTypeCategory physicalTypeCategory = PhysicalTypeCategory.ITD;
	private List<ConstructorMetadata> declaredConstructors = new ArrayList<ConstructorMetadata>();
	private List<FieldMetadata> declaredFields = new ArrayList<FieldMetadata>();
	private List<MethodMetadata> declaredMethods = new ArrayList<MethodMetadata>();
    private List<ClassOrInterfaceTypeDetails> declaredInnerTypes = new ArrayList<ClassOrInterfaceTypeDetails>();
    private List<InitializerMetadata> declaredInitializers = new ArrayList<InitializerMetadata>();
	private List<JavaType> extendsTypes = new ArrayList<JavaType>();
	private List<JavaType> implementsTypes = new ArrayList<JavaType>();
	private Set<JavaType> registeredImports = new HashSet<JavaType>();
	private List<DeclaredFieldAnnotationDetails> fieldAnnotations = new ArrayList<DeclaredFieldAnnotationDetails>();
	private List<DeclaredMethodAnnotationDetails> methodAnnotations = new ArrayList<DeclaredMethodAnnotationDetails>();
	private List<ClassOrInterfaceTypeDetails> innerTypes = new ArrayList<ClassOrInterfaceTypeDetails>();
	
	// package protected to force the use of the corresponding builder
	DefaultItdTypeDetails(CustomData customData, String declaredByMetadataId, int modifier, ClassOrInterfaceTypeDetails governor, JavaType aspect,
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
		super(customData, declaredByMetadataId, modifier, typeAnnotations);
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

    public List<ClassOrInterfaceTypeDetails> getDeclaredInnerTypes() {
        return Collections.unmodifiableList(declaredInnerTypes);
    }

    public List<InitializerMetadata> getDeclaredInitializers() {
        return Collections.unmodifiableList(declaredInitializers);
    }
	
	public List<JavaType> getExtendsTypes() {
		return Collections.unmodifiableList(extendsTypes);
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
	
	@Override
	public int hashCode() {
		int hash = aspect.hashCode() * governor.getName().hashCode() * governor.getModifier() * governor.getCustomData().hashCode() * physicalTypeCategory.hashCode() * (privilegedAspect ? 2 : 3);
		hash = hash * includeCustomDataHash(declaredConstructors);
		hash = hash * includeCustomDataHash(declaredFields);
		hash = hash * includeCustomDataHash(declaredMethods);
		hash = hash * new ItdSourceFileComposer(this).getOutput().hashCode();
		return hash;
	}
	
	private int includeCustomDataHash(Collection<? extends CustomDataAccessor> coll) {
		int result = 1;
		for (CustomDataAccessor accessor : coll) {
			result = result * accessor.getCustomData().hashCode();
		}
		return result;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("declaredByMetadataId", getDeclaredByMetadataId());
		tsc.append("modifier", getModifier());
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
		tsc.append("typeAnnotations", getAnnotations());
		tsc.append("innerTypes", innerTypes);
		tsc.append("customData", getCustomData());
		return tsc.toString();
	}
}
