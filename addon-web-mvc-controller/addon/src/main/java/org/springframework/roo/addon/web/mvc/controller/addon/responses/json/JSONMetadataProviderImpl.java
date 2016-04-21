package org.springframework.roo.addon.web.mvc.controller.addon.responses.json;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
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

  private ControllerMVCService controllerMVCService;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_JSON} as additional 
   * JavaType that will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  @SuppressWarnings("unchecked")
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
    this.entity = (JavaType) controllerAnnotation.getAttribute("entity").getValue();
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

    // Getting methods from related service
    MethodMetadata serviceSaveMethod = serviceMetadata.getSaveMethod();
    MethodMetadata serviceDeleteMethod = serviceMetadata.getDeleteMethod();
    MethodMetadata serviceFindAllMethod = serviceMetadata.getFindAllMethod();
    MethodMetadata serviceFindOneMethod = serviceMetadata.getFindOneMethod();


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
        getListMethod(serviceFindAllMethod), getCreateMethod(serviceSaveMethod),
        getUpdateMethod(serviceSaveMethod), getDeleteMethod(serviceDeleteMethod),
        getShowMethod(serviceFindOneMethod), findersToAdd, this.readOnly);

  }

  /**
   * This method provides the "create" method  using JSON 
   * response type
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

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_POST, "", null,
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(),
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(), "");
    if (existingMVCMethod != null) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("create");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.entity, new AnnotationMetadataBuilder(
        SpringJavaType.REQUEST_BODY).build()));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(getEntityField().getFieldName());

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

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return entityService.SAVE_METHOD(entityField);
    bodyBuilder.appendFormalLine(String.format("return %s.%s(%s);", getServiceField()
        .getFieldName(), serviceSaveMethod.getMethodName(), getEntityField().getFieldName()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            this.entity, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "update" method  using JSON 
   * response type
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

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_PUT, "", null,
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(),
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(), "");
    if (existingMVCMethod != null) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("update");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.entity, new AnnotationMetadataBuilder(
        SpringJavaType.REQUEST_BODY).build()));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(getEntityField().getFieldName());

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_PUT, "", null,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE, ""));

    // Adding @ResponseBody annotation
    AnnotationMetadataBuilder responseBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(responseBodyAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return entityService.SAVE_METHOD(entityField);
    bodyBuilder.appendFormalLine(String.format("return %s.%s(%s);", getServiceField()
        .getFieldName(), serviceSaveMethod.getMethodName(), getEntityField().getFieldName()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            this.entity, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "delete" method  using JSON 
   * response type
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

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_DELETE, "/{id}", null, "", "", "");
    if (existingMVCMethod != null) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("delete");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.identifierType, new AnnotationMetadataBuilder(
        SpringJavaType.PATH_VARIABLE).build()));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("id"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_DELETE, "/{id}", null, "", "", ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // entityService.DELETE_METHOD(id);
    bodyBuilder.appendFormalLine(String.format("%s.%s(id);", getServiceField().getFieldName(),
        serviceDeleteMethod.getMethodName()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.VOID_PRIMITIVE, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "list" method  using JSON 
   * response type
   * 
   * @param serviceFindAllMethod
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getListMethod(MethodMetadata serviceFindAllMethod) {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "", null, null,
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(), "");
    if (existingMVCMethod != null) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("list");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

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

    // return entityService.FIND_ALL_METHOD();
    bodyBuilder.appendFormalLine(String.format("return %s.%s();", getServiceField().getFieldName(),
        serviceFindAllMethod.getMethodName()));

    // Generating returnType
    JavaType returnType =
        new JavaType(new JavaType("java.util.List").getFullyQualifiedTypeName(), 0, DataType.TYPE,
            null, Arrays.asList(this.entity));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            returnType, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides a finder method  using JSON 
   * response type
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
    if (existingMVCMethod != null) {
      return existingMVCMethod;
    }

    // Get parameters
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    for (FinderParameter parameter : finderMethod.getParameters()) {
      parameterTypes.add(new AnnotatedJavaType(parameter.getType(), new AnnotationMetadataBuilder(
          SpringJavaType.REQUEST_PARAM).build()));
      parameterNames.add(parameter.getName());
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

    // return entityService.FINDER_METHOD(%s);
    bodyBuilder.appendFormalLine(String.format("return %s.%s(%s);", getServiceField()
        .getFieldName(), finderMethod.getMethodName(), StringUtils.join(parameters, ", ")));

    // Generating returnType
    JavaType returnType = finderMethod.getReturnType();

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            returnType, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "show" method  using JSON 
   * response type
   * 
   * @param serviceFindOneMethod
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getShowMethod(MethodMetadata serviceFindOneMethod) {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "/{id}", null, null,
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(), "");
    if (existingMVCMethod != null) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("show");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.identifierType, new AnnotationMetadataBuilder(
        SpringJavaType.PATH_VARIABLE).build()));

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

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return entityService.FIND_ONE_METHOD(id);
    bodyBuilder.appendFormalLine(String.format("return %s.%s(id);", getServiceField()
        .getFieldName(), serviceFindOneMethod.getMethodName()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            this.entity, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

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
