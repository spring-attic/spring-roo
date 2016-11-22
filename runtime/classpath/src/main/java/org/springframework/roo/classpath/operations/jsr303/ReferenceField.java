package org.springframework.roo.classpath.operations.jsr303;

import static org.springframework.roo.model.JpaJavaType.CASCADE_TYPE;
import static org.springframework.roo.model.JpaJavaType.FETCH_TYPE;
import static org.springframework.roo.model.JpaJavaType.JOIN_COLUMN;
import static org.springframework.roo.model.JpaJavaType.JOIN_COLUMNS;
import static org.springframework.roo.model.JpaJavaType.JOIN_TABLE;
import static org.springframework.roo.model.JpaJavaType.MANY_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.MANY_TO_ONE;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_ONE;

import org.apache.commons.lang3.StringUtils;
import org.springframework.roo.classpath.details.FieldDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.NestedAnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.classpath.operations.Cascade;
import org.springframework.roo.classpath.operations.Fetch;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

import java.util.ArrayList;
import java.util.List;

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
  private List<AnnotationMetadataBuilder> additionaAnnotations =
      new ArrayList<AnnotationMetadataBuilder>();
  private Cascade[] cascadeType;
  private Boolean orphanRemoval;
  /**
   * Whether the JSR 220 @OneToMany.mappedBy annotation attribute will be
   * added
   */
  private JavaSymbolName mappedBy;

  public ReferenceField(final String physicalTypeIdentifier, final JavaType fieldType,
      final JavaSymbolName fieldName, final Cardinality cardinality, Cascade[] cascadeType) {
    super(physicalTypeIdentifier, fieldType, fieldName);
    this.cardinality = cardinality;
    this.cascadeType = cascadeType;
  }

  @Override
  public void decorateAnnotationsList(final List<AnnotationMetadataBuilder> annotations) {
    super.decorateAnnotationsList(annotations);
    final List<AnnotationAttributeValue<?>> attributes =
        new ArrayList<AnnotationAttributeValue<?>>();

    // Add cascade if option exists
    if (cascadeType != null) {
      List<EnumAttributeValue> cascadeValues = new ArrayList<EnumAttributeValue>();
      for (Cascade type : cascadeType) {
        cascadeValues.add(new EnumAttributeValue(new JavaSymbolName("cascade"), new EnumDetails(
            CASCADE_TYPE, new JavaSymbolName(type.name()))));
      }

      attributes.add(new ArrayAttributeValue<EnumAttributeValue>(new JavaSymbolName("cascade"),
          cascadeValues));
    }

    // Add orphanRemoval if option exists
    if (getOrphanRemoval() != null) {
      attributes.add(new BooleanAttributeValue(new JavaSymbolName("orphanRemoval"),
          getOrphanRemoval().booleanValue()));
    }
    if (fetch != null) {
      JavaSymbolName value = new JavaSymbolName("EAGER");
      if (fetch == Fetch.LAZY) {
        value = new JavaSymbolName("LAZY");
      }
      attributes.add(new EnumAttributeValue(new JavaSymbolName("fetch"), new EnumDetails(
          FETCH_TYPE, value)));
    }
    if (mappedBy != null) {
      attributes.add(new StringAttributeValue(new JavaSymbolName("mappedBy"), mappedBy
          .getSymbolName()));
    }

    switch (cardinality) {
      case ONE_TO_MANY:
        annotations.add(new AnnotationMetadataBuilder(ONE_TO_MANY, attributes));
        break;
      case MANY_TO_MANY:
        annotations.add(new AnnotationMetadataBuilder(MANY_TO_MANY, attributes));
        break;
      case ONE_TO_ONE:
        annotations.add(new AnnotationMetadataBuilder(ONE_TO_ONE, attributes));
        break;
      default:
        annotations.add(new AnnotationMetadataBuilder(MANY_TO_ONE, attributes));
        break;
    }
    // Add additional annotations (if any)
    if (additionaAnnotations != null) {
      annotations.addAll(additionaAnnotations);
    }
  }

  public Fetch getFetch() {
    return fetch;
  }

  public JavaSymbolName getMappedBy() {
    return mappedBy;
  }

  public Boolean getOrphanRemoval() {
    return orphanRemoval;
  }

  public void setFetch(final Fetch fetch) {
    this.fetch = fetch;
  }

  public void setOrphanRemoval(Boolean orphanRemoval) {
    this.orphanRemoval = orphanRemoval;
  }

  public void setMappedBy(JavaSymbolName mappedBy) {
    this.mappedBy = mappedBy;
  }

  /**
   * Add @JoinColumn annotation to field
   *
   * @param joinColumn
   * @param referencedColumn
   */
  public void setJoinColumn(String joinColumn, String referencedColumn) {
    setJoinAnnotations(null, new String[] {joinColumn}, new String[] {referencedColumn}, null, null);
  }

  /**
   * Add @JoinColumn annotation to field
   *
   * @param joinColumn
   */
  public void setJoinColumn(String joinColumn) {
    setJoinAnnotations(null, new String[] {joinColumn}, null, null, null);
  }


  public void addAdditionaAnnotation(AnnotationMetadataBuilder annotationBuilder) {
    additionaAnnotations.add(annotationBuilder);
  }

  /**
   * Fill {@link #joinTableAttributes} for building @JoinTable annotation. The annotation
   * would have some nested @JoinColumn annotations in each of its "joinColumns" and
   * "inverseJoinColumns" attributes.
   *
   * @param joinTableName
   * @param joinColumns
   * @param referencedColumns
   * @param inverseJoinColumns
   * @param inverseReferencedColumns
   */
  public void setJoinAnnotations(String joinTableName, String[] joinColumns,
      String[] referencedColumns, String[] inverseJoinColumns, String[] inverseReferencedColumns) {

    final List<AnnotationMetadataBuilder> joinColumnsBuilders =
        new ArrayList<AnnotationMetadataBuilder>();
    if (joinColumns != null) {

      // Build joinColumns attribute
      for (int i = 0; i < joinColumns.length; i++) {

        // Build @JoinColumn annotation for owner side of the relation
        final AnnotationMetadataBuilder joinColumnAnnotation =
            new AnnotationMetadataBuilder(JOIN_COLUMN);
        joinColumnAnnotation.addStringAttribute("name", joinColumns[i]);
        if (referencedColumns != null) {
          joinColumnAnnotation.addStringAttribute("referencedColumnName", referencedColumns[i]);
        }
        joinColumnsBuilders.add(joinColumnAnnotation);
      }
    }


    final List<AnnotationMetadataBuilder> inverseJoinColumnsBuilders =
        new ArrayList<AnnotationMetadataBuilder>();
    if (inverseJoinColumns != null) {

      // Build inverseJoinColumns attribute
      for (int i = 0; i < inverseJoinColumns.length; i++) {

        // Build @JoinColumn annotation for the not owner side of the relation
        final AnnotationMetadataBuilder inverseJoinColumnsAnnotation =
            new AnnotationMetadataBuilder(JOIN_COLUMN);
        inverseJoinColumnsAnnotation.addStringAttribute("name", inverseJoinColumns[i]);
        inverseJoinColumnsAnnotation.addStringAttribute("referencedColumnName",
            inverseReferencedColumns[i]);
        inverseJoinColumnsBuilders.add(inverseJoinColumnsAnnotation);

      }
    }

    if (StringUtils.isNotBlank(joinTableName) || !inverseJoinColumnsBuilders.isEmpty()) {
      // add @JoinTable annotation

      // Add attributes for @JoinTable annotation
      final List<AnnotationAttributeValue<?>> joinTableAttributes =
          new ArrayList<AnnotationAttributeValue<?>>();

      // If name not specified, use default name value
      joinTableAttributes.add(new StringAttributeValue(new JavaSymbolName("name"), joinTableName));

      // If joinColumns options were not specified, use default @JoinColumn values
      if (joinColumns != null) {
        final List<AnnotationAttributeValue<?>> joinColumnsAnnotations =
            new ArrayList<AnnotationAttributeValue<?>>();
        for (AnnotationMetadataBuilder joinColumnAnnotation : joinColumnsBuilders) {
          joinColumnsAnnotations.add(new NestedAnnotationAttributeValue(new JavaSymbolName(
              "joinColumns"), joinColumnAnnotation.build()));
        }
        joinTableAttributes.add(new ArrayAttributeValue<AnnotationAttributeValue<?>>(
            new JavaSymbolName("joinColumns"), joinColumnsAnnotations));
      }

      // If inverseJoinColumns options were not specified, use default @JoinColumn values
      if (inverseJoinColumns != null) {
        final List<AnnotationAttributeValue<?>> inverseJoinColumnsAnnotations =
            new ArrayList<AnnotationAttributeValue<?>>();
        for (AnnotationMetadataBuilder inverseJoinColumnsAnnotation : inverseJoinColumnsBuilders) {
          inverseJoinColumnsAnnotations.add(new NestedAnnotationAttributeValue(new JavaSymbolName(
              "inverseJoinColumns"), inverseJoinColumnsAnnotation.build()));
        }

        joinTableAttributes.add(new ArrayAttributeValue<AnnotationAttributeValue<?>>(
            new JavaSymbolName("inverseJoinColumns"), inverseJoinColumnsAnnotations));
      }

      // Add @JoinTable to additonalAnnotations
      additionaAnnotations.add(new AnnotationMetadataBuilder(JOIN_TABLE, joinTableAttributes));

    } else if (!joinColumnsBuilders.isEmpty()) {

      // Manage @JoinColumn

      if (joinColumnsBuilders.size() == 1) {

        // Just one @JoinColumn
        additionaAnnotations.add(joinColumnsBuilders.iterator().next());
      } else {

        // Multiple @JoinColumn, wrap with @JoinColumns
        final AnnotationMetadataBuilder joinColumnsAnnotation =
            new AnnotationMetadataBuilder(JOIN_COLUMNS);

        final List<AnnotationAttributeValue<?>> joinColumnsAnnotations =
            new ArrayList<AnnotationAttributeValue<?>>();
        for (AnnotationMetadataBuilder joinColumnAnnotation : joinColumnsBuilders) {
          joinColumnsAnnotations.add(new NestedAnnotationAttributeValue(
              new JavaSymbolName("value"), joinColumnAnnotation.build()));
        }
        joinColumnsAnnotation.addAttribute(new ArrayAttributeValue<AnnotationAttributeValue<?>>(
            new JavaSymbolName("value"), joinColumnsAnnotations));

        // Add @JoinColumns
        additionaAnnotations.add(joinColumnsAnnotation);
      }
    }
  }
}
