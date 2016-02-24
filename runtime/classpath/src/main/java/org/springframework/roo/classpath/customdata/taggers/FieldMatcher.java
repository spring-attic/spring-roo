package org.springframework.roo.classpath.customdata.taggers;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.COLUMN_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.EMBEDDED_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.EMBEDDED_ID_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.ENUMERATED_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.LOB_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MANY_TO_MANY_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MANY_TO_ONE_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.ONE_TO_MANY_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.ONE_TO_ONE_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.TRANSIENT_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.VERSION_FIELD;
import static org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder.JPA_COLUMN_ANNOTATION;
import static org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder.JPA_EMBEDDED_ANNOTATION;
import static org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder.JPA_EMBEDDED_ID_ANNOTATION;
import static org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder.JPA_ENUMERATED_ANNOTATION;
import static org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder.JPA_ID_ANNOTATION;
import static org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder.JPA_LOB_ANNOTATION;
import static org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder.JPA_MANY_TO_MANY_ANNOTATION;
import static org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder.JPA_MANY_TO_ONE_ANNOTATION;
import static org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder.JPA_ONE_TO_MANY_ANNOTATION;
import static org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder.JPA_ONE_TO_ONE_ANNOTATION;
import static org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder.JPA_TRANSIENT_ANNOTATION;
import static org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder.JPA_VERSION_ANNOTATION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.CustomDataKey;
import org.springframework.roo.model.JavaSymbolName;

/**
 * A {@link Matcher} for {@link FieldMetadata}-that matches on the presence of
 * at least one of a given list of annotations.
 * 
 * @author James Tyrrell
 * @author Andrew Swan
 * @since 1.1.3
 */
public class FieldMatcher implements Matcher<FieldMetadata> {

    public static final FieldMatcher JPA_COLUMN = new FieldMatcher(
            COLUMN_FIELD, JPA_COLUMN_ANNOTATION);
    public static final FieldMatcher JPA_EMBEDDED = new FieldMatcher(
            EMBEDDED_FIELD, JPA_EMBEDDED_ANNOTATION);
    public static final FieldMatcher JPA_EMBEDDED_ID = new FieldMatcher(
            EMBEDDED_ID_FIELD, JPA_EMBEDDED_ID_ANNOTATION);
    public static final FieldMatcher JPA_ENUMERATED = new FieldMatcher(
            ENUMERATED_FIELD, JPA_ENUMERATED_ANNOTATION);
    public static final FieldMatcher JPA_ID = new FieldMatcher(
            IDENTIFIER_FIELD, JPA_ID_ANNOTATION);
    public static final FieldMatcher JPA_LOB = new FieldMatcher(LOB_FIELD,
            JPA_LOB_ANNOTATION);
    public static final FieldMatcher JPA_MANY_TO_MANY = new FieldMatcher(
            MANY_TO_MANY_FIELD, JPA_MANY_TO_MANY_ANNOTATION);
    public static final FieldMatcher JPA_MANY_TO_ONE = new FieldMatcher(
            MANY_TO_ONE_FIELD, JPA_MANY_TO_ONE_ANNOTATION);
    public static final FieldMatcher JPA_ONE_TO_MANY = new FieldMatcher(
            ONE_TO_MANY_FIELD, JPA_ONE_TO_MANY_ANNOTATION);
    public static final FieldMatcher JPA_ONE_TO_ONE = new FieldMatcher(
            ONE_TO_ONE_FIELD, JPA_ONE_TO_ONE_ANNOTATION);
    public static final FieldMatcher JPA_TRANSIENT = new FieldMatcher(
            TRANSIENT_FIELD, JPA_TRANSIENT_ANNOTATION);
    public static final FieldMatcher JPA_VERSION = new FieldMatcher(
            VERSION_FIELD, JPA_VERSION_ANNOTATION);

    private final List<AnnotationMetadata> annotations;
    private final CustomDataKey<FieldMetadata> customDataKey;

    /**
     * Constructor for matching on any of the given annotations
     * 
     * @param customDataKey the custom data key indicating the type of field
     *            (required)
     * @param annotations the annotations to match upon
     * @since 1.2.0
     */
    public FieldMatcher(final CustomDataKey<FieldMetadata> customDataKey,
            final AnnotationMetadata... annotations) {
        this(customDataKey, Arrays.asList(annotations));
    }

    /**
     * Constructor for matching on any of the given annotations
     * 
     * @param customDataKey the custom data key indicating the type of field
     *            (required)
     * @param annotations the annotations to match upon (can be null)
     */
    public FieldMatcher(final CustomDataKey<FieldMetadata> customDataKey,
            final Collection<AnnotationMetadata> annotations) {
        Validate.notNull(customDataKey, "Custom data key is required");
        this.annotations = new ArrayList<AnnotationMetadata>();
        this.customDataKey = customDataKey;
        if (annotations != null) {
            this.annotations.addAll(annotations);
        }
    }

    private Map<String, Object> getAttributeMap(final FieldMetadata field) {
        final Map<String, Object> map = new HashMap<String, Object>();
        final AnnotationMetadata annotationMetadata = getMatchingAnnotation(field);
        if (annotationMetadata != null) {
            for (final JavaSymbolName attributeName : annotationMetadata
                    .getAttributeNames()) {
                map.put(attributeName.getSymbolName(), annotationMetadata
                        .getAttribute(attributeName).getValue());
            }
        }
        return map;
    }

    public CustomDataKey<FieldMetadata> getCustomDataKey() {
        return customDataKey;
    }

    /**
     * Returns the first annotation of the given field that matches any of this
     * matcher's target annotations
     * 
     * @param field the field whose annotations are to be checked (required)
     * @return
     */
    private AnnotationMetadata getMatchingAnnotation(final FieldMetadata field) {
        for (final AnnotationMetadata fieldAnnotation : field.getAnnotations()) {
            for (final AnnotationMetadata matchingAnnotation : annotations) {
                if (fieldAnnotation
                        .getAnnotationType()
                        .getFullyQualifiedTypeName()
                        .equals(matchingAnnotation.getAnnotationType()
                                .getFullyQualifiedTypeName())) {
                    return fieldAnnotation;
                }
            }
        }
        return null;
    }

    public Object getTagValue(final FieldMetadata field) {
        return getAttributeMap(field);
    }

    public List<FieldMetadata> matches(
            final List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList) {
        final List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
        for (final MemberHoldingTypeDetails memberHoldingTypeDetails : memberHoldingTypeDetailsList) {
            for (final FieldMetadata field : memberHoldingTypeDetails
                    .getDeclaredFields()) {
                if (getMatchingAnnotation(field) != null) {
                    fields.add(field);
                }
            }
        }
        return fields;
    }
}
