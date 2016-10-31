package org.springframework.roo.addon.web.mvc.controller.addon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.addon.web.mvc.controller.annotations.RooController;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Metadata for {@link RooController}.
 *
 * @author Juan Carlos García
 * @author Jose Manuel Vivó
 * @since 2.0
 */
public class ControllerMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  protected final static Logger LOGGER = HandlerUtils.getLogger(ControllerMetadata.class);

  private static final String PROVIDES_TYPE_STRING = ControllerMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private final JavaType service;
  private final JavaType detailsService;
  private final ControllerType type;
  private final JavaType entity;
  private final JavaType identifierType;
  private final ServiceMetadata serviceMetadata;
  private final FieldMetadata detailsServiceField;
  private final ServiceMetadata detailsServiceMetadata;
  private final String path;
  private final String requestMappingValue;
  private final FieldMetadata identifierField;
  private final JpaEntityMetadata entityMetadata;
  private final RelationInfo detailsFieldInfo;
  private final FieldMetadata serviceField;


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
   * @param controllerValues
   * @param governorPhysicalTypeMetadata
   *            the governor, which is expected to contain a
   *            {@link ClassOrInterfaceTypeDetails} (required)
   * @param entity
   *            Javatype with entity managed by controller
   * @param service
   *            JavaType with service used by controller
   * @param detailsServices
   *            List with all services of every field that will be used as
   *            detail
   * @param path
   *            controllerPath
   * @param type
   *            Indicate the controller type
   * @param identifierType
   *            Indicates the identifier type of the entity which represents
   *            this controller
   * @param serviceMetadata
   *            ServiceMetadata of the service used by controller
   * @param controllerDetailInfo
   *            Contains information relative to detail controller
   */
  public ControllerMetadata(final String identifier, final JavaType aspectName,
      ControllerAnnotationValues controllerValues,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final JavaType entity,
      final JpaEntityMetadata entityMetadata, final JavaType service,
      final JavaType detailsService, final String path, final ControllerType type,
      ServiceMetadata serviceMetadata, ServiceMetadata detailsServiceMetadata,
      RelationInfo detailsFieldInfo) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.type = type;
    this.service = service;
    this.serviceMetadata = serviceMetadata;
    this.detailsService = detailsService;
    this.detailsServiceMetadata = detailsServiceMetadata;
    this.entity = entity;
    this.entityMetadata = entityMetadata;
    this.identifierField = entityMetadata.getCurrentIndentifierField();
    this.identifierType = identifierField.getFieldType();
    this.path = path;
    this.detailsFieldInfo = detailsFieldInfo;

    switch (this.type) {
      case ITEM:
        this.requestMappingValue =
            StringUtils.lowerCase(path).concat(
                "/{" + identifierField.getFieldName().getSymbolName() + " }");
        break;
      case COLLECTION:
        this.requestMappingValue = StringUtils.lowerCase(path);
        break;
      case SEARCH:
        this.requestMappingValue = StringUtils.lowerCase(path).concat("/search");
        break;
      case DETAIL:
        Validate.notNull(detailsFieldInfo, "Missing details information for %s",
            governorPhysicalTypeMetadata.getType());
        this.requestMappingValue =
            StringUtils.lowerCase(path).concat(
                "/{" + StringUtils.uncapitalize(entity.getSimpleTypeName()) + "}/"
                    + detailsFieldInfo.fieldName);
        break;
      default:
        throw new IllegalArgumentException(String.format("Unsupported @%s.type '%s' on %s",
            RooJavaType.ROO_CONTROLLER, this.type.name(), governorPhysicalTypeMetadata.getType()));
    }

    // Adding service field
    this.serviceField = getFieldFor(this.service);
    ensureGovernorHasField(new FieldMetadataBuilder(serviceField));

    if (this.type == ControllerType.DETAIL) {
      // Adding service field
      this.detailsServiceField = getFieldFor(this.detailsService);
      ensureGovernorHasField(new FieldMetadataBuilder(detailsServiceField));
    } else {
      this.detailsServiceField = null;
    }

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * This method returns the controller constructor
   *
   * @return
   */
  /*
  private ConstructorMetadata getConstructor() {

    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Generating constructor
    ConstructorMetadataBuilder constructor = new ConstructorMetadataBuilder(getId());
    constructor.setModifier(Modifier.PUBLIC);
    constructor.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));
    if (this.type == ControllerType.DETAIL) {

      // Getting serviceFieldName
      String serviceFieldName =
          getServiceField(this.controllerDetailInfo.getService()).getFieldName().getSymbolName();

      // Getting parentServiceFieldName
      String parentServiceFieldName =
          getServiceField(this.controllerDetailInfo.getParentService()).getFieldName()
              .getSymbolName();

      // Adding parameters
      constructor
          .addParameter(parentServiceFieldName, this.controllerDetailInfo.getParentService());
      constructor.addParameter(serviceFieldName, this.controllerDetailInfo.getService());

      // Generating body
      bodyBuilder.appendFormalLine(String.format("this.%s = %s;", parentServiceFieldName,
          parentServiceFieldName));
      bodyBuilder.appendFormalLine(String.format("this.%s = %s;", serviceFieldName,
          serviceFieldName));

    } else {
      // Getting serviceFieldName
      String serviceFieldName = getServiceField().getFieldName().getSymbolName();

      // Adding parameters
      constructor.addParameter(serviceFieldName, service);
      for (JavaType detailService : detailsServices) {
        constructor.addParameter(getServiceDetailField(detailService).getFieldName()
            .getSymbolName(), detailService);
      }

      // Generating body
      bodyBuilder.appendFormalLine(String.format("this.%s = %s;", serviceFieldName,
          serviceFieldName));
      for (JavaType detailService : detailsServices) {
        bodyBuilder.appendFormalLine(String.format("this.%s = %s;",
            getServiceDetailField(detailService).getFieldName().getSymbolName(),
            getServiceDetailField(detailService).getFieldName().getSymbolName()));
      }
    }

    // Adding body
    constructor.setBodyBuilder(bodyBuilder);

    return constructor.build();
  }
  */


  /**
   * This method returns service field included on controller that it
   * represents the service spent as parameter
   *
   * @param service
   *            Searched service
   * @return The field that represents the service spent as parameter
   */
  private FieldMetadata getFieldFor(JavaType service) {

    // Generating service field name
    String fieldName =
        new JavaSymbolName(service.getSimpleTypeName()).getSymbolNameUnCapitalisedFirstLetter();

    return new FieldMetadataBuilder(getId(), Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName(fieldName), service).build();
  }

  /**
   * This method returns the model attribute method
   *
   * @return
   */
  /*
  private MethodMetadataBuilder getModelAttributeMethod() {

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    AnnotationMetadataBuilder pathVariable =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVariable.addStringAttribute("value", "id");
    parameterTypes.add(new AnnotatedJavaType(this.identifierType, pathVariable.build()));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("id"));

    JavaSymbolName methodName = new JavaSymbolName("get".concat(this.entity.getSimpleTypeName()));

    // Check if method exists
    if (governorHasMethod(methodName,
        AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes))) {
      return new MethodMetadataBuilder(getGovernorMethod(methodName,
          AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes)));
    }

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return entityNameService.findOne(id);
    // Getting serviceFieldName
    String serviceFieldName = getServiceField().getFieldName().getSymbolName();

    // Getting findOneMethod
    MethodMetadata serviceFindOneMethod = serviceMetadata.getCurrentFindOneMethod();

    bodyBuilder.appendFormalLine(String.format("return this.%s.%s(id);", serviceFieldName,
        serviceFindOneMethod.getMethodName()));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodMetadataBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, this.entity,
            parameterTypes, parameterNames, bodyBuilder);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    annotations.add(new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE));
    methodMetadataBuilder.setAnnotations(annotations);

    return methodMetadataBuilder;
  }
  */

  /**
   * This method returns the model attribute method
   *
   * @return
   */
  /*
  public MethodMetadataBuilder getModelAttributeMethod(JavaType entity, JavaType identifierType,
      JavaType service) {

    // Define method parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    AnnotationMetadataBuilder pathVariable =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVariable.addStringAttribute("value", "id");
    parameterTypes.add(new AnnotatedJavaType(identifierType, pathVariable.build()));

    // Define method parameter names
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("id"));

    JavaSymbolName methodName = new JavaSymbolName("get".concat(entity.getSimpleTypeName()));

    // Check if method exists
    if (governorHasMethod(methodName,
        AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes))) {
      return new MethodMetadataBuilder(getGovernorMethod(methodName,
          AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes)));
    }

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return entityNameService.findOne(id);
    // Getting serviceFieldName
    String serviceFieldName = getServiceField(service).getFieldName().getSymbolName();

    // Getting findOneMethod
    MethodMetadata serviceFindOneMethod = serviceMetadata.getCurrentFindOneMethod();

    bodyBuilder.appendFormalLine(String.format("return this.%s.%s(id);", serviceFieldName,
        serviceFindOneMethod.getMethodName()));

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodMetadataBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, entity, parameterTypes,
            parameterNames, bodyBuilder);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
    annotations.add(new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE));
    methodMetadataBuilder.setAnnotations(annotations);

    return methodMetadataBuilder;
  }*/

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

  public JavaType getEntity() {
    return this.entity;
  }

  public ControllerType getType() {
    return this.type;
  }

  public JavaType getService() {
    return this.service;
  }

  public JavaType getDetailsService() {
    return detailsService;
  }

  public String getPath() {
    return this.path;
  }

  public FieldMetadata getIdentifierField() {
    return identifierField;
  }

  public JavaType getIdentifierType() {
    return identifierType;
  }

  public RelationInfo getDetailsFieldInfo() {
    return detailsFieldInfo;
  }

  public String getRequestMappingValue() {
    return requestMappingValue;
  }

  public FieldMetadata getDetailsServiceField() {
    return detailsServiceField;
  }

  public FieldMetadata getServiceField() {
    return serviceField;
  }
}
