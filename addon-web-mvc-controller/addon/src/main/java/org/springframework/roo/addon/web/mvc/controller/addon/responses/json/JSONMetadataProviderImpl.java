package org.springframework.roo.addon.web.mvc.controller.addon.responses.json;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.finder.addon.parser.FinderMethod;
import org.springframework.roo.addon.finder.addon.parser.FinderParameter;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.addon.plural.addon.PluralMetadata;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMVCService;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.Jsr303JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringEnumDetails;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link JSONMetadataProvider}.
 *
 * @author Juan Carlos García
 * @author Paula Navarro
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class JSONMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements JSONMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils.getLogger(JSONMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToServiceMidMap =
      new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  private boolean readOnly;
  private JavaType entity;
  private JavaType identifierType;
  private JavaType service;
  private String path;
  private String metadataIdentificationString;
  private ClassOrInterfaceTypeDetails controller;
  private final JavaType URI = new JavaType("java.net.URI");
  private ControllerMVCService controllerMVCService;
  private List<JavaType> typesToImport = new ArrayList<JavaType>();
  private JavaType globalSearch;
  private String entityPlural;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_JSON} as additional JavaType that
   * will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    super.setDependsOnGovernorBeingAClass(false);
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(RooJavaType.ROO_JSON);
  }

  /**
   * This service is being deactivated so unregister upstream-downstream
   * dependencies, triggers, matchers and listeners.
   *
   * @param context
   */
  protected void deactivate(final ComponentContext context) {
    MetadataDependencyRegistry registry = this.registryTracker.getService();
    registry.removeNotificationListener(this);
    registry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(),
        getProvidesType());
    this.registryTracker.close();

    removeMetadataTrigger(RooJavaType.ROO_JSON);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return JSONMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = JSONMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = JSONMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "JSON";
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    // Determine the governor for this ITD, and whether any metadata is even
    // hoping to hear about changes to that JavaType and its ITDs
    final JavaType governor = itdTypeDetails.getName();
    final String localMid = domainTypeToServiceMidMap.get(governor);
    if (localMid != null) {
      return localMid;
    }

    final MemberHoldingTypeDetails memberHoldingTypeDetails =
        getTypeLocationService().getTypeDetails(governor);
    if (memberHoldingTypeDetails != null) {
      for (final JavaType type : memberHoldingTypeDetails.getLayerEntities()) {
        final String localMidType = domainTypeToServiceMidMap.get(type);
        if (localMidType != null) {
          return localMidType;
        }
      }
    }
    return null;
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {

    this.controller = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
    this.metadataIdentificationString = metadataIdentificationString;

    AnnotationMetadata controllerAnnotation = controller.getAnnotation(RooJavaType.ROO_CONTROLLER);

    // Getting entity and check if is a readOnly entity or not
    AnnotationAttributeValue<JavaType> entityAttr = controllerAnnotation.getAttribute("entity");
    if (entityAttr != null) {
      this.entity = entityAttr.getValue();
    }

    Validate.notNull(this.entity, String.format(
        "ERROR: You should provide a valid entity for controller '%s'", this.controller.getType()
            .getFullyQualifiedTypeName()));

    AnnotationMetadata entityAnnotation =
        getTypeLocationService().getTypeDetails(this.entity).getAnnotation(
            RooJavaType.ROO_JPA_ENTITY);

    Validate.notNull(entityAnnotation, "ERROR: Entity should be annotated with @RooJpaEntity");

    this.readOnly = false;
    if (entityAnnotation.getAttribute("readOnly") != null) {
      this.readOnly = (Boolean) entityAnnotation.getAttribute("readOnly").getValue();
    }

    // Getting identifierType
    this.identifierType = getPersistenceMemberLocator().getIdentifierType(entity);

    // Get entity plural
    final ClassOrInterfaceTypeDetails details =
        getTypeLocationService().getTypeDetails(this.entity);
    final LogicalPath entityLogicalPath =
        PhysicalTypeIdentifier.getPath(details.getDeclaredByMetadataId());
    final String pluralIdentifier = PluralMetadata.createIdentifier(this.entity, entityLogicalPath);
    final PluralMetadata pluralMetadata =
        (PluralMetadata) getMetadataService().get(pluralIdentifier);
    this.entityPlural = pluralMetadata.getPlural();

    // Getting service and its metadata
    this.service = (JavaType) controllerAnnotation.getAttribute("service").getValue();
    ClassOrInterfaceTypeDetails serviceDetails =
        getTypeLocationService().getTypeDetails(this.service);

    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(serviceDetails.getDeclaredByMetadataId());
    final String serviceMetadataKey =
        ServiceMetadata.createIdentifier(serviceDetails.getType(), logicalPath);
    final ServiceMetadata serviceMetadata =
        (ServiceMetadata) getMetadataService().get(serviceMetadataKey);

    // Getting path
    this.path = (String) controllerAnnotation.getAttribute("path").getValue();

    // Get GlobalSearch type
    Set<ClassOrInterfaceTypeDetails> globalSearchList =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_GLOBAL_SEARCH);
    if (!globalSearchList.isEmpty()) {
      for (ClassOrInterfaceTypeDetails type : globalSearchList) {
        this.globalSearch = type.getType();
      }
    }

    // Getting methods from related service
    MethodMetadata serviceSaveMethod = serviceMetadata.getSaveMethod();
    MethodMetadata serviceDeleteMethod = serviceMetadata.getDeleteMethod();
    MethodMetadata serviceFindOneMethod = serviceMetadata.getFindOneMethod();
    MethodMetadata serviceFindAllGlobalSearchMethod =
        serviceMetadata.getFindAllGlobalSearchMethod();

    List<MethodMetadata> findersToAdd = new ArrayList<MethodMetadata>();

    // Getting annotated finders
    final JSONAnnotationValues annotationValues =
        new JSONAnnotationValues(governorPhysicalTypeMetadata);

    if (annotationValues.getFinders() != null) {
      List<String> finders = new ArrayList<String>(Arrays.asList(annotationValues.getFinders()));

      // Search indicated finders in its related service
      for (FinderMethod serviceFinder : serviceMetadata.getFinders()) {
        if (finders.contains(serviceFinder.getMethodName().toString())) {
          MethodMetadata finderMethod = getFinderMethod(serviceFinder);
          findersToAdd.add(finderMethod);

          // Add dependencies between modules
          List<JavaType> types = new ArrayList<JavaType>();
          types.add(serviceFinder.getReturnType());
          types.addAll(serviceFinder.getReturnType().getParameters());

          for (FinderParameter parameter : serviceFinder.getParameters()) {
            types.add(parameter.getType());
            types.addAll(parameter.getType().getParameters());
          }

          for (JavaType parameter : types) {
            getTypeLocationService().addModuleDependency(
                governorPhysicalTypeMetadata.getType().getModule(), parameter);
          }

          finders.remove(serviceFinder.getMethodName().toString());
        }
      }

      // Check all finders have its service method
      if (!finders.isEmpty()) {
        throw new IllegalArgumentException(String.format(
            "ERROR: Service %s does not have these finder methods: %s ",
            service.getFullyQualifiedTypeName(), StringUtils.join(finders, ", ")));
      }
    }

    return new JSONMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata,
        getListMethod(serviceFindAllGlobalSearchMethod), getCreateMethod(serviceSaveMethod),
        getUpdateMethod(serviceSaveMethod), getDeleteMethod(serviceDeleteMethod),
        getShowMethod(serviceFindOneMethod), getCreateBatchMethod(serviceSaveMethod),
        getUpdateBatchMethod(serviceSaveMethod), getDeleteBatchMethod(serviceDeleteMethod),
        getPopulateHeadersMethod(), findersToAdd, this.readOnly, typesToImport);

  }

  /**
   * This method provides the "create" method using JSON response type
   *
   * @param serviceSaveMethod
   *
   * @return MethodMetadata
   */
  private MethodMetadata getCreateMethod(MethodMetadata serviceSaveMethod) {

    // If provided entity is readOnly, create method is not
    // available
    if (this.readOnly) {
      return null;
    }

    // First of all, check if exists other method with the same
    // @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_POST, "", null,
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(),
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("create");

    // Adding parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.entity, new AnnotationMetadataBuilder(
        Jsr303JavaType.VALID).build(), new AnnotationMetadataBuilder(SpringJavaType.REQUEST_BODY)
        .build()));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.BINDING_RESULT));

    // Adding parameter names
    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(getEntityField().getFieldName());
    parameterNames.add(new JavaSymbolName("result"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_POST, "", null,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE, ""));

    // Adding @ResponseBody annotation
    AnnotationMetadataBuilder responseBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(responseBodyAnnotation);

    // Adding @SuppressWarnings annotation
    AnnotationMetadataBuilder suppressWarningsAnnotation =
        new AnnotationMetadataBuilder(JdkJavaType.SUPPRESS_WARNINGS);
    List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
    attributes.add(new StringAttributeValue(new JavaSymbolName("value"), "rawtypes"));
    attributes.add(new StringAttributeValue(new JavaSymbolName("value"), "unchecked"));
    ArrayAttributeValue<AnnotationAttributeValue<?>> supressWarningsAtributes =
        new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("value"),
            attributes);
    suppressWarningsAnnotation.addAttribute(supressWarningsAtributes);
    annotations.add(suppressWarningsAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (customerOrder.getId() != null) {
    // return new ResponseEntity(HttpStatus.CONFLICT);
    // }
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("if (%s.%s() != null) {", getEntityField()
        .getFieldName(), getPersistenceMemberLocator().getIdentifierAccessor(this.entity)
        .getMethodName()));
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(String.format("return new %s(%s.%s);",
        addTypeToImport(SpringJavaType.RESPONSE_ENTITY).getSimpleTypeName(),
        addTypeToImport(SpringEnumDetails.HTTP_STATUS_CONFLICT.getType()).getSimpleTypeName(),
        SpringEnumDetails.HTTP_STATUS_CONFLICT.getField().getSymbolName()));
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // if (result.hasErrors()) {
    // return new ResponseEntity(result, HttpStatus.CONFLICT);
    // }
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(String.format("return new %s(result, %s.%s);",
        addTypeToImport(SpringJavaType.RESPONSE_ENTITY).getSimpleTypeName(),
        addTypeToImport(SpringEnumDetails.HTTP_STATUS_CONFLICT.getType()).getSimpleTypeName(),
        SpringEnumDetails.HTTP_STATUS_CONFLICT.getField().getSymbolName()));
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // Entity newEntity = entityService.saveMethodName(entity);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("%s new%s = %s.%s(%s);",
        addTypeToImport(this.entity).getSimpleTypeName(),
        StringUtils.capitalize(this.entity.getSimpleTypeName()), getServiceField().getFieldName(),
        serviceSaveMethod.getMethodName(), getEntityField().getFieldName()));

    // HttpHeaders responseHeaders = populateHeaders(newEntity.getId());
    bodyBuilder.appendFormalLine(String.format("%s responseHeaders = populateHeaders(new%s.%s());",
        addTypeToImport(SpringJavaType.HTTP_HEADERS).getSimpleTypeName(), StringUtils
            .capitalize(this.entity.getSimpleTypeName()), getPersistenceMemberLocator()
            .getIdentifierAccessor(this.entity).getMethodName()));

    // return new ResponseEntity(newEntity, responseHeaders,
    // HttpStatus.CREATED);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("return new %s(new%s, responseHeaders, %s.%s);",
        addTypeToImport(SpringJavaType.RESPONSE_ENTITY).getSimpleTypeName(),
        StringUtils.capitalize(this.entity.getSimpleTypeName()),
        addTypeToImport(SpringEnumDetails.HTTP_STATUS_CREATED.getType()).getSimpleTypeName(),
        SpringEnumDetails.HTTP_STATUS_CREATED.getField().getSymbolName()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            SpringJavaType.RESPONSE_ENTITY, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "update" method using JSON response type
   *
   * @param serviceSaveMethod
   *
   * @return MethodMetadata
   */
  private MethodMetadata getUpdateMethod(MethodMetadata serviceSaveMethod) {

    // If provided entity is readOnly, create method is not
    // available
    if (this.readOnly) {
      return null;
    }

    // Build @RequestMapping value attribute
    String value =
        String.format("/{%s}", StringUtils.uncapitalize(this.entity.getSimpleTypeName()));

    // First of all, check if exists other method with the same
    // @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_PUT, value, null,
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(),
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("update");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.entity, new AnnotationMetadataBuilder(
        Jsr303JavaType.VALID).build(), new AnnotationMetadataBuilder(SpringJavaType.REQUEST_BODY)
        .build()));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.BINDING_RESULT));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(getEntityField().getFieldName());
    parameterNames.add(new JavaSymbolName("result"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_PUT, value, null,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE, ""));

    // Adding @ResponseBody annotation
    AnnotationMetadataBuilder responseBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(responseBodyAnnotation);

    // Adding @SuppressWarnings annotation
    AnnotationMetadataBuilder suppressWarningsAnnotation =
        new AnnotationMetadataBuilder(JdkJavaType.SUPPRESS_WARNINGS);
    List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
    attributes.add(new StringAttributeValue(new JavaSymbolName("value"), "rawtypes"));
    attributes.add(new StringAttributeValue(new JavaSymbolName("value"), "unchecked"));
    ArrayAttributeValue<AnnotationAttributeValue<?>> supressWarningsAtributes =
        new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("value"),
            attributes);
    suppressWarningsAnnotation.addAttribute(supressWarningsAtributes);
    annotations.add(suppressWarningsAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (result.hasErrors()) {
    // return new ResponseEntity(result, HttpStatus.CONFLICT);
    // }
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(String.format("return new %s(result, %s.%s);",
        addTypeToImport(SpringJavaType.RESPONSE_ENTITY).getSimpleTypeName(),
        addTypeToImport(SpringEnumDetails.HTTP_STATUS_CONFLICT.getType()).getSimpleTypeName(),
        SpringEnumDetails.HTTP_STATUS_CONFLICT.getField().getSymbolName()));
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // Entity savedEntity = entityService.saveMethodName(entity);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("%s saved%s = %s.%s(%s);",
        addTypeToImport(this.entity).getSimpleTypeName(),
        StringUtils.capitalize(this.entity.getSimpleTypeName()), getServiceField().getFieldName(),
        serviceSaveMethod.getMethodName(), getEntityField().getFieldName()));

    // return new ResponseEntity(savedEntity, HttpStatus.OK);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("return new %s(saved%s, %s.%s);",
        addTypeToImport(SpringJavaType.RESPONSE_ENTITY).getSimpleTypeName(),
        StringUtils.capitalize(this.entity.getSimpleTypeName()),
        addTypeToImport(SpringEnumDetails.HTTP_STATUS_OK.getType()).getSimpleTypeName(),
        SpringEnumDetails.HTTP_STATUS_OK.getField().getSymbolName()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            SpringJavaType.RESPONSE_ENTITY, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "delete" method using JSON response type
   *
   * @param serviceDeleteMethod
   *
   * @return MethodMetadata
   */
  private MethodMetadata getDeleteMethod(MethodMetadata serviceDeleteMethod) {

    // If provided entity is readOnly, create method is not
    // available
    if (this.readOnly) {
      return null;
    }

    // First of all, check if exists other method with the same
    // @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_DELETE, "/{id}", null, null,
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("delete");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    AnnotationMetadataBuilder pathVariable =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVariable.addStringAttribute("value", "id");
    parameterTypes.add(new AnnotatedJavaType(this.identifierType, pathVariable.build()));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("id"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_DELETE, "/{id}", null, null,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE, ""));

    // Adding @ResponseBody annotation
    AnnotationMetadataBuilder responseBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(responseBodyAnnotation);

    // Adding @SuppressWarnings annotation
    AnnotationMetadataBuilder suppressWarningsAnnotation =
        new AnnotationMetadataBuilder(JdkJavaType.SUPPRESS_WARNINGS);
    List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
    attributes.add(new StringAttributeValue(new JavaSymbolName("value"), "rawtypes"));
    ArrayAttributeValue<AnnotationAttributeValue<?>> supressWarningsAtributes =
        new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("value"),
            attributes);
    suppressWarningsAnnotation.addAttribute(supressWarningsAtributes);
    annotations.add(suppressWarningsAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // entityService.DELETE_METHOD(id);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("%s.%s(id);", getServiceField().getFieldName(),
        serviceDeleteMethod.getMethodName()));

    // return new ResponseEntity(HttpStatus.OK);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("return new %s(%s.%s);",
        addTypeToImport(SpringJavaType.RESPONSE_ENTITY).getSimpleTypeName(),
        addTypeToImport(SpringEnumDetails.HTTP_STATUS_OK.getType()).getSimpleTypeName(),
        SpringEnumDetails.HTTP_STATUS_OK.getField().getSymbolName()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            SpringJavaType.RESPONSE_ENTITY, parameterTypes, parameterNames, bodyBuilder);
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
  private MethodMetadata getListMethod(MethodMetadata serviceFindAllGlobalSearchMethod) {

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
    final JavaSymbolName methodName = new JavaSymbolName("list");

    // Create PageableDefault annotation
    AnnotationMetadataBuilder pageableDefaultAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PAGEABLE_DEFAULT);

    String sortFieldName = "";
    MemberDetails entityDetails =
        getMemberDetails(getTypeLocationService().getTypeDetails(this.entity));
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
    Validate.notNull(globalSearch, "Couldn't find GlobalSearch in project.");
    parameterTypes.add(new AnnotatedJavaType(this.globalSearch));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.PAGEABLE, pageableDefaultAnnotation
        .build()));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("search"));
    parameterNames.add(new JavaSymbolName("pageable"));

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

    // Generating returnType
    JavaType returnType = serviceFindAllGlobalSearchMethod.getReturnType();
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

    // ReturnType<ReturnTypeParams> objects = entityService.findAll(search,
    // pageable);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("%s<%s> %s = %s.%s(search, pageable);",
        addTypeToImport(returnType).getSimpleTypeName(), returnTypeParamsString,
        StringUtils.uncapitalize(this.entityPlural), getServiceField().getFieldName(),
        serviceFindAllGlobalSearchMethod.getMethodName()));

    // return objects;
    bodyBuilder.appendFormalLine(String.format("return %s;",
        StringUtils.uncapitalize(this.entityPlural)));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            returnType, parameterTypes, parameterNames, bodyBuilder);
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
  private MethodMetadata getFinderMethod(FinderMethod finderMethod) {

    List<String> parameters = new ArrayList<String>();
    for (FinderParameter parameter : finderMethod.getParameters()) {
      parameters.add(parameter.getName().toString());
    }

    // Define methodName
    final JavaSymbolName methodName = finderMethod.getMethodName();

    // Check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "/" + methodName.toString(), parameters, null,
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Get parameters
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    List<FinderParameter> finderParams = finderMethod.getParameters();
    StringBuffer finderParamsString = new StringBuffer();
    for (int i = 0; i < finderParams.size(); i++) {
      AnnotationMetadataBuilder requestParamAnnotation =
          new AnnotationMetadataBuilder(SpringJavaType.REQUEST_PARAM);
      requestParamAnnotation.addStringAttribute("value", finderParams.get(i).getName()
          .getSymbolName());
      parameterTypes.add(new AnnotatedJavaType(finderParams.get(i).getType(),
          requestParamAnnotation.build()));
      parameterNames.add(finderParams.get(i).getName());

      // Build finder parameters String
      if (i > 0) {
        finderParamsString.append(",");
      }
      finderParamsString.append(finderParams.get(i).getName());
    }

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, "/" + methodName.toString(), parameters, null,
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
      bodyBuilder.appendFormalLine(String.format("%s %s = %s.%s(%s);", addTypeToImport(returnType)
          .getSimpleTypeName(), StringUtils.uncapitalize(this.entityPlural), getServiceField()
          .getFieldName(), methodName, finderParamsString));
    } else {
      bodyBuilder.appendFormalLine(String.format("%s<%s> %s = %s.%s(%s);",
          addTypeToImport(returnType).getSimpleTypeName(), returnTypeParamsString,
          StringUtils.uncapitalize(this.entityPlural), getServiceField().getFieldName(),
          methodName, finderParamsString));
    }

    // return entityService.FINDER_METHOD(%s);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("return %s;",
        StringUtils.uncapitalize(this.entityPlural)));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            returnType, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "show" method using JSON response type
   *
   * @param serviceFindOneMethod
   *
   * @return MethodMetadata
   */
  private MethodMetadata getShowMethod(MethodMetadata serviceFindOneMethod) {

    // First of all, check if exists other method with the same
    // @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "/{id}", null, null,
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("show");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    AnnotationMetadataBuilder pathVariable =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVariable.addStringAttribute("value", "id");
    parameterTypes.add(new AnnotatedJavaType(this.identifierType, pathVariable.build()));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("id"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, "/{id}", null, null,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE, ""));

    // Adding @ResponseBody annotation
    AnnotationMetadataBuilder responseBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(responseBodyAnnotation);

    // Adding @SuppressWarnings annotation
    AnnotationMetadataBuilder suppressWarningsAnnotation =
        new AnnotationMetadataBuilder(JdkJavaType.SUPPRESS_WARNINGS);
    List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
    attributes.add(new StringAttributeValue(new JavaSymbolName("value"), "rawtypes"));
    attributes.add(new StringAttributeValue(new JavaSymbolName("value"), "unchecked"));
    ArrayAttributeValue<AnnotationAttributeValue<?>> supressWarningsAtributes =
        new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("value"),
            attributes);
    suppressWarningsAnnotation.addAttribute(supressWarningsAtributes);
    annotations.add(suppressWarningsAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Entity entity = entityServiceField.FIND_ONE_METHOD(id);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("%s %s = %s.%s(id);", addTypeToImport(this.entity)
        .getSimpleTypeName(), StringUtils.uncapitalize(this.entity.getSimpleTypeName()),
        getServiceField().getFieldName(), serviceFindOneMethod.getMethodName()));

    // if (entity == null) {
    // return new ResponseEntity(HttpStatus.NOT_FOUND);
    // }
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("if (%s == null) {",
        StringUtils.uncapitalize(this.entity.getSimpleTypeName())));
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(String.format("return new %s(%s.%s);",
        addTypeToImport(SpringJavaType.RESPONSE_ENTITY).getSimpleTypeName(),
        addTypeToImport(SpringEnumDetails.HTTP_STATUS_NOT_FOUND.getType()).getSimpleTypeName(),
        SpringEnumDetails.HTTP_STATUS_NOT_FOUND.getField().getSymbolName()));
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // return new ResponseEntity(entity, HttpStatus.FOUND);
    bodyBuilder.appendFormalLine(String.format("return new %s(%s, %s.%s);",
        addTypeToImport(SpringJavaType.RESPONSE_ENTITY).getSimpleTypeName(),
        StringUtils.uncapitalize(this.entity.getSimpleTypeName()),
        addTypeToImport(SpringEnumDetails.HTTP_STATUS_FOUND.getType()).getSimpleTypeName(),
        SpringEnumDetails.HTTP_STATUS_FOUND.getField().getSymbolName()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            SpringJavaType.RESPONSE_ENTITY, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * Creates create batch method
   *
   * @param serviceSaveMethod
   *            the MethodMetadata of entity's service save method
   * @return {@link MethodMetadata}
   */
  private MethodMetadata getCreateBatchMethod(MethodMetadata serviceSaveMethod) {

    // If provided entity is readOnly, create method is not available
    if (this.readOnly) {
      return null;
    }

    // First of all, check if exists other method with the same
    // @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_POST, "/batch", null,
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(),
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("createBatch");

    // Adding parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(new JavaType(JdkJavaType.COLLECTION
        .getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(this.entity)),
        new AnnotationMetadataBuilder(Jsr303JavaType.VALID).build(), new AnnotationMetadataBuilder(
            SpringJavaType.REQUEST_BODY).build()));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.BINDING_RESULT));

    // Adding parameter names
    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(StringUtils.uncapitalize(this.entityPlural)));
    parameterNames.add(new JavaSymbolName("result"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_POST, "/batch", null,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE, ""));

    // Adding @ResponseBody annotation
    AnnotationMetadataBuilder responseBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(responseBodyAnnotation);

    // Adding @SuppressWarnings annotation
    AnnotationMetadataBuilder suppressWarningsAnnotation =
        new AnnotationMetadataBuilder(JdkJavaType.SUPPRESS_WARNINGS);
    List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
    attributes.add(new StringAttributeValue(new JavaSymbolName("value"), "rawtypes"));
    attributes.add(new StringAttributeValue(new JavaSymbolName("value"), "unchecked"));
    ArrayAttributeValue<AnnotationAttributeValue<?>> supressWarningsAtributes =
        new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("value"),
            attributes);
    suppressWarningsAnnotation.addAttribute(supressWarningsAtributes);
    annotations.add(suppressWarningsAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (result.hasErrors()) {
    // return new ResponseEntity(result, HttpStatus.CONFLICT);
    // }
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(String.format("return new %s(result, %s.%s);",
        addTypeToImport(SpringJavaType.RESPONSE_ENTITY).getSimpleTypeName(),
        addTypeToImport(SpringEnumDetails.HTTP_STATUS_CONFLICT.getType()).getSimpleTypeName(),
        SpringEnumDetails.HTTP_STATUS_CONFLICT.getField().getSymbolName()));
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // List<Entity> newEntities = entityService.saveMethodName(entities);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("%s<%s> new%s = %s.%s(%s);",
        addTypeToImport(JdkJavaType.LIST).getSimpleTypeName(), addTypeToImport(this.entity)
            .getSimpleTypeName(), StringUtils.capitalize(this.entityPlural), getServiceField()
            .getFieldName(), serviceSaveMethod.getMethodName(), StringUtils
            .uncapitalize(this.entityPlural)));

    // return new ResponseEntity(newEntities, HttpStatus.CREATED);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("return new %s(new%s, %s.%s);",
        addTypeToImport(SpringJavaType.RESPONSE_ENTITY).getSimpleTypeName(),
        StringUtils.capitalize(this.entityPlural),
        addTypeToImport(SpringEnumDetails.HTTP_STATUS_CREATED.getType()).getSimpleTypeName(),
        SpringEnumDetails.HTTP_STATUS_CREATED.getField().getSymbolName()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            SpringJavaType.RESPONSE_ENTITY, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * Creates update batch method
   *
   * @param serviceSaveMethod
   *            the MethodMetadata of entity's service save method
   * @return {@link MethodMetadata}
   */
  private MethodMetadata getUpdateBatchMethod(MethodMetadata serviceSaveMethod) {

    // If provided entity is readOnly, create method is not available
    if (this.readOnly) {
      return null;
    }

    // First of all, check if exists other method with the same
    // @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_PUT, "/batch", null,
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(),
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("updateBatch");

    // Adding parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(new JavaType(JdkJavaType.COLLECTION
        .getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(this.entity)),
        new AnnotationMetadataBuilder(Jsr303JavaType.VALID).build(), new AnnotationMetadataBuilder(
            SpringJavaType.REQUEST_BODY).build()));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.BINDING_RESULT));

    // Adding parameter names
    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(StringUtils.uncapitalize(this.entityPlural)));
    parameterNames.add(new JavaSymbolName("result"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_PUT, "/batch", null,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE, ""));

    // Adding @ResponseBody annotation
    AnnotationMetadataBuilder responseBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(responseBodyAnnotation);

    // Adding @SuppressWarnings annotation
    AnnotationMetadataBuilder suppressWarningsAnnotation =
        new AnnotationMetadataBuilder(JdkJavaType.SUPPRESS_WARNINGS);
    List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
    attributes.add(new StringAttributeValue(new JavaSymbolName("value"), "rawtypes"));
    attributes.add(new StringAttributeValue(new JavaSymbolName("value"), "unchecked"));
    ArrayAttributeValue<AnnotationAttributeValue<?>> supressWarningsAtributes =
        new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("value"),
            attributes);
    suppressWarningsAnnotation.addAttribute(supressWarningsAtributes);
    annotations.add(suppressWarningsAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (result.hasErrors()) {
    // return new ResponseEntity(result, HttpStatus.CONFLICT);
    // }
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
    bodyBuilder.indent();
    bodyBuilder.appendFormalLine(String.format("return new %s(result, %s.%s);",
        addTypeToImport(SpringJavaType.RESPONSE_ENTITY).getSimpleTypeName(),
        addTypeToImport(SpringEnumDetails.HTTP_STATUS_CONFLICT.getType()).getSimpleTypeName(),
        SpringEnumDetails.HTTP_STATUS_CONFLICT.getField().getSymbolName()));
    bodyBuilder.indentRemove();
    bodyBuilder.appendFormalLine("}");

    // List<Entity> newEntities = entityService.saveMethodName(entities);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("%s<%s> saved%s = %s.%s(%s);",
        addTypeToImport(JdkJavaType.LIST).getSimpleTypeName(), addTypeToImport(this.entity)
            .getSimpleTypeName(), StringUtils.capitalize(this.entityPlural), getServiceField()
            .getFieldName(), serviceSaveMethod.getMethodName(), StringUtils
            .uncapitalize(this.entityPlural)));

    // return new ResponseEntity(newEntities, HttpStatus.OK);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("return new %s(saved%s, %s.%s);",
        addTypeToImport(SpringJavaType.RESPONSE_ENTITY).getSimpleTypeName(),
        StringUtils.capitalize(this.entityPlural),
        addTypeToImport(SpringEnumDetails.HTTP_STATUS_OK.getType()).getSimpleTypeName(),
        SpringEnumDetails.HTTP_STATUS_OK.getField().getSymbolName()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            SpringJavaType.RESPONSE_ENTITY, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * Creates delete batch method
   *
   * @param serviceSaveMethod
   *            the MethodMetadata of entity's service save method
   * @return {@link MethodMetadata}
   */
  private MethodMetadata getDeleteBatchMethod(MethodMetadata serviceDeleteMethod) {

    // If provided entity is readOnly, create method is not available
    if (this.readOnly) {
      return null;
    }

    // First of all, check if exists other method with the same
    // @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_DELETE, "/batch/{ids}", null, null,
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("deleteBatch");

    // Adding parameter types
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    AnnotationMetadataBuilder pathVariable =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVariable.addStringAttribute("value", "ids");
    parameterTypes.add(new AnnotatedJavaType(new JavaType(JdkJavaType.COLLECTION
        .getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(this.identifierType)),
        pathVariable.build()));

    // Adding parameter names
    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("ids"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_DELETE, "/batch/{ids}", null, null,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE, ""));

    // Adding @ResponseBody annotation
    AnnotationMetadataBuilder responseBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(responseBodyAnnotation);

    // Adding @SuppressWarnings annotation
    AnnotationMetadataBuilder suppressWarningsAnnotation =
        new AnnotationMetadataBuilder(JdkJavaType.SUPPRESS_WARNINGS);
    List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
    attributes.add(new StringAttributeValue(new JavaSymbolName("value"), "rawtypes"));
    ArrayAttributeValue<AnnotationAttributeValue<?>> supressWarningsAtributes =
        new ArrayAttributeValue<AnnotationAttributeValue<?>>(new JavaSymbolName("value"),
            attributes);
    suppressWarningsAnnotation.addAttribute(supressWarningsAtributes);
    annotations.add(suppressWarningsAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // serviceField.SERVICE_DELETE_METHOD(ids);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("%s.%s(ids);", getServiceField().getFieldName(),
        serviceDeleteMethod.getMethodName()));

    // return new ResponseEntity(HttpStatus.OK);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("return new %s(%s.%s);",
        addTypeToImport(SpringJavaType.RESPONSE_ENTITY).getSimpleTypeName(),
        addTypeToImport(SpringEnumDetails.HTTP_STATUS_OK.getType()).getSimpleTypeName(),
        SpringEnumDetails.HTTP_STATUS_OK.getField().getSymbolName()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            SpringJavaType.RESPONSE_ENTITY, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the getPopulateHeaders() method, used by create()
   * method
   *
   * @return MethodMetadata
   */
  private MethodMetadata getPopulateHeadersMethod() {

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("populateHeaders");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.identifierType));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("id"));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    final String entityPluralLowercase = this.entityPlural.toLowerCase();

    // UriComponents uriComponents =
    // UriComponentsBuilder.fromUriString("/customerorders/{id}").build();
    bodyBuilder.appendFormalLine(String.format(
        "%s uriComponents = %s.fromUriString(\"/%s/{id}\").build();",
        addTypeToImport(SpringJavaType.URI_COMPONENTS).getSimpleTypeName(),
        addTypeToImport(SpringJavaType.URI_COMPONENTS_BUILDER).getSimpleTypeName(),
        entityPluralLowercase));

    // URI uri = uriComponents.expand(id).encode().toUri();
    bodyBuilder.appendFormalLine(String.format(
        "%s uri = uriComponents.expand(id).encode().toUri();", addTypeToImport(URI)
            .getSimpleTypeName()));

    // HttpHeaders responseHeaders = new HttpHeaders();
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("%s responseHeaders = new %s();",
        addTypeToImport(SpringJavaType.HTTP_HEADERS).getSimpleTypeName(),
        addTypeToImport(SpringJavaType.HTTP_HEADERS).getSimpleTypeName()));

    // responseHeaders.setLocation(uri);
    bodyBuilder.appendFormalLine("responseHeaders.setLocation(uri);");

    // return responseHeaders;
    bodyBuilder.appendFormalLine("return responseHeaders;");

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            SpringJavaType.HTTP_HEADERS, parameterTypes, parameterNames, bodyBuilder);

    return methodBuilder.build();
  }

  /**
   * This method returns entity field included on controller
   *
   * @return
   */
  private FieldMetadata getEntityField() {

    // Generating service field name
    String fieldName =
        new JavaSymbolName(this.entity.getSimpleTypeName()).getSymbolNameUnCapitalisedFirstLetter();

    return new FieldMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName(fieldName), this.service)
        .build();
  }

  /**
   * This method registers a new type on types to import list and then returns
   * it.
   *
   * @param type
   * @return
   */
  private JavaType addTypeToImport(JavaType type) {
    if (!typesToImport.contains(type)) {
      typesToImport.add(type);
    }

    return type;
  }

  /**
   * This method returns service field included on controller
   *
   * @return
   */
  private FieldMetadata getServiceField() {
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(this.controller.getDeclaredByMetadataId());
    final String controllerMetadataKey =
        ControllerMetadata.createIdentifier(this.controller.getType(), logicalPath);
    registerDependency(controllerMetadataKey, metadataIdentificationString);
    final ControllerMetadata controllerMetadata =
        (ControllerMetadata) getMetadataService().get(controllerMetadataKey);

    return controllerMetadata.getServiceField();
  }

  private void registerDependency(final String upstreamDependency, final String downStreamDependency) {

    if (getMetadataDependencyRegistry() != null
        && StringUtils.isNotBlank(upstreamDependency)
        && StringUtils.isNotBlank(downStreamDependency)
        && !upstreamDependency.equals(downStreamDependency)
        && !MetadataIdentificationUtils.getMetadataClass(downStreamDependency).equals(
            MetadataIdentificationUtils.getMetadataClass(upstreamDependency))) {
      getMetadataDependencyRegistry().registerDependency(upstreamDependency, downStreamDependency);
    }
  }

  public String getProvidesType() {
    return JSONMetadata.getMetadataIdentiferType();
  }

  /**
   * Returns last JavaType found in project with provided annotation.
   *
   * @param annotationType
   *            JAvaType with the annotation to search
   * @return last JavaType found with the provided annotation or
   *         {@link IllegalArgumentException} if a type with this annotation
   *         doesn't exist.
   */
  private JavaType getTypeWithAnnotation(JavaType annotationType) {
    Set<JavaType> types = getTypeLocationService().findTypesWithAnnotation(annotationType);

    JavaType typeWithAnnotation = null;
    for (JavaType type : types) {
      typeWithAnnotation = type;
    }

    Validate.notNull(typeWithAnnotation,
        "Couldn't find any type with needed %s annotation in JSONMetadataProviderImpl",
        annotationType.getFullyQualifiedTypeName());

    return typeWithAnnotation;
  }

  public ControllerMVCService getControllerMVCService() {
    if (controllerMVCService == null) {
      // Get all Services implement ControllerMVCService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ControllerMVCService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          controllerMVCService = (ControllerMVCService) this.context.getService(ref);
          return controllerMVCService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ControllerMVCService on JSONMetadataProviderImpl.");
        return null;
      }
    } else {
      return controllerMVCService;
    }
  }

}
