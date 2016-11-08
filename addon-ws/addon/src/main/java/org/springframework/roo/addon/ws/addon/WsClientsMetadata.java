package org.springframework.roo.addon.ws.addon;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.ws.annotations.RooWsClients;
import org.springframework.roo.addon.ws.annotations.SoapBindingType;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Metadata for {@link RooWsClients}.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
public class WsClientsMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  protected final static Logger LOGGER = HandlerUtils.getLogger(WsClientsMetadata.class);

  private static final String PROVIDES_TYPE_STRING = WsClientsMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private JavaType governor;
  private String profile;
  private List<WsClientEndpoint> endPoints;
  private Map<String, MethodMetadataBuilder> endPointMethods =
      new HashMap<String, MethodMetadataBuilder>();
  private Map<String, FieldMetadataBuilder> endPointFields =
      new HashMap<String, FieldMetadataBuilder>();
  private FieldMetadataBuilder loggerField;


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
   * @param endPoints
   * 			the endpoints that this @Configuration class will manage
   * @param profile 
   * 			the profile where this @Configuration class will be active 
   *  
   */
  public WsClientsMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, List<WsClientEndpoint> endPoints,
      String profile) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.governor = governorPhysicalTypeMetadata.getType();
    this.endPoints = endPoints;
    this.profile = profile;

    // Ensure that the annotated class is annotated with @Configuration
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(SpringJavaType.CONFIGURATION));

    // Ensure that the annotated class is annotated with @ConditionalOnWebApplication
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(new JavaType(
        "org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication")));

    // Ensure that the annotated class has LOGGER message
    ensureGovernorHasField(getLoggerField());

    // If developer has specify some profile, annotate class with @Profile
    if (StringUtils.isNotEmpty(profile)) {
      AnnotationMetadataBuilder profileAnnotation =
          new AnnotationMetadataBuilder(SpringJavaType.PROFILE);
      profileAnnotation.addStringAttribute("value", profile);
      ensureGovernorIsAnnotated(profileAnnotation);
    }

    // Include new field and new method for each registered endpoint
    for (WsClientEndpoint endPoint : endPoints) {
      ensureGovernorHasField(getEndPointField(endPoint));
      // Create method
      ensureGovernorHasMethod(getEndpointMethod(endPoint));
    }

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * This method obtains the LOGGER field
   * 
   * @return FieldMetadataBuilder that contians information about
   * the LOGGER field
   */
  public FieldMetadataBuilder getLoggerField() {

    if (loggerField != null) {
      return loggerField;
    }

    // Create the field
    FieldMetadataBuilder loggerField =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE + Modifier.STATIC + Modifier.FINAL,
            new JavaSymbolName("LOGGER"), new JavaType("org.slf4j.Logger"), String.format(
                "%s.getLogger(%s.class);",
                getNameOfJavaType(new JavaType("org.slf4j.LoggerFactory")),
                getNameOfJavaType(this.governor)));

    this.loggerField = loggerField;

    return loggerField;
  }

  /**
     * This method provides the related field for the provided endPoint
     * 
     * @param endPoint to obtain the related field
     * 
     * @return FieldMetadataBuilder that contains all information about the field
     */
  public FieldMetadataBuilder getEndPointField(WsClientEndpoint endPoint) {

    // Checking if already exists the endPoint field to 
    // prevent to generate it again
    if (endPointFields.get(endPoint.getName()) != null) {
      return endPointFields.get(endPoint.getName());
    }

    // Calculate the field name
    JavaSymbolName fieldName =
        new JavaSymbolName(StringUtils.uncapitalize(endPoint.getName()).concat("Url"));

    // Create the field
    FieldMetadataBuilder endPointField =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE, fieldName, JavaType.STRING, null);

    // Include @Value annotation
    AnnotationMetadataBuilder valueAnnotation = new AnnotationMetadataBuilder(SpringJavaType.VALUE);
    valueAnnotation.addStringAttribute("value", String.format("${url/%s}", endPoint.getName()));
    endPointField.addAnnotation(valueAnnotation);

    // Cache generated fields
    endPointFields.put(endPoint.getName(), endPointField);

    return endPointField;
  }

  /**
   * This method provides the related method for the provided endPoint
   * 
   * @param endPoint to obtain the related method
   * 
   * @return MethodMetadataBuilder that contains all information about the method
   */
  public MethodMetadataBuilder getEndpointMethod(WsClientEndpoint endPoint) {

    // Checking if already exists the endPoint method to 
    // prevent to generate it again
    if (endPointMethods.get(endPoint.getName()) != null) {
      return endPointMethods.get(endPoint.getName());
    }

    // If not, obtain necessary info about the provided enpoint
    String targetNameSpace = endPoint.getTargetNameSpace();
    Validate.notEmpty(targetNameSpace,
        "ERROR: Provided endpoint has not been registered inside @RooWsClients annotation");

    // Creating valid endpoint JavaType
    JavaType endPointType =
        new JavaType(String.format("%s.%s", getPackageNameFromTargetNameSpace(targetNameSpace),
            StringUtils.capitalize(endPoint.getName())));

    // Getting method name
    JavaSymbolName methodName = new JavaSymbolName(StringUtils.uncapitalize(endPoint.getName()));

    // Generating method body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // JaxWsProxyFactoryBean jaxWsFactory = new JaxWsProxyFactoryBean();
    bodyBuilder.appendFormalLine("%s jaxWsFactory = new JaxWsProxyFactoryBean();",
        getNameOfJavaType(new JavaType("org.apache.cxf.jaxws.JaxWsProxyFactoryBean")));

    //jaxWsFactory.setServiceClass(ENDPOINT.class);
    bodyBuilder.appendFormalLine("jaxWsFactory.setServiceClass(%s.class);",
        getNameOfJavaType(endPointType));

    // jaxWsFactory.setAddress(this.ENDPOINTFIELD);
    bodyBuilder.appendFormalLine("jaxWsFactory.setAddress(this.%s);", getEndPointField(endPoint)
        .getFieldName());

    // Check bindingType. If is SOAP 1.2, is necessary to generate some extra code
    if (endPoint.getBindingType().getField().getSymbolName().equals(SoapBindingType.SOAP12.name())) {
      // jaxWsFactory.setBindingId(SOAPBinding.SOAP12HTTP_BINDING);
      bodyBuilder.appendFormalLine("jaxWsFactory.setBindingId(%s.SOAP12HTTP_BINDING);",
          getNameOfJavaType(new JavaType("javax.xml.ws.soap.SOAPBinding")));

      // jaxWsFactory.setTransportId(SoapTransportFactory.SOAP_12_HTTP_BINDING);
      bodyBuilder.appendFormalLine("jaxWsFactory.setTransportId(%s.SOAP_12_HTTP_BINDING);",
          getNameOfJavaType(new JavaType("org.apache.cxf.binding.soap.SoapTransportFactory")));
    }

    // jaxWsFactory.setFeatures(Arrays.asList(new TraceeCxfFeature(), new LoggingFeature()));
    bodyBuilder.appendFormalLine("jaxWsFactory.setFeatures(%s.asList(new %s(), new %s()));",
        getNameOfJavaType(JavaType.ARRAYS), getNameOfJavaType(new JavaType(
            "io.tracee.binding.cxf.TraceeCxfFeature")), getNameOfJavaType(new JavaType(
            "org.apache.cxf.feature.LoggingFeature")));

    // LOGGER.info("Web Service client ENDPOINTFIELD has been created. URL: '{}'", this.ENDPOINTFIELD);
    bodyBuilder.appendFormalLine(
        "%s.info(\"Web Service client %s has been created. URL: '{}'\", this.%s);",
        getLoggerField().getFieldName(), getEndPointField(endPoint).getFieldName()
            .getSymbolNameCapitalisedFirstLetter(), getEndPointField(endPoint).getFieldName());

    // return (ENDPOINT) jaxWsFactory.create();
    bodyBuilder.appendFormalLine("return (%s) jaxWsFactory.create();",
        getNameOfJavaType(endPointType));

    // Generate new method related with the provided endpoint
    MethodMetadataBuilder method =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, endPointType, bodyBuilder);
    method.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.BEAN));

    // Cache generated methods
    endPointMethods.put(endPoint.getName(), method);

    return method;
  }

  /**
   * This method obtains a valid package name from the provided targetNamespace.
   * 
   * Ex: Providing 'http://roo.springframework.org/' targetNameSpace, the obtained
   * Package name will be 'org.springframework.roo'.
   * 
   * @param targetNameSpace
   * @return String with the calculated package name
   */
  public String getPackageNameFromTargetNameSpace(String targetNameSpace) {
    Validate.notEmpty(targetNameSpace,
        "ERROR: You must provide valid targetNameSpace to the package name.");
    // Remove protocols and unecessary elements
    targetNameSpace = targetNameSpace.replaceAll("http://", "");
    targetNameSpace = targetNameSpace.replaceAll("https://", "");
    targetNameSpace = targetNameSpace.replaceAll("file://", "");
    targetNameSpace = targetNameSpace.replaceAll("/", "");

    // Return reverse URL delimited by dots
    return StringUtils.reverseDelimited(targetNameSpace.toLowerCase(), '.');
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

  public String getProfile() {
    return profile;
  }

  public List<WsClientEndpoint> getEndPoints() {
    return endPoints;
  }

}
