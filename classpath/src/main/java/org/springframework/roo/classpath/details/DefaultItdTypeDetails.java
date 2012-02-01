package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdSourceFileComposer;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.CustomDataAccessor;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Default representation of an {@link ItdTypeDetails}.
 * <p>
 * Provides a basic {@link #hashCode()} that is used for detecting significant
 * changes in {@link AbstractItdMetadataProvider} and avoiding downstream
 * notifications accordingly.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.0
 */
public class DefaultItdTypeDetails extends AbstractMemberHoldingTypeDetails
        implements ItdTypeDetails {

    static final PhysicalTypeCategory PHYSICAL_TYPE_CATEGORY = PhysicalTypeCategory.ITD;

    private final JavaType aspect;
    private final List<ConstructorMetadata> declaredConstructors = new ArrayList<ConstructorMetadata>();
    private final List<FieldMetadata> declaredFields = new ArrayList<FieldMetadata>();
    private final List<MethodMetadata> declaredMethods = new ArrayList<MethodMetadata>();
    private final List<JavaType> extendsTypes = new ArrayList<JavaType>();
    private final List<DeclaredFieldAnnotationDetails> fieldAnnotations = new ArrayList<DeclaredFieldAnnotationDetails>();
    private final ClassOrInterfaceTypeDetails governor;
    private final List<JavaType> implementsTypes = new ArrayList<JavaType>();
    private final List<ClassOrInterfaceTypeDetails> innerTypes = new ArrayList<ClassOrInterfaceTypeDetails>();
    private final List<DeclaredMethodAnnotationDetails> methodAnnotations = new ArrayList<DeclaredMethodAnnotationDetails>();
    private final boolean privilegedAspect;
    private final Set<JavaType> registeredImports = new HashSet<JavaType>();

    /**
     * Constructor (package protected to enforce the use of the corresponding
     * builder)
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
            final Collection<ClassOrInterfaceTypeDetails> innerTypes) {

        super(customData, declaredByMetadataId, modifier, typeAnnotations);
        Validate.notNull(aspect, "Aspect required");
        Validate.notNull(governor,
                "Governor (to receive the introductions) required");

        this.aspect = aspect;
        this.governor = governor;
        this.privilegedAspect = privilegedAspect;

        CollectionUtils.populate(this.declaredConstructors,
                declaredConstructors);
        CollectionUtils.populate(this.declaredFields, declaredFields);
        CollectionUtils.populate(this.declaredMethods, declaredMethods);
        CollectionUtils.populate(this.extendsTypes, extendsTypes);
        CollectionUtils.populate(this.fieldAnnotations, fieldAnnotations);
        CollectionUtils.populate(this.implementsTypes, implementsTypes);
        CollectionUtils.populate(this.innerTypes, innerTypes);
        CollectionUtils.populate(this.methodAnnotations, methodAnnotations);
        CollectionUtils.populate(this.registeredImports, registeredImports);
    }

    public boolean extendsType(final JavaType type) {
        return extendsTypes.contains(type);
    }

    public JavaType getAspect() {
        return aspect;
    }

    public List<? extends ConstructorMetadata> getDeclaredConstructors() {
        return Collections.unmodifiableList(declaredConstructors);
    }

    public List<FieldMetadata> getDeclaredFields() {
        return Collections.unmodifiableList(declaredFields);
    }

    public List<InitializerMetadata> getDeclaredInitializers() {
        return Collections.emptyList();
    }

    public List<ClassOrInterfaceTypeDetails> getDeclaredInnerTypes() {
        return Collections.emptyList();
    }

    public List<MethodMetadata> getDeclaredMethods() {
        return Collections.unmodifiableList(declaredMethods);
    }

    public List<String> getDynamicFinderNames() {
        return Collections.emptyList();
    }

    public List<JavaType> getExtendsTypes() {
        return Collections.unmodifiableList(extendsTypes);
    }

    public List<DeclaredFieldAnnotationDetails> getFieldAnnotations() {
        return Collections.unmodifiableList(fieldAnnotations);
    }

    public ClassOrInterfaceTypeDetails getGovernor() {
        return governor;
    }

    public List<JavaType> getImplementsTypes() {
        return Collections.unmodifiableList(implementsTypes);
    }

    public List<ClassOrInterfaceTypeDetails> getInnerTypes() {
        return Collections.unmodifiableList(innerTypes);
    }

    public List<DeclaredMethodAnnotationDetails> getMethodAnnotations() {
        return Collections.unmodifiableList(methodAnnotations);
    }

    public JavaType getName() {
        return getType();
    }

    public PhysicalTypeCategory getPhysicalTypeCategory() {
        return PHYSICAL_TYPE_CATEGORY;
    }

    public Set<JavaType> getRegisteredImports() {
        return Collections.unmodifiableSet(registeredImports);
    }

    public JavaType getType() {
        return governor.getType();
    }

    @Override
    public int hashCode() {
        int hash = aspect.hashCode() * governor.getName().hashCode()
                * governor.getModifier() * governor.getCustomData().hashCode()
                * PHYSICAL_TYPE_CATEGORY.hashCode()
                * (privilegedAspect ? 2 : 3);
        hash *= includeCustomDataHash(declaredConstructors);
        hash *= includeCustomDataHash(declaredFields);
        hash *= includeCustomDataHash(declaredMethods);
        hash *= new ItdSourceFileComposer(this).getOutput().hashCode();
        return hash;
    }

    public boolean implementsAny(final JavaType... types) {
        for (final JavaType type : types) {
            if (implementsTypes.contains(type)) {
                return true;
            }
        }
        return false;
    }

    private int includeCustomDataHash(
            final Collection<? extends CustomDataAccessor> coll) {
        int result = 1;
        for (final CustomDataAccessor accessor : coll) {
            result *= accessor.getCustomData().hashCode();
        }
        return result;
    }

    public boolean isPrivilegedAspect() {
        return privilegedAspect;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("declaredByMetadataId", getDeclaredByMetadataId());
        builder.append("modifier", getModifier());
        builder.append("name", governor);
        builder.append("aspect", aspect);
        builder.append("physicalTypeCategory", PHYSICAL_TYPE_CATEGORY);
        builder.append("privilegedAspect", privilegedAspect);
        builder.append("registeredImports", registeredImports);
        builder.append("declaredConstructors", declaredConstructors);
        builder.append("declaredFields", declaredFields);
        builder.append("declaredMethods", declaredMethods);
        builder.append("extendsTypes", extendsTypes);
        builder.append("fieldAnnotations", fieldAnnotations);
        builder.append("methodAnnotations", methodAnnotations);
        builder.append("typeAnnotations", getAnnotations());
        builder.append("innerTypes", innerTypes);
        builder.append("customData", getCustomData());
        return builder.toString();
    }
}
