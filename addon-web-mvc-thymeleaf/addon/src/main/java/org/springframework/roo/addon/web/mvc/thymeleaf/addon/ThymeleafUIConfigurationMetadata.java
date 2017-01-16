package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.web.mvc.thymeleaf.annotations.RooWebMvcThymeleafUIConfiguration;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooWebMvcThymeleafUIConfiguration}.
 *
 * @author Jose Manuel Vivó
 * @since 2.0
 */
public class ThymeleafUIConfigurationMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final List<AnnotationMetadataBuilder> AUTOWIRED_LIST = Arrays
      .asList(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));
  private static final String PROVIDES_TYPE_STRING = ThymeleafUIConfigurationMetadata.class
      .getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private static final JavaType THYMELEAF_VIEW_RESOLVER = new JavaType(
      "org.thymeleaf.spring4.view.ThymeleafViewResolver");
  private static final JavaType SPRING_RESOURCE_TEMPLATE_RESOLVER = new JavaType(
      "org.thymeleaf.spring4.templateresolver.SpringResourceTemplateResolver");
  private static final JavaType APPLICATION_CONTEXT_AWARE = new JavaType(
      "org.springframework.context.ApplicationContextAware");
  private static final JavaType TEMPLATE_ENGINE = new JavaType("org.thymeleaf.TemplateEngine");
  private static final JavaType THYMELEAF_PROPERTIES = new JavaType(
      "org.springframework.boot.autoconfigure.thymeleaf.ThymeleafProperties");
  private static final JavaType TEMPLATE_MODE = new JavaType(
      "org.thymeleaf.templatemode.TemplateMode");
  private static final JavaType APPLICATION_CONTEXT = new JavaType(
      "org.springframework.context.ApplicationContext");

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


  private final MethodMetadata javascriptThymeleafViewResolverMethod;
  private final MethodMetadata javascriptTemplateResolverMethod;
  private final FieldMetadata applicationContextAwareField;
  private final MethodMetadata setApplicationContextMethod;
  private final FieldMetadata thymeleafPropertiesField;
  private final FieldMetadata templateEngineField;

  /**
   * Constructor
   *
   * @param identifier the identifier for this item of metadata (required)
   * @param aspectName the Java type of the ITD (required)
   * @param governorPhysicalTypeMetadata the governor, which is expected to
   *            contain a {@link ClassOrInterfaceTypeDetails} (required)
   */
  public ThymeleafUIConfigurationMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    ensureGovernorImplements(APPLICATION_CONTEXT_AWARE);

    this.thymeleafPropertiesField = getThymeleafPropertiesField();
    this.templateEngineField = getTemplateEngineField();
    this.applicationContextAwareField = getApplicationContextField();

    // Adding fields
    ensureGovernorHasField(new FieldMetadataBuilder(this.thymeleafPropertiesField));
    ensureGovernorHasField(new FieldMetadataBuilder(this.templateEngineField));
    ensureGovernorHasField(new FieldMetadataBuilder(this.applicationContextAwareField));

    this.setApplicationContextMethod = getMutatorMethod(this.applicationContextAwareField);
    this.javascriptThymeleafViewResolverMethod = getJavascriptThymeleafViewResolverMethod();
    this.javascriptTemplateResolverMethod = getJavascriptTemplateResolverMethod();

    // Add index method
    ensureGovernorHasMethod(new MethodMetadataBuilder(javascriptThymeleafViewResolverMethod));
    ensureGovernorHasMethod(new MethodMetadataBuilder(javascriptTemplateResolverMethod));


    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /*
   * =====================================================================================
   */


  private FieldMetadata getTemplateEngineField() {
    return new FieldMetadataBuilder(getId(), Modifier.PRIVATE, AUTOWIRED_LIST, new JavaSymbolName(
        "templateEngine"), TEMPLATE_ENGINE).build();
  }

  private FieldMetadata getThymeleafPropertiesField() {
    return new FieldMetadataBuilder(getId(), Modifier.PRIVATE, AUTOWIRED_LIST, new JavaSymbolName(
        "thymeleafProperties"), THYMELEAF_PROPERTIES).build();
  }

  /**
   * Field for applicationContext
   *
   * @return
   */
  private FieldMetadata getApplicationContextField() {
    return new FieldMetadataBuilder(getId(), Modifier.PRIVATE,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName("applicationContext"),
        APPLICATION_CONTEXT).build();
  }

  /**
   * Returns method which configure ThymeleafViewResolver
   *
   * @return
   */
  private MethodMetadata getJavascriptThymeleafViewResolverMethod() {
    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("javascriptThymeleafViewResolver");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }
    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    // add @Bean
    annotations.add(new AnnotationMetadataBuilder(SpringJavaType.BEAN));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    /*
        ThymeleafViewResolver resolver = new ThymeleafViewResolver();
        resolver.setTemplateEngine(this.templateEngine);
        resolver.setCharacterEncoding(UTF8);
        resolver.setContentType("application/javascript");
        resolver.setViewNames(new String[] {"*.js"});
        resolver.setCache(this.properties.isCache());
        return resolver;
     */
    bodyBuilder.appendFormalLine("%1$s resolver = new %1$s();",
        getNameOfJavaType(THYMELEAF_VIEW_RESOLVER));
    bodyBuilder.appendFormalLine("resolver.setTemplateEngine(%s());",
        getAccessorMethod(this.templateEngineField).getMethodName());
    bodyBuilder.appendFormalLine("resolver.setCharacterEncoding(\"UTF-8\");");
    bodyBuilder.appendFormalLine("resolver.setContentType(\"application/javascript\");");
    bodyBuilder.appendFormalLine("resolver.setViewNames(new String[] {\"*.js\"});");
    bodyBuilder.appendFormalLine("resolver.setCache(%s().isCache());",
        getAccessorMethod(this.thymeleafPropertiesField).getMethodName());
    bodyBuilder.appendFormalLine("return resolver;");


    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, THYMELEAF_VIEW_RESOLVER,
            parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * Returns method which configure SpringResourceTemplateResolve
   *
   * @return
   */
  private MethodMetadata getJavascriptTemplateResolverMethod() {
    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("javascriptTemplateResolver");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }
    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    // add @Bean
    annotations.add(new AnnotationMetadataBuilder(SpringJavaType.BEAN));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    /*
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(getApplicationContext());
        resolver.setPrefix("classpath:/templates/fragments/js/");
        resolver.setTemplateMode(TemplateMode.JAVASCRIPT);
        resolver.setCharacterEncoding(UTF8);
        resolver.setCheckExistence(true);
        resolver.setCacheable(this.properties.isCache());
        return resolver;
     */
    bodyBuilder.appendFormalLine("%1$s resolver = new %1$s();",
        getNameOfJavaType(SPRING_RESOURCE_TEMPLATE_RESOLVER));
    bodyBuilder.appendFormalLine("resolver.setApplicationContext(%s());",
        getAccessorMethod(this.applicationContextAwareField).getMethodName());
    bodyBuilder.appendFormalLine("resolver.setPrefix(\"classpath:/templates/fragments/js/\");");
    bodyBuilder.appendFormalLine("resolver.setTemplateMode(%s.JAVASCRIPT);",
        getNameOfJavaType(TEMPLATE_MODE));
    bodyBuilder.appendFormalLine("resolver.setCharacterEncoding(\"UTF-8\");");
    bodyBuilder.appendFormalLine("resolver.setCheckExistence(true);");
    bodyBuilder.appendFormalLine("resolver.setCacheable(%s().isCache());",
        getAccessorMethod(this.thymeleafPropertiesField).getMethodName());
    bodyBuilder.appendFormalLine("return resolver;");


    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            SPRING_RESOURCE_TEMPLATE_RESOLVER, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
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
