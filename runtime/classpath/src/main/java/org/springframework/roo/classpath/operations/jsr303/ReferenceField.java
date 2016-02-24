package org.springframework.roo.classpath.operations.jsr303;

import static org.springframework.roo.model.JpaJavaType.FETCH_TYPE;
import static org.springframework.roo.model.JpaJavaType.JOIN_COLUMN;
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
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Properties used by the many-to-one side of a relationship (called a
 * "reference").
 * <p>
 * For example, an Order-LineItem link would have the LineItem contain a
 * "reference" back to Order.
 * <p>
 * Limited support for collection mapping is provided. This reflects the
 * pragmatic goals of ROO and the fact a user can edit the generated files by
 * hand anyway.
 * <p>
 * This field is intended for use with JSR 220 and will create a @ManyToOne and @JoinColumn
 * annotation.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class ReferenceField extends FieldDetails {

    private final Cardinality cardinality;
    private Fetch fetch;
    private String joinColumnName;
    private String referencedColumnName;

    public ReferenceField(final String physicalTypeIdentifier,
            final JavaType fieldType, final JavaSymbolName fieldName,
            final Cardinality cardinality) {
        super(physicalTypeIdentifier, fieldType, fieldName);
        this.cardinality = cardinality;
    }

    @Override
    public void decorateAnnotationsList(
            final List<AnnotationMetadataBuilder> annotations) {
        super.decorateAnnotationsList(annotations);
        final List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();

        if (fetch != null) {
            JavaSymbolName value = new JavaSymbolName("EAGER");
            if (fetch == Fetch.LAZY) {
                value = new JavaSymbolName("LAZY");
            }
            attributes.add(new EnumAttributeValue(new JavaSymbolName("fetch"),
                    new EnumDetails(FETCH_TYPE, value)));
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

        if (joinColumnName != null) {
            final List<AnnotationAttributeValue<?>> joinColumnAttrs = new ArrayList<AnnotationAttributeValue<?>>();
            joinColumnAttrs.add(new StringAttributeValue(new JavaSymbolName(
                    "name"), joinColumnName));

            if (referencedColumnName != null) {
                joinColumnAttrs.add(new StringAttributeValue(
                        new JavaSymbolName("referencedColumnName"),
                        referencedColumnName));
            }
            annotations.add(new AnnotationMetadataBuilder(JOIN_COLUMN,
                    joinColumnAttrs));
        }
    }

    public Fetch getFetch() {
        return fetch;
    }

    public String getJoinColumnName() {
        return joinColumnName;
    }

    public String getReferencedColumnName() {
        return referencedColumnName;
    }

    public void setFetch(final Fetch fetch) {
        this.fetch = fetch;
    }

    public void setJoinColumnName(final String joinColumnName) {
        this.joinColumnName = joinColumnName;
    }

    public void setReferencedColumnName(final String referencedColumnName) {
        this.referencedColumnName = referencedColumnName;
    }
}
