package org.springframework.roo.addon.web.mvc.controller.addon.config;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooWebMvcJSONConfiguration}.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
public class WebMvcJSONConfigurationMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final String PROVIDES_TYPE_STRING = WebMvcJSONConfigurationMetadata.class
      .getName();

  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private static final JavaType CONFIGURATION = new JavaType(
      "org.springframework.context.annotation.Configuration");

  private static final JavaType DESERIALIZATION_FEATURE = new JavaType(
      "com.fasterxml.jackson.databind.DeserializationFeature");

  private static final JavaType SIMPLE_MODULE = new JavaType(
      "com.fasterxml.jackson.databind.module.SimpleModule");

  private static final JavaType MODULE = new JavaType("com.fasterxml.jackson.databind.Module");

  private static final EnumDetails DESERIALIZATION_WRAP_EXCEPTIONS = new EnumDetails(
      DESERIALIZATION_FEATURE, new JavaSymbolName("WRAP_EXCEPTIONS"));

  private ImportRegistrationResolver importResolver;

  private final JavaPackage convertersJavaPackage;

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
   *          contain a {@link ClassOrInterfaceTypeDetails} (required)
   * @param convertersJavaPackage
   * @param formatters list with registered formatters
   */
  public WebMvcJSONConfigurationMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, JavaPackage convertersJavaPackage) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.importResolver = builder.getImportRegistrationResolver();
    this.convertersJavaPackage = convertersJavaPackage;

    // Add @Configuration
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(CONFIGURATION));

    // Add FormattingConversionService field
    ensureGovernorHasField(getFormattingConversionServiceField());

    // Add LocalValidatorFactoryBean field
    ensureGovernorHasField(getLocalValidatorFactoryBeanField());

    // Add objectMapper method
    ensureGovernorHasMethod(new MethodMetadataBuilder(getJacksonDatabindModule()));

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * Creates FormattingConversionService field with @Autowired
   * 
   * @return FieldMetadataBuilder
   */
  private FieldMetadataBuilder getFormattingConversionServiceField() {
    List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    annotations.add(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));

    return new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, new JavaSymbolName(
        "conversionService"), SpringJavaType.FORMATTING_CONVERSION_SERVICE);
  }

  /**
   * Creates LocalValidatorFactoryBean field with @Autowired
   * 
   * @return FieldMetadataBuilder
   */
  private FieldMetadataBuilder getLocalValidatorFactoryBeanField() {
    List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    annotations.add(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));

    return new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, new JavaSymbolName(
        "validatorFactory"), SpringJavaType.LOCAL_VALIDATOR_FACTORY_BEAN);
  }

  /**
   * Method that generates "jacksonDatabindModule" method.
   * 
   * @return MethodMetadataBuilder
   */
  public MethodMetadata getJacksonDatabindModule() {

    // Define method name
    JavaSymbolName methodName = new JavaSymbolName("jacksonDatabindModule");

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

    // SimpleModule module = new SimpleModule();
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("%s module = new %s();",
        SIMPLE_MODULE.getNameIncludingTypeParameters(false, importResolver),
        SIMPLE_MODULE.getNameIncludingTypeParameters(false, importResolver)));

    // module.setSerializerModifier(new ConversionServiceBeanSerializerModifier(conversionService));
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format(
        "module.setSerializerModifier(new %s(conversionService));",
        new JavaType(String.format("%s.%s",
            this.convertersJavaPackage.getFullyQualifiedPackageName(),
            "ConversionServiceBeanSerializerModifier"), convertersJavaPackage.getModule())
            .getNameIncludingTypeParameters(false, importResolver)));

    // module.setDeserializerModifier(new DataBinderBeanDeserializerModifier(conversionService, validatorFactory));
    bodyBuilder.appendFormalLine(String.format(
        "module.setDeserializerModifier(new %s(conversionService, validatorFactory));",
        new JavaType(String.format("%s.%s",
            this.convertersJavaPackage.getFullyQualifiedPackageName(),
            "DataBinderBeanDeserializerModifier"), this.convertersJavaPackage.getModule())
            .getNameIncludingTypeParameters(false, importResolver)));

    // module.addSerializer(BindingResult.class, new BindingResultSerializer());
    bodyBuilder.appendFormalLine(String.format(
        "module.addSerializer(%s.class, new %s());",
        SpringJavaType.BINDING_RESULT.getNameIncludingTypeParameters(false, importResolver),
        new JavaType(String.format("%s.%s",
            this.convertersJavaPackage.getFullyQualifiedPackageName(), "BindingResultSerializer"),
            this.convertersJavaPackage.getModule()).getNameIncludingTypeParameters(false,
            importResolver)));

    // module.addSerializer(FieldError.class, new FieldErrorSerializer());
    bodyBuilder.appendFormalLine(String.format(
        "module.addSerializer(%s.class, new %s());",
        new JavaType("org.springframework.validation.FieldError").getNameIncludingTypeParameters(
            false, importResolver),
        new JavaType(String.format("%s.%s",
            this.convertersJavaPackage.getFullyQualifiedPackageName(), "FieldErrorSerializer"),
            this.convertersJavaPackage.getModule()).getNameIncludingTypeParameters(false,
            importResolver)));

    // return module;
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("return module;");

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, MODULE, parameterTypes,
            parameterNames, bodyBuilder);

    // Add @Bean and @Primary annotations
    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.BEAN));

    // Build and return a MethodMetadata instance
    return methodBuilder.build();
  }

  /**
   * This method uses the provided service JavaType to generate a serviceField
   * FieldMetadataBuilder
   * 
   * @param service
   * @return
   */
  private FieldMetadataBuilder getServiceField(JavaType service) {
    List<AnnotationMetadataBuilder> serviceAnnotations = new ArrayList<AnnotationMetadataBuilder>();

    AnnotationMetadataBuilder autowiredAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED);
    serviceAnnotations.add(autowiredAnnotation);

    FieldMetadataBuilder serviceField =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE, serviceAnnotations, new JavaSymbolName(
            getServiceFieldName(service)), service);

    return serviceField;
  }

  /**
   * This method uses the provided service JavaType to generate a serviceField
   * name
   * 
   * @param service
   * @return
   */
  private String getServiceFieldName(JavaType service) {
    return StringUtils.uncapitalize(service.getSimpleTypeName());
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
