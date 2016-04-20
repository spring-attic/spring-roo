package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import static org.springframework.roo.model.RooJavaType.ROO_THYMELEAF;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.jvnet.inflector.Noun;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMVCService;
import org.springframework.roo.classpath.PhysicalTypeCategory;
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
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringEnumDetails;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link ThymeleafMetadataProvider}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ThymeleafMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements ThymeleafMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(ThymeleafMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToServiceMidMap =
      new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  private boolean readOnly;
  private JavaType entity;
  private JavaType identifierType;
  private MethodMetadata identifierAccessor;
  private JavaType service;
  private String path;
  private String metadataIdentificationString;
  private ClassOrInterfaceTypeDetails controller;

  private JavaType globalSearchType;
  private JavaType datatablesDataType;

  private ControllerMVCService controllerMVCService;

  private List<JavaType> typesToImport;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_THYMELEAF} as additional 
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

    addMetadataTrigger(ROO_THYMELEAF);
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

    removeMetadataTrigger(ROO_THYMELEAF);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return ThymeleafMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = ThymeleafMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = ThymeleafMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Thymeleaf";
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

    this.typesToImport = new ArrayList<JavaType>();
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
    this.identifierAccessor = getPersistenceMemberLocator().getIdentifierAccessor(entity);

    // Getting service and its metadata
    this.service = (JavaType) controllerAnnotation.getAttribute("service").getValue();
    ClassOrInterfaceTypeDetails serviceDetails =
        getTypeLocationService().getTypeDetails(this.service);

    List<MethodMetadata> finders = new ArrayList<MethodMetadata>();
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
    MethodMetadata serviceCountMethod = serviceMetadata.getCountMethod();

    return new ThymeleafMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, getListFormMethod(), getListMethod(serviceFindAllMethod,
            serviceCountMethod), getCreateFormMethod(), getCreateMethod(serviceSaveMethod),
        getEditFormMethod(), getUpdateMethod(serviceSaveMethod),
        getDeleteMethod(serviceDeleteMethod), getShowMethod(), getPopulateFormMethod(),
        this.readOnly, typesToImport);
  }

  /**
   * This method provides the "list" method  using JSON 
   * response type and returns Datatables element
   * 
   * @param serviceFindAllMethod
   * @param serviceCountMethod
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getListMethod(MethodMetadata serviceFindAllMethod,
      MethodMetadata serviceCountMethod) {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "", null, null, null,
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON, "");
    if (existingMVCMethod != null) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("list");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    // TODO
    //parameterTypes.add(AnnotatedJavaType.convertFromJavaType(this.globalSearchType));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.PAGEABLE));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(JavaType.INT_OBJECT));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    // TODO
    //parameterNames.add(new JavaSymbolName("search"));
    parameterNames.add(new JavaSymbolName("pageable"));
    parameterNames.add(new JavaSymbolName("draw"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, "", null, null, null,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON, ""));

    // Adding @ResponseBody annotation
    AnnotationMetadataBuilder responseBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(responseBodyAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Page<Entity> entityField = serviceField.findAll(search, pageable);
    bodyBuilder.appendFormalLine(String.format("%s<%s> %s = %s.%s(search, pageable);",
        SpringJavaType.PAGE, this.entity, getEntityField().getFieldName(), getServiceField()
            .getFieldName(), serviceFindAllMethod.getMethodName()));

    // long allAvailableEntityField = serviceField.count();
    bodyBuilder.appendFormalLine(String.format("%s allAvailable%s = %s.%s();", serviceCountMethod
        .getReturnType(), StringUtils.capitalize(getEntityField().getFieldName().getSymbolName()),
        getServiceField().getFieldName(), serviceCountMethod.getMethodName()));

    // return new DatatablesData<Entity>(entityField.getContent(),
    //          Long.valueOf(allAvailableEntityField), Long.valueOf(entityField.getTotalElements()), draw);
    // TODO
    /*bodyBuilder.appendFormalLine(String.format("return new %s<%s>(%s.getContent(), "
        + "Long.valueOf(allAvailable%s), Long.valueOf(%s.getTotalElements()), draw);",
        this.datatablesDataType, this.entity, getEntityField().getFieldName(), StringUtils
            .capitalize(getEntityField().getFieldName().getSymbolName()), getEntityField()
            .getFieldName()));*/

    // Generating returnType
    // TODO
    /*JavaType returnType =
        new JavaType(this.datatablesDataType.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
            Arrays.asList(this.entity));*/
    JavaType returnType = JavaType.VOID_PRIMITIVE;

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            returnType, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "list" form method  using Thymeleaf view 
   * response type
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getListFormMethod() {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "", null, null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, "");
    if (existingMVCMethod != null) {
      throw new RuntimeException(
          "ERROR: You are trying to generate more than one method with the same @RequestMapping");
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("list");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, "", null, null, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return "path/list";
    bodyBuilder.appendFormalLine(String.format("return \"%s/list\";", getViewsPath()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "create" form method  using Thymeleaf view 
   * response type
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getCreateFormMethod() {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "/create-form", null, null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, "");
    if (existingMVCMethod != null) {
      throw new RuntimeException(
          "ERROR: You are trying to generate more than one method with the same @RequestMapping");
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("createForm");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, "/create-form", null, null, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // model.addAttribute(new Entity());
    bodyBuilder.appendFormalLine(String.format("model.addAttribute(new %s());",
        this.entity.getSimpleTypeName()));

    // populateForm(model);
    bodyBuilder.appendFormalLine("populateForm(model);");

    // return "path/create";
    bodyBuilder.appendFormalLine(String.format("return \"%s/create\";", getViewsPath()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "create" method  using Thymeleaf view 
   * response type
   * 
   * @param serviceSaveMethod MethodMetadata
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getCreateMethod(MethodMetadata serviceSaveMethod) {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_POST, "", null, null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, "");
    if (existingMVCMethod != null) {
      throw new RuntimeException(
          "ERROR: You are trying to generate more than one method with the same @RequestMapping");
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("create");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.entity, new AnnotationMetadataBuilder(
        new JavaType("javax.validation.Valid")).build(), new AnnotationMetadataBuilder(
        SpringJavaType.MODEL_ATTRIBUTE).build()));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.BINDING_RESULT));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.REDIRECT_ATTRIBUTES));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(getEntityField().getFieldName());
    parameterNames.add(new JavaSymbolName("result"));
    parameterNames.add(new JavaSymbolName("redirectAttrs"));
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_POST, "", null, null, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (result.hasErrors()) {
    bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
    bodyBuilder.indent();

    // populateForm(model);
    bodyBuilder.appendFormalLine("populateForm(model);");

    // return "path/create";
    bodyBuilder.appendFormalLine(String.format("return \"%s/create\";", getViewsPath()));
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    // Entity newEntity = entityService.SAVE_METHOD(entityField);
    bodyBuilder.appendFormalLine(String.format("%s new%s = %s.%s(%s);", this.entity
        .getSimpleTypeName(), this.entity.getSimpleTypeName(), getServiceField().getFieldName(),
        serviceSaveMethod.getMethodName(), getEntityField().getFieldName()));


    // redirectAttrs.addAttribute("id", newEntity.ACCESSOR_METHOD());
    bodyBuilder.appendFormalLine(String.format("redirectAttrs.addAttribute(\"id\", new%s.%s());",
        this.entity.getSimpleTypeName(), this.identifierAccessor.getMethodName()));

    // return "redirect:/path/{id}";
    bodyBuilder.appendFormalLine(String.format("return \"redirect:/%s/{id}\";", getViewsPath()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "edit" form method  using Thymeleaf view 
   * response type
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getEditFormMethod() {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET,
            String.format("/{%s}/edit-form", getEntityField().getFieldName()), null, null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, "");
    if (existingMVCMethod != null) {
      throw new RuntimeException(
          "ERROR: You are trying to generate more than one method with the same @RequestMapping");
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("editForm");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.entity, new AnnotationMetadataBuilder(
        SpringJavaType.PATH_VARIABLE).build()));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(getEntityField().getFieldName());
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET,
        String.format("/{%s}/edit-form", getEntityField().getFieldName()), null, null, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // populateForm(model);
    bodyBuilder.appendFormalLine("populateForm(model);");

    // return "path/create";
    bodyBuilder.appendFormalLine(String.format("return \"%s/edit\";", getViewsPath()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "update" method  using Thymeleaf view 
   * response type
   * 
   * @param serviceSaveMethod MethodMetadata
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getUpdateMethod(MethodMetadata serviceSaveMethod) {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_PUT,
            String.format("/{%s}", getEntityField().getFieldName()), null, null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, "");
    if (existingMVCMethod != null) {
      throw new RuntimeException(
          "ERROR: You are trying to generate more than one method with the same @RequestMapping");
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("update");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.entity, new AnnotationMetadataBuilder(
        new JavaType("javax.validation.Valid")).build(), new AnnotationMetadataBuilder(
        SpringJavaType.MODEL_ATTRIBUTE).build()));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.BINDING_RESULT));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.REDIRECT_ATTRIBUTES));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(getEntityField().getFieldName());
    parameterNames.add(new JavaSymbolName("result"));
    parameterNames.add(new JavaSymbolName("redirectAttrs"));
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_PUT,
        String.format("/{%s}", getEntityField().getFieldName()), null, null, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (result.hasErrors()) {
    bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
    bodyBuilder.indent();

    // populateForm(model);
    bodyBuilder.appendFormalLine("populateForm(model);");

    // return "path/create";
    bodyBuilder.appendFormalLine(String.format("return \"%s/edit\";", getViewsPath()));
    bodyBuilder.indentRemove();

    // }
    bodyBuilder.appendFormalLine("}");

    // Entity savedEntity = entityService.SAVE_METHOD(entityField);
    bodyBuilder.appendFormalLine(String.format("%s saved%s = %s.%s(%s);", this.entity
        .getSimpleTypeName(), this.entity.getSimpleTypeName(), getServiceField().getFieldName(),
        serviceSaveMethod.getMethodName(), getEntityField().getFieldName()));

    // redirectAttrs.addAttribute("id", savedEntity.ACCESSOR_METHOD());
    bodyBuilder.appendFormalLine(String.format("redirectAttrs.addAttribute(\"id\", saved%s.%s());",
        this.entity.getSimpleTypeName(), this.identifierAccessor.getMethodName()));

    // return "redirect:/path/{id}";
    bodyBuilder.appendFormalLine(String.format("return \"redirect:/%s/{id}\";", getViewsPath()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "delete" method using Thymeleaf view 
   * response type
   * 
   * @param serviceDeleteMethod
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getDeleteMethod(MethodMetadata serviceDeleteMethod) {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_DELETE, "/{id}", null, null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, "");
    if (existingMVCMethod != null) {
      throw new RuntimeException(
          "ERROR: You are trying to generate more than one method with the same @RequestMapping");
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("delete");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    AnnotationMetadataBuilder pathVariableAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVariableAnnotation.addStringAttribute("value", "id");

    parameterTypes.add(new AnnotatedJavaType(this.identifierType, pathVariableAnnotation.build()));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("id"));
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_DELETE, "/{id}", null, null, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // entityService.DELETE_METHOD(id);
    bodyBuilder.appendFormalLine(String.format("%s.%s(id);", getServiceField().getFieldName(),
        serviceDeleteMethod.getMethodName()));

    // return "redirect:/path";
    bodyBuilder.appendFormalLine(String.format("return \"redirect:/%s\";", getViewsPath()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "show" method  using Thymeleaf view 
   * response type
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getShowMethod() {

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET,
            String.format("/{%s}", getEntityField().getFieldName()), null, null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, "");
    if (existingMVCMethod != null) {
      throw new RuntimeException(
          "ERROR: You are trying to generate more than one method with the same @RequestMapping");
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("show");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(new AnnotatedJavaType(this.entity, new AnnotationMetadataBuilder(
        SpringJavaType.PATH_VARIABLE).build()));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(getEntityField().getFieldName());
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET,
        String.format("/{%s}", getEntityField().getFieldName()), null, null, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return "path/show";
    bodyBuilder.appendFormalLine(String.format("return \"%s/show\";", getViewsPath()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "populateForm" method 
   * 
   * @return MethodMetadata
   */
  private MethodMetadata getPopulateFormMethod() {

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("populateForm");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("model"));

    // Check if exists other populateForm method in this controller
    MemberDetails controllerMemberDetails = getMemberDetails(this.controller);
    MethodMetadata existingMethod =
        controllerMemberDetails.getMethod(methodName,
            AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameterTypes));
    if (existingMethod != null) {
      return existingMethod;
    }

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Getting all enum types from provided entity
    MemberDetails entityDetails =
        getMemberDetails(getTypeLocationService().getTypeDetails(this.entity));
    List<FieldMetadata> fields = entityDetails.getFields();
    for (FieldMetadata field : fields) {
      if (isEnumType(field.getFieldType())) {
        // model.addAttribute("enumField", Arrays.asList(Enum.values()));
        bodyBuilder.appendFormalLine(String.format(
            "model.addAttribute(\"%s\", %s.asList(%s.values()));",
            Noun.pluralOf(field.getFieldName().getSymbolName(), Locale.ENGLISH),
            addTypeToImport(new JavaType("java.util.Arrays")).getSimpleTypeName(),
            addTypeToImport(field.getFieldType()).getSimpleTypeName()));
      }
    }

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.VOID_PRIMITIVE, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method checks if the provided type is enum or not
   * 
   * @param fieldType
   * @return
   */
  private boolean isEnumType(JavaType fieldType) {
    Validate.notNull(fieldType, "Java type required");
    final ClassOrInterfaceTypeDetails javaTypeDetails =
        getTypeLocationService().getTypeDetails(fieldType);
    if (javaTypeDetails != null) {
      if (javaTypeDetails.getPhysicalTypeCategory().equals(PhysicalTypeCategory.ENUMERATION)) {
        return true;
      }
    }
    return false;
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

    // Generating service field name
    String fieldName =
        new JavaSymbolName(this.service.getSimpleTypeName())
            .getSymbolNameUnCapitalisedFirstLetter();

    return new FieldMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName(fieldName), this.service)
        .build();
  }


  /**
   * This method returns the final views path to be used
   * 
   * @return
   */
  private String getViewsPath() {
    return this.path.startsWith("/") ? this.path.substring(1) : this.path;
  }

  /**
   * This method registers a new type on types to import list
   * and then returns it.
   * 
   * @param type
   * @return
   */
  private JavaType addTypeToImport(JavaType type) {
    typesToImport.add(type);
    return type;
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
    return ThymeleafMetadata.getMetadataIdentiferType();
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
