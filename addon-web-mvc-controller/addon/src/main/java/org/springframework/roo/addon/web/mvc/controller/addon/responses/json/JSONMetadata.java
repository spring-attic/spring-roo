package org.springframework.roo.addon.web.mvc.controller.addon.responses.json;

import static org.springframework.roo.model.SpringJavaType.DELETE_MAPPING;
import static org.springframework.roo.model.SpringJavaType.GET_MAPPING;
import static org.springframework.roo.model.SpringJavaType.POST_MAPPING;
import static org.springframework.roo.model.SpringJavaType.PUT_MAPPING;
import static org.springframework.roo.model.SpringJavaType.RESPONSE_ENTITY;
import static org.springframework.roo.model.SpringletsJavaType.SPRINGLETS_GLOBAL_SEARCH;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.jpa.annotations.entity.JpaRelationType;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;
import org.springframework.roo.addon.web.mvc.controller.addon.RelationInfoExtended;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.addon.web.mvc.controller.annotations.responses.json.RooJSON;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Metadata for {@link RooJSON}.
 *
 * @author Juan Carlos García
 * @author Jose Manuel Vivó
 * @author Sergio Clares
 * @since 2.0
 */
public class JSONMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

  private static final AnnotationMetadata ANN_REQUEST_BODY = AnnotationMetadataBuilder
      .getInstance(SpringJavaType.REQUEST_BODY);
  private static final AnnotationMetadata ANN_MODEL_ATTRIBUTE = AnnotationMetadataBuilder
      .getInstance(SpringJavaType.MODEL_ATTRIBUTE);
  private static final JavaSymbolName LIST_URI_METHOD_NAME = new JavaSymbolName("listURI");
  private static final JavaSymbolName SHOW_URI_METHOD_NAME = new JavaSymbolName("showURI");
  private static final AnnotationMetadata ANN_METADATA_REQUEST_BODY =
      new AnnotationMetadataBuilder(SpringJavaType.REQUEST_BODY).build();
  private static final AnnotationMetadata ANN_METADATA_VALID = new AnnotationMetadataBuilder(
      Jsr303JavaType.VALID).build();
  private static final AnnotatedJavaType PAGEABLE_PARAM = new AnnotatedJavaType(
      SpringJavaType.PAGEABLE);
  private static final JavaSymbolName PAGEABLE_PARAM_NAME = new JavaSymbolName("pageable");
  private static final JavaSymbolName GLOBAL_SEARCH_NAME = new JavaSymbolName("globalSearch");
  private static final JavaSymbolName GLOBAL_SEARCH_PARAM_NAME = new JavaSymbolName("search");

  private static final String PROVIDES_TYPE_STRING = JSONMetadata.class.getName();
  private static final String PROVIDES_TYPE = MetadataIdentificationUtils
      .create(PROVIDES_TYPE_STRING);

  private static final JavaSymbolName FORM_BEAN_PARAM_NAME = new JavaSymbolName("formBean");

  private static final AnnotatedJavaType GLOBAL_SEARCH_PARAM = new AnnotatedJavaType(
      SPRINGLETS_GLOBAL_SEARCH);

  private static final AnnotationMetadataBuilder RESPONSE_BODY_ANNOTATION =
      new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);

  private final boolean readOnly;
  private final ControllerMetadata controllerMetadata;
  private final Map<String, MethodMetadata> finderMethods;
  private final ControllerType type;
  private final ConstructorMetadata constructor;
  private final List<MethodMetadata> allMethods;
  private final ServiceMetadata serviceMetadata;
  private final JavaType entity;
  private final String entityPlural;
  private final List<Pair<RelationInfo, JpaEntityMetadata>> compositionRelationOneToOne;
  private final String entityItemName;
  private final JpaEntityMetadata entityMetadata;
  private final JavaType itemController;
  private final String entityIdentifierPlural;
  private final String entityIdentifier;
  private final Map<RelationInfo, MethodMetadata> modelAttributeDetailsMethod;

  private final MethodMetadata listMethod;
  private final MethodMetadata showMethod;
  private final MethodMetadata createMethod;
  private final MethodMetadata updateMethod;
  private final MethodMetadata deleteMethod;
  private final MethodMetadata createBatchMethod;
  private final MethodMetadata updateBatchMethod;
  private final MethodMetadata deleteBatchMethod;
  private final MethodMetadata modelAttributeMethod;
  private final MethodMetadata listDetailsMethod;
  private final MethodMetadata addToDetailsMethod;
  private final MethodMetadata removeFromDetailsMethod;
  private final MethodMetadata addToDetailsBatchMethod;
  private final MethodMetadata removeFromDetailsBatchMethod;
  private final MethodMetadata showDetailMethod;
  private final MethodMetadata updateDetailMethod;
  private final MethodMetadata deleteDetailMethod;
  private final MethodMetadata showURIMethod;
  private final MethodMetadata listURIMethod;

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
   * @param findersToAdd
   */
  public JSONMetadata(final String identifier, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      ControllerMetadata controllerMetadata, ServiceMetadata serviceMetadata,
      JpaEntityMetadata entityMetadata, String entityPlural, String entityIdentifierPlural,
      final List<Pair<RelationInfo, JpaEntityMetadata>> compositionRelationOneToOne,
      final JavaType itemController, final Map<String, MethodMetadata> findersToAdd) {
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
      case COLLECTION: {
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
        this.modelAttributeDetailsMethod = null;
        this.listDetailsMethod = null;
        this.addToDetailsMethod = null;
        this.removeFromDetailsMethod = null;
        this.addToDetailsBatchMethod = null;
        this.removeFromDetailsBatchMethod = null;
        this.showDetailMethod = null;
        this.updateDetailMethod = null;
        this.deleteDetailMethod = null;
        break;
      }
      case ITEM: {
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
        this.modelAttributeDetailsMethod = null;
        this.listDetailsMethod = null;
        this.addToDetailsMethod = null;
        this.removeFromDetailsMethod = null;
        this.addToDetailsBatchMethod = null;
        this.removeFromDetailsBatchMethod = null;
        this.showDetailMethod = null;
        this.updateDetailMethod = null;
        this.deleteDetailMethod = null;

        break;
      }
      case SEARCH: {
        Map<String, MethodMetadata> tmpFinders = new TreeMap<String, MethodMetadata>();
        MethodMetadata finderMethod;
        for (Entry<String, MethodMetadata> finder : findersToAdd.entrySet()) {
          finderMethod = getFinderMethodForFinderInService(finder.getKey(), finder.getValue());
          tmpFinders.put(finder.getKey(), addAndGet(finderMethod, allMethods));
        }
        this.finderMethods = Collections.unmodifiableMap(tmpFinders);

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
        this.modelAttributeDetailsMethod = null;
        this.listDetailsMethod = null;
        this.addToDetailsMethod = null;
        this.removeFromDetailsMethod = null;
        this.addToDetailsBatchMethod = null;
        this.removeFromDetailsBatchMethod = null;
        this.showDetailMethod = null;
        this.updateDetailMethod = null;
        this.deleteDetailMethod = null;
        break;
      }
      case DETAIL: {
        this.modelAttributeMethod = addAndGet(getModelAttributeMethod(), allMethods);
        Map<RelationInfo, MethodMetadata> modelAtributeDetailsMethod =
            new TreeMap<RelationInfo, MethodMetadata>();
        for (int i = 0; i < controllerMetadata.getDetailsFieldInfo().size() - 1; i++) {
          RelationInfo info = controllerMetadata.getDetailsFieldInfo().get(i);
          JavaType entityType = info.childType;
          MethodMetadata method =
              addAndGet(
                  getModelAttributeMethod(info.fieldName,
                      controllerMetadata.getServiceMetadataForEntity(entityType),
                      controllerMetadata.getDetailsServiceFields().get(entityType)), allMethods);
          modelAtributeDetailsMethod.put(info, method);
        }
        this.modelAttributeDetailsMethod = Collections.unmodifiableMap(modelAtributeDetailsMethod);
        this.listDetailsMethod = addAndGet(getListDetailsMethod(), allMethods);
        if (!entityMetadata.isReadOnly()) {
          this.addToDetailsMethod = addAndGet(getAddToDetailsMethod(), allMethods);
          this.addToDetailsBatchMethod = addAndGet(getAddToDetailsBatchMethod(), allMethods);
          if (controllerMetadata.getLastDetailsInfo().type == JpaRelationType.AGGREGATION) {
            this.removeFromDetailsMethod = addAndGet(getRemoveFromDetailsMethod(), allMethods);
            this.removeFromDetailsBatchMethod =
                addAndGet(getRemoveFromDetailsBatchMethod(), allMethods);
          } else {
            this.removeFromDetailsMethod = null;
            this.removeFromDetailsBatchMethod = null;
          }

        } else {
          this.addToDetailsMethod = null;
          this.addToDetailsBatchMethod = null;
          this.removeFromDetailsMethod = null;
          this.removeFromDetailsBatchMethod = null;
        }

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
        this.finderMethods = null;
        this.showDetailMethod = null;
        this.updateDetailMethod = null;
        this.deleteDetailMethod = null;
        break;
      }
      case DETAIL_ITEM: {
        this.modelAttributeMethod = addAndGet(getModelAttributeMethod(), allMethods);
        Map<RelationInfo, MethodMetadata> modelAtributeDetailsMethod =
            new TreeMap<RelationInfo, MethodMetadata>();
        for (RelationInfo info : controllerMetadata.getDetailsFieldInfo()) {
          JavaType entityType = info.childType;
          MethodMetadata method =
              addAndGet(
                  getModelAttributeMethod(info.fieldName,
                      controllerMetadata.getServiceMetadataForEntity(entityType),
                      controllerMetadata.getDetailsServiceFields().get(entityType)), allMethods);
          modelAtributeDetailsMethod.put(info, method);
        }
        this.modelAttributeDetailsMethod = Collections.unmodifiableMap(modelAtributeDetailsMethod);
        this.showDetailMethod = addAndGet(getShowDetailMethod(), allMethods);
        this.updateDetailMethod = addAndGet(getUpdateDetailMethod(), allMethods);
        this.deleteDetailMethod = addAndGet(getDeleteDetailMethod(), allMethods);

        this.listDetailsMethod = null;
        this.addToDetailsMethod = null;
        this.removeFromDetailsMethod = null;
        this.addToDetailsBatchMethod = null;
        this.removeFromDetailsBatchMethod = null;
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
        this.finderMethods = null;
        break;
      }
      default:
        throw new IllegalArgumentException("Unsupported Controller type: " + this.type.name());
    }

    this.allMethods = Collections.unmodifiableList(allMethods);

    // Build the ITD
    itdTypeDetails = builder.build();
  }

  private MethodMetadata getDeleteDetailMethod() {
    RelationInfoExtended detailsInfo = controllerMetadata.getLastDetailsInfo();
    final ServiceMetadata detailsServiceMetadata =
        controllerMetadata.getServiceMetadataForEntity(detailsInfo.entityType);
    final MethodMetadata removeFromMethod =
        detailsServiceMetadata.getRemoveFromRelationMethods().get(detailsInfo);

    final FieldMetadata detailsServiceField =
        controllerMetadata.getDetailsServiceFields(detailsInfo.entityType);

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("delete");

    JavaSymbolName itemsName = detailsInfo.fieldMetadata.getFieldName();

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(removeFromMethod.getParameterTypes().get(0)
        .getJavaType(), ANN_MODEL_ATTRIBUTE));
    AnnotationMetadata modelAttributAnnotation =
        AnnotationMetadataBuilder.getInstance(SpringJavaType.MODEL_ATTRIBUTE);

    parameterTypes.add(new AnnotatedJavaType(detailsInfo.childType, modelAttributAnnotation));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }
    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @DeleteMapping annotation
    AnnotationMetadataBuilder postMappingAnnotation = new AnnotationMetadataBuilder(DELETE_MAPPING);
    postMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(postMappingAnnotation);

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(removeFromMethod.getParameterNames().get(0));
    parameterNames.add(itemsName);


    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // customerService.addToOrders(customer, order.getId());
    bodyBuilder.appendFormalLine("%s().%s(%s,%s.singleton(%s.%s()));",
        getAccessorMethod(detailsServiceField).getMethodName(), removeFromMethod.getMethodName(),
        removeFromMethod.getParameterNames().get(0), getNameOfJavaType(JavaType.COLLECTIONS),
        itemsName, detailsInfo.childEntityMetadata.getCurrentIdentifierAccessor().getMethodName());

    // return ResponseEntity.ok().build();
    bodyBuilder.appendFormalLine("return %s.ok().build();", getNameOfJavaType(RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  private MethodMetadata getUpdateDetailMethod() {
    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("update");


    final RelationInfoExtended info = controllerMetadata.getLastDetailsInfo();
    final JavaType parentEntity = info.entityType;
    final JavaType entity = info.childType;
    final JpaEntityMetadata entityMetadata = info.childEntityMetadata;
    final FieldMetadata entityIdentifier = entityMetadata.getCurrentIndentifierField();
    final String entityItemName = StringUtils.uncapitalize(entity.getSimpleTypeName());
    final ServiceMetadata serviceMetadata = controllerMetadata.getServiceMetadataForEntity(entity);


    // Define parameters
    final List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(parentEntity, ANN_MODEL_ATTRIBUTE));
    parameterTypes.add(new AnnotatedJavaType(entity, ANN_MODEL_ATTRIBUTE));
    parameterTypes
        .add(new AnnotatedJavaType(entity, ANN_METADATA_VALID, ANN_METADATA_REQUEST_BODY));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.BINDING_RESULT));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }


    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    final String storedName = "stored".concat(entity.getSimpleTypeName());
    parameterNames.add(new JavaSymbolName(
        StringUtils.uncapitalize(parentEntity.getSimpleTypeName())));
    parameterNames.add(new JavaSymbolName(storedName));
    parameterNames.add(new JavaSymbolName(entityItemName));
    parameterNames.add(new JavaSymbolName("result"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @PutMapping annotation
    AnnotationMetadataBuilder putMappingAnnotation = new AnnotationMetadataBuilder(PUT_MAPPING);
    putMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
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
        getNameOfJavaType(RESPONSE_ENTITY), getNameOfJavaType(SpringJavaType.HTTP_STATUS));
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // orderDetail.setId(storedOrderDetails.getId());
    bodyBuilder.appendFormalLine("%s.set%s(%s.get%s());", entityItemName, entityIdentifier
        .getFieldName().getSymbolNameCapitalisedFirstLetter(), storedName, entityIdentifier
        .getFieldName().getSymbolNameCapitalisedFirstLetter());

    // customerService.save(customer);
    bodyBuilder.appendFormalLine("%s().%s(%s);",
        getAccessorMethod(controllerMetadata.getLastDetailServiceField()).getMethodName(),
        serviceMetadata.getCurrentSaveMethod().getMethodName(), entityItemName);

    // return ResponseEntity.ok().build();
    bodyBuilder.appendFormalLine("return %s.ok().build();", getNameOfJavaType(RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * Generates a finder method which delegates on entity service to get result
   *
   * @param finderName
   * @param serviceFinderMethod
   * @return
   */
  private MethodMetadata getFinderMethodForFinderInService(String finderName,
      MethodMetadata serviceFinderMethod) {

    // Define methodName
    String pathName = finderName;
    if (pathName.startsWith("findBy")) {
      pathName = pathName.replace("findBy", "by");
    }
    final JavaSymbolName methodName = new JavaSymbolName(pathName);

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    // Form Bean is always the first parameter of finder
    final JavaType formBean = serviceFinderMethod.getParameterTypes().get(0).getJavaType();
    List<AnnotationMetadata> formBeanAnnotations = new ArrayList<AnnotationMetadata>();
    AnnotationMetadataBuilder formBeanAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE);
    formBeanAnnotation.addStringAttribute("value", FORM_BEAN_PARAM_NAME.getSymbolName());
    formBeanAnnotations.add(formBeanAnnotation.build());
    AnnotatedJavaType annotatedFormBean = new AnnotatedJavaType(formBean, formBeanAnnotations);
    parameterTypes.add(annotatedFormBean);

    // Including GlobalSearch parameter and DatatablesPageable parameter
    parameterTypes.add(GLOBAL_SEARCH_PARAM);
    parameterTypes.add(PAGEABLE_PARAM);

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    final List<String> parameterStrings = new ArrayList<String>();
    parameterNames.add(FORM_BEAN_PARAM_NAME);
    parameterStrings.add(FORM_BEAN_PARAM_NAME.getSymbolName());
    parameterNames.add(GLOBAL_SEARCH_PARAM_NAME);
    parameterStrings.add(GLOBAL_SEARCH_PARAM_NAME.getSymbolName());
    parameterNames.add(PAGEABLE_PARAM_NAME);
    parameterStrings.add(PAGEABLE_PARAM_NAME.getSymbolName());

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @GetMapping annotation
    AnnotationMetadataBuilder getMappingAnnotation = new AnnotationMetadataBuilder(GET_MAPPING);
    getMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    // TODO Delegates on ControllerOperations to obtain the URL for this finder
    getMappingAnnotation.addStringAttribute("value", "/" + pathName);
    annotations.add(getMappingAnnotation);

    // Generating returnType
    JavaType serviceReturnType = serviceFinderMethod.getReturnType();
    JavaType datatablesDataReturnType =
        serviceReturnType.getParameters().isEmpty() ? serviceReturnType.getBaseType()
            : serviceReturnType.getParameters().get(0);
    JavaType returnType =
        JavaType.wrapperOf(RESPONSE_ENTITY,
            JavaType.wrapperOf(SpringJavaType.PAGE, datatablesDataReturnType));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    final String itemNames = StringUtils.uncapitalize(this.entityPlural);

    // Page<Customer> customers = customerService.findAll(formBean, globalSearch, pageable);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("%s %s = %s().%s(%s);", getNameOfJavaType(serviceReturnType),
        itemNames, getAccessorMethod(controllerMetadata.getServiceField()).getMethodName(),
        serviceFinderMethod.getMethodName(), StringUtils.join(parameterStrings, ","));

    // return ResponseEntity.ok(owners);
    bodyBuilder.appendFormalLine(String.format("return %s.ok(%s);",
        getNameOfJavaType(RESPONSE_ENTITY), itemNames));


    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, parameterTypes,
            parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  private MethodMetadata getRemoveFromDetailsBatchMethod() {
    RelationInfo detailsInfo = controllerMetadata.getLastDetailsInfo();
    final ServiceMetadata detailsServiceMetadata =
        controllerMetadata.getServiceMetadataForEntity(detailsInfo.entityType);
    final MethodMetadata removeFromDetailsMethod =
        detailsServiceMetadata.getRemoveFromRelationMethods().get(detailsInfo);
    final FieldMetadata detailsServiceField =
        controllerMetadata.getDetailsServiceFields(detailsInfo.entityType);

    // Define methodName
    final JavaSymbolName methodName =
        new JavaSymbolName(removeFromDetailsMethod.getMethodName().getSymbolName() + "Batch");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(removeFromDetailsMethod.getParameterTypes().get(0)
        .getJavaType(), ANN_MODEL_ATTRIBUTE));
    parameterTypes.add(new AnnotatedJavaType(removeFromDetailsMethod.getParameterTypes().get(1)
        .getJavaType(), ANN_REQUEST_BODY));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }
    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @DeleteMapping annotation
    AnnotationMetadataBuilder postMappingAnnotation = new AnnotationMetadataBuilder(DELETE_MAPPING);
    postMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    postMappingAnnotation.addStringAttribute("value", "/batch");
    annotations.add(postMappingAnnotation);

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.addAll(removeFromDetailsMethod.getParameterNames());

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // customerService.removeFromOrders(customer, orders);
    bodyBuilder.appendFormalLine("%s().%s(%s,%s);", getAccessorMethod(detailsServiceField)
        .getMethodName(), removeFromDetailsMethod.getMethodName(), removeFromDetailsMethod
        .getParameterNames().get(0), removeFromDetailsMethod.getParameterNames().get(1));

    // return ResponseEntity.ok().build();
    bodyBuilder.appendFormalLine("return %s.ok().build();", getNameOfJavaType(RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  private MethodMetadata getAddToDetailsBatchMethod() {
    RelationInfo detailsInfo = controllerMetadata.getLastDetailsInfo();
    final ServiceMetadata detailsServiceMetadata =
        controllerMetadata.getServiceMetadataForEntity(detailsInfo.entityType);
    final MethodMetadata addToMethod =
        detailsServiceMetadata.getAddToRelationMethods().get(detailsInfo);
    final FieldMetadata detailsServiceField =
        controllerMetadata.getDetailsServiceFields(detailsInfo.entityType);

    // Define methodName
    final JavaSymbolName methodName =
        new JavaSymbolName(addToMethod.getMethodName().getSymbolName() + "Batch");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(addToMethod.getParameterTypes().get(0).getJavaType(),
        ANN_MODEL_ATTRIBUTE));
    parameterTypes.add(new AnnotatedJavaType(addToMethod.getParameterTypes().get(1).getJavaType(),
        ANN_REQUEST_BODY));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }
    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @PostMapping annotation
    AnnotationMetadataBuilder postMappingAnnotation = new AnnotationMetadataBuilder(POST_MAPPING);
    postMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    postMappingAnnotation.addStringAttribute("value", "/batch");
    annotations.add(postMappingAnnotation);

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.addAll(addToMethod.getParameterNames());

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // customerService.addToOrders(customer, order);
    bodyBuilder.appendFormalLine("%s().%s(%s,%s);", getAccessorMethod(detailsServiceField)
        .getMethodName(), addToMethod.getMethodName(), addToMethod.getParameterNames().get(0),
        addToMethod.getParameterNames().get(1));

    // return ResponseEntity.ok().build();
    bodyBuilder.appendFormalLine("return %s.ok().build();", getNameOfJavaType(RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }


  private MethodMetadata getRemoveFromDetailsMethod() {
    RelationInfo detailsInfo = controllerMetadata.getLastDetailsInfo();
    final ServiceMetadata detailsServiceMetadata =
        controllerMetadata.getServiceMetadataForEntity(detailsInfo.entityType);
    final MethodMetadata removeFromMethod =
        detailsServiceMetadata.getRemoveFromRelationMethods().get(detailsInfo);

    final FieldMetadata detailsServiceField =
        controllerMetadata.getDetailsServiceFields(detailsInfo.entityType);

    // Define methodName
    final JavaSymbolName methodName = removeFromMethod.getMethodName();

    JavaSymbolName itemsName = removeFromMethod.getParameterNames().get(1);

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(removeFromMethod.getParameterTypes().get(0)
        .getJavaType(), ANN_MODEL_ATTRIBUTE));
    AnnotationMetadataBuilder pathVariableAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVariableAnnotation.addStringAttribute("value", itemsName.getSymbolName());

    parameterTypes.add(new AnnotatedJavaType(removeFromMethod.getParameterTypes().get(1)
        .getJavaType().getParameters().get(0), pathVariableAnnotation.build()));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }
    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @DeleteMapping annotation
    AnnotationMetadataBuilder postMappingAnnotation = new AnnotationMetadataBuilder(DELETE_MAPPING);
    postMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(postMappingAnnotation);

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.addAll(removeFromMethod.getParameterNames());

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // customerService.addToOrders(customer, Collections.singleton(order));
    bodyBuilder.appendFormalLine("%s().%s(%s,%s.singleton(%s));",
        getAccessorMethod(detailsServiceField).getMethodName(), removeFromMethod.getMethodName(),
        removeFromMethod.getParameterNames().get(0), getNameOfJavaType(JavaType.COLLECTIONS),
        itemsName);

    // return ResponseEntity.ok().build();
    bodyBuilder.appendFormalLine("return %s.ok().build();", getNameOfJavaType(RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  private MethodMetadata getAddToDetailsMethod() {
    RelationInfo detailsInfo = controllerMetadata.getLastDetailsInfo();
    final ServiceMetadata detailsServiceMetadata =
        controllerMetadata.getServiceMetadataForEntity(detailsInfo.entityType);
    final MethodMetadata addToMethod =
        detailsServiceMetadata.getAddToRelationMethods().get(detailsInfo);
    final FieldMetadata detailsServiceField =
        controllerMetadata.getDetailsServiceFields(detailsInfo.entityType);

    // Define methodName
    final JavaSymbolName methodName = addToMethod.getMethodName();
    JavaSymbolName itemsName = addToMethod.getParameterNames().get(1);

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(addToMethod.getParameterTypes().get(0).getJavaType(),
        ANN_MODEL_ATTRIBUTE));
    AnnotationMetadataBuilder pathVariableAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVariableAnnotation.addStringAttribute("value", itemsName.getSymbolName());

    parameterTypes.add(new AnnotatedJavaType(addToMethod.getParameterTypes().get(1).getJavaType()
        .getParameters().get(0), pathVariableAnnotation.build()));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }
    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @PostMapping annotation
    AnnotationMetadataBuilder postMappingAnnotation = new AnnotationMetadataBuilder(POST_MAPPING);
    postMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(postMappingAnnotation);

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.addAll(addToMethod.getParameterNames());

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // customerService.addToOrders(customer, Collections.singleton(order));
    bodyBuilder.appendFormalLine("%s().%s(%s,%s.singleton(%s));",
        getAccessorMethod(detailsServiceField).getMethodName(), addToMethod.getMethodName(),
        addToMethod.getParameterNames().get(0), getNameOfJavaType(JavaType.COLLECTIONS), itemsName);

    // return ResponseEntity.ok().build();
    bodyBuilder.appendFormalLine("return %s.ok().build();", getNameOfJavaType(RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  private MethodMetadata getListDetailsMethod() {
    RelationInfo detailsInfo = controllerMetadata.getLastDetailsInfo();
    final ServiceMetadata detailsServiceMetadata =
        controllerMetadata.getServiceMetadataForEntity(detailsInfo.childType);
    final MethodMetadata findAllMethod =
        detailsServiceMetadata.getRefencedFieldFindAllDefinedMethod(detailsInfo.mappedBy);
    final FieldMetadata detailsServiceField =
        controllerMetadata.getDetailsServiceFields(detailsInfo.childType);

    // Define methodName
    final JavaSymbolName methodName =
        new JavaSymbolName("list" + StringUtils.capitalize(detailsInfo.fieldName));

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(
        findAllMethod.getParameterTypes().get(0).getJavaType(), ANN_MODEL_ATTRIBUTE));
    parameterTypes.add(new AnnotatedJavaType(SpringletsJavaType.SPRINGLETS_GLOBAL_SEARCH));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.PAGEABLE));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    final JavaSymbolName parentParamName = findAllMethod.getParameterNames().get(0);
    parameterNames.add(parentParamName);
    parameterNames.add(GLOBAL_SEARCH_NAME);
    parameterNames.add(PAGEABLE_PARAM_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @GetMapping annotation
    AnnotationMetadataBuilder getMappingAnnotation = new AnnotationMetadataBuilder(GET_MAPPING);
    getMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(getMappingAnnotation);



    // Generating returnType
    JavaType serviceReturnType = findAllMethod.getReturnType();
    JavaType returnType = JavaType.wrapperOf(RESPONSE_ENTITY, serviceReturnType);

    // TODO
    // Add module dependency
    //getTypeLocationService().addModuleDependency(this.controller.getType().getModule(),
    //    returnParameterTypes.get(i));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    final String itemNames = StringUtils.uncapitalize(detailsInfo.fieldName);

    // Page<Customer> customers = customerService.findAll(globalSearch, pageable);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("%s %s = %s().%s(%s, %s, %s);",
        getNameOfJavaType(serviceReturnType), itemNames, getAccessorMethod(detailsServiceField)
            .getMethodName(), findAllMethod.getMethodName(), parentParamName, GLOBAL_SEARCH_NAME,
        PAGEABLE_PARAM_NAME);

    // return ResponseEntity.ok(customers);
    bodyBuilder.appendFormalLine(String.format("return %s.ok(%s);",
        getNameOfJavaType(RESPONSE_ENTITY), itemNames));


    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, parameterTypes,
            parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
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
    AnnotationMetadataBuilder annotationBuilder =
        new AnnotationMetadataBuilder(SpringJavaType.REQUEST_MAPPING);

    // Adding path attribute
    annotationBuilder.addStringAttribute("value", controllerMetadata.getRequestMappingValue());

    // Add name attribute
    annotationBuilder.addStringAttribute("name", getDestination().getSimpleTypeName());

    // Add produces
    annotationBuilder.addEnumAttribute("produces",
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE);

    return annotationBuilder;
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

    if (this.type == ControllerType.DETAIL || this.type == ControllerType.DETAIL_ITEM) {

      for (FieldMetadata serviceField : controllerMetadata.getDetailsServiceFields().values()) {

        // Getting parentServiceFieldName
        String childServiceFieldName = serviceField.getFieldName().getSymbolName();

        // Adding parameters
        constructor.addParameter(childServiceFieldName, serviceField.getFieldType());

        // Generating body
        bodyBuilder.appendFormalLine(String.format("this.%s = %s;", childServiceFieldName,
            childServiceFieldName));
      }
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
    AnnotationMetadataBuilder postMappingAnnotation = new AnnotationMetadataBuilder(POST_MAPPING);
    postMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(postMappingAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (customer.getId() != null || customer.getVersion() != null
    bodyBuilder.newLine();
    bodyBuilder.appendIndent();
    bodyBuilder.append("if (%s || %s",
        createNullExpression(entityMetadata.getCurrentIndentifierField()),
        createNullExpression(entityMetadata.getCurrentVersionField()));
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
        getNameOfJavaType(RESPONSE_ENTITY), getNameOfJavaType(SpringJavaType.HTTP_STATUS));
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // if (result.hasErrors()) {
    // return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
    // }
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine("return %s.status(%s.CONFLICT).body(result);",
        getNameOfJavaType(RESPONSE_ENTITY), getNameOfJavaType(SpringJavaType.HTTP_STATUS));

    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // Entity newEntity = entityService.saveMethodName(entity);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("%s new%s = %s().%s(%s);", getNameOfJavaType(entity), StringUtils
        .capitalize(entityItemName), getAccessorMethod(controllerMetadata.getServiceField())
        .getMethodName(), serviceMetadata.getCurrentSaveMethod().getMethodName(), entityItemName);

    // UriComponents showURI = CustomersItemJsonController.showURI(newCustomer);
    bodyBuilder.appendFormalLine("%s showURI = %s.%s(new%s);",
        getNameOfJavaType(SpringJavaType.URI_COMPONENTS), getNameOfJavaType(itemController),
        SHOW_URI_METHOD_NAME, StringUtils.capitalize(entityItemName));

    // return ResponseEntity.created(showURI.toUri()).build();
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("return %s.created(showURI.toUri()).build();",
        getNameOfJavaType(RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
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
    parameterTypes.add(new AnnotatedJavaType(this.entity, ANN_MODEL_ATTRIBUTE));
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
    AnnotationMetadataBuilder putMappingAnnotation = new AnnotationMetadataBuilder(PUT_MAPPING);
    putMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
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
        getNameOfJavaType(RESPONSE_ENTITY), getNameOfJavaType(SpringJavaType.HTTP_STATUS));
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // customer.setId(storedCustomer.getId());
    bodyBuilder.appendFormalLine("%s.set%s(%s.get%s());", entityItemName,
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
    bodyBuilder.appendFormalLine("%s().save(%s);",
        getAccessorMethod(controllerMetadata.getServiceField()).getMethodName(), entityItemName);

    // return ResponseEntity.ok().build();
    bodyBuilder.appendFormalLine("return %s.ok().build();", getNameOfJavaType(RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
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
    parameterTypes.add(new AnnotatedJavaType(entity, ANN_MODEL_ATTRIBUTE));

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
    AnnotationMetadataBuilder deleteMappingAnnotation =
        new AnnotationMetadataBuilder(DELETE_MAPPING);
    deleteMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(deleteMappingAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // customerService.delete(customer);
    bodyBuilder.appendFormalLine("%s().%s(%s);",
        getAccessorMethod(controllerMetadata.getServiceField()).getMethodName(), serviceMetadata
            .getCurrentDeleteMethod().getMethodName(), entityItemName);

    // return ResponseEntity.ok().build();
    bodyBuilder.appendFormalLine("return %s.ok().build();", getNameOfJavaType(RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
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
    parameterNames.add(PAGEABLE_PARAM_NAME);

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @GetMapping annotation
    AnnotationMetadataBuilder getMappingAnnotation = new AnnotationMetadataBuilder(GET_MAPPING);
    getMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(getMappingAnnotation);


    final MethodMetadata findAllMethod = serviceMetadata.getCurrentFindAllWithGlobalSearchMethod();
    // Generating returnType
    JavaType serviceReturnType = findAllMethod.getReturnType();
    JavaType returnType = JavaType.wrapperOf(RESPONSE_ENTITY, serviceReturnType);

    // TODO
    // Add module dependency
    //getTypeLocationService().addModuleDependency(this.controller.getType().getModule(),
    //    returnParameterTypes.get(i));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    final String itemNames = StringUtils.uncapitalize(this.entityPlural);

    // Page<Customer> customers = customerService.findAll(globalSearch, pageable);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("%s %s = %s().%s(%s, %s);", getNameOfJavaType(serviceReturnType),
        itemNames, getAccessorMethod(controllerMetadata.getServiceField()).getMethodName(),
        findAllMethod.getMethodName(), GLOBAL_SEARCH_NAME, PAGEABLE_PARAM_NAME);

    // return ResponseEntity.ok(customers);
    bodyBuilder.appendFormalLine(String.format("return %s.ok(%s);",
        getNameOfJavaType(RESPONSE_ENTITY), itemNames));


    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, parameterTypes,
            parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
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
    parameterTypes.add(new AnnotatedJavaType(entity, ANN_MODEL_ATTRIBUTE));

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
    AnnotationMetadataBuilder getMappingAnnotation = new AnnotationMetadataBuilder(GET_MAPPING);
    getMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(getMappingAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return ResponseEntity.ok(customer);
    bodyBuilder.appendFormalLine("return %s.ok(%s);", getNameOfJavaType(RESPONSE_ENTITY),
        entityItemName);

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "show" method using JSON response type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getShowDetailMethod() {
    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("show");

    // Define parameters
    final List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    JavaType parentEntity = controllerMetadata.getLastDetailsInfo().entityType;
    JavaType entity = controllerMetadata.getLastDetailEntity();
    String entityItemName = StringUtils.uncapitalize(entity.getSimpleTypeName());

    parameterTypes.add(new AnnotatedJavaType(parentEntity, ANN_MODEL_ATTRIBUTE));
    parameterNames.add(new JavaSymbolName(
        StringUtils.uncapitalize(parentEntity.getSimpleTypeName())));


    parameterTypes.add(new AnnotatedJavaType(entity, ANN_MODEL_ATTRIBUTE));
    parameterNames.add(new JavaSymbolName(entityItemName));

    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.MODEL));
    parameterNames.add(new JavaSymbolName("model"));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @GetMapping annotation
    AnnotationMetadataBuilder getMappingAnnotation = new AnnotationMetadataBuilder(GET_MAPPING);
    getMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(getMappingAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return ResponseEntity.ok(customer);
    bodyBuilder.appendFormalLine("return %s.ok(%s);", getNameOfJavaType(RESPONSE_ENTITY),
        entityItemName);

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
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
    AnnotationMetadataBuilder postMappingAnnotation = new AnnotationMetadataBuilder(POST_MAPPING);
    postMappingAnnotation.addStringAttribute("value", "/batch");
    postMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
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
        getNameOfJavaType(RESPONSE_ENTITY), getNameOfJavaType(SpringJavaType.HTTP_STATUS));
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // customerService.save(customers);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("%s().%s(%s);",
        getAccessorMethod(controllerMetadata.getServiceField()).getMethodName(), serviceMetadata
            .getCurrentSaveBatchMethod().getMethodName(), StringUtils
            .uncapitalize(this.entityPlural));

    // return ResponseEntity.created(listURI().toUri()).build();
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("return %s.created(%s().toUri()).build();",
        getNameOfJavaType(RESPONSE_ENTITY), LIST_URI_METHOD_NAME);

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
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
    AnnotationMetadataBuilder putMappingAnnotation = new AnnotationMetadataBuilder(PUT_MAPPING);
    putMappingAnnotation.addStringAttribute("value", "/batch");
    putMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
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
        getNameOfJavaType(RESPONSE_ENTITY), getNameOfJavaType(SpringJavaType.HTTP_STATUS));
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // customerService.save(customers);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("%s().%s(%s);",
        getAccessorMethod(controllerMetadata.getServiceField()).getMethodName(), serviceMetadata
            .getCurrentSaveBatchMethod().getMethodName(), StringUtils
            .uncapitalize(this.entityPlural));

    // return ResponseEntity.ok().build();
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("return %s.ok().build();", getNameOfJavaType(RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
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
        new AnnotationMetadataBuilder(DELETE_MAPPING);
    deleteMappingAnnotation.addStringAttribute("value", "/batch/{" + entityIdentifierPlural + "}");
    deleteMappingAnnotation.addStringAttribute("name", methodName.getSymbolName());
    annotations.add(deleteMappingAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // serviceField.SERVICE_DELETE_METHOD(ids);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("%s().%s(%s);",
        getAccessorMethod(controllerMetadata.getServiceField()).getMethodName(), serviceMetadata
            .getCurrentDeleteBatchMethod().getMethodName(), entityIdentifierPlural);

    // return ResponseEntity.ok().build();
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("return %s.ok().build();", getNameOfJavaType(RESPONSE_ENTITY));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
            JavaType.wrapperWilcard(RESPONSE_ENTITY), parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the getModelAttributeMethod() method
   *
   * @return MethodMetadata
   */
  private MethodMetadata getModelAttributeMethod() {
    return getModelAttributeMethod(StringUtils.uncapitalize(entity.getSimpleTypeName()),
        serviceMetadata, controllerMetadata.getServiceField());
  }

  /**
   * This method provides the method to add to manage a model attribute from a
   * path variable
   *
   * @param pathVariable
   * @param serviceMetadata
   * @param serviceField
   * @return MethodMetadata
   */
  private MethodMetadata getModelAttributeMethod(String pathVariable,
      ServiceMetadata serviceMetadata, FieldMetadata serviceField) {
    // Define methodName
    final JavaSymbolName methodName =
        new JavaSymbolName("get" + StringUtils.capitalize(pathVariable));

    final JavaType idType =
        serviceMetadata.getCurrentFindOneMethod().getParameterTypes().get(0).getJavaType();
    final JavaSymbolName idName =
        serviceMetadata.getCurrentFindOneMethod().getParameterNames().get(0);
    final JavaType entityType = serviceMetadata.getCurrentFindOneMethod().getReturnType();

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    AnnotationMetadataBuilder pathVariableAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVariableAnnotation.addStringAttribute("value", StringUtils.uncapitalize(pathVariable));

    parameterTypes.add(new AnnotatedJavaType(idType, pathVariableAnnotation.build()));

    MethodMetadata existingMethod =
        getGovernorMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(idName);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Customer customer = customerService.findOne(id);
    bodyBuilder.appendFormalLine("%s %s = %s.%s(%s);", getNameOfJavaType(entityType), pathVariable,
        serviceField.getFieldName(), serviceMetadata.getCurrentFindOneMethod().getMethodName(),
        idName);

    // if (customer == null) {
    //   throw new NotFoundException("Customer not found");
    // }
    bodyBuilder.appendFormalLine("if (%s == null) {", pathVariable);
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(
        "throw new %s(String.format(\"%s with identifier '%%s' not found\",%s));",
        getNameOfJavaType(SpringletsJavaType.SPRINGLETS_NOT_FOUND_EXCEPTION),
        entityType.getSimpleTypeName(), idName);
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // return customer;
    bodyBuilder.appendFormalLine("return %s;", pathVariable);

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, entityType, parameterTypes,
            parameterNames, bodyBuilder);

    methodBuilder.addAnnotation(ANN_MODEL_ATTRIBUTE);

    return methodBuilder.build();
  }

  /**
   * Checks field type and creates a proper expression to check against null. 
   * Mainly differences will be between primitive and non primitive values.
   * 
   * @return a String with the expression to check.
   */
  private String createNullExpression(FieldMetadata field) {
    String expression = "";
    JavaType versionType = field.getFieldType();

    if (versionType.isPrimitive()) {
      expression =
          String.format("%s.get%s() != 0", entityItemName, field.getFieldName()
              .getSymbolNameCapitalisedFirstLetter());
    } else {
      expression =
          String.format("%s.get%s() != null", entityItemName, field.getFieldName()
              .getSymbolNameCapitalisedFirstLetter());
    }

    return expression;
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
   * @return {@link Map<String,MethodMetadata>}
   */
  public Map<String, MethodMetadata> getCurrentFinderMethods() {
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
