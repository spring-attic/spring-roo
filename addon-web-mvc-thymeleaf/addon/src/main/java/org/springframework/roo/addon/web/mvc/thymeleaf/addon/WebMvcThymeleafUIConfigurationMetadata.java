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
      final JavaType datatablesPageableHandler, final JavaType datatablesSortHandler) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.importResolver = builder.getImportRegistrationResolver();
    this.datatablesPageableHandler = datatablesPageableHandler;
    this.datatablesSortHandler = datatablesSortHandler;

    // Add @Configuration
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(CONFIGURATION));

    // Add extends WebMvcConfigurerAdapter
    ensureGovernorExtends(new JavaType(
        "org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter"));

    // Add addArgumentResolvers() @Override method
    ensureGovernorHasMethod(new MethodMetadataBuilder(getAddArgumentResolvers()));

    // Add datatablesPageableResolver() 
    ensureGovernorHasMethod(new MethodMetadataBuilder(getDatatablesPageableResolver()));

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * This method returns pageableResolver() method annotated with @Bean
   * 
   * @return MethodMetadata that contains all information about pageableResolver 
   * method.
   */
  public MethodMetadata getDatatablesPageableResolver() {
    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("datatablesPageableResolver");

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

    // return new DATATABLES_PAGEABLE_HANDLER();
    bodyBuilder.appendFormalLine(String.format("return new %s();",
        this.datatablesPageableHandler.getNameIncludingTypeParameters(false, importResolver)));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            this.datatablesPageableHandler, parameterTypes, parameterNames, bodyBuilder);

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

    // argumentResolvers.add(datatablesPageableResolver());
    bodyBuilder.appendFormalLine("argumentResolvers.add(datatablesPageableResolver());");

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE,
            parameterTypes, parameterNames, bodyBuilder);

    // Add @Override annotation
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
