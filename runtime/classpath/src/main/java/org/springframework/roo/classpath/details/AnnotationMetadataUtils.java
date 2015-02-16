package org.springframework.roo.classpath.details;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.CharAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.DoubleAttributeValue;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.IntegerAttributeValue;
import org.springframework.roo.classpath.details.annotations.LongAttributeValue;
import org.springframework.roo.classpath.details.annotations.NestedAnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Utilities to use with {@link AnnotationMetadata}.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
public abstract class AnnotationMetadataUtils {

    private static String computeAttributeValue(
            final AnnotationAttributeValue<?> value,
            final ImportRegistrationResolver resolver) {
        String attributeValue = null;
        if (value instanceof BooleanAttributeValue) {
            attributeValue = ((BooleanAttributeValue) value).getValue()
                    .toString();
        }
        else if (value instanceof CharAttributeValue) {
            attributeValue = "'"
                    + ((CharAttributeValue) value).getValue().toString() + "'";
        }
        else if (value instanceof ClassAttributeValue) {
            final JavaType clazz = ((ClassAttributeValue) value).getValue();
            if (resolver == null
                    || resolver
                            .isFullyQualifiedFormRequiredAfterAutoImport(clazz)) {
                attributeValue = clazz.getFullyQualifiedTypeName() + ".class";
            }
            else {
                attributeValue = clazz.getSimpleTypeName() + ".class";
            }
        }
        else if (value instanceof DoubleAttributeValue) {
            final DoubleAttributeValue dbl = (DoubleAttributeValue) value;
            if (dbl.isFloatingPrecisionOnly()) {
                attributeValue = dbl.getValue().toString() + "F";
            }
            else {
                attributeValue = dbl.getValue().toString() + "D";
            }
        }
        else if (value instanceof EnumAttributeValue) {
            final EnumDetails enumDetails = ((EnumAttributeValue) value)
                    .getValue();
            final JavaType clazz = enumDetails.getType();
            if (resolver == null
                    || resolver
                            .isFullyQualifiedFormRequiredAfterAutoImport(clazz)) {
                attributeValue = clazz.getFullyQualifiedTypeName() + "."
                        + enumDetails.getField().getSymbolName();
            }
            else {
                attributeValue = clazz.getSimpleTypeName() + "."
                        + enumDetails.getField().getSymbolName();
            }
        }
        else if (value instanceof IntegerAttributeValue) {
            attributeValue = ((IntegerAttributeValue) value).getValue()
                    .toString();
        }
        else if (value instanceof LongAttributeValue) {
            attributeValue = ((LongAttributeValue) value).getValue().toString()
                    + "L";
        }
        else if (value instanceof StringAttributeValue) {
            attributeValue = "\"" + ((StringAttributeValue) value).getValue()
                    + "\"";
        }
        else if (value instanceof NestedAnnotationAttributeValue) {
            final AnnotationMetadata annotationMetadata = ((NestedAnnotationAttributeValue) value)
                    .getValue();
            final StringBuilder data = new StringBuilder("@");
            final JavaType annotationType = annotationMetadata
                    .getAnnotationType();
            if (resolver == null
                    || resolver
                            .isFullyQualifiedFormRequiredAfterAutoImport(annotationType)) {
                data.append(annotationType.getFullyQualifiedTypeName());
            }
            else {
                data.append(annotationType.getSimpleTypeName());
            }
            if (!annotationMetadata.getAttributeNames().isEmpty()) {
                data.append("(");
                int i = 0;
                for (final JavaSymbolName attributeName : annotationMetadata
                        .getAttributeNames()) {
                    i++;
                    if (i > 1) {
                        data.append(", ");
                    }
                    data.append(attributeName.getSymbolName()).append(" = ");
                    data.append(computeAttributeValue(
                            annotationMetadata.getAttribute(attributeName),
                            resolver));
                }
                data.append(")");
            }
            attributeValue = data.toString();
        }
        else if (value instanceof ArrayAttributeValue<?>) {
            final ArrayAttributeValue<?> array = (ArrayAttributeValue<?>) value;
            final StringBuilder data = new StringBuilder("{ ");
            int i = 0;
            for (final AnnotationAttributeValue<?> val : array.getValue()) {
                i++;
                if (i > 1) {
                    data.append(", ");
                }
                data.append(computeAttributeValue(val, resolver));
            }
            data.append(" }");
            attributeValue = data.toString();
        }
        return attributeValue;
    }

    /**
     * Converts the annotation into a string-based form.
     * 
     * @param annotation to covert (required)
     * @return a string-based representation (never null)
     */
    public static String toSourceForm(final AnnotationMetadata annotation) {
        return toSourceForm(annotation, null);
    }

    /**
     * Converts the annotation into a string-based form.
     * 
     * @param annotation to covert (required)
     * @param resolver to use for automatic addition of used types (may be null)
     * @return a string-based representation (never null)
     */
    public static String toSourceForm(final AnnotationMetadata annotation,
            final ImportRegistrationResolver resolver) {
        Validate.notNull(annotation, "Annotation required");

        final StringBuilder sb = new StringBuilder();
        sb.append("@");

        if (resolver != null) {
            if (resolver.isFullyQualifiedFormRequiredAfterAutoImport(annotation
                    .getAnnotationType())) {
                sb.append(annotation.getAnnotationType()
                        .getFullyQualifiedTypeName());
            }
            else {
                sb.append(annotation.getAnnotationType().getSimpleTypeName());
            }
        }
        else {
            sb.append(annotation.getAnnotationType()
                    .getFullyQualifiedTypeName());
        }

        if (annotation.getAttributeNames().isEmpty()) {
            return sb.toString();
        }

        sb.append("(");
        boolean requireComma = false;
        for (final JavaSymbolName attributeName : annotation
                .getAttributeNames()) {
            // Add a comma, to separate the last annotation attribute
            if (requireComma) {
                sb.append(", ");
                requireComma = false;
            }

            // Compute the value
            final AnnotationAttributeValue<?> value = annotation
                    .getAttribute(attributeName);

            final String attributeValue = computeAttributeValue(value, resolver);

            if (attributeValue != null) {
                // We have a supported attribute
                if (!"value".equals(attributeName.getSymbolName())
                        || annotation.getAttributeNames().size() > 1) {
                    sb.append(attributeName.getSymbolName());
                    sb.append(" = ");
                }
                sb.append(attributeValue);
                requireComma = true;
            }
        }
        sb.append(")");
        return sb.toString();
    }
}
