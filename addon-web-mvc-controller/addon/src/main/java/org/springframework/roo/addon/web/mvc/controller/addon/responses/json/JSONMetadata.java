package org.springframework.roo.addon.web.mvc.controller.addon.responses.json;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.addon.web.mvc.controller.annotations.responses.json.RooJSON;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.Jsr303JavaType;
import org.springframework.roo.model.SpringEnumDetails;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.model.SpringletsJavaType;
import org.springframework.roo.project.LogicalPath;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Metadata for {@link RooJSON}.
 *
 * @author Juan Carlos García
 * @author Jose Manuel Vivó
 * @since 2.0
 */
public class JSONMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final JavaSymbolName LIST_URI_METHOD_NAME = new JavaSymbolName("listURI");
  private static final JavaSymbolName SHOW_URI_METHOD_NAME = new JavaSymbolName("showURI");
  private static final AnnotationMetadata ANN_METADATA_REQUEST_BODY =
      new AnnotationMetadataBuilder(SpringJavaType.REQUEST_BODY).build();
  private static final AnnotationMetadata ANN_METADATA_VALID = new AnnotationMetadataBuilder(
      Jsr303JavaType.VALID).build();
  private static final JavaSymbolName PAGEABLE_NAME = new JavaSymbolName("pageable");
  private static final JavaSymbolName GLOBAL_SEARCH_NAME = new JavaSymbolName("globalSearch");
  private static final JavaSymbolName CONSUMES_SYMBOL_NAME = new JavaSymbolName("consumes");
  private static final JavaSymbolName VALUE_SYMBOL_NAME = new JavaSymbolName("value");
  private static final JavaSymbolName PRODUCES_SYMBOL_NAME = new JavaSymbolName("produces");

  private static final String PROVIDES_TYPE_STRING = JSONMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private final boolean readOnly;
  private final ControllerMetadata controllerMetadata;
  private final MethodMetadata listMethod;
  private final MethodMetadata showMethod;
  private final MethodMetadata createMethod;
  private final MethodMetadata updateMethod;
  private final MethodMetadata deleteMethod;
  private final MethodMetadata createBatchMethod;
  private final MethodMetadata updateBatchMethod;
  private final MethodMetadata deleteBatchMethod;
  private final List<MethodMetadata> finderMethods;
  private final MethodMetadata modelAttributeMethod;
  private final ControllerType type;
  private final ConstructorMetadata constructor;
  private final MethodMetadata listURIMethod;
  private final List<MethodMetadata> allMethods;
  private final ServiceMetadata serviceMetadata;
  private final JavaType entity;
  private final String entityPlural;
  private final List<Pair<RelationInfo, JpaEntityMetadata>> compositionRelationOneToOne;
  private final String entityItemName;
  private final JpaEntityMetadata entityMetadata;
  private final JavaType itemController;
  private final String entityIdentifierPlural;
  private final MethodMetadata showURIMethod;
  private final String entityIdentifier;

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
   * @param controllerMetadata
   * @param serviceMetadata
   * @param entityMetadata
   * @param entityPlural
   * @param entityIdentifierPlural
   * @param compositionRelationOneToOne
   *            list with pairs of {@link RelationInfo} and related child entity {@link JpaEntityMetadata}
   * @param itemController
   */
  public JSONMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      ControllerMetadata controllerMetadata, ServiceMetadata serviceMetadata,
      JpaEntityMetadata entityMetadata, String entityPlural, String entityIdentifierPlural,
      final List<Pair<RelationInfo, JpaEntityMetadata>> compositionRelationOneToOne,
      final JavaType itemController) {
    super(identifier, aspectName, governorPhysicalTypeMetadata);

    this.readOnly = entityMetadata.isReadOnly();
    this.controllerMetadata = controllerMetadata;
    this.type = this.controllerMetadata.getType();
    this.serviceMetadata = serviceMetadata;
    this.entity = serviceMetadata.getEntity();
    this.entityMetadata = entityMetadata;
    this.entityIdentifier =
        entityMetadata.getCurrentIndentifierField().getFieldName().getSymbolName();
    this.entityIdentifierPlural = entityIdentifierPlural;
    this.entityItemName = StringUtils.uncapitalize(entity.getSimpleTypeName());
    this.entityPlural = entityPlural;
    this.compositionRelationOneToOne = compositionRelationOneToOne;
    this.itemController = itemController;

    //Add @RequestController
    ensureGovernorIsAnnotated(new AnnotationMetadataBuilder(SpringJavaType.REST_CONTROLLER));
    // Add @RequestMapping
    ensureGovernorIsAnnotated(getRequestMappingAnnotation());


    this.constructor = getConstructor();
    ensureGovernorHasConstructor(new ConstructorMetadataBuilder(constructor));

    List<MethodMetadata> allMethods = new ArrayList<MethodMetadata>();

    switch (this.type) {
      case COLLECTION:
        this.listMethod = addAndGet(getListMethod(), allMethods);
        this.listURIMethod = addAndGet(getListURIMethod(), allMethods);

        if (readOnly) {
          this.createMethod = null;
          this.createBatchMethod = null;
          this.updateBatchMethod = null;
          this.deleteBatchMethod = null;
        } else {
          this.createMethod = addAndGet(getCreateMethod(), allMethods);
          this.createBatchMethod = addAndGet(getCreateBatchMethod(), allMethods);
          this.updateBatchMethod = addAndGet(getUpdateBatchMethod(), allMethods);
          this.deleteBatchMethod = addAndGet(getDeleteBatchMethod(), allMethods);
        }

        this.updateMethod = null;
        this.deleteMethod = null;
        this.showMethod = null;
        this.showURIMethod = null;
        this.modelAttributeMethod = null;
        this.finderMethods = null;
        break;

      case ITEM:
        this.modelAttributeMethod = addAndGet(getModelAttributeMethod(), allMethods);
        this.showMethod = addAndGet(getShowMethod(), allMethods);
        this.showURIMethod = addAndGet(getShowURIMethod(), allMethods);
        if (readOnly) {
          this.updateMethod = null;
          this.deleteMethod = null;
        } else {
          this.updateMethod = addAndGet(getUpdateMethod(), allMethods);
          this.deleteMethod = addAndGet(getDeleteMethod(), allMethods);
        }


        this.listMethod = null;
        this.listURIMethod = null;
        this.createMethod = null;
        this.createBatchMethod = null;
        this.updateBatchMethod = null;
        this.deleteBatchMethod = null;
        this.finderMethods = null;

        break;
      case SEARCH:
        this.finderMethods = new ArrayList<MethodMetadata>();
        // TODO

        this.listMethod = null;
        this.listURIMethod = null;
        this.createMethod = null;
        this.createBatchMethod = null;
        this.updateBatchMethod = null;
        this.deleteBatchMethod = null;
        this.updateMethod = null;
        this.deleteMethod = null;
        this.showMethod = null;
        this.showURIMethod = null;
        this.modelAttributeMethod = null;
        break;

      case DETAIL:
        // TODO

        this.listMethod = null;
        this.listURIMethod = null;
        this.createMethod = null;
        this.createBatchMethod = null;
        this.updateBatchMethod = null;
        this.deleteBatchMethod = null;
        this.updateMethod = null;
        this.deleteMethod = null;
        this.showMethod = null;
        this.showURIMethod = null;
        this.modelAttributeMethod = null;
        this.finderMethods = null;
        break;

      default:
        throw new IllegalArgumentException("Unsupported Controller type: " + this.type.name());
    }

    this.allMethods = Collections.unmodifiableList(allMethods);

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  private MethodMetadata addAndGet(MethodMetadata method, List<MethodMetadata> allMethods) {
    allMethods.add(method);
    ensureGovernorHasMethod(new MethodMetadataBuilder(method));
    return method;
  }

  /**
   * Creates listURI method
   *
   * @return
   */
  private MethodMetadata getListURIMethod() {
    // Define method parameter types and parameter names
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    MethodMetadata existingMethod =
        getGovernorMethod(LIST_URI_METHOD_NAME,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // return MvcUriComponentsBuilder
    //    .fromMethodCall(
    //     MvcUriComponentsBuilder.on(CustomersCollectionJsonController.class).list(null, null))
    //     .build().encode();
    InvocableMemberBodyBuilder body = new InvocableMemberBodyBuilder();

    body.appendFormalLine("return %s", getNameOfJavaType(SpringJavaType.MVC_URI_COMPONENTS_BUILDER));
    body.indent();
    body.appendFormalLine(".fromMethodCall(");
    body.indent();
    body.appendFormalLine("%s.on(%s.class).%s(null, null))",
        getNameOfJavaType(SpringJavaType.MVC_URI_COMPONENTS_BUILDER),
        getNameOfJavaType(getDestination()), this.listMethod.getMethodName());
    body.indentRemove();
    body.appendFormalLine(".build().encode();");
    body.reset();

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.STATIC, LIST_URI_METHOD_NAME,
            SpringJavaType.URI_COMPONENTS, parameterTypes, parameterNames, body);

    return methodBuilder.build();
  }

  /**
   * Creates showURI method
   *
   * @return
   */
  private MethodMetadata getShowURIMethod() {
    // Define method parameter types and parameter names
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(entity));
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(entityItemName));

    MethodMetadata existingMethod =
        getGovernorMethod(SHOW_URI_METHOD_NAME,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // return MvcUriComponentsBuilder
    //    .fromMethodCall(
    //     MvcUriComponentsBuilder.on(CustomersItemJsonController.class).show(customer))
    //     .buildAndExpand(customer.getId()).encode();
    InvocableMemberBodyBuilder body = new InvocableMemberBodyBuilder();

    body.appendFormalLine("return %s", getNameOfJavaType(SpringJavaType.MVC_URI_COMPONENTS_BUILDER));
    body.indent();
    body.appendFormalLine(".fromMethodCall(");
    body.indent();
    body.appendFormalLine("%s.on(%s.class).%s(%s))",
        getNameOfJavaType(SpringJavaType.MVC_URI_COMPONENTS_BUILDER),
        getNameOfJavaType(getDestination()), this.showMethod.getMethodName(), entityItemName);
    body.indentRemove();
    body.appendFormalLine(".buildAndExpand(%s.get%s()).encode();", entityItemName,
        StringUtils.capitalize(entityIdentifier));
    body.indentRemove();

    // Use the MethodMetadataBuilder for easy creation of MethodMetadata
    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC + Modifier.STATIC, SHOW_URI_METHOD_NAME,
            SpringJavaType.URI_COMPONENTS, parameterTypes, parameterNames, body);

    return methodBuilder.build();
  }


  private AnnotationMetadataBuilder getRequestMappingAnnotation() {
    List<AnnotationAttributeValue<?>> requestMappingAttributes =
        new ArrayList<AnnotationAttributeValue<?>>();

    String path = controllerMetadata.getPath();
    if (type == ControllerType.ITEM) {
      path += "/{" + entityItemName + "}";
    }
    // Adding path attribute
    requestMappingAttributes.add(new StringAttributeValue(VALUE_SYMBOL_NAME, path));


    // Adding consumes attribute
    requestMappingAttributes.add(new EnumAttributeValue(CONSUMES_SYMBOL_NAME,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE));

    requestMappingAttributes.add(new EnumAttributeValue(PRODUCES_SYMBOL_NAME,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE));

    return new AnnotationMetadataBuilder(SpringJavaType.REQUEST_MAPPING, requestMappingAttributes);
  }

  private ConstructorMetadata getConstructor() {
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Generating constructor
    ConstructorMetadataBuilder constructor = new ConstructorMetadataBuilder(getId());
    constructor.setModifier(Modifier.PUBLIC);
    constructor.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));

    // Getting serviceFieldName
    String serviceFieldName = controllerMetadata.getServiceField().getFieldName().getSymbolName();
    constructor.addParameter(serviceFieldName, controllerMetadata.getService());

    // Generating body
    bodyBuilder
        .appendFormalLine(String.format("this.%s = %s;", serviceFieldName, serviceFieldName));

    if (this.type == ControllerType.DETAIL) {

      // Getting parentServiceFieldName
      String childServiceFieldName =
          controllerMetadata.getDetailsServiceField().getFieldName().getSymbolName();

      // Adding parameters
      constructor.addParameter(childServiceFieldName, controllerMetadata.getDetailsService());

      // Generating body
      bodyBuilder.appendFormalLine(String.format("this.%s = %s;", childServiceFieldName,
          childServiceFieldName));
    }

    // Adding body
    constructor.setBodyBuilder(bodyBuilder);

    return constructor.build();

  }

  /**
   * This method provides the "create" method using JSON response type
   *
   * @param serviceSaveMethod
   *
   * @return MethodMetadata
   */
  private MethodMetadata getCreateMethod() {
    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("create");

    // Adding parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.entity, ANN_METADATA_VALID,
        ANN_METADATA_REQUEST_BODY));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.BINDING_RESULT));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Adding parameter names
    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(entityItemName));
    parameterNames.add(new JavaSymbolName("result"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @PostMapping annotation
    AnnotationMetadataBuilder postMappingAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.POST_MAPPING);
    annotations.add(postMappingAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (customer.getId() != null || customer.getVersion() != null
    bodyBuilder.newLine();
    bodyBuilder.appendIndent();
    bodyBuilder.append("if (%s.get%s() != null || %s.get%s() != null", entityItemName,
        entityMetadata.getCurrentIndentifierField().getFieldName()
            .getSymbolNameCapitalisedFirstLetter(), entityItemName, entityMetadata
            .getCurrentVersionField().getFieldName().getSymbolNameCapitalisedFirstLetter());
    if (compositionRelationOneToOne.isEmpty()) {
      bodyBuilder.append(") {");
      bodyBuilder.newLine();
    } else {
      bodyBuilder.indent();
      bodyBuilder.indent();
      for (Pair<RelationInfo, JpaEntityMetadata> item : compositionRelationOneToOne) {
        JavaSymbolName versionFieldName = item.getRight().getCurrentVersionField().getFieldName();
        JavaSymbolName idFieldName = item.getRight().getCurrentIndentifierField().getFieldName();
        JavaSymbolName relationFieldName = item.getKey().fieldMetadata.getFieldName();

        // || (customer.getAddress() != null && (customer.getAddress().getId() != null || customer.getAddress().getVersion() != null))
        bodyBuilder.newLine();
        bodyBuilder.appendIndent();
        bodyBuilder.append("|| ( ");
        bodyBuilder
            .append(
                "%1$s.get%2$s() != null && (%1$s.get%2$s().get%3$s() != null || %1$s.get%2$s().get%4$s() != null)",
                entityItemName, relationFieldName.getSymbolNameCapitalisedFirstLetter(),
                idFieldName.getSymbolNameCapitalisedFirstLetter(),
                versionFieldName.getSymbolNameCapitalisedFirstLetter());
        bodyBuilder.append(")");
      }
      bodyBuilder.append(") {");
      bodyBuilder.newLine();

      bodyBuilder.indentRemove();
      bodyBuilder.indentRemove();
    }
    bodyBuilder.indent();
    // return ResponseEntity.status(HttpStatus.CONFLICT).build();
    bodyBuilder.appendFormalLine("return %s.status(%s.CONFLICT).build();",
        getNameOfJavaType(SpringJavaType.RESPONSE_ENTITY),
        getNameOfJavaType(SpringJavaType.HTTP_STATUS));
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // if (result.hasErrors()) {
    // return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
    // }
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("return %s.status(%s.CONFLICT).body(result);",
        getNameOfJavaType(SpringJavaType.RESPONSE_ENTITY),
        getNameOfJavaType(SpringJavaType.HTTP_STATUS));

    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // Entity newEntity = entityService.saveMethodName(entity);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("%s new%s = %s.%s(%s);", getNameOfJavaType(entity), StringUtils
        .capitalize(entityItemName), controllerMetadata.getServiceField().getFieldName(),
        serviceMetadata.getCurrentSaveMethod().getMethodName(), entityItemName);

    // UriComponents showURI = CustomersItemJsonController.showURI(newCustomer);
    bodyBuilder.appendFormalLine("%s showURI = %s.%s(new%s);",
        getNameOfJavaType(SpringJavaType.URI_COMPONENTS), getNameOfJavaType(itemController),
        SHOW_URI_METHOD_NAME, StringUtils.capitalize(entityItemName));

    // return ResponseEntity.created(showURI.toUri()).build();
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("return %s.created(showURI.toUri()).build();",
        getNameOfJavaType(SpringJavaType.RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(SpringJavaType.RESPONSE_ENTITY), parameterTypes,
            parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "update" method using JSON response type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getUpdateMethod() {
    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("update");

    // Define parameters
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    AnnotationMetadataBuilder modelAttributeAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE);
    parameterTypes.add(new AnnotatedJavaType(this.entity, modelAttributeAnnotation.build()));
    parameterTypes.add(new AnnotatedJavaType(this.entity, ANN_METADATA_VALID,
        ANN_METADATA_REQUEST_BODY));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.BINDING_RESULT));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }


    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    final String storedName = "stored".concat(this.entity.getSimpleTypeName());
    parameterNames.add(new JavaSymbolName(storedName));
    parameterNames.add(new JavaSymbolName(entityItemName));
    parameterNames.add(new JavaSymbolName("result"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @PutMapping annotation
    AnnotationMetadataBuilder putMappingAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PUT_MAPPING);
    annotations.add(putMappingAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (result.hasErrors()) {
    // return new ResponseEntity.status(HttpStatus.CONFLICT).body(result);
    // }
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("return %s.status(%s.CONFLICT).body(result);",
        getNameOfJavaType(SpringJavaType.RESPONSE_ENTITY),
        getNameOfJavaType(SpringJavaType.HTTP_STATUS));
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // customer.setId(storedCustomer.getId());
    bodyBuilder.appendFormalLine("%s.set%s(%s.getId());", entityItemName,
        StringUtils.capitalize(entityIdentifier), storedName,
        StringUtils.capitalize(entityIdentifier));

    for (Pair<RelationInfo, JpaEntityMetadata> item : compositionRelationOneToOne) {
      // customer.getAddress().setId(storedCustomer.getAddress().getId());
      JavaSymbolName relationField = item.getLeft().fieldMetadata.getFieldName();
      JavaSymbolName relatedEntityIdentifier =
          item.getRight().getCurrentIndentifierField().getFieldName();

      bodyBuilder.appendFormalLine("%1$s.get%2$s().set%3$s(%4$s.get%2$s().get%3$s());",
          entityItemName, relationField.getSymbolNameCapitalisedFirstLetter(),
          relatedEntityIdentifier.getSymbolNameCapitalisedFirstLetter(), storedName);
    }
    // customerService.save(customer);
    bodyBuilder.appendFormalLine("%s.save(%s);", controllerMetadata.getServiceField()
        .getFieldName(), entityItemName);

    // return ResponseEntity.ok().build();
    bodyBuilder.appendFormalLine("return %s.ok().build();",
        getNameOfJavaType(SpringJavaType.RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(SpringJavaType.RESPONSE_ENTITY), parameterTypes,
            parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "delete" method using JSON response type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getDeleteMethod() {
    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("delete");

    // Define parameters
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    AnnotationMetadataBuilder modelAttributeAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE);
    parameterTypes.add(new AnnotatedJavaType(entity, modelAttributeAnnotation.build()));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(entityItemName));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @DeleteMapping annotation
    annotations.add(new AnnotationMetadataBuilder(SpringJavaType.DELETE_MAPPING));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // customerService.delete(customer);
    bodyBuilder.appendFormalLine("%s.%s(%s);", controllerMetadata.getServiceField().getFieldName(),
        serviceMetadata.getCurrentDeleteMethod().getMethodName(), entityItemName);

    // return ResponseEntity.ok().build();
    bodyBuilder.appendFormalLine("return %s.ok().build();",
        getNameOfJavaType(SpringJavaType.RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(SpringJavaType.RESPONSE_ENTITY), parameterTypes,
            parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "list" method using JSON response type
   *
   * @param serviceFindAllMethod
   *
   * @return MethodMetadata
   */
  private MethodMetadata getListMethod() {

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("list");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(SpringletsJavaType.SPRINGLETS_GLOBAL_SEARCH));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.PAGEABLE));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(GLOBAL_SEARCH_NAME);
    parameterNames.add(PAGEABLE_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @GetMapping annotation
    annotations.add(new AnnotationMetadataBuilder(SpringJavaType.GET_MAPPING));


    final MethodMetadata findAllMethod = serviceMetadata.getCurrentFindAllWithGlobalSearchMethod();
    // Generating returnType
    JavaType serviceReturnType = findAllMethod.getReturnType();
    JavaType returnType = JavaType.wrapperOf(SpringJavaType.RESPONSE_ENTITY, serviceReturnType);

    // TODO
    // Add module dependency
    //getTypeLocationService().addModuleDependency(this.controller.getType().getModule(),
    //    returnParameterTypes.get(i));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    final String itemNames = StringUtils.uncapitalize(this.entityPlural);

    // Page<Customer> customers = customerService.findAll(globalSearch, pageable);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("%s %s = %s.%s(%s, %s);", getNameOfJavaType(serviceReturnType),
        itemNames, controllerMetadata.getServiceField().getFieldName(),
        findAllMethod.getMethodName(), GLOBAL_SEARCH_NAME, PAGEABLE_NAME);

    // return ResponseEntity.status(HttpStatus.FOUND).body(customers);
    bodyBuilder.appendFormalLine(String.format("return %s.status(%s.FOUND).body(%s);",
        getNameOfJavaType(SpringJavaType.RESPONSE_ENTITY),
        getNameOfJavaType(SpringJavaType.HTTP_STATUS), itemNames));


    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, parameterTypes,
            parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides a finder method using JSON response type
   *
   * @param finderMethod
   *
   * @return MethodMetadata
   */
  private MethodMetadata getFinderMethod(MethodMetadata finderMethod) {
    // TODO
    return null;
    /*
    final List<AnnotatedJavaType> originalParameterTypes = finderMethod.getParameterTypes();

    // Get finder parameter names
    final List<JavaSymbolName> originalParameterNames = finderMethod.getParameterNames();
    List<String> stringParameterNames = new ArrayList<String>();
    for (JavaSymbolName parameterName : originalParameterNames) {
      stringParameterNames.add(parameterName.getSymbolName());
    }

    // Define methodName
    final JavaSymbolName methodName = finderMethod.getMethodName();

    // Define path
    String path = "";
    if (StringUtils.startsWith(methodName.getSymbolName(), "count")) {
      path = StringUtils.uncapitalize(StringUtils.removeStart(methodName.getSymbolName(), "count"));
    } else if (StringUtils.startsWith(methodName.getSymbolName(), "find")) {
      path = StringUtils.uncapitalize(StringUtils.removeStart(methodName.getSymbolName(), "find"));
    } else if (StringUtils.startsWith(methodName.getSymbolName(), "query")) {
      path = StringUtils.uncapitalize(StringUtils.removeStart(methodName.getSymbolName(), "query"));
    } else if (StringUtils.startsWith(methodName.getSymbolName(), "read")) {
      path = StringUtils.uncapitalize(StringUtils.removeStart(methodName.getSymbolName(), "read"));
    } else {
      path = methodName.getSymbolName();
    }

    // Check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "/" + path, stringParameterNames, null,
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Get parameters
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    StringBuffer finderParamsString = new StringBuffer();
    for (int i = 0; i < originalParameterTypes.size(); i++) {
      if (originalParameterTypes.get(i).getJavaType().getSimpleTypeName().equals("GlobalSearch")) {
        if (i > 0) {
          finderParamsString.append(", ");
        }
        finderParamsString.append("null");
        continue;
      }

      // Add @ModelAttribute if not Pageable type
      if (!originalParameterTypes.get(i).getJavaType().getSimpleTypeName().equals("Pageable")) {
        AnnotationMetadataBuilder requestParamAnnotation =
            new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE);
        requestParamAnnotation.addStringAttribute("value", originalParameterNames.get(i)
            .getSymbolName());
        parameterTypes.add(new AnnotatedJavaType(originalParameterTypes.get(i).getJavaType(),
            requestParamAnnotation.build()));
      } else {
        parameterTypes.add(originalParameterTypes.get(i));
      }
      addTypeToImport(originalParameterTypes.get(i).getJavaType());
      parameterNames.add(originalParameterNames.get(i));

      // Build finder parameters String
      if (i > 0) {
        finderParamsString.append(", ");
      }
      finderParamsString.append(originalParameterNames.get(i));
    }

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, "/" + path, stringParameterNames, null,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE, ""));

    // Adding @ResponseBody annotation
    AnnotationMetadataBuilder responseBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(responseBodyAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Generating returnType
    JavaType returnType = finderMethod.getReturnType();
    List<JavaType> returnParameterTypes = returnType.getParameters();
    StringBuffer returnTypeParamsString = new StringBuffer();
    for (int i = 0; i < returnParameterTypes.size(); i++) {
      addTypeToImport(returnParameterTypes.get(i));
      if (i > 0) {
        returnTypeParamsString.append(",");
      }
      returnTypeParamsString.append(returnParameterTypes.get(i).getSimpleTypeName());

      // Add module dependency
      getTypeLocationService().addModuleDependency(this.controller.getType().getModule(),
          returnParameterTypes.get(i));
    }

    // ReturnType<ReturnTypeParams> entity =
    // ENTITY_SERVICE_FIELD.FINDER_NAME(SEARCH_PARAMS);
    bodyBuilder.newLine();
    if (StringUtils.isEmpty(returnTypeParamsString)) {
      bodyBuilder.appendFormalLine(String.format("%s returnObject = %s.%s(%s);",
          addTypeToImport(returnType).getSimpleTypeName(), getServiceField().getFieldName(),
          methodName, finderParamsString));
    } else {
      bodyBuilder.appendFormalLine(String.format("%s<%s> returnObject = %s.%s(%s);",
          addTypeToImport(returnType).getSimpleTypeName(), returnTypeParamsString,
          getServiceField().getFieldName(), methodName, finderParamsString));
    }

    // return returnObject;
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("return returnObject;");

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            returnType, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
    */
  }

  /**
   * This method provides the "show" method using JSON response type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getShowMethod() {
    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("show");

    // Define parameters
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    AnnotationMetadataBuilder modelAttributeAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE);
    parameterTypes.add(new AnnotatedJavaType(entity, modelAttributeAnnotation.build()));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(entityItemName));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @GetMapping annotation
    annotations.add(new AnnotationMetadataBuilder(SpringJavaType.GET_MAPPING));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return ResponseEntity.status(HttpStatus.FOUND).body(customer);
    bodyBuilder.appendFormalLine("return %s.status(%s.FOUND).body(%s);",
        getNameOfJavaType(SpringJavaType.RESPONSE_ENTITY),
        getNameOfJavaType(SpringJavaType.HTTP_STATUS), entityItemName);

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(SpringJavaType.RESPONSE_ENTITY), parameterTypes,
            parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * Creates create batch method
   *
   * @return {@link MethodMetadata}
   */
  private MethodMetadata getCreateBatchMethod() {
    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("createBatch");

    // Adding parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(JavaType.collectionOf(this.entity),
        ANN_METADATA_VALID, ANN_METADATA_REQUEST_BODY));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.BINDING_RESULT));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Adding parameter names
    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(StringUtils.uncapitalize(this.entityPlural)));
    parameterNames.add(new JavaSymbolName("result"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @PostMapping annotation
    AnnotationMetadataBuilder postMappingAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.POST_MAPPING);
    postMappingAnnotation.addStringAttribute("value", "/batch");
    annotations.add(postMappingAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (result.hasErrors()) {
    // return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
    // }
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("return %s.status(%s.CONFLICT).body(result);",
        getNameOfJavaType(SpringJavaType.RESPONSE_ENTITY),
        getNameOfJavaType(SpringJavaType.HTTP_STATUS));
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // customerService.save(customers);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("%s.%s(%s);", controllerMetadata.getServiceField().getFieldName(),
        serviceMetadata.getCurrentSaveBatchMethod().getMethodName(),
        StringUtils.uncapitalize(this.entityPlural));

    // return ResponseEntity.created(listURI().toUri()).build();
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("return %s.created(%s().toUri()).build();",
        getNameOfJavaType(SpringJavaType.RESPONSE_ENTITY), LIST_URI_METHOD_NAME);

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(SpringJavaType.RESPONSE_ENTITY), parameterTypes,
            parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * Creates update batch method
   *
   * @return {@link MethodMetadata}
   */
  private MethodMetadata getUpdateBatchMethod() {
    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("updateBatch");

    // Adding parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(JavaType.collectionOf(this.entity),
        ANN_METADATA_VALID, ANN_METADATA_REQUEST_BODY));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.BINDING_RESULT));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Adding parameter names
    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(StringUtils.uncapitalize(this.entityPlural)));
    parameterNames.add(new JavaSymbolName("result"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @PutMapping annotation
    AnnotationMetadataBuilder putMappingAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PUT_MAPPING);
    putMappingAnnotation.addStringAttribute("value", "/batch");
    annotations.add(putMappingAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (result.hasErrors()) {
    // return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
    // }
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("return %s.status(%s.CONFLICT).body(result);",
        getNameOfJavaType(SpringJavaType.RESPONSE_ENTITY),
        getNameOfJavaType(SpringJavaType.HTTP_STATUS));
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // customerService.save(customers);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("%s.%s(%s);", controllerMetadata.getServiceField().getFieldName(),
        serviceMetadata.getCurrentSaveBatchMethod().getMethodName(),
        StringUtils.uncapitalize(this.entityPlural));

    // return ResponseEntity.ok().build();
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("return %s.ok().build();",
        getNameOfJavaType(SpringJavaType.RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(SpringJavaType.RESPONSE_ENTITY), parameterTypes,
            parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * Creates delete batch method
   *
   * @return {@link MethodMetadata}
   */
  private MethodMetadata getDeleteBatchMethod() {
    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("deleteBatch");

    // Adding parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    AnnotationMetadataBuilder pathVariable =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVariable.addStringAttribute("value", entityIdentifierPlural);
    parameterTypes.add(new AnnotatedJavaType(JavaType.collectionOf(entityMetadata
        .getCurrentIndentifierField().getFieldType()), pathVariable.build()));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Adding parameter names
    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(entityIdentifierPlural));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @DeleteMapping annotation
    AnnotationMetadataBuilder deleteMappingAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.DELETE_MAPPING);
    deleteMappingAnnotation.addStringAttribute("value", "/batch/{" + entityIdentifierPlural + "}");
    annotations.add(deleteMappingAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // serviceField.SERVICE_DELETE_METHOD(ids);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("%s.%s(%s);", controllerMetadata.getServiceField().getFieldName(),
        serviceMetadata.getCurrentDeleteBatchMethod().getMethodName(), entityIdentifierPlural);

    // return ResponseEntity.ok().build();
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("return %s.ok().build();",
        getNameOfJavaType(SpringJavaType.RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(SpringJavaType.RESPONSE_ENTITY), parameterTypes,
            parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the getModelAttributeMethod() method
   *
   * @return MethodMetadata
   */
  private MethodMetadata getModelAttributeMethod() {

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("get" + entity.getSimpleTypeName());

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    AnnotationMetadataBuilder pathVariable =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVariable.addStringAttribute("value", entityItemName);
    parameterTypes.add(new AnnotatedJavaType(entityMetadata.getCurrentIndentifierField()
        .getFieldType(), pathVariable.build()));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(entityIdentifier));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Customer customer = customerService.findOne(id);
    bodyBuilder.appendFormalLine("%s %s = %s.%s(%s);", getNameOfJavaType(entity), entityItemName,
        controllerMetadata.getServiceField().getFieldName(), serviceMetadata
            .getCurrentFindOneMethod().getMethodName(), entityIdentifier);

    // if (customer == null) {
    //   throw new NotFoundException("Customer not found");
    // }
    bodyBuilder.appendFormalLine("if (%s == null) {", entityItemName);
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(
        "throw new %s(String.format(\"%s with identifier '%%s' not found\",%s));",
        getNameOfJavaType(SpringletsJavaType.SPRINGLETS_NOT_FOUND_EXCEPTION),
        entity.getSimpleTypeName(), entityIdentifier);
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // return customer;
    bodyBuilder.appendFormalLine("return %s;", entityItemName);

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, entity, parameterTypes,
            parameterNames, bodyBuilder);

    methodBuilder.addAnnotation(new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE)
        .build());

    return methodBuilder.build();
  }


  /**
   * This method provides all detail methods using JSON response type
   *
   * @return List of MethodMetadata
   */
  private List<MethodMetadata> getDetailMethods() {

    List<MethodMetadata> detailMethods = new ArrayList<MethodMetadata>();

    if (this.type != ControllerType.DETAIL) {
      return detailMethods;
    }

    MethodMetadata listDetailMethod = getListDetailMethod();
    if (listDetailMethod != null) {
      detailMethods.add(listDetailMethod);
    }

    return detailMethods;
  }


  /**
   * This method provides detail list method using JSON response type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getListDetailMethod() {
    // TODO
    return null;
    /*
    // First of all, check if exists other method with the same
    // @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "", null, null,
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName =
        new JavaSymbolName("list".concat(this.detailFieldInfo.getEntity().getSimpleTypeName()));

    // Create PageableDefault annotation
    AnnotationMetadataBuilder pageableDefaultAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PAGEABLE_DEFAULT);

    String sortFieldName = "";
    MemberDetails entityDetails =
        getMemberDetails(getTypeLocationService().getTypeDetails(
            this.detailFieldInfo.getEntity()));
    List<FieldMetadata> fields = entityDetails.getFields();
    for (FieldMetadata field : fields) {
      if (field.getAnnotation(new JavaType("javax.persistence.Id")) != null) {
        sortFieldName = field.getFieldName().getSymbolName();
      }
    }
    if (!sortFieldName.isEmpty()) {
      pageableDefaultAnnotation.addStringAttribute("sort", sortFieldName);
    }

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    AnnotationMetadataBuilder modelAttributeAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE);
    parameterTypes.add(new AnnotatedJavaType(this.detailFieldInfo.getParentEntity(),
        modelAttributeAnnotation.build()));
    parameterTypes.add(new AnnotatedJavaType(SpringletsJavaType.SPRINGLETS_GLOBAL_SEARCH));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.PAGEABLE, pageableDefaultAnnotation
        .build()));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(StringUtils.uncapitalize(this.detailFieldInfo
        .getParentEntity().getSimpleTypeName())));
    parameterNames.add(new JavaSymbolName("search"));
    parameterNames.add(PAGEABLE_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, "", null, null,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE, ""));

    // Adding @ResponseBody annotation
    AnnotationMetadataBuilder responseBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(responseBodyAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Get finder method
    ClassOrInterfaceTypeDetails serviceDetails =
        getTypeLocationService().getTypeDetails(this.detailFieldInfo.getService());

    final LogicalPath serviceLogicalPath =
        PhysicalTypeIdentifier.getPath(serviceDetails.getDeclaredByMetadataId());
    final String serviceMetadataKey =
        ServiceMetadata.createIdentifier(serviceDetails.getType(), serviceLogicalPath);
    final ServiceMetadata serviceMetadata =
        (ServiceMetadata) getMetadataService().get(serviceMetadataKey);

    // Get parent field
    FieldMetadata parentRelationField = null;
    MemberDetails memberDetails = getMemberDetails(this.detailFieldInfo.getParentEntity());
    List<FieldMetadata> parentFields = memberDetails.getFields();
    for (FieldMetadata parentField : parentFields) {
      if (parentField.getFieldName().getSymbolName()
          .equals(this.detailFieldInfo.getParentReferenceFieldName())) {
        AnnotationMetadata oneToManyAnnotation = parentField.getAnnotation(JpaJavaType.ONE_TO_MANY);
        if (oneToManyAnnotation != null
            && (parentField.getFieldType().getFullyQualifiedTypeName()
                .equals(JavaType.LIST.getFullyQualifiedTypeName()) || parentField.getFieldType()
                .getFullyQualifiedTypeName().equals(JavaType.SET.getFullyQualifiedTypeName()))) {
          parentRelationField = parentField;
          break;
        }
      }
    }

    Validate.notNull(parentRelationField, String.format(
        "ERROR: '%s' must have a field related to '%s'", this.detailFieldInfo
            .getParentEntity().getSimpleTypeName(), this.detailFieldInfo.getEntity()
            .getSimpleTypeName()));

    // Generating returnType
    Map<FieldMetadata, MethodMetadata> referencedFieldsFindAllDefinedMethods =
        serviceMetadata.getReferencedFieldsFindAllDefinedMethods();
    AnnotationAttributeValue<Object> attributeMappedBy =
        parentRelationField.getAnnotation(JpaJavaType.ONE_TO_MANY).getAttribute("mappedBy");

    Validate.notNull(attributeMappedBy, String.format(
        "ERROR: The field '%s' of '%s' must have 'mappedBy' value", parentRelationField
            .getFieldName(), this.detailFieldInfo.getParentEntity().getSimpleTypeName()));

    String mappedBy = (String) attributeMappedBy.getValue();
    MethodMetadata findByMethod = null;
    Iterator<Entry<FieldMetadata, MethodMetadata>> it =
        referencedFieldsFindAllDefinedMethods.entrySet().iterator();
    while (it.hasNext()) {
      Entry<FieldMetadata, MethodMetadata> finder = it.next();
      if (finder.getKey().getFieldName().getSymbolName().equals(mappedBy)) {
        findByMethod = finder.getValue();
        break;
      }
    }

    JavaType returnType = findByMethod.getReturnType();
    List<JavaType> returnParameterTypes = returnType.getParameters();
    StringBuffer returnTypeParamsString = new StringBuffer();
    for (int i = 0; i < returnParameterTypes.size(); i++) {
      addTypeToImport(returnParameterTypes.get(i));
      if (i > 0) {
        returnTypeParamsString.append(",");
      }
      returnTypeParamsString.append(returnParameterTypes.get(i).getSimpleTypeName());

      // Add module dependency
      getTypeLocationService().addModuleDependency(this.controller.getType().getModule(),
          returnParameterTypes.get(i));
    }

    // Page<ENTITYREL> entityrelplural =
    // entityRelNameService.findAllByENTITYNAME(ENTITYNAME, search,
    // pageable);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("%s<%s> %s = %s.%s(%s, search, pageable);",
        addTypeToImport(returnType).getSimpleTypeName(), returnTypeParamsString, StringUtils
            .uncapitalize(StringUtils.lowerCase(getPluralService().getPlural(
                this.detailFieldInfo.getEntity()))),
        getServiceField(this.detailFieldInfo.getService()).getFieldName()
            .getSymbolNameUnCapitalisedFirstLetter(), findByMethod.getMethodName(), StringUtils
            .uncapitalize(this.detailFieldInfo.getParentEntity().getSimpleTypeName())));

    // return entityrelplural;
    bodyBuilder.appendFormalLine(String.format(
        "return %s;",
        StringUtils.uncapitalize(StringUtils.lowerCase(getPluralService().getPlural(
            this.detailFieldInfo.getEntity())))));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            returnType, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);
    return methodBuilder.build();
    */
  }


  /**
   * Method that returns list JSON method
   *
   * @return {@link MethodMetadata}
   */
  public MethodMetadata getCurrentListMethod() {
    return this.listMethod;
  }

  /**
   * Method that returns create JSON method
   *
   * @return {@link MethodMetadata}
   */
  public MethodMetadata getCurrentCreateMethod() {
    return this.createMethod;
  }

  /**
   * Method that returns update JSON method
   *
   * @return {@link MethodMetadata}
   */
  public MethodMetadata getCurrentUpdateMethod() {
    return this.updateMethod;
  }

  /**
   * Method that returns delete JSON method
   *
   * @return {@link MethodMetadata}
   */
  public MethodMetadata getCurrentDeleteMethod() {
    return this.deleteMethod;
  }

  /**
   * Method that returns delete batch JSON method
   *
   * @return {@link MethodMetadata}
   */
  public MethodMetadata getCurrentDeleteBatchMethod() {
    return this.deleteBatchMethod;
  }

  /**
   * Method that returns create batch JSON method
   *
   * @return {@link MethodMetadata}
   */
  public MethodMetadata getCurrentCreateBatchMethod() {
    return this.createBatchMethod;
  }

  /**
   * Method that returns update batch JSON method
   *
   * @return {@link MethodMetadata}
   */
  public MethodMetadata getCurrentUpdateBatchMethod() {
    return this.updateBatchMethod;
  }

  /**
   * Method that returns show JSON method
   *
   * @return {@link MethodMetadata}
   */
  public MethodMetadata getCurrentShowMethod() {
    return this.showMethod;
  }

  /**
   * Method that returns finder JSON methods
   *
   * @return {@link List<MethodMetadata>}
   */
  public List<MethodMetadata> getCurrentFinderMethods() {
    return this.finderMethods;
  }

  /**
   * Method that returns populateHeaders method
   *
   * @return {@link MethodMetadata}
   */
  public MethodMetadata getCurrentModelAttributeMethod() {
    return this.modelAttributeMethod;
  }

  /**
   * Method that returns if related entity is readOnly or not.
   *
   * @return
   */
  public boolean isReadOnly() {
    return this.readOnly;
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
