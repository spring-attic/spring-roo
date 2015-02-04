package org.springframework.roo.classpath.operations.jsr303;

import static org.springframework.roo.model.Jsr303JavaType.PATTERN;
import static org.springframework.roo.model.Jsr303JavaType.SIZE;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.IntegerAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Extra validation properties specified to String properties.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class StringField extends StringOrNumericField {

    /** Whether the JSR 3030 @Pattern annotation will be added */
    private String regexp;

    /**
     * Whether the JSR 303 @Size annotation will be added; provides the "max"
     * attribute (defaults to {@link Integer#MAX_VALUE})
     */
    private Integer sizeMax;

    /**
     * Whether the JSR 303 @Size annotation will be added; provides the "min"
     * attribute (defaults to 0)
     */
    private Integer sizeMin;

    public StringField(final String physicalTypeIdentifier,
            final JavaSymbolName fieldName) {
        super(physicalTypeIdentifier, JavaType.STRING, fieldName);
    }

    @Deprecated
    public StringField(final String physicalTypeIdentifier,
            final JavaType fieldType, final JavaSymbolName fieldName) {
        super(physicalTypeIdentifier, fieldType, fieldName);
    }

    @Override
    public void decorateAnnotationsList(
            final List<AnnotationMetadataBuilder> annotations) {
        super.decorateAnnotationsList(annotations);
        if (sizeMin != null || sizeMax != null) {
            final List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
            if (sizeMin != null) {
                attrs.add(new IntegerAttributeValue(new JavaSymbolName("min"),
                        sizeMin));
            }
            if (sizeMax != null) {
                attrs.add(new IntegerAttributeValue(new JavaSymbolName("max"),
                        sizeMax));
            }
            annotations.add(new AnnotationMetadataBuilder(SIZE, attrs));
        }
        if (regexp != null) {
            final List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
            attrs.add(new StringAttributeValue(new JavaSymbolName("regexp"),
                    regexp));
            annotations.add(new AnnotationMetadataBuilder(PATTERN, attrs));
        }
    }

    public String getRegexp() {
        return regexp;
    }

    public Integer getSizeMax() {
        return sizeMax;
    }

    public Integer getSizeMin() {
        return sizeMin;
    }

    public void setRegexp(final String regexp) {
        this.regexp = regexp;
    }

    public void setSizeMax(final Integer sizeMax) {
        this.sizeMax = sizeMax;
    }

    public void setSizeMin(final Integer sizeMin) {
        this.sizeMin = sizeMin;
    }
}
