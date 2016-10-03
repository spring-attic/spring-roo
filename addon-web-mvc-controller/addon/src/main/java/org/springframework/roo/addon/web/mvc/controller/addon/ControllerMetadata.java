package org.springframework.roo.addon.web.mvc.controller.addon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.addon.web.mvc.controller.annotations.RooController;
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
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Metadata for {@link RooController}.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
public class ControllerMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  protected final static Logger LOGGER = HandlerUtils.getLogger(ControllerMetadata.class);

  private static final String PROVIDES_TYPE_STRING = ControllerMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private static final JavaType CONTROLLER_ANNOTATION = new JavaType(
      "org.springframework.stereotype.Controller");
  private static final JavaType REQUEST_MAPPING_ANNOTATION = new JavaType(
      "org.springframework.web.bind.annotation.RequestMapping");

  private JavaType service;
  private List<JavaType> detailsServices;
  private ControllerType type;
  private JavaType entity;
  private JavaType identifierType;
  private ServiceMetadata serviceMetadata;
  private String path;
  private ControllerDetailInfo controllerDetailInfo;

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
   *            the identifier for this item of metadata (required)
   * @param aspectName
   *            the Java type of the ITD (required)
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
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final JavaType entity,
      final JavaType service, final List<JavaType> detailsServices, final String path,
      final ControllerType type, final JavaType identifierType, ServiceMetadata serviceMetadata,
      ControllerDetailInfo controllerDetailInfo) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.type = type;
    this.service = service;
    this.detailsServices = detailsServices;
    this.entity = entity;
    this.identifierType = identifierType;
    this.serviceMetadata = serviceMetadata;
    this.path = path;
    this.controllerDetailInfo = controllerDetailInfo;

    // Add @Controller annotation
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(CONTROLLER_ANNOTATION));

    // Add @RequestMapping annotation
    AnnotationMetadataBuilder requestMappingAnnotation =
        new AnnotationMetadataBuilder(REQUEST_MAPPING_ANNOTATION);

    if (this.type == ControllerType.ITEM) {
      requestMappingAnnotation.addStringAttribute("value",
          StringUtils.lowerCase(path).concat("/{id}"));
    } else if (this.type == ControllerType.COLLECTION) {
      requestMappingAnnotation.addStringAttribute("value", StringUtils.lowerCase(path));
    } else if (this.type == ControllerType.SEARCH) {
      requestMappingAnnotation.addStringAttribute("value",
          StringUtils.lowerCase(path).concat("/search"));
    } else if (this.type == ControllerType.DETAIL) {

      if (controllerDetailInfo == null) {
        return;
      }

      requestMappingAnnotation.addStringAttribute("value", controllerDetailInfo.getPath());
    }

    ensureGovernorIsAnnotated(requestMappingAnnotation);

    if (this.type == ControllerType.DETAIL) {

      // Adding reference service
      ensureGovernorHasField(new FieldMetadataBuilder(
          getServiceField(this.controllerDetailInfo.getParentService())));

      // Adding detail service
      ensureGovernorHasField(new FieldMetadataBuilder(
          getServiceField(this.controllerDetailInfo.getService())));
    } else {

      // Adding service reference
      FieldMetadata serviceField = getServiceField();
      ensureGovernorHasField(new FieldMetadataBuilder(serviceField));

      // Adding services of every fields that will be used as detail
      for (JavaType detailService : detailsServices) {
        FieldMetadata detailServiceField = getServiceDetailField(detailService);
        ensureGovernorHasField(new FieldMetadataBuilder(detailServiceField));
      }
    }


    // Adding constructor
    ConstructorMetadata constructor = getConstructor();
    ensureGovernorHasConstructor(new ConstructorMetadataBuilder(constructor));

    // Adding ModelAttribute method
    if (this.type == ControllerType.ITEM) {
      ensureGovernorHasMethod(getModelAttributeMethod());
    } else if (this.type == ControllerType.DETAIL) {

      // Add model attribute method for parent entity
      ensureGovernorHasMethod(getModelAttributeMethod(controllerDetailInfo.getParentEntity(),
          controllerDetailInfo.getParentIdentifierType(), controllerDetailInfo.getParentService()));
    }

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  /**
   * This method returns the controller constructor
   *
   * @return
   */
  public ConstructorMetadata getConstructor() {

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

  /**
   * This method returns service field included on controller
   *
   * @return
   */
  public FieldMetadata getServiceField() {

    // Generating service field name
    String fieldName =
        new JavaSymbolName(this.service.getSimpleTypeName())
            .getSymbolNameUnCapitalisedFirstLetter();

    return new FieldMetadataBuilder(getId(), Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName(fieldName), this.service)
        .build();
  }

  /**
   * This method returns service field included on controller that it
   * represents the service spent as parameter
   *
   * @param service
   *            Searched service
   * @return The field that represents the service spent as parameter
   */
  public FieldMetadata getServiceField(JavaType service) {

    // Generating service field name
    String fieldName =
        new JavaSymbolName(service.getSimpleTypeName()).getSymbolNameUnCapitalisedFirstLetter();

    return new FieldMetadataBuilder(getId(), Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName(fieldName), service).build();
  }

  /**
   * This method returns service field included on controller
   *
   * @param service
   * @return
   */
  public FieldMetadata getServiceDetailField(JavaType detailService) {

    // Generating service field name
    String fieldName =
        new JavaSymbolName(detailService.getSimpleTypeName())
            .getSymbolNameUnCapitalisedFirstLetter();

    return new FieldMetadataBuilder(getId(), Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName(fieldName), detailService)
        .build();
  }

  /**
   * This method returns the model attribute method
   *
   * @return
   */
  public MethodMetadataBuilder getModelAttributeMethod() {

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
    MethodMetadata serviceFindOneMethod = serviceMetadata.getFindOneMethod();

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

  /**
   * This method returns the model attribute method
   *
   * @return
   */
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
    MethodMetadata serviceFindOneMethod = serviceMetadata.getFindOneMethod();

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

  public JavaType getEntity() {
    return this.entity;
  }

  public ControllerType getType() {
    return this.type;
  }

  public JavaType getService() {
    return this.service;
  }

  public String getPath() {
    return this.path;
  }

  public ControllerDetailInfo getControllerDetailInfo() {
    return this.controllerDetailInfo;
  }

}
