package org.springframework.roo.addon.security.addon.security;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.security.annotations.RooSecurityAuthorizations;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DeclaredMethodAnnotationDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Metadata for {@link RooSecurityAuthorizations}.
 *
 * @author Manuel Iborra
 * @since 2.0
 */
public class SecurityAuthorizationsMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(SecurityAuthorizationsMetadata.class);

  private static final String PROVIDES_TYPE_STRING = SecurityAuthorizationsMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  public static String createIdentifier(final JavaType javaType, final LogicalPath path) {
    return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
  }

  public static String createIdentifier(ClassOrInterfaceTypeDetails details) {
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(details.getDeclaredByMetadataId());
    return createIdentifier(details.getType(), logicalPath);
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
   *            the identifier for this item of metadata (required)
   * @param aspectName
   *            the Java type of the ITD (required)
   * @param governorPhysicalTypeMetadata
   *            the governor, which is expected to contain a
   *            {@link ClassOrInterfaceTypeDetails} (required)
   * @param preAuthorizeMethods
   *            Map with the methods on which to create the annotation @PreAuthorize
   */
  public SecurityAuthorizationsMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      Map<MethodMetadata, String> preAuthorizeMethods) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    // Declare precendence
    builder.setDeclarePrecedence(aspectName);

    for (Map.Entry<MethodMetadata, String> entry : preAuthorizeMethods.entrySet()) {

      // Define @PreAuthorize annotation
      AnnotationMetadataBuilder preauthorizeAnnotation =
          new AnnotationMetadataBuilder(SpringJavaType.PRE_AUTHORIZE);
      preauthorizeAnnotation.addStringAttribute("value", entry.getValue());
      DeclaredMethodAnnotationDetails preauthorizeAnnotationInGetterMethod =
          new DeclaredMethodAnnotationDetails(entry.getKey(), preauthorizeAnnotation.build());
      builder.addMethodAnnotation(preauthorizeAnnotationInGetterMethod);
    }

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  @Override
  public String toString() {
    final ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("identifier", getId());
    builder.append("valid", valid);
    builder.append("aspectName", aspectName);
    builder.append("destinationType", destination);
    builder.append("governor", governorPhysicalTypeMetadata.getId());
    builder.append("itdTypeDetails", itdTypeDetails);
    return builder.toString();
  }
}
