package org.springframework.roo.classpath.operations.jsr303;

import static org.springframework.roo.model.Jsr303JavaType.SIZE;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.IntegerAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

public abstract class CollectionField extends FieldDetails {

    /** The generic type that will be used within the collection */
    private JavaType genericParameterTypeName;

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

    public CollectionField(final String physicalTypeIdentifier,
            final JavaType fieldType, final JavaSymbolName fieldName,
            final JavaType genericParameterTypeName) {
        super(physicalTypeIdentifier, fieldType, fieldName);
        Validate.notNull(genericParameterTypeName,
                "Generic parameter type name is required");
        this.genericParameterTypeName = genericParameterTypeName;
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
    }

    public JavaType getGenericParameterTypeName() {
        return genericParameterTypeName;
    }

    public abstract JavaType getInitializer();

    public Integer getSizeMax() {
        return sizeMax;
    }

    public Integer getSizeMin() {
        return sizeMin;
    }

    public void setGenericParameterTypeName(
            final JavaType genericParameterTypeName) {
        this.genericParameterTypeName = genericParameterTypeName;
    }

    public void setSizeMax(final Integer sizeMax) {
        this.sizeMax = sizeMax;
    }

    public void setSizeMin(final Integer sizeMin) {
        this.sizeMin = sizeMin;
    }
}
