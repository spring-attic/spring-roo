package org.springframework.roo.classpath.layers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.customdata.taggers.AnnotatedTypeMatcher;
import org.springframework.roo.classpath.customdata.taggers.Matcher;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * A {@link Matcher} used for layering support; identifies layer components
 * (services, repositories, etc) by the presence of a given tag, and sets each
 * such component's {@link CustomDataKeys#LAYER_TYPE} tag to a list of the
 * domain types managed by that component (as a
 * <code>List&lt;{@link JavaType}&gt;</code>).
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public class LayerTypeMatcher extends AnnotatedTypeMatcher {

    private final JavaSymbolName domainTypesAttribute;
    private final JavaType layerAnnotationType;

    /**
     * Constructor
     * 
     * @param layerAnnotation the annotation type to match on and read
     *            attributes of (required)
     * @param domainTypesAttribute the attribute of the above annotation that
     *            identifies the domain type(s) being managed (required)
     */
    public LayerTypeMatcher(final JavaType layerAnnotation,
            final JavaSymbolName domainTypesAttribute) {
        super(CustomDataKeys.LAYER_TYPE, layerAnnotation);
        Validate.notNull(layerAnnotation, "Layer annotation is required");
        Validate.notNull(domainTypesAttribute,
                "Domain types attribute is required");
        this.domainTypesAttribute = domainTypesAttribute;
        layerAnnotationType = layerAnnotation;
    }

    @Override
    public Object getTagValue(final MemberHoldingTypeDetails type) {
        final AnnotationMetadata layerAnnotation = MemberFindingUtils
                .getAnnotationOfType(type.getAnnotations(), layerAnnotationType);
        if (layerAnnotation == null
                || layerAnnotation.getAttribute(domainTypesAttribute) == null) {
            return null;
        }
        final AnnotationAttributeValue<?> value = layerAnnotation
                .getAttribute(domainTypesAttribute);
        final List<JavaType> domainTypes = new ArrayList<JavaType>();
        if (value instanceof ClassAttributeValue) {
            domainTypes.add(((ClassAttributeValue) value).getValue());
        }
        else if (value instanceof ArrayAttributeValue<?>) {
            final ArrayAttributeValue<?> castValue = (ArrayAttributeValue<?>) value;
            for (final AnnotationAttributeValue<?> val : castValue.getValue()) {
                if (val instanceof ClassAttributeValue) {
                    domainTypes.add(((ClassAttributeValue) val).getValue());
                }
            }
        }
        return domainTypes;
    }
}
