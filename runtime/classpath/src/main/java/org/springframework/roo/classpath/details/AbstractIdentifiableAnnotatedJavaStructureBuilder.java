package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.Builder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Assists in the creation of a {@link Builder} for types that eventually
 * implement {@link IdentifiableAnnotatedJavaStructure}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public abstract class AbstractIdentifiableAnnotatedJavaStructureBuilder<T extends IdentifiableAnnotatedJavaStructure>
        extends AbstractIdentifiableJavaStructureBuilder<T> {
    private List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    protected AbstractIdentifiableAnnotatedJavaStructureBuilder(
            final IdentifiableAnnotatedJavaStructure existing) {
        super(existing);
        init(existing);
    }

    protected AbstractIdentifiableAnnotatedJavaStructureBuilder(
            final String declaredbyMetadataId) {
        super(declaredbyMetadataId);
    }

    protected AbstractIdentifiableAnnotatedJavaStructureBuilder(
            final String declaredbyMetadataId,
            final IdentifiableAnnotatedJavaStructure existing) {
        super(declaredbyMetadataId, existing);
        init(existing);
    }

    public final boolean addAnnotation(
            final AnnotationMetadata annotationMetadata) {
        if (annotationMetadata == null) {
            return false;
        }
        return addAnnotation(new AnnotationMetadataBuilder(annotationMetadata));
    }

    public final boolean addAnnotation(
            final AnnotationMetadataBuilder annotationBuilder) {
        if (annotationBuilder == null) {
            return false;
        }
        onAddAnnotation(annotationBuilder);
        return annotations.add(annotationBuilder);
    }

    public final List<AnnotationMetadata> buildAnnotations() {
        final List<AnnotationMetadata> result = new ArrayList<AnnotationMetadata>();
        for (final AnnotationMetadataBuilder annotationBuilder : annotations) {
            result.add(annotationBuilder.build());
        }
        return result;
    }

    public final List<AnnotationMetadataBuilder> getAnnotations() {
        return annotations;
    }

    /**
     * Locates the specified type-level annotation.
     * 
     * @param type to locate (can be <code>null</code>)
     * @return the annotation, or null if not found
     * @since 1.2.0
     */
    public AnnotationMetadataBuilder getDeclaredTypeAnnotation(
            final JavaType type) {
        for (final AnnotationMetadataBuilder annotationBuilder : getAnnotations()) {
            if (annotationBuilder.getAnnotationType().equals(type)) {
                return annotationBuilder;
            }
        }
        return null;
    }

    private void init(final IdentifiableAnnotatedJavaStructure existing) {
        for (final AnnotationMetadata element : existing.getAnnotations()) {
            this.annotations.add(new AnnotationMetadataBuilder(element));
        }
    }

    protected void onAddAnnotation(
            final AnnotationMetadataBuilder annotationMetadata) {
    }

    public void removeAnnotation(final JavaType annotationType) {
        for (final AnnotationMetadataBuilder annotationMetadataBuilder : annotations) {
            if (annotationMetadataBuilder.getAnnotationType().equals(
                    annotationType)) {
                annotations.remove(annotationMetadataBuilder);
                break;
            }
        }
    }

    public final void setAnnotations(
            final Collection<AnnotationMetadata> annotations) {
        final List<AnnotationMetadataBuilder> annotationBuilders = new ArrayList<AnnotationMetadataBuilder>();
        for (final AnnotationMetadata annotationMetadata : annotations) {
            annotationBuilders.add(new AnnotationMetadataBuilder(
                    annotationMetadata));
        }
        setAnnotations(annotationBuilders);
    }

    public final void setAnnotations(
            final List<AnnotationMetadataBuilder> annotations) {
        this.annotations = annotations;
    }

    public boolean updateTypeAnnotation(final AnnotationMetadata annotation) {
        return updateTypeAnnotation(annotation, null);
    }

    public boolean updateTypeAnnotation(final AnnotationMetadata annotation,
            final Set<JavaSymbolName> attributesToDeleteIfPresent) {
        boolean hasChanged = false;

        // We are going to build a replacement AnnotationMetadata.
        // This variable tracks the new attribute values the replacement will
        // hold.
        final Map<JavaSymbolName, AnnotationAttributeValue<?>> replacementAttributeValues = new LinkedHashMap<JavaSymbolName, AnnotationAttributeValue<?>>();

        final AnnotationMetadataBuilder existingBuilder = getDeclaredTypeAnnotation(annotation
                .getAnnotationType());

        if (existingBuilder == null) {
            // Not already present, so just go and add it
            for (final JavaSymbolName incomingAttributeName : annotation
                    .getAttributeNames()) {
                // Do not copy incoming attributes which exist in the
                // attributesToDeleteIfPresent Set
                if (attributesToDeleteIfPresent == null
                        || !attributesToDeleteIfPresent
                                .contains(incomingAttributeName)) {
                    final AnnotationAttributeValue<?> incomingValue = annotation
                            .getAttribute(incomingAttributeName);
                    replacementAttributeValues.put(incomingAttributeName,
                            incomingValue);
                }
            }

            final AnnotationMetadataBuilder replacement = new AnnotationMetadataBuilder(
                    annotation.getAnnotationType(),
                    new ArrayList<AnnotationAttributeValue<?>>(
                            replacementAttributeValues.values()));
            addAnnotation(replacement);
            return true;
        }

        final AnnotationMetadata existing = existingBuilder.build();

        // Copy the existing attributes into the new attributes
        for (final JavaSymbolName existingAttributeName : existing
                .getAttributeNames()) {
            if (attributesToDeleteIfPresent != null
                    && attributesToDeleteIfPresent
                            .contains(existingAttributeName)) {
                hasChanged = true;
            }
            else {
                final AnnotationAttributeValue<?> existingValue = existing
                        .getAttribute(existingAttributeName);
                replacementAttributeValues.put(existingAttributeName,
                        existingValue);
            }
        }

        // Now we ensure every incoming attribute replaces the existing
        for (final JavaSymbolName incomingAttributeName : annotation
                .getAttributeNames()) {
            final AnnotationAttributeValue<?> incomingValue = annotation
                    .getAttribute(incomingAttributeName);

            // Add this attribute to the end of the list if the attribute is not
            // already present
            if (replacementAttributeValues.keySet().contains(
                    incomingAttributeName)) {
                // There was already an attribute. Need to determine if this new
                // attribute value is materially different
                final AnnotationAttributeValue<?> existingValue = replacementAttributeValues
                        .get(incomingAttributeName);
                Validate.notNull(existingValue,
                        "Existing value should have been provided by earlier loop");
                if (!existingValue.equals(incomingValue)) {
                    replacementAttributeValues.put(incomingAttributeName,
                            incomingValue);
                    hasChanged = true;
                }
            }
            else if (attributesToDeleteIfPresent == null
                    || !attributesToDeleteIfPresent
                            .contains(incomingAttributeName)) {
                // This is a new attribute that does not already exist, so add
                // it to the end of the replacement attributes
                replacementAttributeValues.put(incomingAttributeName,
                        incomingValue);
                hasChanged = true;
            }
        }
        // Were there any material changes?
        if (!hasChanged) {
            return false;
        }

        // Make a new AnnotationMetadata representing the replacement
        final AnnotationMetadataBuilder replacement = new AnnotationMetadataBuilder(
                annotation.getAnnotationType(),
                new ArrayList<AnnotationAttributeValue<?>>(
                        replacementAttributeValues.values()));
        annotations.remove(existingBuilder);
        addAnnotation(replacement);

        return true;
    }

    public boolean updateTypeAnnotation(
            final AnnotationMetadataBuilder annotationBuilder) {
        return updateTypeAnnotation(annotationBuilder.build());
    }
}
