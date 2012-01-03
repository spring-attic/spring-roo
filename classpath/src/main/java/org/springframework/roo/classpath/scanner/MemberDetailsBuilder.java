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

    // Fields
    private final Map<String, MemberHoldingTypeDetails> memberHoldingTypeDetailsMap = new LinkedHashMap<String, MemberHoldingTypeDetails>();
    private final Map<String, TypeDetailsBuilder> typeDetailsBuilderMap = new LinkedHashMap<String, TypeDetailsBuilder>();
    private final MemberDetails originalMemberDetails;
    private boolean changed = false;

    public MemberDetailsBuilder(final MemberDetails memberDetails) {
        this.originalMemberDetails = memberDetails;
        for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails
                .getDetails()) {
            memberHoldingTypeDetailsMap.put(
                    memberHoldingTypeDetails.getDeclaredByMetadataId(),
                    memberHoldingTypeDetails);
        }
    }

    public MemberDetailsBuilder(
            final Collection<? extends MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
        this.originalMemberDetails = new MemberDetailsImpl(
                memberHoldingTypeDetailsList);
        for (MemberHoldingTypeDetails memberHoldingTypeDetails : originalMemberDetails
                .getDetails()) {
            memberHoldingTypeDetailsMap.put(
                    memberHoldingTypeDetails.getDeclaredByMetadataId(),
                    memberHoldingTypeDetails);
        }
    }

    public MemberDetails build() {
        if (changed) {
            for (TypeDetailsBuilder typeDetailsBuilder : typeDetailsBuilderMap
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

    public <T> void tag(final T toModify, final CustomDataKey<T> key,
            final Object value) {
        if (toModify instanceof FieldMetadata) {
            CustomDataBuilder customDataBuilder = new CustomDataBuilder();
            customDataBuilder.put(key, value);
            doModification((FieldMetadata) toModify, customDataBuilder.build());
        }
        else if (toModify instanceof MethodMetadata) {
            CustomDataBuilder customDataBuilder = new CustomDataBuilder();
            customDataBuilder.put(key, value);
            doModification((MethodMetadata) toModify, customDataBuilder.build());
        }
        else if (toModify instanceof ConstructorMetadata) {
            CustomDataBuilder customDataBuilder = new CustomDataBuilder();
            customDataBuilder.put(key, value);
            doModification((ConstructorMetadata) toModify,
                    customDataBuilder.build());
        }
        else if (toModify instanceof MemberHoldingTypeDetails) {
            CustomDataBuilder customDataBuilder = new CustomDataBuilder();
            customDataBuilder.put(key, value);
            doModification((MemberHoldingTypeDetails) toModify,
                    customDataBuilder.build());
        }
    }

    private void doModification(final FieldMetadata field,
            final CustomData customData) {
        MemberHoldingTypeDetails memberHoldingTypeDetails = memberHoldingTypeDetailsMap
                .get(field.getDeclaredByMetadataId());
        if (memberHoldingTypeDetails != null) {
            FieldMetadata matchedField = memberHoldingTypeDetails
                    .getField(field.getFieldName());
            if (matchedField != null
                    && !matchedField.getCustomData().keySet()
                            .containsAll(customData.keySet())) {
                TypeDetailsBuilder typeDetailsBuilder = getTypeDetailsBuilder(memberHoldingTypeDetails);
                typeDetailsBuilder.addDataToField(field, customData);
                changed = true;
            }
        }
    }

    private void doModification(final MethodMetadata method,
            final CustomData customData) {
        MemberHoldingTypeDetails memberHoldingTypeDetails = memberHoldingTypeDetailsMap
                .get(method.getDeclaredByMetadataId());
        if (memberHoldingTypeDetails != null) {
            MethodMetadata matchedMethod = memberHoldingTypeDetails.getMethod(
                    method.getMethodName(), AnnotatedJavaType
                            .convertFromAnnotatedJavaTypes(method
                                    .getParameterTypes()));
            if (matchedMethod != null
                    && !matchedMethod.getCustomData().keySet()
                            .containsAll(customData.keySet())) {
                TypeDetailsBuilder typeDetailsBuilder = getTypeDetailsBuilder(memberHoldingTypeDetails);
                typeDetailsBuilder.addDataToMethod(method, customData);
                changed = true;
            }
        }
    }

    private void doModification(final ConstructorMetadata constructor,
            final CustomData customData) {
        MemberHoldingTypeDetails memberHoldingTypeDetails = memberHoldingTypeDetailsMap
                .get(constructor.getDeclaredByMetadataId());
        if (memberHoldingTypeDetails != null) {
            ConstructorMetadata matchedConstructor = memberHoldingTypeDetails
                    .getDeclaredConstructor(AnnotatedJavaType
                            .convertFromAnnotatedJavaTypes(constructor
                                    .getParameterTypes()));
            if (matchedConstructor != null
                    && !matchedConstructor.getCustomData().keySet()
                            .containsAll(customData.keySet())) {
                TypeDetailsBuilder typeDetailsBuilder = getTypeDetailsBuilder(memberHoldingTypeDetails);
                typeDetailsBuilder
                        .addDataToConstructor(constructor, customData);
                changed = true;
            }
        }
    }

    private void doModification(final MemberHoldingTypeDetails type,
            final CustomData customData) {
        MemberHoldingTypeDetails memberHoldingTypeDetails = memberHoldingTypeDetailsMap
                .get(type.getDeclaredByMetadataId());
        if (memberHoldingTypeDetails != null) {
            if (memberHoldingTypeDetails.getName().equals(type.getName())
                    && !memberHoldingTypeDetails.getCustomData().keySet()
                            .containsAll(customData.keySet())) {
                TypeDetailsBuilder typeDetailsBuilder = getTypeDetailsBuilder(memberHoldingTypeDetails);
                typeDetailsBuilder.getCustomData().append(customData);
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
        TypeDetailsBuilder typeDetailsBuilder = new TypeDetailsBuilder(
                memberHoldingTypeDetails);
        typeDetailsBuilderMap.put(
                memberHoldingTypeDetails.getDeclaredByMetadataId(),
                typeDetailsBuilder);
        return typeDetailsBuilder;
    }

    static class TypeDetailsBuilder extends
            AbstractMemberHoldingTypeDetailsBuilder<MemberHoldingTypeDetails> {

        // Fields
        private final MemberHoldingTypeDetails existing;

        protected TypeDetailsBuilder(final MemberHoldingTypeDetails existing) {
            super(existing);
            this.existing = existing;
        }

        public MemberHoldingTypeDetails build() {
            if (existing instanceof ItdTypeDetails) {
                ItdTypeDetailsBuilder itdBuilder = new ItdTypeDetailsBuilder(
                        (ItdTypeDetails) existing);
                // Push in all members that may have been modified
                itdBuilder.setDeclaredFields(this.getDeclaredFields());
                itdBuilder.setDeclaredMethods(this.getDeclaredMethods());
                itdBuilder.setAnnotations(this.getAnnotations());
                itdBuilder.setCustomData(this.getCustomData());
                itdBuilder.setDeclaredConstructors(this
                        .getDeclaredConstructors());
                itdBuilder.setDeclaredInitializers(this
                        .getDeclaredInitializers());
                itdBuilder.setDeclaredInnerTypes(this.getDeclaredInnerTypes());
                itdBuilder.setExtendsTypes(this.getExtendsTypes());
                itdBuilder.setImplementsTypes(this.getImplementsTypes());
                itdBuilder.setModifier(this.getModifier());
                return itdBuilder.build();
            }
            else if (existing instanceof ClassOrInterfaceTypeDetails) {
                ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                        (ClassOrInterfaceTypeDetails) existing);
                // Push in all members that may
                cidBuilder.setDeclaredFields(this.getDeclaredFields());
                cidBuilder.setDeclaredMethods(this.getDeclaredMethods());
                cidBuilder.setAnnotations(this.getAnnotations());
                cidBuilder.setCustomData(this.getCustomData());
                cidBuilder.setDeclaredConstructors(this
                        .getDeclaredConstructors());
                cidBuilder.setDeclaredInitializers(this
                        .getDeclaredInitializers());
                cidBuilder.setDeclaredInnerTypes(this.getDeclaredInnerTypes());
                cidBuilder.setExtendsTypes(this.getExtendsTypes());
                cidBuilder.setImplementsTypes(this.getImplementsTypes());
                cidBuilder.setModifier(this.getModifier());
                return cidBuilder.build();
            }
            else {
                throw new IllegalStateException(
                        "Unknown instance of MemberHoldingTypeDetails");
            }
        }

        public void addDataToField(final FieldMetadata replacement,
                final CustomData customData) {
            // If the MIDs don't match then the proposed can't be a replacement
            if (!replacement.getDeclaredByMetadataId().equals(
                    getDeclaredByMetadataId())) {
                return;
            }
            for (FieldMetadataBuilder existingField : getDeclaredFields()) {
                if (existingField.getFieldName().equals(
                        replacement.getFieldName())) {
                    for (Object key : customData.keySet()) {
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
            for (MethodMetadataBuilder existingMethod : getDeclaredMethods()) {
                if (existingMethod.getMethodName().equals(
                        replacement.getMethodName())) {
                    if (AnnotatedJavaType.convertFromAnnotatedJavaTypes(
                            existingMethod.getParameterTypes()).equals(
                            AnnotatedJavaType
                                    .convertFromAnnotatedJavaTypes(replacement
                                            .getParameterTypes()))) {
                        for (Object key : customData.keySet()) {
                            existingMethod.putCustomData(key,
                                    customData.get(key));
                        }
                        break;
                    }
                }
            }
        }

        public void addDataToConstructor(final ConstructorMetadata replacement,
                final CustomData customData) {
            // If the MIDs don't match then the proposed can't be a replacement
            if (!replacement.getDeclaredByMetadataId().equals(
                    getDeclaredByMetadataId())) {
                return;
            }
            for (ConstructorMetadataBuilder existingConstructor : getDeclaredConstructors()) {
                if (AnnotatedJavaType.convertFromAnnotatedJavaTypes(
                        existingConstructor.getParameterTypes()).equals(
                        AnnotatedJavaType
                                .convertFromAnnotatedJavaTypes(replacement
                                        .getParameterTypes()))) {
                    for (Object key : customData.keySet()) {
                        existingConstructor.putCustomData(key,
                                customData.get(key));
                    }
                    break;
                }
            }
        }

        @Override
        public void addImports(final Collection<ImportMetadata> imports) {
            throw new UnsupportedOperationException(); // No known use case
        }
    }
}
