package org.springframework.roo.addon.web.mvc.controller.addon.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Metadata for {@link RooWebMVCConfiguration}.
 *
 * @author Juan Carlos García
 * @author Jose Manuel Vivó
 * @since 2.0
 */
public class WebMvcConfigurationMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = WebMvcConfigurationMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);
  private static final JavaType TRACEE_INTERCEPTOR_JAVATYPE = new JavaType(
      "io.tracee.binding.springmvc.TraceeInterceptor");


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
   * @param identifier the identifier for this item of metadata (required)
   * @param aspectName the Java type of the ITD (required)
   * @param governorPhysicalTypeMetadata the governor, which is expected to
   *            contain a {@link ClassOrInterfaceTypeDetails} (required)
   *
   */
  public WebMvcConfigurationMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String defaultLanguage) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    // Add @Configuration
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(SpringJavaType.CONFIGURATION));

    // Add extends WebMvcConfigurerAdapter
    ensureGovernorExtends(SpringJavaType.WEB_MVC_CONFIGURER_ADAPTER);

    // Add validator method
    ensureGovernorHasMethod(new MethodMetadataBuilder(getValidatorMethod()));

    // Add localResolver
    ensureGovernorHasMethod(new MethodMetadataBuilder(getLocaleResolver(defaultLanguage)));

    // Add localeChangeInterceptor
    ensureGovernorHasMethod(new MethodMetadataBuilder(getLocaleChangeInterceptor()));

    // Add addInterceptors
    ensureGovernorHasMethod(new MethodMetadataBuilder(getAddInterceptors()));

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Method that generates "validator" method.
   *
   * @return MethodMetadata
   */
  private MethodMetadata getValidatorMethod() {

    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("validator");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    if (governorHasMethod(methodName,
        AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes))) {
      return getGovernorMethod(methodName,
          AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    }

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return new LocalValidatorFactoryBean();
    bodyBuilder.appendFormalLine("return new %s();",
        getNameOfJavaType(SpringJavaType.LOCAL_VALIDATOR_FACTORY_BEAN));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SpringJavaType.LOCAL_VALIDATOR_FACTORY_BEAN, parameterTypes, parameterNames,
            bodyBuilder);

    // Add @Bean and @Primary annotations
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.PRIMARY));
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.BEAN));

    // Build and return a MethodMetadata instance
    return methodBuilder.build();
  }


  /**
   * Method that generates "localeResolver" method.
   *
   * @return MethodMetadata
   */
  private MethodMetadata getLocaleResolver(String defaultLanguage) {

    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("localeResolver");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    // Define method return type
    JavaType returnType = SpringJavaType.LOCALE_RESOLVER;

    if (governorHasMethod(methodName,
        AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes))) {
      return getGovernorMethod(methodName,
          AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    }

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // SessionLocaleResolver localeResolver = new SessionLocaleResolver();
    bodyBuilder.appendFormalLine("%1$s localeResolver = new %1$s();",
        getNameOfJavaType(SpringJavaType.SESSION_LOCALE_RESOLVER));

    // localeResolver.setDefaultLocale(new Locale(\"en\", \"EN\"));
    if (StringUtils.isNotBlank(defaultLanguage)) {
      bodyBuilder.appendFormalLine("localeResolver.setDefaultLocale(new %s(\"%s\"));",
          getNameOfJavaType(JdkJavaType.LOCALE), defaultLanguage);
    }

    // return
    bodyBuilder.appendFormalLine("return localeResolver;");


    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, parameterTypes,
            parameterNames, bodyBuilder);

    // Add Bean annotation
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.BEAN));

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }


  /**
   * Method that generates "localeChangeInterceptor" method.
   *
   * @return MethodMetadata
   */
  private MethodMetadata getLocaleChangeInterceptor() {

    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("localeChangeInterceptor");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    // Define method return type
    JavaType returnType =
        new JavaType("org.springframework.web.servlet.i18n.LocaleChangeInterceptor");

    if (governorHasMethod(methodName,
        AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes))) {
      return getGovernorMethod(methodName,
          AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    }

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    bodyBuilder
        .appendFormalLine("LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();");
    bodyBuilder.appendFormalLine("localeChangeInterceptor.setParamName(\"lang\");");

    // return
    bodyBuilder.appendFormalLine("return localeChangeInterceptor;");


    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, parameterTypes,
            parameterNames, bodyBuilder);

    // Add Bean annotation
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.BEAN));

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }


  /**
   * Method that generates "addInterceptors" method.
   *
   * @return MethodMetadata
   */
  private MethodMetadata getAddInterceptors() {

    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("addInterceptors");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(new JavaType(
        "org.springframework.web.servlet.config.annotation.InterceptorRegistry")));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("registry"));

    if (governorHasMethod(methodName,
        AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes))) {
      return getGovernorMethod(methodName,
          AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    }

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    bodyBuilder.appendFormalLine("registry.addInterceptor(localeChangeInterceptor());");

    // Add TracEE interceptor
    bodyBuilder.appendFormalLine("registry.addInterceptor(new %s());",
        getNameOfJavaType(TRACEE_INTERCEPTOR_JAVATYPE));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
            parameterTypes, parameterNames, bodyBuilder);

    // Add Bean annotation
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(JavaType.OVERRIDE));

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
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
