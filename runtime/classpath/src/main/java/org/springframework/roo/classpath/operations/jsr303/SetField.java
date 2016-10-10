package org.springframework.roo.classpath.operations.jsr303;

import static org.springframework.roo.model.JdkJavaType.HASH_SET;
import static org.springframework.roo.model.JpaJavaType.ELEMENT_COLLECTION;
import static org.springframework.roo.model.JpaJavaType.CASCADE_TYPE;
import static org.springframework.roo.model.JpaJavaType.FETCH_TYPE;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.MANY_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_ONE;

import org.apache.commons.lang3.StringUtils;

import static org.springframework.roo.model.JpaJavaType.MANY_TO_ONE;
import static org.springframework.roo.model.JpaJavaType.JOIN_COLUMN;
import static org.springframework.roo.model.JpaJavaType.JOIN_COLUMNS;
import static org.springframework.roo.model.JpaJavaType.JOIN_TABLE;

import java.util.*;

import org.springframework.roo.classpath.details.annotations.*;
import org.springframework.roo.classpath.operations.*;
import org.springframework.roo.model.*;

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
  private List<AnnotationMetadataBuilder> additionaAnnotations =
      new ArrayList<AnnotationMetadataBuilder>();
  private Cascade[] cascadeType;
  private final boolean isDto;
  private Boolean orphanRemoval;
  /**
   * Whether the JSR 220 @OneToMany.mappedBy annotation attribute will be
   * added
   */
  private JavaSymbolName mappedBy;

  /**
   * Constructor for SetField
   *
   * @param physicalTypeIdentifier
   * @param fieldType
   * @param fieldName
   * @param genericParameterTypeName
   * @param cardinality
   * @param cascadeType
   * @param isDto whether target class is a DTO
   */
  public SetField(final String physicalTypeIdentifier, final JavaType fieldType,
      final JavaSymbolName fieldName, final JavaType genericParameterTypeName,
      final Cardinality cardinality, final Cascade[] cascadeType, final boolean isDto) {
    super(physicalTypeIdentifier, fieldType, fieldName, genericParameterTypeName);
    this.cardinality = cardinality;
    this.cascadeType = cascadeType;
    this.isDto = isDto;
  }

  @Override
  public void decorateAnnotationsList(final List<AnnotationMetadataBuilder> annotations) {
    super.decorateAnnotationsList(annotations);
    final List<AnnotationAttributeValue<?>> attributes =
        new ArrayList<AnnotationAttributeValue<?>>();

    // Manage cardinality only if class is not a DTO
    if (!isDto) {
      if (cardinality == null) {
        // Assume set field is an enum
        annotations.add(new AnnotationMetadataBuilder(ELEMENT_COLLECTION));
      } else {
        // Add cascade if option exists
        if (cascadeType != null) {
          List<EnumAttributeValue> cascadeValues = new ArrayList<EnumAttributeValue>();
          for (Cascade type : cascadeType) {
            cascadeValues.add(new EnumAttributeValue(new JavaSymbolName("cascade"),
                new EnumDetails(CASCADE_TYPE, new JavaSymbolName(type.name()))));
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
      }
    }

    // Add additional annotations (if any)
    if (additionaAnnotations != null) {
      annotations.addAll(additionaAnnotations);
    }
  }

  public Fetch getFetch() {
    return fetch;
  }

  @Override
  public JavaType getInitializer() {
    final List<JavaType> params = new ArrayList<JavaType>();
    params.add(getGenericParameterTypeName());
    return new JavaType(HASH_SET.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, params);
  }

  public void addAdditionaAnnotation(AnnotationMetadataBuilder annotationBuilder) {
    additionaAnnotations.add(annotationBuilder);
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

  public void setMappedBy(final JavaSymbolName mappedBy) {
    this.mappedBy = mappedBy;
  }

  public void setOrphanRemoval(Boolean orphanRemoval) {
    this.orphanRemoval = orphanRemoval;
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
        joinColumnAnnotation.addStringAttribute("referencedColumnName", referencedColumns[i]);
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
