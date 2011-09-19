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
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Default representation of an {@link ItdTypeDetails}.
 * 
 * <p>
 * Provides a basic {@link #hashCode()} that is used for detecting significant
 * changes in {@link AbstractItdMetadataProvider} and avoiding downstream
 * notifications accordingly.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.0
 */
public class DefaultItdTypeDetails extends AbstractMemberHoldingTypeDetails implements ItdTypeDetails {

	// Constants
	static final PhysicalTypeCategory PHYSICAL_TYPE_CATEGORY = PhysicalTypeCategory.ITD;
	
	// Fields
	private final boolean privilegedAspect;
	private final ClassOrInterfaceTypeDetails governor;
	private final JavaType aspect;
	private final List<ClassOrInterfaceTypeDetails> innerTypes = new ArrayList<ClassOrInterfaceTypeDetails>();
	private final List<ConstructorMetadata> declaredConstructors = new ArrayList<ConstructorMetadata>();
	private final List<DeclaredFieldAnnotationDetails> fieldAnnotations = new ArrayList<DeclaredFieldAnnotationDetails>();
	private final List<DeclaredMethodAnnotationDetails> methodAnnotations = new ArrayList<DeclaredMethodAnnotationDetails>();
	private final List<FieldMetadata> declaredFields = new ArrayList<FieldMetadata>();
	private final List<JavaType> extendsTypes = new ArrayList<JavaType>();
	private final List<JavaType> implementsTypes = new ArrayList<JavaType>();
	private final List<MethodMetadata> declaredMethods = new ArrayList<MethodMetadata>();
	private final Set<JavaType> registeredImports = new HashSet<JavaType>();
	
	/**
	 * Constructor (package protected to enforce the use of the corresponding builder)
	 *
	 * @param customData
	 * @param declaredByMetadataId
	 * @param modifier
	 * @param governor the type to receive the introductions (required)
	 * @param aspect (required)
	 * @param privilegedAspect
	 * @param registeredImports can be <code>null</code>
	 * @param declaredConstructors can be <code>null</code>
	 * @param declaredFields can be <code>null</code>
	 * @param declaredMethods can be <code>null</code>
	 * @param extendsTypes can be <code>null</code>
	 * @param implementsTypes can be <code>null</code>
	 * @param typeAnnotations can be <code>null</code>
	 * @param fieldAnnotations can be <code>null</code>
	 * @param methodAnnotations can be <code>null</code>
	 * @param innerTypes can be <code>null</code>
	 */
	DefaultItdTypeDetails(
		final CustomData customData,
		final String declaredByMetadataId,
		final int modifier,
		final ClassOrInterfaceTypeDetails governor,
		final JavaType aspect,
		final boolean privilegedAspect,
		final Collection<? extends JavaType> registeredImports,
		final Collection<ConstructorMetadata> declaredConstructors,
		final Collection<FieldMetadata> declaredFields,
		final Collection<MethodMetadata> declaredMethods,
		final Collection<? extends JavaType> extendsTypes,
		final Collection<? extends JavaType> implementsTypes,
		final Collection<AnnotationMetadata> typeAnnotations,
		final Collection<? extends DeclaredFieldAnnotationDetails> fieldAnnotations,
		final Collection<? extends DeclaredMethodAnnotationDetails> methodAnnotations,
		final Collection<ClassOrInterfaceTypeDetails> innerTypes)
	{
		super(customData, declaredByMetadataId, modifier, typeAnnotations);
		Assert.notNull(aspect, "Aspect required");
		Assert.notNull(governor, "Governor (to receive the introductions) required");
		
		this.aspect = aspect;
		this.governor = governor;
		this.privilegedAspect = privilegedAspect;
		
		CollectionUtils.populate(this.declaredConstructors, declaredConstructors);
		CollectionUtils.populate(this.declaredFields, declaredFields);
		CollectionUtils.populate(this.declaredMethods, declaredMethods);
		CollectionUtils.populate(this.extendsTypes, extendsTypes);
		CollectionUtils.populate(this.fieldAnnotations, fieldAnnotations);
		CollectionUtils.populate(this.implementsTypes, implementsTypes);
		CollectionUtils.populate(this.innerTypes, innerTypes);
		CollectionUtils.populate(this.methodAnnotations, methodAnnotations);
		CollectionUtils.populate(this.registeredImports, registeredImports);
	}
	
	public Set<JavaType> getRegisteredImports() {
		return Collections.unmodifiableSet(registeredImports);
	}

	public List<JavaType> getImplementsTypes() {
		return Collections.unmodifiableList(implementsTypes);
	}

	public PhysicalTypeCategory getPhysicalTypeCategory() {
		return PHYSICAL_TYPE_CATEGORY;
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
	
	public List<FieldMetadata> getDeclaredFields() {
		return Collections.unmodifiableList(declaredFields);
	}
	
	public List<MethodMetadata> getDeclaredMethods() {
		return Collections.unmodifiableList(declaredMethods);
	}

	public List<ClassOrInterfaceTypeDetails> getDeclaredInnerTypes() {
		return Collections.emptyList();
	}

	public List<InitializerMetadata> getDeclaredInitializers() {
		return Collections.emptyList();
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
		int hash = aspect.hashCode() * governor.getName().hashCode() * governor.getModifier() * governor.getCustomData().hashCode() * PHYSICAL_TYPE_CATEGORY.hashCode() * (privilegedAspect ? 2 : 3);
		hash *= includeCustomDataHash(declaredConstructors);
		hash *= includeCustomDataHash(declaredFields);
		hash *= includeCustomDataHash(declaredMethods);
		hash *= new ItdSourceFileComposer(this).getOutput().hashCode();
		return hash;
	}
	
	private int includeCustomDataHash(final Collection<? extends CustomDataAccessor> coll) {
		int result = 1;
		for (final CustomDataAccessor accessor : coll) {
			result = result * accessor.getCustomData().hashCode();
		}
		return result;
	}

	@Override
	public String toString() {
		final ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("declaredByMetadataId", getDeclaredByMetadataId());
		tsc.append("modifier", getModifier());
		tsc.append("name", governor);
		tsc.append("aspect", aspect);
		tsc.append("physicalTypeCategory", PHYSICAL_TYPE_CATEGORY);
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

	public boolean extendsType(final JavaType type) {
		return this.extendsTypes.contains(type);
	}
}
