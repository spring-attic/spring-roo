package org.springframework.roo.addon.security.addon.security;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.addon.security.annotations.RooModelGlobalSecurityConfig;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooModelGlobalSecurityConfig}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class ModelGlobalSecurityConfigMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = ModelGlobalSecurityConfigMetadata.class
      .getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);
  private static final JavaType AUDITOR_AWARE = new JavaType(
      "org.springframework.data.domain.AuditorAware");
  private static final JavaType BEAN = new JavaType("org.springframework.context.annotation.Bean");
  private static final JavaType CONFIGURATION = new JavaType(
      "org.springframework.context.annotation.Configuration");
  private static final JavaType PROFILE = new JavaType(
      "org.springframework.context.annotation.Profile");


  private final ModelGlobalSecurityConfigAnnotationValues annnotationValues;
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
  public ModelGlobalSecurityConfigMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      ModelGlobalSecurityConfigAnnotationValues annotationValues) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate
        .isTrue(
            isValid(identifier),
            "Metadata identification string '%s' does not appear to be a valid physical type identifier",
            identifier);

    this.annnotationValues = annotationValues;
    this.importResolver = builder.getImportRegistrationResolver();

    // GlobalSecurityConfig.java should extends GlobalAuthenticationConfigurerAdapter
    ensureGovernorExtends(new JavaType(
        "org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter"));

    // GlobalSecurityConfig.java should be annotated with @Configuration
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(CONFIGURATION));

    // GlobalSecurityConfig.java should be annotated with @Profile
    AnnotationMetadataBuilder profileAnnotation = new AnnotationMetadataBuilder(PROFILE);
    profileAnnotation.addStringAttribute("value", annotationValues.getProfile());
    ensureGovernorIsAnnotated(profileAnnotation);

    // GlobalSecurityConfig.java needs UserDetailsService from Springlets
    List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    annotations.add(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));
    FieldMetadataBuilder userDetailsService =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, new JavaSymbolName(
            "userDetailsService"), SpringletsJavaType.SPRINGLETS_USER_DETAILS_SERVICE);
    ensureGovernorHasField(userDetailsService);

    // GlobalSecurityConfig.java needs AuthenticationEventPublisher
    FieldMetadataBuilder authenticationEventPublisher =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, new JavaSymbolName(
            "authenticationEventPublisher"), SpringJavaType.AUTHENTICATION_EVENT_PUBLISHER);
    ensureGovernorHasField(authenticationEventPublisher);

    // Include init method
    ensureGovernorHasMethod(getInitMethod());

    // Create a representation of the desired output ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Obtains the "init" method for this type
   * 
   * @return the "init" method declared on this type
   */
  private MethodMetadataBuilder getInitMethod() {

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType
        .convertFromJavaType(SpringJavaType.AUTHENTICATION_MANAGER_BUILDER));

    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("auth"));

    // Compute the relevant configure method name
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // // Allow to listen authentication events
    bodyBuilder.appendFormalLine("// Allow to listen authentication events");

    // auth.authenticationEventPublisher(authenticationEventPublisher);
    bodyBuilder
        .appendFormalLine("auth.authenticationEventPublisher(authenticationEventPublisher);");
    bodyBuilder.newLine();

    // // Set the userDetailsService to use and the mechanism to encode the password
    bodyBuilder
        .appendFormalLine("// Set the userDetailsService to use and the mechanism to encode the password");

    // auth.userDetailsService(userDetailsService).passwordEncoder(new BCryptPasswordEncoder());
    bodyBuilder.appendFormalLine(String.format(
        "auth.userDetailsService(userDetailsService).passwordEncoder(new %s());",
        SpringJavaType.BCRYPT_PASSWORD_ENCODER
            .getNameIncludingTypeParameters(false, importResolver)));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName("init"),
            JavaType.VOID_PRIMITIVE, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(JavaType.OVERRIDE));
    methodBuilder.addThrowsType(new JavaType("java.lang.Exception"));

    return methodBuilder;
  }

}
