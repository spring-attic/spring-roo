package org.springframework.roo.classpath.operations.jsr303;

import static org.springframework.roo.model.Jsr303JavaType.DIGITS;
import static org.springframework.roo.model.Jsr303JavaType.MAX;
import static org.springframework.roo.model.Jsr303JavaType.MIN;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.IntegerAttributeValue;
import org.springframework.roo.classpath.details.annotations.LongAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;

public class NumericField extends StringOrNumericField {

    /**
     * Whether the JSR 303 @Digits annotation will be added (you must also set
     * digitsInteger)
     */
    private Integer digitsFraction;

    /**
     * Whether the JSR 303 @Digits annotation will be added (you must also set
     * digitsFractional)
     */
    private Integer digitsInteger;

    /** Whether the JSR 303 @Max annotation will be added */
    private Long max;

    /** Whether the JSR 303 @Min annotation will be added */
    private Long min;

    public NumericField(final String physicalTypeIdentifier,
            final JavaType fieldType, final JavaSymbolName fieldName) {
        super(physicalTypeIdentifier, fieldType, fieldName);
    }

    @Override
    public void decorateAnnotationsList(
            final List<AnnotationMetadataBuilder> annotations) {
        super.decorateAnnotationsList(annotations);
        if (min != null) {
            final List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
            attrs.add(new LongAttributeValue(new JavaSymbolName("value"), min));
            annotations.add(new AnnotationMetadataBuilder(MIN, attrs));
        }
        if (max != null) {
            final List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
            attrs.add(new LongAttributeValue(new JavaSymbolName("value"), max));
            annotations.add(new AnnotationMetadataBuilder(MAX, attrs));
        }
        Validate.isTrue(isDigitsSetCorrectly(),
                "Validation constraints for @Digit are not correctly set");
        if (digitsInteger != null) {
            final List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
            attrs.add(new IntegerAttributeValue(new JavaSymbolName("integer"),
                    digitsInteger));
            attrs.add(new IntegerAttributeValue(new JavaSymbolName("fraction"),
                    digitsFraction));
            annotations.add(new AnnotationMetadataBuilder(DIGITS, attrs));
        }
    }

    public Integer getDigitsFraction() {
        return digitsFraction;
    }

    public Integer getDigitsInteger() {
        return digitsInteger;
    }

    public Long getMax() {
        return max;
    }

    public Long getMin() {
        return min;
    }

    public boolean isDigitsSetCorrectly() {
        return digitsInteger == null && digitsFraction == null
                || digitsInteger != null && digitsFraction != null;
    }

    public void setDigitsFraction(final Integer digitsFractional) {
        digitsFraction = digitsFractional;
    }

    public void setDigitsInteger(final Integer digitsInteger) {
        this.digitsInteger = digitsInteger;
    }

    public void setMax(final Long max) {
        if (JdkJavaType.isDoubleOrFloat(getFieldType())) {
            LOGGER.warning("@Max constraint is not supported for double or float fields");
        }
        this.max = max;
    }

    public void setMin(final Long min) {
        if (JdkJavaType.isDoubleOrFloat(getFieldType())) {
            LOGGER.warning("@Min constraint is not supported for double or float fields");
        }
        this.min = min;
    }
}
