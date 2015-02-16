package org.springframework.roo.classpath.operations.jsr303;

import static org.springframework.roo.model.Jsr303JavaType.DECIMAL_MAX;
import static org.springframework.roo.model.Jsr303JavaType.DECIMAL_MIN;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.support.logging.HandlerUtils;

public class StringOrNumericField extends FieldDetails {

    protected static final Logger LOGGER = HandlerUtils
            .getLogger(StringOrNumericField.class);

    /** Whether the JSR 303 @DecimalMax annotation will be added */
    private String decimalMax;

    /** Whether the JSR 303 @DecimalMin annotation will be added */
    private String decimalMin;

    public StringOrNumericField(final String physicalTypeIdentifier,
            final JavaType fieldType, final JavaSymbolName fieldName) {
        super(physicalTypeIdentifier, fieldType, fieldName);
    }

    @Override
    public void decorateAnnotationsList(
            final List<AnnotationMetadataBuilder> annotations) {
        super.decorateAnnotationsList(annotations);
        if (decimalMin != null) {
            final List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
            attrs.add(new StringAttributeValue(new JavaSymbolName("value"),
                    decimalMin));
            annotations.add(new AnnotationMetadataBuilder(DECIMAL_MIN, attrs));
        }
        if (decimalMax != null) {
            final List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
            attrs.add(new StringAttributeValue(new JavaSymbolName("value"),
                    decimalMax));
            annotations.add(new AnnotationMetadataBuilder(DECIMAL_MAX, attrs));
        }
    }

    public String getDecimalMax() {
        return decimalMax;
    }

    public String getDecimalMin() {
        return decimalMin;
    }

    public void setDecimalMax(final String decimalMax) {
        if (JdkJavaType.isDoubleOrFloat(getFieldType())) {
            LOGGER.warning("@DecimalMax constraint is not supported for double or float fields");
        }
        this.decimalMax = decimalMax;
    }

    public void setDecimalMin(final String decimalMin) {
        if (JdkJavaType.isDoubleOrFloat(getFieldType())) {
            LOGGER.warning("@DecimalMin constraint is not supported for double or float fields");
        }
        this.decimalMin = decimalMin;
    }
}
