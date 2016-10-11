package org.springframework.roo.addon.security.addon.security;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;
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
 * @author Juan Carlos García
 * @since 2.0
 */
public class WebSecurityConfigurationMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = WebSecurityConfigurationMetadata.class
      .getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);
  private static final JavaType AUDITOR_AWARE = new JavaType(
      "org.springframework.data.domain.AuditorAware");
  private static final JavaType BEAN = new JavaType("org.springframework.context.annotation.Bean");
  private static final JavaType ENABLE_JPA_AUDITING = new JavaType(
      "org.springframework.data.jpa.repository.config.EnableJpaAuditing");
  private static final JavaType CONFIGURATION = new JavaType(
      "org.springframework.context.annotation.Configuration");
  private static final JavaType PROFILE = new JavaType(
      "org.springframework.context.annotation.Profile");
  private static final JavaType ENABLE_WEB_SECURITY = new JavaType(
      "org.springframework.security.config.annotation.web.configuration.EnableWebSecurity");


  private final JavaType authenticationAuditorAware;
  private final WebSecurityConfigurationAnnotationValues annnotationValues;
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
  public WebSecurityConfigurationMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final JavaType authenticationAuditorAware,
      WebSecurityConfigurationAnnotationValues annotationValues) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);
    Validate
        .isTrue(
            isValid(identifier),
            "Metadata identification string '%s' does not appear to be a valid physical type identifier",
            identifier);

    this.authenticationAuditorAware = authenticationAuditorAware;
    this.annnotationValues = annotationValues;
    this.importResolver = builder.getImportRegistrationResolver();

    // WebSecurityConfig.java should extends WebSecurityConfigurerAdapter
    ensureGovernorExtends(new JavaType(
        "org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter"));

    // WebSecurityConfig.java should be annotated with @Configuration
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(CONFIGURATION));

    // WebSecurityConfig.java should be annotated with @Profile
    AnnotationMetadataBuilder profileAnnotation = new AnnotationMetadataBuilder(PROFILE);
    profileAnnotation.addStringAttribute("value", annotationValues.getProfile());
    ensureGovernorIsAnnotated(profileAnnotation);

    // WebSecurityConfig.java should be annotated with @EnableWebSecurity
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(ENABLE_WEB_SECURITY));

    // Generate configure method
    ensureGovernorHasMethod(getConfigureMethod());

    // Adding necessary code to enable jpa auditing
    if (annotationValues.getEnableJpaAuditing()) {
      // Generate the auditorProvider method
      ensureGovernorHasMethod(getAuditorProviderMethod());
      // Add @EnableJpaAuditing
      ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(ENABLE_JPA_AUDITING));
    }

    // Create a representation of the desired output ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Obtains the "configure" method for this type
   * 
   * @return the "configure" method declared on this type
   */
  private MethodMetadataBuilder getConfigureMethod() {

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(new JavaType(
        "org.springframework.security.config.annotation.web.builders.HttpSecurity")));

    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("http"));

    // Compute the relevant configure method name
    final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // // Configuring Content Security Policy
    bodyBuilder.appendFormalLine("// Configuring Content Security Policy");

    // http.headers()
    bodyBuilder.appendFormalLine("http.headers()");
    // .addHeaderWriter(new StaticHeadersWriter("X-Content-Security-Policy",
    bodyBuilder.appendFormalLine(String.format(
        ".addHeaderWriter(new %s(\"X-Content-Security-Policy\",", new JavaType(
            "org.springframework.security.web.header.writers.StaticHeadersWriter")
            .getNameIncludingTypeParameters(false, importResolver)));
    // "script-src 'self' 'unsafe-inline' "))
    bodyBuilder.appendFormalLine("\"script-src 'self' 'unsafe-inline' \"))");
    // .addHeaderWriter(new StaticHeadersWriter("Content-Security-Policy",
    bodyBuilder
        .appendFormalLine(".addHeaderWriter(new StaticHeadersWriter(\"Content-Security-Policy\",");
    // "script-src 'self' 'unsafe-inline' "))
    bodyBuilder.appendFormalLine("\"script-src 'self' 'unsafe-inline' \"))");
    // .addHeaderWriter(
    bodyBuilder.appendFormalLine(".addHeaderWriter(");
    // new StaticHeadersWriter("X-WebKit-CSP", "script-src 'self' 'unsafe-inline' "));
    bodyBuilder
        .appendFormalLine("new StaticHeadersWriter(\"X-WebKit-CSP\", \"script-src 'self' 'unsafe-inline' \"));");
    bodyBuilder.newLine();

    // // Access management
    bodyBuilder.appendFormalLine("// Access management");
    // http.authorizeRequests()
    bodyBuilder.appendFormalLine("http.authorizeRequests()");
    // //     static resources
    bodyBuilder.appendFormalLine("// static resources");
    // .antMatchers("/public/**", "/webjars/**").permitAll()
    bodyBuilder.appendFormalLine(".antMatchers(\"/public/**\", \"/webjars/**\").permitAll()");
    // // All the other requests nned to be authenticated
    bodyBuilder.appendFormalLine("// All the other requests need to be authenticated");
    // .anyRequest().authenticated()
    bodyBuilder.appendFormalLine(".anyRequest().authenticated()");
    // // Configuring form login page
    bodyBuilder.appendFormalLine("// Configuring form login page");
    // .and().formLogin().loginPage("/login").permitAll();
    bodyBuilder.appendFormalLine(".and().formLogin().loginPage(\"/login\").permitAll();");

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName("configure"),
            JavaType.VOID_PRIMITIVE, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(JavaType.OVERRIDE));
    methodBuilder.addThrowsType(new JavaType("java.lang.Exception"));

    return methodBuilder;
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
