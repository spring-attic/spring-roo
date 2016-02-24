package org.springframework.roo.classpath.scanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.roo.classpath.details.AbstractMemberHoldingTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.ImportMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.CustomDataBuilder;
import org.springframework.roo.model.CustomDataKey;

/**
 * Builder for {@link MemberDetails}.
 * 
 * @author Alan Stewart
 * @author Ben Alex
 * @author James Tyrrell
 * @since 1.1.3
 */
public class MemberDetailsBuilder {

    static class TypeDetailsBuilder extends
            AbstractMemberHoldingTypeDetailsBuilder<MemberHoldingTypeDetails> {

        private final MemberHoldingTypeDetails existing;

        protected TypeDetailsBuilder(final MemberHoldingTypeDetails existing) {
            super(existing);
            this.existing = existing;
        }

        public void addDataToConstructor(final ConstructorMetadata replacement,
                final CustomData customData) {
            // If the MIDs don't match then the proposed can't be a replacement
            if (!replacement.getDeclaredByMetadataId().equals(
                    getDeclaredByMetadataId())) {
                return;
            }
            for (final ConstructorMetadataBuilder existingConstructor : getDeclaredConstructors()) {
                if (AnnotatedJavaType.convertFromAnnotatedJavaTypes(
                        existingConstructor.getParameterTypes()).equals(
                        AnnotatedJavaType
                                .convertFromAnnotatedJavaTypes(replacement
                                        .getParameterTypes()))) {
                    for (final Object key : customData.keySet()) {
                        existingConstructor.putCustomData(key,
                                customData.get(key));
                    }
                    break;
                }
            }
        }

        public void addDataToField(final FieldMetadata replacement,
                final CustomData customData) {
            // If the MIDs don't match then the proposed can't be a replacement
            if (!replacement.getDeclaredByMetadataId().equals(
                    getDeclaredByMetadataId())) {
                return;
            }
            for (final FieldMetadataBuilder existingField : getDeclaredFields()) {
                if (existingField.getFieldName().equals(
                        replacement.getFieldName())) {
                    for (final Object key : customData.keySet()) {
                        existingField.putCustomData(key, customData.get(key));
                    }
                    break;
                }
            }
        }

        public void addDataToMethod(final MethodMetadata replacement,
                final CustomData customData) {
            // If the MIDs don't match then the proposed can't be a replacement
            if (!replacement.getDeclaredByMetadataId().equals(
                    getDeclaredByMetadataId())) {
                return;
            }
            for (final MethodMetadataBuilder existingMethod : getDeclaredMethods()) {
                if (existingMethod.getMethodName().equals(
                        replacement.getMethodName())) {
                    if (AnnotatedJavaType.convertFromAnnotatedJavaTypes(
                            existingMethod.getParameterTypes()).equals(
                            AnnotatedJavaType
                                    .convertFromAnnotatedJavaTypes(replacement
                                            .getParameterTypes()))) {
                        for (final Object key : customData.keySet()) {
                            existingMethod.putCustomData(key,
                                    customData.get(key));
                        }
                        break;
                    }
                }
            }
        }

        @Override
        public void addImports(final Collection<ImportMetadata> imports) {
            throw new UnsupportedOperationException(); // No known use case
        }

        public MemberHoldingTypeDetails build() {
            if (existing instanceof ItdTypeDetails) {
                final ItdTypeDetailsBuilder itdBuilder = new ItdTypeDetailsBuilder(
                        (ItdTypeDetails) existing);
                // Push in all members that may have been modified
                itdBuilder.setDeclaredFields(getDeclaredFields());
                itdBuilder.setDeclaredMethods(getDeclaredMethods());
                itdBuilder.setAnnotations(getAnnotations());
                itdBuilder.setCustomData(getCustomData());
                itdBuilder.setDeclaredConstructors(getDeclaredConstructors());
                itdBuilder.setDeclaredInitializers(getDeclaredInitializers());
                itdBuilder.setDeclaredInnerTypes(getDeclaredInnerTypes());
                itdBuilder.setExtendsTypes(getExtendsTypes());
                itdBuilder.setImplementsTypes(getImplementsTypes());
                itdBuilder.setModifier(getModifier());
                return itdBuilder.build();
            }
            else if (existing instanceof ClassOrInterfaceTypeDetails) {
                final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                        (ClassOrInterfaceTypeDetails) existing);
                // Push in all members that may
                cidBuilder.setDeclaredFields(getDeclaredFields());
                cidBuilder.setDeclaredMethods(getDeclaredMethods());
                cidBuilder.setAnnotations(getAnnotations());
                cidBuilder.setCustomData(getCustomData());
                cidBuilder.setDeclaredConstructors(getDeclaredConstructors());
                cidBuilder.setDeclaredInitializers(getDeclaredInitializers());
                cidBuilder.setDeclaredInnerTypes(getDeclaredInnerTypes());
                cidBuilder.setExtendsTypes(getExtendsTypes());
                cidBuilder.setImplementsTypes(getImplementsTypes());
                cidBuilder.setModifier(getModifier());
                return cidBuilder.build();
            }
            else {
                throw new IllegalStateException(
                        "Unknown instance of MemberHoldingTypeDetails");
            }
        }
    }

    private boolean changed = false;
    private final Map<String, MemberHoldingTypeDetails> memberHoldingTypeDetailsMap = new LinkedHashMap<String, MemberHoldingTypeDetails>();
    private final MemberDetails originalMemberDetails;

    private final Map<String, TypeDetailsBuilder> typeDetailsBuilderMap = new LinkedHashMap<String, TypeDetailsBuilder>();

    public MemberDetailsBuilder(
            final Collection<? extends MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
        originalMemberDetails = new MemberDetailsImpl(
                memberHoldingTypeDetailsList);
        for (final MemberHoldingTypeDetails memberHoldingTypeDetails : originalMemberDetails
                .getDetails()) {
            memberHoldingTypeDetailsMap.put(
                    memberHoldingTypeDetails.getDeclaredByMetadataId(),
                    memberHoldingTypeDetails);
        }
    }

    public MemberDetailsBuilder(final MemberDetails memberDetails) {
        originalMemberDetails = memberDetails;
        for (final MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails
                .getDetails()) {
            memberHoldingTypeDetailsMap.put(
                    memberHoldingTypeDetails.getDeclaredByMetadataId(),
                    memberHoldingTypeDetails);
        }
    }

    public MemberDetails build() {
        if (changed) {
            for (final TypeDetailsBuilder typeDetailsBuilder : typeDetailsBuilderMap
                    .values()) {
                memberHoldingTypeDetailsMap.put(
                        typeDetailsBuilder.getDeclaredByMetadataId(),
                        typeDetailsBuilder.build());
            }
            return new MemberDetailsImpl(
                    new ArrayList<MemberHoldingTypeDetails>(
                            memberHoldingTypeDetailsMap.values()));
        }
        return originalMemberDetails;
    }

    private void doModification(final ConstructorMetadata constructor,
            final CustomData customData) {
        final MemberHoldingTypeDetails memberHoldingTypeDetails = memberHoldingTypeDetailsMap
                .get(constructor.getDeclaredByMetadataId());
        if (memberHoldingTypeDetails != null) {
            final ConstructorMetadata matchedConstructor = memberHoldingTypeDetails
                    .getDeclaredConstructor(AnnotatedJavaType
                            .convertFromAnnotatedJavaTypes(constructor
                                    .getParameterTypes()));
            if (matchedConstructor != null
                    && !matchedConstructor.getCustomData().keySet()
                            .containsAll(customData.keySet())) {
                final TypeDetailsBuilder typeDetailsBuilder = getTypeDetailsBuilder(memberHoldingTypeDetails);
                typeDetailsBuilder
                        .addDataToConstructor(constructor, customData);
                changed = true;
            }
        }
    }

    private void doModification(final FieldMetadata field,
            final CustomData customData) {
        final MemberHoldingTypeDetails memberHoldingTypeDetails = memberHoldingTypeDetailsMap
                .get(field.getDeclaredByMetadataId());
        if (memberHoldingTypeDetails != null) {
            final FieldMetadata matchedField = memberHoldingTypeDetails
                    .getField(field.getFieldName());
            if (matchedField != null
                    && !matchedField.getCustomData().keySet()
                            .containsAll(customData.keySet())) {
                final TypeDetailsBuilder typeDetailsBuilder = getTypeDetailsBuilder(memberHoldingTypeDetails);
                typeDetailsBuilder.addDataToField(field, customData);
                changed = true;
            }
        }
    }

    private void doModification(final MemberHoldingTypeDetails type,
            final CustomData customData) {
        final MemberHoldingTypeDetails memberHoldingTypeDetails = memberHoldingTypeDetailsMap
                .get(type.getDeclaredByMetadataId());
        if (memberHoldingTypeDetails != null) {
            if (memberHoldingTypeDetails.getName().equals(type.getName())
                    && !memberHoldingTypeDetails.getCustomData().keySet()
                            .containsAll(customData.keySet())) {
                final TypeDetailsBuilder typeDetailsBuilder = getTypeDetailsBuilder(memberHoldingTypeDetails);
                typeDetailsBuilder.getCustomData().append(customData);
                changed = true;
            }
        }
    }

    private void doModification(final MethodMetadata method,
            final CustomData customData) {
        final MemberHoldingTypeDetails memberHoldingTypeDetails = memberHoldingTypeDetailsMap
                .get(method.getDeclaredByMetadataId());
        if (memberHoldingTypeDetails != null) {
            final MethodMetadata matchedMethod = memberHoldingTypeDetails
                    .getMethod(method.getMethodName(), AnnotatedJavaType
                            .convertFromAnnotatedJavaTypes(method
                                    .getParameterTypes()));
            if (matchedMethod != null
                    && !matchedMethod.getCustomData().keySet()
                            .containsAll(customData.keySet())) {
                final TypeDetailsBuilder typeDetailsBuilder = getTypeDetailsBuilder(memberHoldingTypeDetails);
                typeDetailsBuilder.addDataToMethod(method, customData);
                changed = true;
            }
        }
    }

    private TypeDetailsBuilder getTypeDetailsBuilder(
            final MemberHoldingTypeDetails memberHoldingTypeDetails) {
        if (typeDetailsBuilderMap.containsKey(memberHoldingTypeDetails
                .getDeclaredByMetadataId())) {
            return typeDetailsBuilderMap.get(memberHoldingTypeDetails
                    .getDeclaredByMetadataId());
        }
        final TypeDetailsBuilder typeDetailsBuilder = new TypeDetailsBuilder(
                memberHoldingTypeDetails);
        typeDetailsBuilderMap.put(
                memberHoldingTypeDetails.getDeclaredByMetadataId(),
                typeDetailsBuilder);
        return typeDetailsBuilder;
    }

    public <T> void tag(final T toModify, final CustomDataKey<T> key,
            final Object value) {
        if (toModify instanceof FieldMetadata) {
            final CustomDataBuilder customDataBuilder = new CustomDataBuilder();
            customDataBuilder.put(key, value);
            doModification((FieldMetadata) toModify, customDataBuilder.build());
        }
        else if (toModify instanceof MethodMetadata) {
            final CustomDataBuilder customDataBuilder = new CustomDataBuilder();
            customDataBuilder.put(key, value);
            doModification((MethodMetadata) toModify, customDataBuilder.build());
        }
        else if (toModify instanceof ConstructorMetadata) {
            final CustomDataBuilder customDataBuilder = new CustomDataBuilder();
            customDataBuilder.put(key, value);
            doModification((ConstructorMetadata) toModify,
                    customDataBuilder.build());
        }
        else if (toModify instanceof MemberHoldingTypeDetails) {
            final CustomDataBuilder customDataBuilder = new CustomDataBuilder();
            customDataBuilder.put(key, value);
            doModification((MemberHoldingTypeDetails) toModify,
                    customDataBuilder.build());
        }
    }
}
