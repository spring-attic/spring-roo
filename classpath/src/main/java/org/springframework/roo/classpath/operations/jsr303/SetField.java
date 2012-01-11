package org.springframework.roo.classpath.operations.jsr303;

import static org.springframework.roo.model.JdkJavaType.HASH_SET;
import static org.springframework.roo.model.JpaJavaType.CASCADE_TYPE;
import static org.springframework.roo.model.JpaJavaType.ELEMENT_COLLECTION;
import static org.springframework.roo.model.JpaJavaType.FETCH_TYPE;
import static org.springframework.roo.model.JpaJavaType.MANY_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.MANY_TO_ONE;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_ONE;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.classpath.operations.Fetch;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Properties used by the one side of a many-to-one relationship or an @ElementCollection
 * of enums (called a "set").
 * <p>
 * For example, an Order-LineItem link would have the Order contain a "set" of
 * Orders.
 * <p>
 * Limited support for collection mapping is provided. This reflects the
 * pragmatic goals of the tool and the fact a user can edit the generated files
 * by hand anyway.
 * <p>
 * This field is intended for use with JSR 220 and will create a @OneToMany
 * annotation or in the case of enums, an @ElementCollection annotation will be
 * created.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class SetField extends CollectionField {

    private final Cardinality cardinality;

    private Fetch fetch;
    /**
     * Whether the JSR 220 @OneToMany.mappedBy annotation attribute will be
     * added
     */
    private JavaSymbolName mappedBy;

    public SetField(final String physicalTypeIdentifier,
            final JavaType fieldType, final JavaSymbolName fieldName,
            final JavaType genericParameterTypeName,
            final Cardinality cardinality) {
        super(physicalTypeIdentifier, fieldType, fieldName,
                genericParameterTypeName);
        this.cardinality = cardinality;
    }

    @Override
    public void decorateAnnotationsList(
            final List<AnnotationMetadataBuilder> annotations) {
        super.decorateAnnotationsList(annotations);
        final List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();

        if (cardinality == null) {
            // Assume set field is an enum
            annotations.add(new AnnotationMetadataBuilder(ELEMENT_COLLECTION));
        }
        else {
            attributes.add(new EnumAttributeValue(
                    new JavaSymbolName("cascade"), new EnumDetails(
                            CASCADE_TYPE, new JavaSymbolName("ALL"))));
            if (fetch != null) {
                JavaSymbolName value = new JavaSymbolName("EAGER");
                if (fetch == Fetch.LAZY) {
                    value = new JavaSymbolName("LAZY");
                }
                attributes.add(new EnumAttributeValue(new JavaSymbolName(
                        "fetch"), new EnumDetails(FETCH_TYPE, value)));
            }
            if (mappedBy != null) {
                attributes.add(new StringAttributeValue(new JavaSymbolName(
                        "mappedBy"), mappedBy.getSymbolName()));
            }

            switch (cardinality) {
            case ONE_TO_MANY:
                annotations.add(new AnnotationMetadataBuilder(ONE_TO_MANY,
                        attributes));
                break;
            case MANY_TO_MANY:
                annotations.add(new AnnotationMetadataBuilder(MANY_TO_MANY,
                        attributes));
                break;
            case ONE_TO_ONE:
                annotations.add(new AnnotationMetadataBuilder(ONE_TO_ONE,
                        attributes));
                break;
            default:
                annotations.add(new AnnotationMetadataBuilder(MANY_TO_ONE,
                        attributes));
                break;
            }
        }
    }

    public Fetch getFetch() {
        return fetch;
    }

    @Override
    public JavaType getInitializer() {
        final List<JavaType> params = new ArrayList<JavaType>();
        params.add(getGenericParameterTypeName());
        return new JavaType(HASH_SET.getFullyQualifiedTypeName(), 0,
                DataType.TYPE, null, params);
    }

    public JavaSymbolName getMappedBy() {
        return mappedBy;
    }

    public void setFetch(final Fetch fetch) {
        this.fetch = fetch;
    }

    public void setMappedBy(final JavaSymbolName mappedBy) {
        this.mappedBy = mappedBy;
    }
}
