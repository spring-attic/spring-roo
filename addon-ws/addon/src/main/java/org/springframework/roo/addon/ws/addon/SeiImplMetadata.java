package org.springframework.roo.addon.ws.addon;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.ws.annotations.RooSeiImpl;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Metadata for {@link RooSeiImpl}.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
public class SeiImplMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  protected final static Logger LOGGER = HandlerUtils.getLogger(SeiImplMetadata.class);

  private static final String PROVIDES_TYPE_STRING = SeiImplMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private final JavaPackage projectTopLevelPackage;
  private final ClassOrInterfaceTypeDetails endpoint;
  private final JavaType sei;
  private final JavaType service;
  private final Map<MethodMetadata, MethodMetadata> seiMethods;
  private List<MethodMetadata> endpointMethods;
  private Map<MethodMetadata, MethodMetadataBuilder> endpointMethodsFromSeiMethods;

  private FieldMetadata serviceField;
  private ConstructorMetadata constructor;

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
   * @param projectTopLevelPackage the base package of the project that will be 
   * 								used to create the targetNamespace      
   * @param endpoint the annotated endpoint
   * @param sei the related SEI
   * @param service the related Service
   * @param seiMethods
   * 			the methods registered in based SEI
   */
  public SeiImplMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, JavaPackage projectTopLevelPackage,
      ClassOrInterfaceTypeDetails endpoint, JavaType sei, JavaType service,
      Map<MethodMetadata, MethodMetadata> seiMethods) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.projectTopLevelPackage = projectTopLevelPackage;
    this.endpoint = endpoint;
    this.sei = sei;
    this.service = service;
    this.seiMethods = seiMethods;

    // Initialize collections
    endpointMethodsFromSeiMethods = new HashMap<MethodMetadata, MethodMetadataBuilder>();
    endpointMethods = new ArrayList<MethodMetadata>();

    // Include @WebService annotation
    AnnotationMetadataBuilder webServiceAnnotation =
        new AnnotationMetadataBuilder(JavaType.WEB_SERVICE);
    webServiceAnnotation.addStringAttribute("endpointInterface", sei.getFullyQualifiedTypeName());
    webServiceAnnotation.addStringAttribute("portName",
        String.format("%sPort", sei.getSimpleTypeName()));
    webServiceAnnotation.addStringAttribute("serviceName", sei.getSimpleTypeName());
    webServiceAnnotation.addStringAttribute(
        "targetNamespace",
        String.format("http://ws.%s/", StringUtils.reverseDelimited(
            projectTopLevelPackage.getFullyQualifiedPackageName(), '.')));
    ensureGovernorIsAnnotated(webServiceAnnotation);

    // Include service field
    ensureGovernorHasField(new FieldMetadataBuilder(getServiceField()));

    // Include constructor
    ensureGovernorHasConstructor(new ConstructorMetadataBuilder(getConstructor()));

    // Implements SEI Methods
    for (Entry<MethodMetadata, MethodMetadata> seiMethod : seiMethods.entrySet()) {
      ensureGovernorHasMethod(getEndpointMethodFromSEIMethod(seiMethod.getKey(),
          seiMethod.getValue()));
    }

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * This method obtains the constructor method of the annotated endpoint
   * 
   * @return constructor method for this endpoint
   */
  private ConstructorMetadata getConstructor() {
    // Check if constructor has been generated before
    if (constructor != null) {
      return constructor;
    }
    // Creating new constructor
    ConstructorMetadataBuilder constructorMethod = new ConstructorMetadataBuilder(getId());
    constructorMethod.setModifier(Modifier.PUBLIC);
    constructorMethod.addParameter(getServiceField().getFieldName()
        .getSymbolNameUnCapitalisedFirstLetter(), getServiceField().getFieldType());
    // Generating constructor body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.appendFormalLine("%s(%s);", getMutatorMethod(getServiceField()).getMethodName(),
        getServiceField().getFieldName().getSymbolNameUnCapitalisedFirstLetter());
    constructorMethod.setBodyBuilder(bodyBuilder);

    constructor = constructorMethod.build();

    return constructor;
  }

  /**
     * This method obtains the service field used in this Endpoint
     * 
     * @return FieldMEtadataBuilder with the current service field
     */
  private FieldMetadata getServiceField() {
    // Check if already exists
    if (serviceField != null) {
      return serviceField;
    }
    // If not, create a new one using the provided service
    serviceField =
        new FieldMetadataBuilder(getId(), Modifier.PRIVATE, new JavaSymbolName(
            StringUtils.uncapitalize(service.getSimpleTypeName())), service, null).build();
    return serviceField;
  }

  /**
   * This method obtains an Endpoint method from a provided SEI method.
   * 
   * This method caches the generated methods
   * 
   * @param seiMethod defined in a SEI interface
   * @param serviceMethod where this enpoint should delegate
   * 
   * @return MethodMetadataBuilder that contains all the information about the new Endpoint method.
   */
  private MethodMetadataBuilder getEndpointMethodFromSEIMethod(MethodMetadata seiMethod,
      MethodMetadata serviceMethod) {

    // Check if already exists the method
    if (endpointMethodsFromSeiMethods.get(seiMethod) != null) {
      return endpointMethodsFromSeiMethods.get(seiMethod);
    }

    // If not exists, generate it and cache it.

    // First of all, obtain the SEI method parameters and remove the @WebParam annotation from them.
    // Is not necessary in the endpoint because is already defined in the SEI
    List<JavaType> parameters = new ArrayList<JavaType>();
    for (AnnotatedJavaType type : seiMethod.getParameterTypes()) {
      parameters.add(type.getJavaType());
    }

    // Create the new endpoint method wind the updated information
    MethodMetadataBuilder endpointMethod =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, seiMethod.getMethodName(),
            seiMethod.getReturnType(), AnnotatedJavaType.convertFromJavaTypes(parameters),
            seiMethod.getParameterNames(), null);

    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Getting parameters
    String parametersList = "";
    for (JavaSymbolName param : seiMethod.getParameterNames()) {
      parametersList = parametersList.concat(param.getSymbolName()).concat(", ");
    }

    if (StringUtils.isNotBlank(parametersList)) {
      parametersList = parametersList.substring(0, parametersList.length() - 2);
    }

    bodyBuilder.appendFormalLine("%s%s().%s(%s);",
        seiMethod.getReturnType() != JavaType.VOID_PRIMITIVE ? "return " : "",
        getAccessorMethod(getServiceField()).getMethodName(), serviceMethod.getMethodName()
            .getSymbolName(), parametersList);

    endpointMethod.setBodyBuilder(bodyBuilder);

    endpointMethodsFromSeiMethods.put(seiMethod, endpointMethod);
    endpointMethods.add(endpointMethod.build());

    return endpointMethod;
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

  public Map<MethodMetadata, MethodMetadata> getSeiMethods() {
    return this.seiMethods;
  }

  public List<MethodMetadata> getEndpointMethods() {
    return this.endpointMethods;
  }

  public JavaType getService() {
    return service;
  }

  public ClassOrInterfaceTypeDetails getEndpoint() {
    return endpoint;
  }

  public JavaType getSei() {
    return sei;
  }

}
