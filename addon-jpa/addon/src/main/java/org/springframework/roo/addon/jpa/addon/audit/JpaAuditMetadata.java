package org.springframework.roo.addon.jpa.addon.audit;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.addon.jpa.annotations.audit.RooJpaAudit;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
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

  private List<FieldMetadata> auditFields = new ArrayList<FieldMetadata>();

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
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, List<FieldMetadata> auditFields) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate
        .isTrue(
            isValid(identifier),
            "Metadata identification string '%s' does not appear to be a valid physical type identifier",
            identifier);

    // Saving audit fields to provide them to other components
    // that invoke this Metadata
    this.auditFields = auditFields;

    // Add @EntityListeners annotation
    ensureGovernorIsAnnotated(getEntityListenersAnnotation());

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

}
