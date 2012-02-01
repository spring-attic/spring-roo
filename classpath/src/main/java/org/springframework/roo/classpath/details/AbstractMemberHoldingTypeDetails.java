package org.springframework.roo.classpath.details;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.LAYER_TYPE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Convenient superclass for {@link MemberHoldingTypeDetails} implementations.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public abstract class AbstractMemberHoldingTypeDetails extends
        AbstractIdentifiableAnnotatedJavaStructureProvider implements
        MemberHoldingTypeDetails {

    /**
     * Constructor
     * 
     * @param customData
     * @param declaredByMetadataId
     * @param modifier
     * @param annotations
     */
    protected AbstractMemberHoldingTypeDetails(final CustomData customData,
            final String declaredByMetadataId, final int modifier,
            final Collection<AnnotationMetadata> annotations) {
        super(customData, declaredByMetadataId, modifier, annotations);
    }

    public ConstructorMetadata getDeclaredConstructor(
            final List<JavaType> parameters) {
        final Collection<JavaType> parameterList = CollectionUtils.populate(
                new ArrayList<JavaType>(), parameters);
        for (final ConstructorMetadata constructor : getDeclaredConstructors()) {
            if (parameterList.equals(AnnotatedJavaType
                    .convertFromAnnotatedJavaTypes(constructor
                            .getParameterTypes()))) {
                return constructor;
            }
        }
        return null;
    }

    public FieldMetadata getDeclaredField(final JavaSymbolName fieldName) {
        for (final FieldMetadata field : getDeclaredFields()) {
            if (field.getFieldName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    public ClassOrInterfaceTypeDetails getDeclaredInnerType(
            final JavaType typeName) {
        Validate.notNull(typeName, "Name of inner type required");
        for (final ClassOrInterfaceTypeDetails cid : getDeclaredInnerTypes()) {
            if (cid.getName().getSimpleTypeName()
                    .equals(typeName.getSimpleTypeName())) {
                return cid;
            }
        }
        return null;
    }

    public FieldMetadata getField(final JavaSymbolName fieldName) {
        Validate.notNull(fieldName, "Field name required");
        MemberHoldingTypeDetails current = this;
        while (current != null) {
            final FieldMetadata result = current.getDeclaredField(fieldName);
            if (result != null) {
                return result;
            }
            if (current instanceof ClassOrInterfaceTypeDetails) {
                current = ((ClassOrInterfaceTypeDetails) current)
                        .getSuperclass();
            }
            else {
                current = null;
            }
        }
        return null;
    }

    public List<FieldMetadata> getFieldsWithAnnotation(final JavaType annotation) {
        Validate.notNull(annotation, "Annotation required");
        final List<FieldMetadata> result = new ArrayList<FieldMetadata>();
        MemberHoldingTypeDetails current = this;
        while (current != null) {
            for (final FieldMetadata field : current.getDeclaredFields()) {
                if (MemberFindingUtils.getAnnotationOfType(
                        field.getAnnotations(), annotation) != null) {
                    // Found the annotation on this field
                    result.add(field);
                }
            }
            if (current instanceof ClassOrInterfaceTypeDetails) {
                current = ((ClassOrInterfaceTypeDetails) current)
                        .getSuperclass();
            }
            else {
                current = null;
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<JavaType> getLayerEntities() {
        final Object entities = getCustomData().get(LAYER_TYPE);
        if (entities == null) {
            return Collections.emptyList();
        }
        return (List<JavaType>) entities;
    }

    public MethodMetadata getMethod(final JavaSymbolName methodName) {
        Validate.notNull(methodName, "Method name required");

        MemberHoldingTypeDetails current = this;
        while (current != null) {
            final MethodMetadata result = MemberFindingUtils.getDeclaredMethod(
                    current, methodName);
            if (result != null) {
                return result;
            }
            if (current instanceof ClassOrInterfaceTypeDetails) {
                current = ((ClassOrInterfaceTypeDetails) current)
                        .getSuperclass();
            }
            else {
                current = null;
            }
        }
        return null;
    }

    public MethodMetadata getMethod(final JavaSymbolName methodName,
            List<JavaType> parameters) {
        Validate.notNull(methodName, "Method name required");
        if (parameters == null) {
            parameters = new ArrayList<JavaType>();
        }

        MemberHoldingTypeDetails current = this;
        while (current != null) {
            final MethodMetadata result = MemberFindingUtils.getDeclaredMethod(
                    current, methodName, parameters);
            if (result != null) {
                return result;
            }
            if (current instanceof ClassOrInterfaceTypeDetails) {
                current = ((ClassOrInterfaceTypeDetails) current)
                        .getSuperclass();
            }
            else {
                current = null;
            }
        }
        return null;
    }

    public List<MethodMetadata> getMethods() {
        final List<MethodMetadata> result = new ArrayList<MethodMetadata>();
        MemberHoldingTypeDetails current = this;
        while (current != null) {
            for (final MethodMetadata method : current.getDeclaredMethods()) {
                result.add(method);
            }
            if (current instanceof ClassOrInterfaceTypeDetails) {
                current = ((ClassOrInterfaceTypeDetails) current)
                        .getSuperclass();
            }
            else {
                current = null;
            }
        }
        return result;
    }

    public JavaSymbolName getUniqueFieldName(final String proposedName) {
        Validate.notBlank(proposedName, "Proposed field name is required");
        String candidateName = proposedName;
        while (getField(new JavaSymbolName(candidateName)) != null) {
            // The proposed field name is taken; differentiate it
            candidateName += "_";
        }
        // We've derived a unique name
        return new JavaSymbolName(candidateName);
    }

    public boolean implementsType(final JavaType interfaceType) {
        return getImplementsTypes().contains(interfaceType);
    }
}