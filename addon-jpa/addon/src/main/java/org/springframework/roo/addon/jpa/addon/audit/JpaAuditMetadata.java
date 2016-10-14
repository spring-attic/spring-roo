package org.springframework.roo.addon.jpa.addon.audit;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.addon.jpa.annotations.audit.RooJpaAudit;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooJpaAudit}.
 * <p>
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class JpaAuditMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = JpaAuditMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private final JpaAuditAnnotationValues annotationValues;
  private ArrayList<FieldMetadata> auditFields = new ArrayList<FieldMetadata>();

  public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
  }

  public static JavaType getJavaType(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  public static String getMetadataIdentiferType() {
    return PROVIDES_TYPE;
  }

  public static LogicalPath getPath(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  public static boolean isValid(final String metadataIdentificationString) {
    return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING,
        metadataIdentificationString);
  }

  /**
   * Constructor
   * 
   * @param identifier
   * @param aspectName
   * @param governorPhysicalTypeMetadata
   */
  public JpaAuditMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final JpaAuditAnnotationValues annotationValues) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate
        .isTrue(
            isValid(identifier),
            "Metadata identification string '%s' does not appear to be a valid physical type identifier",
            identifier);

    this.annotationValues = annotationValues;

    // Add audit fields
    FieldMetadataBuilder createdDateField = getCreatedDateField();
    ensureGovernorHasField(createdDateField);
    FieldMetadataBuilder modifiedDateField = getModifiedDateField();
    ensureGovernorHasField(modifiedDateField);
    FieldMetadataBuilder createdByField = getCreatedByField();
    ensureGovernorHasField(createdByField);
    FieldMetadataBuilder modifiedByField = getModifiedByField();
    ensureGovernorHasField(modifiedByField);

    // Add getters for audit fields
    MethodMetadataBuilder createdDateGetter = getDeclaredGetter(createdDateField);
    if (createdDateGetter != null) {
      ensureGovernorHasMethod(getDeclaredGetter(createdDateField));
    }
    MethodMetadataBuilder modifiedDateGetter = getDeclaredGetter(modifiedDateField);
    if (modifiedDateGetter != null) {
      ensureGovernorHasMethod(getDeclaredGetter(modifiedDateField));
    }
    MethodMetadataBuilder createdByGetter = getDeclaredGetter(createdByField);
    if (createdByGetter != null) {
      ensureGovernorHasMethod(getDeclaredGetter(createdByField));
    }
    MethodMetadataBuilder modifiedByGetter = getDeclaredGetter(modifiedByField);
    if (modifiedByGetter != null) {
      ensureGovernorHasMethod(getDeclaredGetter(modifiedByField));
    }

    // Add @EntityListeners annotation
    ensureGovernorIsAnnotated(getEntityListenersAnnotation());

    // Save audit fields
    auditFields.addAll(Arrays.asList(createdDateField.build(), modifiedDateField.build(),
        createdByField.build(), modifiedByField.build()));

    // Build ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Returns the list of audit fields
   * 
   * @return
   */
  public List<FieldMetadata> getAuditFields() {
    return auditFields;
  }

  /**
     * Builds createdDate field for storing entity's created date 
     * 
     * @return FieldMetadataBuilder for building field into ITD
     */
  private FieldMetadataBuilder getCreatedDateField() {

    // Create field annotations
    List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Only add @Column if required by annotation @RooJpaAudit
    if (StringUtils.isNotBlank(this.annotationValues.getCreatedDateColumn())) {
      AnnotationMetadataBuilder columnAnnotation =
          new AnnotationMetadataBuilder(JpaJavaType.COLUMN);
      columnAnnotation.addStringAttribute("name", this.annotationValues.getCreatedDateColumn());
      annotations.add(columnAnnotation);
    }

    AnnotationMetadataBuilder createdDateAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.CREATED_DATE);
    annotations.add(createdDateAnnotation);
    AnnotationMetadataBuilder temporalAnnotation =
        new AnnotationMetadataBuilder(JpaJavaType.TEMPORAL);
    temporalAnnotation.addEnumAttribute("value", new EnumDetails(JpaJavaType.TEMPORAL_TYPE,
        new JavaSymbolName("TIMESTAMP")));
    annotations.add(temporalAnnotation);

    // Create field
    FieldMetadataBuilder fieldBuilder =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, new JavaSymbolName(
            "createdDate"), JdkJavaType.CALENDAR);

    return fieldBuilder;
  }

  /**
   * Builds modifiedDate field for storing entity's last modified date 
   * 
   * @return FieldMetadataBuilder for building field into ITD
   */
  private FieldMetadataBuilder getModifiedDateField() {

    // Create field annotations
    List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Only add @Column if required by annotation @RooJpaAudit
    if (StringUtils.isNotBlank(this.annotationValues.getModifiedDateColumn())) {
      AnnotationMetadataBuilder columnAnnotation =
          new AnnotationMetadataBuilder(JpaJavaType.COLUMN);
      columnAnnotation.addStringAttribute("name", this.annotationValues.getModifiedDateColumn());
      annotations.add(columnAnnotation);
    }

    AnnotationMetadataBuilder createdDateAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.LAST_MODIFIED_DATE);
    annotations.add(createdDateAnnotation);
    AnnotationMetadataBuilder temporalAnnotation =
        new AnnotationMetadataBuilder(JpaJavaType.TEMPORAL);
    temporalAnnotation.addEnumAttribute("value", new EnumDetails(JpaJavaType.TEMPORAL_TYPE,
        new JavaSymbolName("TIMESTAMP")));
    annotations.add(temporalAnnotation);

    // Create field
    FieldMetadataBuilder fieldBuilder =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, new JavaSymbolName(
            "modifiedDate"), JdkJavaType.CALENDAR);

    return fieldBuilder;
  }

  /**
   * Builds createdBy field for storing user who creates entity registers
   * 
   * @return FieldMetadataBuilder for building field into ITD
   */
  private FieldMetadataBuilder getCreatedByField() {

    // Create field annotations
    List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Only add @Column if required by annotation @RooJpaAudit
    if (StringUtils.isNotBlank(this.annotationValues.getCreatedByColumn())) {
      AnnotationMetadataBuilder columnAnnotation =
          new AnnotationMetadataBuilder(JpaJavaType.COLUMN);
      columnAnnotation.addStringAttribute("name", this.annotationValues.getCreatedByColumn());
      annotations.add(columnAnnotation);
    }

    AnnotationMetadataBuilder createdDateAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.CREATED_BY);
    annotations.add(createdDateAnnotation);

    // Create field
    FieldMetadataBuilder fieldBuilder =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, new JavaSymbolName(
            "createdBy"), JavaType.STRING);

    return fieldBuilder;
  }

  /**
   * Builds modifiedBy field for storing user who last modifies entity registers
   * 
   * @return FieldMetadataBuilder for building field into ITD
   */
  private FieldMetadataBuilder getModifiedByField() {

    // Create field annotations
    List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Only add @Column if required by annotation @RooJpaAudit
    if (StringUtils.isNotBlank(this.annotationValues.getModifiedByColumn())) {
      AnnotationMetadataBuilder columnAnnotation =
          new AnnotationMetadataBuilder(JpaJavaType.COLUMN);
      columnAnnotation.addStringAttribute("name", this.annotationValues.getModifiedByColumn());
      annotations.add(columnAnnotation);
    }

    AnnotationMetadataBuilder createdDateAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.LAST_MODIFIED_BY);
    annotations.add(createdDateAnnotation);

    // Create field
    FieldMetadataBuilder fieldBuilder =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, new JavaSymbolName(
            "modifiedBy"), JavaType.STRING);

    return fieldBuilder;
  }

  /**
   * Builds @EntityListeners annotation
   * 
   * @return AnnotationMetadataBuilder with the prepared annotation
   */
  private AnnotationMetadataBuilder getEntityListenersAnnotation() {
    AnnotationMetadataBuilder annotation =
        new AnnotationMetadataBuilder(JpaJavaType.ENTITY_LISTENERS);
    annotation.addClassAttribute("value", SpringJavaType.AUDITING_ENTITY_LISTENER);

    return annotation;
  }

  /**
   * Obtains the specific accessor method contained within this ITD.
   * 
   * @param field
   *            that already exists on the type either directly or via
   *            introduction (required; must be declared by this type to be
   *            located)
   * @return the method corresponding to an accessor, or null if not found
   */
  private MethodMetadataBuilder getDeclaredGetter(final FieldMetadataBuilder field) {
    Validate.notNull(field, "Field required");

    // Compute the mutator method name
    final JavaSymbolName methodName = BeanInfoUtils.getAccessorMethodName(field.build());

    // Decide whether we need to produce the accessor method
    if (!Modifier.isTransient(field.getModifier()) && !Modifier.isStatic(field.getModifier())) {
      final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
      bodyBuilder.appendFormalLine("return this." + field.getFieldName().getSymbolName() + ";");

      return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, field.getFieldType(),
          bodyBuilder);
    }

    return null;
  }
}
