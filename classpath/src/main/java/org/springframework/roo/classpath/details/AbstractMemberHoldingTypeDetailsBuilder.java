package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.roo.model.Builder;
import org.springframework.roo.model.JavaType;

/**
 * Abstract {@link Builder} to assist building {@link MemberHoldingTypeDetails}
 * implementations.
 * 
 * @author Ben Alex
 * @since 1.1
 * @param <T> the type of {@link MemberHoldingTypeDetails} being built
 */
public abstract class AbstractMemberHoldingTypeDetailsBuilder<T extends MemberHoldingTypeDetails>
        extends AbstractIdentifiableAnnotatedJavaStructureBuilder<T> {

    private final List<ConstructorMetadataBuilder> declaredConstructors = new ArrayList<ConstructorMetadataBuilder>();
    private final List<FieldMetadataBuilder> declaredFields = new ArrayList<FieldMetadataBuilder>();
    private final List<InitializerMetadataBuilder> declaredInitializers = new ArrayList<InitializerMetadataBuilder>();
    private final List<ClassOrInterfaceTypeDetailsBuilder> declaredInnerTypes = new ArrayList<ClassOrInterfaceTypeDetailsBuilder>();
    private final List<MethodMetadataBuilder> declaredMethods = new ArrayList<MethodMetadataBuilder>();
    private final List<JavaType> extendsTypes = new ArrayList<JavaType>();
    private final List<JavaType> implementsTypes = new ArrayList<JavaType>();

    /**
     * Constructor
     * 
     * @param existing
     */
    protected AbstractMemberHoldingTypeDetailsBuilder(
            final MemberHoldingTypeDetails existing) {
        super(existing);
        init(existing);
    }

    /**
     * Constructor
     * 
     * @param declaredbyMetadataId
     */
    protected AbstractMemberHoldingTypeDetailsBuilder(
            final String declaredbyMetadataId) {
        super(declaredbyMetadataId);
    }

    /**
     * Constructor
     * 
     * @param declaredbyMetadataId
     * @param existing
     */
    protected AbstractMemberHoldingTypeDetailsBuilder(
            final String declaredbyMetadataId,
            final MemberHoldingTypeDetails existing) {
        super(declaredbyMetadataId, existing);
        init(existing);
    }

    public final boolean addConstructor(final ConstructorMetadata constructor) {
        if (constructor == null) {
            return false;
        }
        return addConstructor(new ConstructorMetadataBuilder(constructor));
    }

    public final boolean addConstructor(
            final ConstructorMetadataBuilder constructorBuilder) {
        if (constructorBuilder == null
                || !getDeclaredByMetadataId().equals(
                        constructorBuilder.getDeclaredByMetadataId())) {
            return false;
        }
        onAddConstructor(constructorBuilder);
        return declaredConstructors.add(constructorBuilder);
    }

    public final boolean addExtendsTypes(final JavaType extendsType) {
        if (extendsType == null) {
            return false;
        }
        onAddExtendsTypes(extendsType);
        return extendsTypes.add(extendsType);
    }

    public final boolean addField(final FieldMetadata field) {
        if (field == null) {
            return false;
        }
        return addField(new FieldMetadataBuilder(field));
    }

    public final boolean addField(final FieldMetadataBuilder fieldBuilder) {
        if (fieldBuilder == null
                || !getDeclaredByMetadataId().equals(
                        fieldBuilder.getDeclaredByMetadataId())) {
            return false;
        }
        onAddField(fieldBuilder);
        return declaredFields.add(fieldBuilder);
    }

    public final boolean addImplementsType(final JavaType implementsType) {
        if (implementsType == null) {
            return false;
        }
        onAddImplementType(implementsType);
        return implementsTypes.add(implementsType);
    }

    /**
     * Adds the given imports to this builder if not already present
     * 
     * @param imports the imports to add; can be <code>null</code>
     * @since 1.2.0
     */
    public abstract void addImports(Collection<ImportMetadata> imports);

    public final boolean addInitializer(
            final InitializerMetadataBuilder initializer) {
        if (initializer == null
                || !getDeclaredByMetadataId().equals(
                        initializer.getDeclaredByMetadataId())) {
            return false;
        }
        onAddInitializer(initializer);
        return declaredInitializers.add(initializer);
    }

    public final boolean addInnerType(
            final ClassOrInterfaceTypeDetails innerType) {
        if (innerType == null) {
            return false;
        }
        return addInnerType(new ClassOrInterfaceTypeDetailsBuilder(innerType));
    }

    public final boolean addInnerType(
            final ClassOrInterfaceTypeDetailsBuilder innerType) {
        /*
         * There was originally a check to see if the declaredMIDs matched, but
         * this doesn't really make much sense. We need to come up with a better
         * model for inner types. I thought about adding an enclosingType
         * attribute but this prototype just felt like a hack. In the short term
         * I have just disabled the MID comparison as I think this is fairly
         * reasonable until this is given some more time. JTT - 24/08/11
         */
        if (innerType == null) {
            return false;
        }
        onAddInnerType(innerType);
        return declaredInnerTypes.add(innerType);
    }

    /**
     * Adds the given method to this builder
     * 
     * @param method the method to add; can be <code>null</code>
     * @return <code>true</code> if the state of this builder changed
     */
    public final boolean addMethod(final MethodMetadata method) {
        if (method == null) {
            return false;
        }
        return addMethod(new MethodMetadataBuilder(method));
    }

    /**
     * Adds the given method to this builder
     * 
     * @param methodBuilder the method builder to add; ignored if
     *            <code>null</code> or if its MID doesn't match this builder's
     *            MID
     * @return true if the state of this builder changed
     */
    public final boolean addMethod(final MethodMetadataBuilder methodBuilder) {
        if (methodBuilder == null
                || !getDeclaredByMetadataId().equals(
                        methodBuilder.getDeclaredByMetadataId())) {
            return false;
        }
        onAddMethod(methodBuilder);
        return declaredMethods.add(methodBuilder);
    }

    public final List<ConstructorMetadata> buildConstructors() {
        final List<ConstructorMetadata> result = new ArrayList<ConstructorMetadata>();
        for (final ConstructorMetadataBuilder builder : declaredConstructors) {
            result.add(builder.build());
        }
        return result;
    }

    public final List<FieldMetadata> buildFields() {
        final List<FieldMetadata> result = new ArrayList<FieldMetadata>();
        for (final FieldMetadataBuilder builder : declaredFields) {
            result.add(builder.build());
        }
        return result;
    }

    public final List<InitializerMetadata> buildInitializers() {
        final List<InitializerMetadata> result = new ArrayList<InitializerMetadata>();
        for (final InitializerMetadataBuilder builder : declaredInitializers) {
            result.add(builder.build());
        }
        return result;
    }

    public final List<ClassOrInterfaceTypeDetails> buildInnerTypes() {
        final List<ClassOrInterfaceTypeDetails> result = new ArrayList<ClassOrInterfaceTypeDetails>();
        for (final ClassOrInterfaceTypeDetailsBuilder cidBuilder : declaredInnerTypes) {
            result.add(cidBuilder.build());
        }
        return result;
    }

    public final List<MethodMetadata> buildMethods() {
        final List<MethodMetadata> result = new ArrayList<MethodMetadata>();
        for (final MethodMetadataBuilder builder : declaredMethods) {
            result.add(builder.build());
        }
        return result;
    }

    /**
     * Removes all declared methods from this builder
     */
    public void clearDeclaredMethods() {
        this.declaredMethods.clear();
    }

    public final List<ConstructorMetadataBuilder> getDeclaredConstructors() {
        return declaredConstructors;
    }

    public final List<FieldMetadataBuilder> getDeclaredFields() {
        return declaredFields;
    }

    public List<InitializerMetadataBuilder> getDeclaredInitializers() {
        return declaredInitializers;
    }

    public List<ClassOrInterfaceTypeDetailsBuilder> getDeclaredInnerTypes() {
        return declaredInnerTypes;
    }

    /**
     * Returns the declared methods in this builder
     * 
     * @return an unmodifiable copy of this list
     */
    public final List<MethodMetadataBuilder> getDeclaredMethods() {
        return Collections.unmodifiableList(declaredMethods);
    }

    /**
     * Returns the types that the built instance will extend, if any. Does not
     * return a copy, i.e. modifying the returned list will modify this builder!
     * TODO improve encapsulation by returning a defensive copy and
     * <em>updating callers accordingly</em>
     * 
     * @return a non-<code>null</code> list
     */
    public final List<JavaType> getExtendsTypes() {
        return extendsTypes;
    }

    public final List<JavaType> getImplementsTypes() {
        return implementsTypes;
    }

    private void init(final MemberHoldingTypeDetails existing) {
        for (final ConstructorMetadata element : existing
                .getDeclaredConstructors()) {
            declaredConstructors.add(new ConstructorMetadataBuilder(element));
        }
        for (final FieldMetadata element : existing.getDeclaredFields()) {
            declaredFields.add(new FieldMetadataBuilder(element));
        }
        for (final MethodMetadata element : existing.getDeclaredMethods()) {
            declaredMethods.add(new MethodMetadataBuilder(element));
        }
        for (final ClassOrInterfaceTypeDetails element : existing
                .getDeclaredInnerTypes()) {
            declaredInnerTypes.add(new ClassOrInterfaceTypeDetailsBuilder(
                    element));
        }
        for (final InitializerMetadata element : existing
                .getDeclaredInitializers()) {
            declaredInitializers.add(new InitializerMetadataBuilder(element));
        }
        extendsTypes.addAll(existing.getExtendsTypes());
        implementsTypes.addAll(existing.getImplementsTypes());
    }

    protected void onAddConstructor(
            final ConstructorMetadataBuilder constructorBuilder) {
    }

    protected void onAddExtendsTypes(final JavaType extendsType) {
    }

    protected void onAddField(final FieldMetadataBuilder fieldBuilder) {
    }

    protected void onAddImplementType(final JavaType implementsType) {
    }

    protected void onAddInitializer(final InitializerMetadataBuilder initializer) {
    }

    protected void onAddInnerType(
            final ClassOrInterfaceTypeDetailsBuilder innerType) {
    }

    /**
     * Subclasses can perform their own actions upon a method builder being
     * added. This implementation does nothing.
     * 
     * @param methodBuilder the method being added; never <code>null</code>
     */
    protected void onAddMethod(final MethodMetadataBuilder methodBuilder) {
    }

    /**
     * Removes the given methods from this builder
     * 
     * @param methodsToRemove can be <code>null</code> for none
     * @return true if this builder changed as a result
     * @see List#removeAll(Collection)
     */
    public boolean removeAll(
            final Collection<? extends MethodMetadataBuilder> methodsToRemove) {
        if (methodsToRemove == null) {
            return false;
        }
        return this.declaredMethods.removeAll(methodsToRemove);
    }

    /**
     * Ensures that the type being built does not extend any of the given types
     * 
     * @param superTypes the types to remove as supertypes
     */
    public void removeExtendsTypes(final JavaType... superTypes) {
        extendsTypes.removeAll(Arrays.asList(superTypes));
    }

    /**
     * Sets the builders for the constructors that are to be declared
     * 
     * @param declaredConstructors can be <code>null</code> for none
     */
    public final void setDeclaredConstructors(
            final Collection<? extends ConstructorMetadataBuilder> declaredConstructors) {
        this.declaredConstructors.clear();
        if (declaredConstructors != null) {
            this.declaredConstructors.addAll(declaredConstructors);
        }
    }

    /**
     * Sets the builders for the fields to be declared by the type being built
     * 
     * @param declaredFields the builders to set (can be <code>null</code> for
     *            none)
     */
    public final void setDeclaredFields(
            final Collection<? extends FieldMetadataBuilder> declaredFields) {
        this.declaredFields.clear();
        if (declaredFields != null) {
            this.declaredFields.addAll(declaredFields);
        }
    }

    /**
     * Sets the builders for the initializers of the type being built
     * 
     * @param declaredInitializers the builders to set; can be <code>null</code>
     *            for none
     */
    public void setDeclaredInitializers(
            final Collection<? extends InitializerMetadataBuilder> declaredInitializers) {
        this.declaredInitializers.clear();
        if (declaredInitializers != null) {
            this.declaredInitializers.addAll(declaredInitializers);
        }
    }

    /**
     * Sets the builders for the inner types of the type being built
     * 
     * @param declaredInnerTypes the builders to set; can be <code>null</code>
     *            for none
     */
    public void setDeclaredInnerTypes(
            final Collection<? extends ClassOrInterfaceTypeDetailsBuilder> declaredInnerTypes) {
        this.declaredInnerTypes.clear();
        if (declaredInnerTypes != null) {
            this.declaredInnerTypes.addAll(declaredInnerTypes);
        }
    }

    /**
     * Sets the declared methods for this builder; equivalent to calling
     * {@link #addMethod(MethodMetadataBuilder)} once for each item of the given
     * {@link Iterable}.
     * 
     * @param declaredMethods the methods to set; can be <code>null</code> for
     *            none, otherwise the {@link Iterable} is defensively copied
     */
    public final void setDeclaredMethods(
            final Iterable<? extends MethodMetadataBuilder> declaredMethods) {
        this.declaredMethods.clear();
        if (declaredMethods != null) {
            for (final MethodMetadataBuilder methodBuilder : declaredMethods) {
                addMethod(methodBuilder);
            }
        }
    }

    /**
     * Sets the types that the built instance will extend
     * 
     * @param extendsTypes can be <code>null</code> for none
     */
    public final void setExtendsTypes(
            final Collection<? extends JavaType> extendsTypes) {
        this.extendsTypes.clear();
        if (extendsTypes != null) {
            this.extendsTypes.addAll(extendsTypes);
        }
    }

    /**
     * Sets the types to be implemented by the type being built
     * 
     * @param implementsTypes can be <code>null</code> for none
     */
    public final void setImplementsTypes(
            final Collection<? extends JavaType> implementsTypes) {
        this.implementsTypes.clear();
        if (implementsTypes != null) {
            this.implementsTypes.addAll(implementsTypes);
        }
    }
}
