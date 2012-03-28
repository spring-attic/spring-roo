package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Builder for {@link ClassOrInterfaceTypeDetails}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public class ClassOrInterfaceTypeDetailsBuilder extends
        AbstractMemberHoldingTypeDetailsBuilder<ClassOrInterfaceTypeDetails> {

    private final List<JavaSymbolName> enumConstants = new ArrayList<JavaSymbolName>();
    private JavaType name;
    private PhysicalTypeCategory physicalTypeCategory;
    private final Set<ImportMetadata> registeredImports = new HashSet<ImportMetadata>();
    private ClassOrInterfaceTypeDetailsBuilder superclass;

    /**
     * Constructor
     * 
     * @param existing
     */
    public ClassOrInterfaceTypeDetailsBuilder(
            final ClassOrInterfaceTypeDetails existing) {
        super(existing);
        init(existing);
    }

    /**
     * Constructor
     * 
     * @param declaredbyMetadataId
     */
    public ClassOrInterfaceTypeDetailsBuilder(final String declaredbyMetadataId) {
        super(declaredbyMetadataId);
    }

    /**
     * Constructor
     * 
     * @param declaredbyMetadataId
     * @param existing
     */
    public ClassOrInterfaceTypeDetailsBuilder(
            final String declaredbyMetadataId,
            final ClassOrInterfaceTypeDetails existing) {
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
    public ClassOrInterfaceTypeDetailsBuilder(
            final String declaredbyMetadataId, final int modifier,
            final JavaType name, final PhysicalTypeCategory physicalTypeCategory) {
        this(declaredbyMetadataId);
        setModifier(modifier);
        this.name = name;
        this.physicalTypeCategory = physicalTypeCategory;
    }

    /**
     * Adds the given imports to this builder, if not already present
     * 
     * @param imports the imports to add; can be <code>null</code> for none
     * @return <code>true</code> if the state of this builder changed
     */
    public boolean add(final Collection<ImportMetadata> imports) {
        if (imports == null) {
            return false;
        }
        return registeredImports.addAll(imports);
    }

    /**
     * Adds the given import to this builder
     * 
     * @param importMetadata the import to add; can be <code>null</code> not to
     *            add anything
     * @return <code>true</code> if the state of this builder changed
     */
    public boolean add(final ImportMetadata importMetadata) {
        if (importMetadata == null) {
            return false;
        }
        return registeredImports.add(importMetadata);
    }

    public boolean addEnumConstant(final JavaSymbolName javaSymbolName) {
        return enumConstants.add(javaSymbolName);
    }

    @Override
    public void addImports(final Collection<ImportMetadata> imports) {
        if (imports != null) {
            registeredImports.addAll(imports);
        }
    }

    public ClassOrInterfaceTypeDetails build() {
        ClassOrInterfaceTypeDetails superclass = null;
        if (this.superclass != null) {
            superclass = this.superclass.build();
        }
        return new DefaultClassOrInterfaceTypeDetails(getCustomData().build(),
                getDeclaredByMetadataId(), getModifier(), buildAnnotations(),
                getName(), getPhysicalTypeCategory(), buildConstructors(),
                buildFields(), buildMethods(), buildInnerTypes(),
                buildInitializers(), superclass, getExtendsTypes(),
                getImplementsTypes(), getEnumConstants(),
                getRegisteredImports());
    }

    /**
     * Copies this builder's modifications into the given ITD builder
     * 
     * @param targetBuilder the ITD builder to receive the additions (required)
     * @param governorTypeDetails the {@link ClassOrInterfaceTypeDetails} of the
     *            governor (required)
     */
    public void copyTo(
            final AbstractMemberHoldingTypeDetailsBuilder<?> targetBuilder,
            final ClassOrInterfaceTypeDetails governorTypeDetails) {
        Validate.notNull(targetBuilder, "Target builder required");
        Validate.notNull(governorTypeDetails,
                "Governor member holding types required");
        // Copy fields
        fieldAdditions: for (final FieldMetadataBuilder field : getDeclaredFields()) {
            for (final FieldMetadataBuilder targetField : targetBuilder
                    .getDeclaredFields()) {
                if (targetField.getFieldType().equals(field.getFieldType())
                        && targetField.getFieldName().equals(
                                field.getFieldName())) {
                    // The field already exists, so move on
                    continue fieldAdditions;
                }
            }
            if (!governorTypeDetails.declaresField(field.getFieldName())) {
                targetBuilder.addField(field);
            }
        }

        // Copy methods
        methodAdditions: for (final MethodMetadataBuilder method : getDeclaredMethods()) {
            for (final MethodMetadataBuilder targetMethod : targetBuilder
                    .getDeclaredMethods()) {
                if (targetMethod.getMethodName().equals(method.getMethodName())
                        && targetMethod.getParameterTypes().equals(
                                method.getParameterTypes())) {
                    continue methodAdditions;
                }
            }
            targetBuilder.addMethod(method);
        }

        // Copy annotations
        annotationAdditions: for (final AnnotationMetadataBuilder annotation : getAnnotations()) {
            for (final AnnotationMetadataBuilder targetAnnotation : targetBuilder
                    .getAnnotations()) {
                if (targetAnnotation.getAnnotationType().equals(
                        annotation.getAnnotationType())) {
                    continue annotationAdditions;
                }
            }
            targetBuilder.addAnnotation(annotation);
        }

        // Copy custom data
        if (getCustomData() != null) {
            targetBuilder.append(getCustomData().build());
        }

        // Copy constructors
        constructorAdditions: for (final ConstructorMetadataBuilder constructor : getDeclaredConstructors()) {
            for (final ConstructorMetadataBuilder targetConstructor : targetBuilder
                    .getDeclaredConstructors()) {
                if (targetConstructor.getParameterTypes().equals(
                        constructor.getParameterTypes())) {
                    continue constructorAdditions;
                }
            }
            targetBuilder.addConstructor(constructor);
        }

        // Copy initializers
        for (final InitializerMetadataBuilder initializer : getDeclaredInitializers()) {
            targetBuilder.addInitializer(initializer);
        }

        // Copy inner types
        innerTypeAdditions: for (final ClassOrInterfaceTypeDetailsBuilder innerType : getDeclaredInnerTypes()) {
            for (final ClassOrInterfaceTypeDetailsBuilder targetInnerType : targetBuilder
                    .getDeclaredInnerTypes()) {
                if (targetInnerType.getName().equals(innerType.getName())) {
                    continue innerTypeAdditions;
                }
            }
            targetBuilder.addInnerType(innerType);
        }

        // Copy extends types
        for (final JavaType type : getExtendsTypes()) {
            if (!targetBuilder.getExtendsTypes().contains(type)) {
                targetBuilder.addExtendsTypes(type);
            }
        }

        // Copy implements types
        for (final JavaType type : getImplementsTypes()) {
            if (!targetBuilder.getImplementsTypes().contains(type)) {
                targetBuilder.addImplementsType(type);
            }
        }

        // Copy imports
        targetBuilder.addImports(getRegisteredImports());
    }

    public List<JavaSymbolName> getEnumConstants() {
        return enumConstants;
    }

    public JavaType getName() {
        return name;
    }

    public PhysicalTypeCategory getPhysicalTypeCategory() {
        return physicalTypeCategory;
    }

    /**
     * Returns this builder's imports
     * 
     * @return a non-<code>null</code> copy
     */
    public Set<ImportMetadata> getRegisteredImports() {
        return new HashSet<ImportMetadata>(registeredImports);
    }

    public ClassOrInterfaceTypeDetailsBuilder getSuperclass() {
        return superclass;
    }

    private void init(final ClassOrInterfaceTypeDetails existing) {
        name = existing.getName();
        physicalTypeCategory = existing.getPhysicalTypeCategory();
        if (existing.getSuperclass() != null) {
            superclass = new ClassOrInterfaceTypeDetailsBuilder(
                    existing.getSuperclass());
        }
        enumConstants.addAll(existing.getEnumConstants());
        registeredImports.clear();
        registeredImports.addAll(existing.getRegisteredImports());
    }

    /**
     * Sets this builder's enum constants to the given collection
     * 
     * @param enumConstants can be <code>null</code> for none, otherwise is
     *            defensively copied
     */
    public void setEnumConstants(
            final Collection<? extends JavaSymbolName> enumConstants) {
        this.enumConstants.clear();
        if (enumConstants != null) {
            this.enumConstants.addAll(enumConstants);
        }
    }

    public void setName(final JavaType name) {
        this.name = name;
    }

    public void setPhysicalTypeCategory(
            final PhysicalTypeCategory physicalTypeCategory) {
        this.physicalTypeCategory = physicalTypeCategory;
    }

    /**
     * Sets this builder's imports
     * 
     * @param registeredImports can be <code>null</code> for none; defensively
     *            copied
     */
    public void setRegisteredImports(
            final Collection<ImportMetadata> registeredImports) {
        this.registeredImports.clear();
        if (registeredImports != null) {
            this.registeredImports.addAll(registeredImports);
        }
    }

    public void setSuperclass(final ClassOrInterfaceTypeDetails superclass) {
        setSuperclass(new ClassOrInterfaceTypeDetailsBuilder(superclass));
    }

    public void setSuperclass(
            final ClassOrInterfaceTypeDetailsBuilder superclass) {
        this.superclass = superclass;
    }
}
