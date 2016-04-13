package org.springframework.roo.addon.security.addon.security;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.addon.security.annotations.RooSecurityConfiguration;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooSecurityConfiguration}.
 * <p>
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class SecurityMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = SecurityMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);
  private static final JavaType AUDITOR_AWARE = new JavaType(
      "org.springframework.data.domain.AuditorAware");
  private static final JavaType BEAN = new JavaType("org.springframework.context.annotation.Bean");
  private static final JavaType ENABLE_JPA_AUDITING = new JavaType(
      "org.springframework.data.jpa.repository.config.EnableJpaAuditing");
  private static final JavaType CONFIGURATION = new JavaType(
      "org.springframework.context.annotation.Configuration");

  private final JavaType authenticationAuditorAware;
  private final SecurityConfigurationAnnotationValues annnotationValues;
  private final ImportRegistrationResolver importResolver;

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
   * @param annotationValues 
   */
  public SecurityMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final JavaType authenticationAuditorAware,
      SecurityConfigurationAnnotationValues annotationValues) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate
        .isTrue(
            isValid(identifier),
            "Metadata identification string '%s' does not appear to be a valid physical type identifier",
            identifier);

    this.authenticationAuditorAware = authenticationAuditorAware;
    this.annnotationValues = annotationValues;
    this.importResolver = builder.getImportRegistrationResolver();

    if (annotationValues.getEnableJpaAuditing()) {

      // Generate the auditorProvider method
      ensureGovernorHasMethod(getAuditorProviderMethod());

      // Add @EnableJpaAuditing
      ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(ENABLE_JPA_AUDITING));

      // Add @Configuration
      ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(CONFIGURATION));

      // Create a representation of the desired output ITD
      itdTypeDetails = builder.build();
    }
  }

  /**
   * Obtains the "auditorProvider" method for this type, if available.
   * 
   * @return the "auditorProvider" method declared on this type or that will be
   *         introduced (or null if undeclared and not introduced)
   */
  private MethodMetadataBuilder getAuditorProviderMethod() {

    // Compute the relevant auditorProvider method name
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.appendFormalLine(String.format("return new %s();",
        authenticationAuditorAware.getNameIncludingTypeParameters(false, importResolver)));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName("auditorProvider"),
            new JavaType(AUDITOR_AWARE.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
                Arrays.asList(JavaType.STRING)), bodyBuilder);

    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(BEAN).build());

    return methodBuilder;
  }
}
