package org.springframework.roo.classpath.operations.jsr303;

import static org.springframework.roo.model.JpaJavaType.ENUMERATED;
import static org.springframework.roo.model.JpaJavaType.ENUM_TYPE;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.operations.EnumType;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * This field is intended for use with JSR 220 and will create a @Enumerated
 * annotation.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class EnumField extends FieldDetails {

    private EnumType enumType;

    public EnumField(final String physicalTypeIdentifier,
            final JavaType fieldType, final JavaSymbolName fieldName) {
        super(physicalTypeIdentifier, fieldType, fieldName);
    }

    @Override
    public void decorateAnnotationsList(
            final List<AnnotationMetadataBuilder> annotations) {
        super.decorateAnnotationsList(annotations);
        final List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();

        if (enumType != null) {
            JavaSymbolName value = new JavaSymbolName("ORDINAL");
            if (enumType == EnumType.STRING) {
                value = new JavaSymbolName("STRING");
            }
            attributes.add(new EnumAttributeValue(new JavaSymbolName("value"),
                    new EnumDetails(ENUM_TYPE, value)));
        }

        annotations.add(new AnnotationMetadataBuilder(ENUMERATED, attributes));
    }

    public EnumType getEnumType() {
        return enumType;
    }

    public void setEnumType(final EnumType enumType) {
        this.enumType = enumType;
    }
}
