package org.springframework.roo.classpath.details;

import static org.springframework.roo.classpath.PhysicalTypeCategory.CLASS;
import static org.springframework.roo.classpath.PhysicalTypeCategory.ENUMERATION;
import static org.springframework.roo.classpath.PhysicalTypeCategory.INTERFACE;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Default representation of a {@link ClassOrInterfaceTypeDetails}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class DefaultClassOrInterfaceTypeDetails extends
        AbstractMemberHoldingTypeDetails implements ClassOrInterfaceTypeDetails {

    private List<ConstructorMetadata> declaredConstructors = new ArrayList<ConstructorMetadata>();
    private List<FieldMetadata> declaredFields = new ArrayList<FieldMetadata>();
    private List<InitializerMetadata> declaredInitializers = new ArrayList<InitializerMetadata>();
    private List<ClassOrInterfaceTypeDetails> declaredInnerTypes = new ArrayList<ClassOrInterfaceTypeDetails>();
    private List<MethodMetadata> declaredMethods = new ArrayList<MethodMetadata>();
    private List<JavaSymbolName> enumConstants = new ArrayList<JavaSymbolName>();
    private List<JavaType> extendsTypes = new ArrayList<JavaType>();
    private List<JavaType> implementsTypes = new ArrayList<JavaType>();
    private final JavaType name;
    private final PhysicalTypeCategory physicalTypeCategory;
    private Set<ImportMetadata> registeredImports = new HashSet<ImportMetadata>();
    private final ClassOrInterfaceTypeDetails superclass;

    /**
     * Constructor is package protected to mandate the use of
     * {@link ClassOrInterfaceTypeDetailsBuilder}
     * 
     * @param customData
     * @param declaredByMetadataId
     * @param modifier
     * @param annotations
     * @param name
     * @param physicalTypeCategory
     * @param declaredConstructors
     * @param declaredFields
     * @param declaredMethods
     * @param declaredInnerTypes
     * @param declaredInitializers
     * @param superclass
     * @param extendsTypes
     * @param implementsTypes
     * @param enumConstants
     * @param registeredImports
     */
    DefaultClassOrInterfaceTypeDetails(final CustomData customData,
            final String declaredByMetadataId, final int modifier,
            final List<AnnotationMetadata> annotations, final JavaType name,
            final PhysicalTypeCategory physicalTypeCategory,
            final List<ConstructorMetadata> declaredConstructors,
            final List<FieldMetadata> declaredFields,
            final List<MethodMetadata> declaredMethods,
            final List<ClassOrInterfaceTypeDetails> declaredInnerTypes,
            final List<InitializerMetadata> declaredInitializers,
            final ClassOrInterfaceTypeDetails superclass,
            final List<JavaType> extendsTypes,
            final List<JavaType> implementsTypes,
            final List<JavaSymbolName> enumConstants,
            final Collection<ImportMetadata> registeredImports) {

        super(customData, declaredByMetadataId, modifier, annotations);
        Validate.notNull(name, "Name required");
        Validate.notNull(physicalTypeCategory,
                "Physical type category required");

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

        if (enumConstants != null && physicalTypeCategory == ENUMERATION) {
            this.enumConstants = enumConstants;
        }

        this.registeredImports = new HashSet<ImportMetadata>();
        if (registeredImports != null) {
            this.registeredImports.addAll(registeredImports);
        }
    }

    public boolean declaresField(final JavaSymbolName fieldName) {
        return getDeclaredField(fieldName) != null;
    }

    public boolean extendsType(final JavaType type) {
        return extendsTypes.contains(type);
    }

    public List<? extends ConstructorMetadata> getDeclaredConstructors() {
        return Collections.unmodifiableList(declaredConstructors);
    }

    public List<? extends FieldMetadata> getDeclaredFields() {
        return Collections.unmodifiableList(declaredFields);
    }

    public List<InitializerMetadata> getDeclaredInitializers() {
        return Collections.unmodifiableList(declaredInitializers);
    }

    public List<ClassOrInterfaceTypeDetails> getDeclaredInnerTypes() {
        return Collections.unmodifiableList(declaredInnerTypes);
    }

    public List<? extends MethodMetadata> getDeclaredMethods() {
        return Collections.unmodifiableList(declaredMethods);
    }

    @SuppressWarnings("unchecked")
    public List<String> getDynamicFinderNames() {
        final List<String> dynamicFinders = new ArrayList<String>();
        final Object finders = getCustomData().get(
                CustomDataKeys.DYNAMIC_FINDER_NAMES);
        if (finders instanceof Collection) {
            dynamicFinders.addAll((Collection<String>) finders);
        }
        return dynamicFinders;
    }

    public List<JavaSymbolName> getEnumConstants() {
        return Collections.unmodifiableList(enumConstants);
    }

    public List<JavaType> getExtendsTypes() {
        return Collections.unmodifiableList(extendsTypes);
    }

    public List<JavaType> getImplementsTypes() {
        return Collections.unmodifiableList(implementsTypes);
    }

    public JavaType getName() {
        return getType();
    }

    public PhysicalTypeCategory getPhysicalTypeCategory() {
        return physicalTypeCategory;
    }

    public Set<ImportMetadata> getRegisteredImports() {
        return Collections.unmodifiableSet(registeredImports);
    }

    public ClassOrInterfaceTypeDetails getSuperclass() {
        return superclass;
    }

    public JavaType getType() {
        return name;
    }

    public boolean implementsAny(final JavaType... types) {
        for (final JavaType type : types) {
            if (implementsTypes.contains(type)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAbstract() {
        return physicalTypeCategory == INTERFACE
                || physicalTypeCategory == CLASS
                && Modifier.isAbstract(getModifier());
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("name", name);
        builder.append("modifier", Modifier.toString(getModifier()));
        builder.append("physicalTypeCategory", physicalTypeCategory);
        builder.append("declaredByMetadataId", getDeclaredByMetadataId());
        builder.append("declaredConstructors", declaredConstructors);
        builder.append("declaredFields", declaredFields);
        builder.append("declaredMethods", declaredMethods);
        builder.append("enumConstants", enumConstants);
        builder.append("superclass", superclass);
        builder.append("extendsTypes", extendsTypes);
        builder.append("implementsTypes", implementsTypes);
        builder.append("annotations", getAnnotations());
        builder.append("customData", getCustomData());
        return builder.toString();
    }
}
