package org.springframework.roo.addon.ws.addon;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.ws.annotations.RooWsEndpoints;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
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
 * Metadata for {@link RooWsEndpoints}.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
public class WsEndpointsMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  protected final static Logger LOGGER = HandlerUtils.getLogger(WsEndpointsMetadata.class);

  private static final String PROVIDES_TYPE_STRING = WsEndpointsMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private JavaType governor;
  private FieldMetadata loggerField;
  private final Map<JavaType, JavaType> endpointsAndSeis;
  private final Map<JavaType, JavaType> endpointsAndServices;
  private final String profile;
  private FieldMetadata busField;
  private FieldMetadata servletField;
  private MethodMetadata openEntityManagerInViewFilterMethod;

  private Map<JavaType, FieldMetadata> serviceFields;
  private Map<JavaType, MethodMetadata> endpointMethods;

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
   * @param endpointsAndSeis map that includes information about the endpoint
   * 		and its associated SEIs.
   * @param endpointsAndServices map that includes information about the endpoint 
   * 			and its associated service
   * @param profile the provided profile
   */
  public WsEndpointsMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      Map<JavaType, JavaType> endpointsAndSeis, Map<JavaType, JavaType> endpointsAndServices,
      String profile) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.governor = governorPhysicalTypeMetadata.getType();
    this.endpointsAndSeis = endpointsAndSeis;
    this.endpointsAndServices = endpointsAndServices;
    this.profile = profile;

    // Initializing collections
    serviceFields = new HashMap<JavaType, FieldMetadata>();
    endpointMethods = new HashMap<JavaType, MethodMetadata>();

    // Ensure that the annotated class is annotated with @Configuration
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(SpringJavaType.CONFIGURATION));

    // Ensure that the annotated class is annotated with @ConditionalOnWebApplication
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(new JavaType(
        "org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication")));

    // Ensure that the annotated class has LOGGER message
    ensureGovernorHasField(new FieldMetadataBuilder(getLoggerField()));

    // If developer has specify some profile, annotate class with @Profile
    if (StringUtils.isNotEmpty(profile)) {
      AnnotationMetadataBuilder profileAnnotation =
          new AnnotationMetadataBuilder(SpringJavaType.PROFILE);
      profileAnnotation.addStringAttribute("value", profile);
      ensureGovernorIsAnnotated(profileAnnotation);
    }

    // Include Bus field
    ensureGovernorHasField(new FieldMetadataBuilder(getBusField()));

    // Include cxfServletPath field
    ensureGovernorHasField(new FieldMetadataBuilder(getServletField()));

    // Include service @Autowire field
    for (Entry<JavaType, JavaType> endpointAndService : endpointsAndServices.entrySet()) {
      ensureGovernorHasField(new FieldMetadataBuilder(
          getServiceField(endpointAndService.getValue())));
      ensureGovernorHasMethod(new MethodMetadataBuilder(
          getEndpointMethod(endpointAndService.getKey())));
    }

    // Include openEntityManagerInViewFilter method
    ensureGovernorHasMethod(new MethodMetadataBuilder(getOpenEntityManagerInViewFilterMethod()));

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * This method obtains the method that register the openEntityManagerInView 
   * filter
   * 
   * @return MethodMetadata with the information about the new method
   */
  private MethodMetadata getOpenEntityManagerInViewFilterMethod() {
    // Check if already exists
    if (openEntityManagerInViewFilterMethod != null) {
      return openEntityManagerInViewFilterMethod;
    }

    JavaType filterRegistrationBeanType =
        new JavaType("org.springframework.boot.context.embedded.FilterRegistrationBean");

    // Generating method body 
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // FilterRegistrationBean filterRegBean = new FilterRegistrationBean();
    bodyBuilder.appendFormalLine("%s filterRegBean = new FilterRegistrationBean();",
        getNameOfJavaType(filterRegistrationBeanType));

    // filterRegBean.setFilter(new OpenEntityManagerInViewFilter());
    bodyBuilder.appendFormalLine("filterRegBean.setFilter(new %s());",
        getNameOfJavaType(new JavaType(
            "org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter")));

    // List<String> urlPatterns = new ArrayList<String>();
    bodyBuilder.appendFormalLine("%s<String> urlPatterns = new %s<String>();",
        getNameOfJavaType(JavaType.LIST), getNameOfJavaType(JavaType.ARRAY_LIST));

    // urlPatterns.add(this.cxfServletPath + "/*");
    bodyBuilder.appendFormalLine("urlPatterns.add(%s() + \"/*\");",
        getAccessorMethod(getServletField()).getMethodName());

    // filterRegBean.setUrlPatterns(urlPatterns);
    bodyBuilder.appendFormalLine("filterRegBean.setUrlPatterns(urlPatterns);");

    // if (LOG.isDebugEnabled()) {
    bodyBuilder.appendFormalLine("if (%s().isDebugEnabled()) {",
        getAccessorMethod(getLoggerField()).getMethodName());

    // LOG.debug("Registering the 'OpenEntityManagerInViewFilter' filter for the '"
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(
        "%s().debug(\"Registering the 'OpenEntityManagerInViewFilter' filter for the '\"",
        getAccessorMethod(getLoggerField()).getMethodName());

    // .concat(this.cxfServletPath + "/*").concat("' URL."));
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(".concat(%s() + \"/*\").concat(\"' URL.\"));",
        getAccessorMethod(getServletField()).getMethodName());
    bodyBuilder.indentRemove();
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    // return filterRegBean;
    bodyBuilder.appendFormalLine("return filterRegBean;");

    MethodMetadataBuilder method =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName(
            "openEntityManagerInViewFilter"), filterRegistrationBeanType, bodyBuilder);
    method.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.BEAN));

    openEntityManagerInViewFilterMethod = method.build();

    return openEntityManagerInViewFilterMethod;
  }

  /**
     * This method obtains the method that will be used to registered the endpoint
     * 
     * @param endpoint JavaType with the information about the related endpoint to 
     * be registered
     * 
     * @return MethodMetadata with the necessary information about the new method
     */
  private MethodMetadata getEndpointMethod(JavaType endpoint) {
    // Check if already exists
    if (endpointMethods.get(endpoint) != null) {
      return endpointMethods.get(endpoint);
    }

    // Generating method body 
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // EndpointImpl endpoint = new EndpointImpl(this.bus, new ENDPOINT(this.SERVICENAME));
    bodyBuilder.appendFormalLine("%s endpoint = new EndpointImpl(%s(), new %s(%s()));",
        getNameOfJavaType(new JavaType("org.apache.cxf.jaxws.EndpointImpl")),
        getAccessorMethod(getBusField()).getMethodName(), getNameOfJavaType(endpoint),
        getAccessorMethod(getServiceField(getServiceFromEndpoint(endpoint))).getMethodName());

    // endpoint.setFeatures(Arrays.asList(new TraceeCxfFeature(), new LoggingFeature()));
    bodyBuilder.appendFormalLine("endpoint.setFeatures(%s.asList(new %s(), new %s()));",
        getNameOfJavaType(JavaType.ARRAYS), getNameOfJavaType(new JavaType(
            "io.tracee.binding.cxf.TraceeCxfFeature")), getNameOfJavaType(new JavaType(
            "org.apache.cxf.feature.LoggingFeature")));

    // endpoint.publish("/SEI");
    bodyBuilder.appendFormalLine("endpoint.publish(\"/%s\");", getSeiFromEndpoint(endpoint)
        .getSimpleTypeName());

    // return endpoint;
    bodyBuilder.appendFormalLine("return endpoint;");

    MethodMetadataBuilder endpointMethod =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName(
            StringUtils.uncapitalize(endpoint.getSimpleTypeName())), JavaType.ENDPOINT, bodyBuilder);
    endpointMethod.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.BEAN));

    endpointMethods.put(endpoint, endpointMethod.build());

    return endpointMethod.build();
  }

  /**
   * This method obtains the associated service of the provided endpoint
   * 
   * @param endpoint
   * @return
   */
  private JavaType getServiceFromEndpoint(JavaType endpoint) {
    return endpointsAndServices.get(endpoint);
  }

  /**
   * This method obtains the associated SEI of the provided endpoint
   * 
   * @param endpoint
   * @return
   */
  private JavaType getSeiFromEndpoint(JavaType endpoint) {
    return endpointsAndSeis.get(endpoint);
  }

  /**
     * This method obtains the service field that will be used in some different
     * methods.
     * 
     * @param service JavaType with the information about the service related with the new Service field
     * 
     * @return FieldMetadata with the necessary information about the service
     */
  private FieldMetadata getServiceField(JavaType service) {

    // Check if already exists
    if (serviceFields.get(service) != null) {
      return serviceFields.get(service);
    }

    // Create the field
    FieldMetadataBuilder serviceField =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE, new JavaSymbolName(
            StringUtils.uncapitalize(service.getSimpleTypeName())), service, null);
    serviceField.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));

    serviceFields.put(service, serviceField.build());

    return serviceField.build();
  }

  /**
     * This method obtains the servlet field that will be used in some different methods
     * 
     * @return FieldMetadata that contains all the necessary information
     * about the servlet field
     */
  private FieldMetadata getServletField() {

    // Check if already exists
    if (this.servletField != null) {
      return this.servletField;
    }
    // Create the field
    FieldMetadataBuilder servlet =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE, new JavaSymbolName("cxfServletPath"),
            JavaType.STRING, null);

    AnnotationMetadataBuilder valueAnnotation = new AnnotationMetadataBuilder(SpringJavaType.VALUE);
    valueAnnotation.addStringAttribute("value", "${cxf.path}");
    servlet.addAnnotation(valueAnnotation);

    servletField = servlet.build();

    return servletField;
  }

  /**
   * This method obtains the bus field that will be used in some different methods
   * 
   * @return FieldMetadata that contains all the necessary information
   * about the bus field
   */
  private FieldMetadata getBusField() {

    // Check if already exists
    if (this.busField != null) {
      return this.busField;
    }
    // Create the field
    FieldMetadataBuilder bus =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE, new JavaSymbolName("bus"),
            new JavaType("org.apache.cxf.Bus"), null);
    bus.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));

    busField = bus.build();

    return busField;
  }

  /**
     * This method obtains the LOGGER field
     * 
     * @return FieldMetadataBuilder that contians information about
     * the LOGGER field
     */
  public FieldMetadata getLoggerField() {

    if (loggerField != null) {
      return loggerField;
    }

    // Create the field
    FieldMetadataBuilder loggger =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE + Modifier.STATIC + Modifier.FINAL,
            new JavaSymbolName("LOGGER"), new JavaType("org.slf4j.Logger"), String.format(
                "%s.getLogger(%s.class)",
                getNameOfJavaType(new JavaType("org.slf4j.LoggerFactory")),
                getNameOfJavaType(this.governor)));

    this.loggerField = loggger.build();

    return loggerField;
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

  public Map<JavaType, JavaType> getEndpointsAndServices() {
    return endpointsAndServices;
  }

  public Map<JavaType, JavaType> getEndpointsAndSeis() {
    return endpointsAndSeis;
  }


}
