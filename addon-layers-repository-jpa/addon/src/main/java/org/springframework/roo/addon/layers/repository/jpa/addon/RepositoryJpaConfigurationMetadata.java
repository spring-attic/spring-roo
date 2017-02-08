package org.springframework.roo.addon.layers.repository.jpa.addon;

import org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepository;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * = RepositoryJpaConfigurationMetadata
 * 
 * Metadata for {@link RooJpaRepository}.
 *
 * @author Sergio Clares
 * @since 2.0
 */
public class RepositoryJpaConfigurationMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = RepositoryJpaConfigurationMetadata.class
      .getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
  }

  public static String createIdentifier(ClassOrInterfaceTypeDetails repositoryDetails) {
    final LogicalPath repositoryLogicalPath =
        PhysicalTypeIdentifier.getPath(repositoryDetails.getDeclaredByMetadataId());
    return createIdentifier(repositoryDetails.getType(), repositoryLogicalPath);
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
   * @param identifier the identifier for this item of metadata (required)
   * @param aspectName the Java type of the ITD (required)
   * @param governorPhysicalTypeMetadata the governor, which is expected to
   *            contain a {@link ClassOrInterfaceTypeDetails} (required)
   *
   */
  public RepositoryJpaConfigurationMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final JavaType applicationMainType,
      final boolean isSpringletsSecurityEnabled) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    // Build @Configuration
    AnnotationMetadataBuilder configurationAnnotationBuilder =
        new AnnotationMetadataBuilder(SpringJavaType.CONFIGURATION);
    // Add annotation
    ensureGovernorIsAnnotated(configurationAnnotationBuilder);

    // Build @EnableJpaRepositories
    AnnotationMetadataBuilder enableRepositoriesAnnotationBuilder =
        new AnnotationMetadataBuilder(SpringJavaType.ENABLE_JPA_REPOSITORIES);
    // Add default Spring Roo repository class from Springlets to new annotation
    enableRepositoriesAnnotationBuilder.addClassAttribute("repositoryBaseClass",
        SpringletsJavaType.SPRINGLETS_DETACHABLE_JPA_REPOSITORY_IMPL);
    // Add main application configuration class to the annotation
    enableRepositoriesAnnotationBuilder
        .addClassAttribute("basePackageClasses", applicationMainType);
    // Add annnotation
    ensureGovernorIsAnnotated(enableRepositoriesAnnotationBuilder);

    // Build @EntityScan
    if (isSpringletsSecurityEnabled) {
      AnnotationMetadataBuilder entityScanAnnotationBuilder =
          new AnnotationMetadataBuilder(SpringJavaType.ENTITY_SCAN);
      entityScanAnnotationBuilder.addClassAttribute("basePackageClasses", applicationMainType);
      ensureGovernorIsAnnotated(entityScanAnnotationBuilder);
    }

    // Build the ITD
    itdTypeDetails = builder.build();
  }

}
