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
import org.springframework.roo.classpath.details.MethodMetadata;
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
 * Metadata for {@link RooWebMvcThymeleafUIConfiguration}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class WebMvcThymeleafUIConfigurationMetadata extends
    AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = WebMvcThymeleafUIConfigurationMetadata.class
      .getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private static final JavaType CONFIGURATION = new JavaType(
      "org.springframework.context.annotation.Configuration");

  private ImportRegistrationResolver importResolver;
  private JavaType datatablesPageableHandler;
  private JavaType datatablesSortHandler;
  private JavaType globalSearchHandler;

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
   */
  public WebMvcThymeleafUIConfigurationMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final JavaType datatablesPageableHandler, final JavaType datatablesSortHandler,
      final JavaType globalSearchHandler) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.importResolver = builder.getImportRegistrationResolver();
    this.datatablesPageableHandler = datatablesPageableHandler;
    this.datatablesSortHandler = datatablesSortHandler;
    this.globalSearchHandler = globalSearchHandler;

    // Add @Configuration
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(CONFIGURATION));

    // Add extends WebMvcConfigurerAdapter
    ensureGovernorExtends(new JavaType(
        "org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter"));

    // Add @Bean datatablesFilter()
    ensureGovernorHasMethod(new MethodMetadataBuilder(getDatatablesFilter()));

    // Add @Bean dandelionServletRegistrationBean()
    ensureGovernorHasMethod(new MethodMetadataBuilder(getDandelionServletRegistrationBean()));

    // Add @Bean datatablesDialect()
    ensureGovernorHasMethod(new MethodMetadataBuilder(getDatatablesDialect()));

    // Add addArgumentResolvers() @Override method
    ensureGovernorHasMethod(new MethodMetadataBuilder(getAddArgumentResolvers()));

    // Add @Bean pageableResolver() 
    ensureGovernorHasMethod(new MethodMetadataBuilder(getPageableResolver()));

    // Add @Bean sortResolver() 
    ensureGovernorHasMethod(new MethodMetadataBuilder(getSortResolver()));

    // Add @Bean globalSearchResolver() 
    ensureGovernorHasMethod(new MethodMetadataBuilder(getGlobalSearchResolver()));

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * This method returns globalSearchResolver() method annotated with @Bean
   * 
   * @return MethodMetadata that contains all information about globalSearchResolver 
   * method.
   */
  public MethodMetadata getGlobalSearchResolver() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("globalSearchResolver");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return new GLOBAL_SEARCH_HANDLER(sortResolver());
    bodyBuilder.appendFormalLine(String.format("return new %s();",
        this.globalSearchHandler.getNameIncludingTypeParameters(false, importResolver)));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, this.globalSearchHandler,
            parameterTypes, parameterNames, bodyBuilder);

    // Add @Override annotation
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(
        "org.springframework.context.annotation.Bean"));

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * This method returns sortResolver() method annotated with @Bean
   * 
   * @return MethodMetadata that contains all information about sortResolver 
   * method.
   */
  public MethodMetadata getSortResolver() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("sortResolver");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return new DATATABLES_SORT_HANDLER(sortResolver());
    bodyBuilder.appendFormalLine(String.format("return new %s();",
        this.datatablesSortHandler.getNameIncludingTypeParameters(false, importResolver)));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, this.datatablesSortHandler,
            parameterTypes, parameterNames, bodyBuilder);

    // Add @Override annotation
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(
        "org.springframework.context.annotation.Bean"));

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * This method returns pageableResolver() method annotated with @Bean
   * 
   * @return MethodMetadata that contains all information about pageableResolver 
   * method.
   */
  public MethodMetadata getPageableResolver() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("pageableResolver");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return new DATATABLES_PAGEABLE_HANDLER(sortResolver());
    bodyBuilder.appendFormalLine(String.format("return new %s(sortResolver());",
        this.datatablesPageableHandler.getNameIncludingTypeParameters(false, importResolver)));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            this.datatablesPageableHandler, parameterTypes, parameterNames, bodyBuilder);

    // Add @Override annotation
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(
        "org.springframework.context.annotation.Bean"));

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * This method returns addArgumentResolvers() method annotated with @Override
   * 
   * @return MethodMetadata that contains all information about addArgumentResolvers 
   * method.
   */
  public MethodMetadata getAddArgumentResolvers() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("addArgumentResolvers");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(new JavaType("java.util.List", 0,
        DataType.TYPE, null, Arrays.asList(new JavaType(
            "org.springframework.web.method.support.HandlerMethodArgumentResolver")))));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("argumentResolvers"));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // argumentResolvers.add(new DatatablesCriteriasMethodArgumentResolver());
    bodyBuilder
        .appendFormalLine(String
            .format(
                "argumentResolvers.add(new %s());",
                new JavaType(
                    "com.github.dandelion.datatables.extras.spring3.ajax.DatatablesCriteriasMethodArgumentResolver")
                    .getNameIncludingTypeParameters(false, importResolver)));

    // argumentResolvers.add(sortResolver());
    bodyBuilder.appendFormalLine("argumentResolvers.add(sortResolver());");

    // argumentResolvers.add(pageableResolver());
    bodyBuilder.appendFormalLine("argumentResolvers.add(pageableResolver());");

    // argumentResolvers.add(globalSearchResolver());
    bodyBuilder.appendFormalLine("argumentResolvers.add(globalSearchResolver());");


    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
            parameterTypes, parameterNames, bodyBuilder);

    // Add @Override annotation
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(JavaType.OVERRIDE));

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * This method returns datatablesDialect() method annotated with @Bean
   * 
   * @return MethodMetadata that contains all information about datatablesDialect 
   * method.
   */
  public MethodMetadata getDatatablesDialect() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("datatablesDialect");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return new DatatablesDialect();
    bodyBuilder.appendFormalLine(String.format("return new %s();", new JavaType(
        "com.github.dandelion.datatables.thymeleaf.dialect.DataTablesDialect")
        .getNameIncludingTypeParameters(false, importResolver)));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, new JavaType(
            "com.github.dandelion.datatables.thymeleaf.dialect.DataTablesDialect"), parameterTypes,
            parameterNames, bodyBuilder);

    // Add @Bean annotation
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType(
        "org.springframework.context.annotation.Bean")));

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * This method returns dandelionServletRegistrationBean() method annotated with @Bean
   * 
   * @return MethodMetadata that contains all information about dandelionServletRegistrationBean 
   * method.
   */
  public MethodMetadata getDandelionServletRegistrationBean() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("dandelionServletRegistrationBean");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(new DandelionServlet(), "/dandelion-assets/*");
    bodyBuilder
        .appendFormalLine(String
            .format(
                "ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean(new %s(), \"/dandelion-assets/*\");",
                new JavaType("com.github.dandelion.core.web.DandelionServlet")
                    .getNameIncludingTypeParameters(false, importResolver)));

    // servletRegistrationBean.setName("dandelionServlet");
    bodyBuilder.appendFormalLine("servletRegistrationBean.setName(\"dandelionServlet\");");

    // return servletRegistrationBean;
    bodyBuilder.appendFormalLine("return servletRegistrationBean;");

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, new JavaType(
            "org.springframework.boot.context.embedded.ServletRegistrationBean"), parameterTypes,
            parameterNames, bodyBuilder);
    // Add @Bean annotation
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType(
        "org.springframework.context.annotation.Bean")));

    return methodBuilder.build(); // Build and return a MethodMetadata
    // instance
  }

  /**
   * This method returns datatablesFilter() method annotated with @Bean
   * 
   * @return MethodMetadata that contains all information about datatablesFilter 
   * method.
   */

  public MethodMetadata getDatatablesFilter() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("datatablesFilter");

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // FilterRegistrationBean registration = new FilterRegistrationBean();
    bodyBuilder
        .appendFormalLine("FilterRegistrationBean registration = new FilterRegistrationBean();");

    // registration.setFilter(new DandelionFilter());
    bodyBuilder.appendFormalLine(String.format("registration.setFilter(new %s());", new JavaType(
        "com.github.dandelion.core.web.DandelionFilter").getNameIncludingTypeParameters(false,
        importResolver)));

    // registration.setName("dandelionFilter");
    bodyBuilder.appendFormalLine("registration.setName(\"dandelionFilter\");");

    // registration.addUrlPatterns("/*");
    bodyBuilder.appendFormalLine("registration.addUrlPatterns(\"/*\");");

    // registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ERROR);
    bodyBuilder
        .appendFormalLine(String
            .format(
                "registration.setDispatcherTypes(%s.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ERROR);",
                new JavaType("javax.servlet.DispatcherType").getNameIncludingTypeParameters(false,
                    importResolver)));

    // return registration;
    bodyBuilder.appendFormalLine("return registration;");

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, new JavaType(
            "org.springframework.boot.context.embedded.FilterRegistrationBean"), parameterTypes,
            parameterNames, bodyBuilder);
    // Add @Bean annotation
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType(
        "org.springframework.context.annotation.Bean")));

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
