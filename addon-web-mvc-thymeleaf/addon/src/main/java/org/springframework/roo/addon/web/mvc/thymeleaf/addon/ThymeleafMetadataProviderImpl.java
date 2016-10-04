package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import static org.springframework.roo.model.RooJavaType.ROO_THYMELEAF;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.jvnet.inflector.Noun;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerDetailInfo;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMVCService;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;
import org.springframework.roo.addon.web.mvc.controller.addon.finder.SearchAnnotationValues;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.addon.web.mvc.views.AbstractViewGeneratorMetadataProvider;
import org.springframework.roo.addon.web.mvc.views.MVCViewGenerationService;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
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
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.Jsr303JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringEnumDetails;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Implementation of {@link ThymeleafMetadataProvider}.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ThymeleafMetadataProviderImpl extends AbstractViewGeneratorMetadataProvider implements
    ThymeleafMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(ThymeleafMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToServiceMidMap =
      new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  private JavaType globalSearchType;
  private JavaType datatablesDataType;
  private JavaType datatablesPageable;

  private ControllerMVCService controllerMVCService;
  private MVCViewGenerationService viewGenerationService;

  private List<JavaType> typesToImport;
  private ControllerType type;
  private String entityPlural;
  private String pathPrefix;
  private ControllerDetailInfo controllerDetailInfo;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_THYMELEAF} as additional JavaType
   * that will trigger metadata registration.</li>
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
  protected MVCViewGenerationService getViewGenerationService() {
    if (viewGenerationService == null) {
      // Get all Services implement MVCViewGenerationService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(MVCViewGenerationService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          MVCViewGenerationService viewService =
              (MVCViewGenerationService) this.context.getService(ref);
          if (viewService.getName().equals("THYMELEAF")) {
            viewGenerationService = viewService;
            return viewGenerationService;
          }
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load MVCViewGenerationService on ThymeleafMetadataProviderImpl.");
        return null;
      }
    } else {
      return viewGenerationService;
    }
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem createMetadataInstance() {

    this.typesToImport = new ArrayList<JavaType>();

    // Getting service details
    final ServiceMetadata serviceMetadata = getServiceMetadata();

    // Getting Global search class
    Set<ClassOrInterfaceTypeDetails> globalSearchClasses =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_GLOBAL_SEARCH);
    if (globalSearchClasses.isEmpty()) {
      throw new RuntimeException("ERROR: GlobalSearch.java file doesn't exist or has been deleted.");
    }
    Iterator<ClassOrInterfaceTypeDetails> gobalSearchClassIterator = globalSearchClasses.iterator();
    while (gobalSearchClassIterator.hasNext()) {
      this.globalSearchType = gobalSearchClassIterator.next().getType();
      break;
    }

    // Getting DatatablesDataType
    Set<ClassOrInterfaceTypeDetails> datatablesDataClasses =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_THYMELEAF_DATATABLES_DATA);
    if (datatablesDataClasses.isEmpty()) {
      throw new RuntimeException(
          "ERROR: DatatablesData.java file doesn't exist or has been deleted.");
    }
    Iterator<ClassOrInterfaceTypeDetails> datatablesDataClassIterator =
        datatablesDataClasses.iterator();
    while (datatablesDataClassIterator.hasNext()) {
      this.datatablesDataType = datatablesDataClassIterator.next().getType();
      break;
    }

    // Getting DatatablesPageable
    Set<ClassOrInterfaceTypeDetails> datatablesPageableClasses =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_THYMELEAF_DATATABLES_PAGEABLE);
    if (datatablesPageableClasses.isEmpty()) {
      throw new RuntimeException(
          "ERROR: DatatablesPageable.java file doesn't exist or has been deleted.");
    }
    Iterator<ClassOrInterfaceTypeDetails> datatablesPageableClassIterator =
        datatablesPageableClasses.iterator();
    while (datatablesPageableClassIterator.hasNext()) {
      this.datatablesPageable = datatablesPageableClassIterator.next().getType();
      break;
    }

    // Getting type
    AnnotationMetadata controllerAnnotation = controller.getAnnotation(RooJavaType.ROO_CONTROLLER);
    this.type =
        ControllerType.getControllerType(((EnumDetails) controllerAnnotation.getAttribute("type")
            .getValue()).getField().getSymbolName());

    this.entityPlural =
        StringUtils.uncapitalize(Noun.pluralOf(this.entity.getSimpleTypeName(), Locale.ENGLISH));

    if (controllerAnnotation.getAttribute("pathPrefix") != null) {
      this.pathPrefix = (String) controllerAnnotation.getAttribute("pathPrefix").getValue();
    }

    // Getting controller metadata
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(controller.getDeclaredByMetadataId());
    final String controllerMetadataKey =
        ControllerMetadata.createIdentifier(controller.getType(), logicalPath);
    final ControllerMetadata controllerMetadata =
        (ControllerMetadata) getMetadataService().get(controllerMetadataKey);

    // Getting detail controller info
    this.controllerDetailInfo = controllerMetadata.getControllerDetailInfo();

    // Getting methods from related service
    MethodMetadata serviceSaveMethod = serviceMetadata.getSaveMethod();
    MethodMetadata serviceDeleteMethod = serviceMetadata.getDeleteMethod();
    MethodMetadata serviceFindAllGlobalSearchMethod =
        serviceMetadata.getFindAllGlobalSearchMethod();
    MethodMetadata serviceCountMethod = serviceMetadata.getCountMethod();

    // Add finder methods
    List<MethodMetadata> findersToAdd = new ArrayList<MethodMetadata>();

    // Getting annotated finders
    final SearchAnnotationValues annotationValues =
        new SearchAnnotationValues(governorPhysicalTypeMetadata);

    // Add finders only if controller is of search type
    if (this.type == ControllerType.getControllerType(ControllerType.SEARCH.name())
        && annotationValues != null && annotationValues.getFinders() != null) {
      List<String> finders = new ArrayList<String>(Arrays.asList(annotationValues.getFinders()));

      // Search indicated finders in its related service
      for (MethodMetadata serviceFinder : serviceMetadata.getFinders()) {
        if (finders.contains(serviceFinder.getMethodName().toString())) {
          MethodMetadata finderMethod = getFinderMethod(serviceFinder);
          findersToAdd.add(finderMethod);

          // Add each finder support methods
          findersToAdd.add(getFinderFormMethod(finderMethod));
          findersToAdd.add(getFinderRedirectMethod(finderMethod));
          findersToAdd.add(getFinderListMethod(finderMethod));

          // Add dependencies between modules
          List<JavaType> types = new ArrayList<JavaType>();
          types.add(serviceFinder.getReturnType());
          types.addAll(serviceFinder.getReturnType().getParameters());

          for (AnnotatedJavaType parameter : serviceFinder.getParameterTypes()) {
            types.add(parameter.getJavaType());
            types.addAll(parameter.getJavaType().getParameters());
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

    return new ThymeleafMetadata(metadataIdentificationString, this.aspectName,
        this.governorPhysicalTypeMetadata, getListFormMethod(),
        getListJSONMethod(serviceFindAllGlobalSearchMethod),
        getListDatatablesJSONMethod(serviceCountMethod), getCreateFormMethod(),
        getCreateMethod(serviceSaveMethod), getEditFormMethod(),
        getUpdateMethod(serviceSaveMethod), getDeleteMethod(serviceDeleteMethod), getShowMethod(),
        getPopulateFormMethod(), getPopulateFormatsMethod(),
        getDeleteBatchMethod(serviceDeleteMethod), getCreateBatchMethod(serviceSaveMethod),
        getUpdateBatchMethod(serviceSaveMethod), getDetailMethods(), isReadOnly(), typesToImport,
        this.type, findersToAdd);
  }

  private ServiceMetadata getServiceMetadata() {
    ClassOrInterfaceTypeDetails serviceDetails =
        getTypeLocationService().getTypeDetails(getService());

    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(serviceDetails.getDeclaredByMetadataId());
    final String serviceMetadataKey =
        ServiceMetadata.createIdentifier(serviceDetails.getType(), logicalPath);
    final ServiceMetadata serviceMetadata =
        (ServiceMetadata) getMetadataService().get(serviceMetadataKey);
    return serviceMetadata;
  }

  /**
   * This method provides populateFormats method that allows to configure date
   * time format for every entity
   *
   * @return
   */
  private MethodMetadata getPopulateFormatsMethod() {
    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("populateFormats");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("model"));

    // Check if exists other addDateTimeFormatPatterns method in this
    // controller
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

    // Always save locale
    bodyBuilder.appendFormalLine(String.format(
        "model.addAttribute(\"application_locale\", %s.getLocale().getLanguage());",
        addTypeToImport(SpringJavaType.LOCALE_CONTEXT_HOLDER).getSimpleTypeName()));

    // Getting all enum types from provided entity
    MemberDetails entityDetails =
        getMemberDetails(getTypeLocationService().getTypeDetails(this.entity));
    List<FieldMetadata> fields = entityDetails.getFields();
    for (FieldMetadata field : fields) {
      JavaType type = field.getFieldType();
      if (type.getFullyQualifiedTypeName().equals(Date.class.getName())
          || type.getFullyQualifiedTypeName().equals(Calendar.class.getName())) {

        // Getting annotation format
        AnnotationMetadata dateTimeFormatAnnotation =
            field.getAnnotation(SpringJavaType.DATE_TIME_FORMAT);

        if (dateTimeFormatAnnotation != null
            && (dateTimeFormatAnnotation.getAttribute("style") != null || dateTimeFormatAnnotation
                .getAttribute("pattern") != null)) {

          AnnotationAttributeValue<String> formatAttr =
              dateTimeFormatAnnotation.getAttribute("style");
          if (formatAttr != null) {
            String format = formatAttr.getValue();
            // model.addAttribute("field_date_format",
            // DateTimeFormat.patternForStyle("M-",
            // LocaleContextHolder.getLocale()));
            bodyBuilder
                .appendFormalLine(String
                    .format(
                        "model.addAttribute(\"%s_date_format\", %s.patternForStyle(\"%s\", %s.getLocale()));",
                        field.getFieldName().getSymbolName(),
                        addTypeToImport(new JavaType("org.joda.time.format.DateTimeFormat"))
                            .getSimpleTypeName(), format,
                        addTypeToImport(SpringJavaType.LOCALE_CONTEXT_HOLDER).getSimpleTypeName()));
          } else {
            formatAttr = dateTimeFormatAnnotation.getAttribute("pattern");
            String format = formatAttr.getValue();
            // model.addAttribute("field_date_format", "pattern");
            bodyBuilder.appendFormalLine(String.format(
                "model.addAttribute(\"%s_date_format\", \"%s\");", field.getFieldName()
                    .getSymbolName(), format));
          }
        }

      }
    }

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PRIVATE, methodName,
            JavaType.VOID_PRIMITIVE, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "list" JSON method using JSON response type and
   * returns Page element
   *
   * @param serviceFindAllGlobalSearchMethod
   *
   * @return MethodMetadata
   */
  private MethodMetadata getListJSONMethod(MethodMetadata serviceFindAllGlobalSearchMethod) {

    // If provided entity is readOnly or annotated controller is not a
    // Collection controller
    // create method will not be available
    if (this.type != ControllerType.COLLECTION) {
      return null;
    }

    // First of all, check if exists other method with the same
    // @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(this.controller.getType(),
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
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(this.globalSearchType));
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

    // Page<Entity> entityNamePlural = serviceField.findAll(search,
    // pageable);
    bodyBuilder.appendFormalLine(String.format("%s<%s> %s = %s.%s(search, pageable);",
        addTypeToImport(SpringJavaType.PAGE).getSimpleTypeName(), addTypeToImport(this.entity)
            .getSimpleTypeName(), this.entityPlural, getServiceField().getFieldName(),
        serviceFindAllGlobalSearchMethod.getMethodName()));

    // return entityNamePlural;
    bodyBuilder.appendFormalLine(String.format("return %s;", this.entityPlural));

    // Generating returnType
    JavaType returnType =
        new JavaType(SpringJavaType.PAGE.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
            Arrays.asList(this.entity));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            returnType, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "list" Datatables JSON method using JSON
   * response type and returns Datatables element
   *
   * @param serviceCountMethod
   *
   * @return MethodMetadata
   */
  private MethodMetadata getListDatatablesJSONMethod(MethodMetadata serviceCountMethod) {

    // If provided entity is readOnly or annotated controller is not a
    // Collection controller
    // create method will not be available
    if (this.type != ControllerType.COLLECTION) {
      return null;
    }

    // First of all, check if exists other method with the same
    // @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "", null, "", "application/vnd.datatables+json",
            "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("list");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(this.globalSearchType));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(this.datatablesPageable));
    AnnotationMetadataBuilder requestParamAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.REQUEST_PARAM);
    requestParamAnnotation.addStringAttribute("value", "draw");
    parameterTypes.add(new AnnotatedJavaType(JavaType.INT_OBJECT, requestParamAnnotation.build()));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("search"));
    parameterNames.add(new JavaSymbolName("pageable"));
    parameterNames.add(new JavaSymbolName("draw"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, "", null, "", "application/vnd.datatables+json", ""));

    // Adding @ResponseBody annotation
    AnnotationMetadataBuilder responseBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(responseBodyAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Page<Entity> entityNamePlural = list(search, pageable);
    bodyBuilder.appendFormalLine(String.format("%s<%s> %s = list(search, pageable);",
        addTypeToImport(SpringJavaType.PAGE).getSimpleTypeName(), addTypeToImport(this.entity)
            .getSimpleTypeName(), this.entityPlural));

    // long allAvailableentityNamePlural = serviceField.count();
    bodyBuilder.appendFormalLine(String.format("long allAvailable%s = %s.%s();",
        StringUtils.capitalize(this.entityPlural), getServiceField().getFieldName(),
        serviceCountMethod.getMethodName()));

    // return new DatatablesData<Entity>(entityNamePlural,
    // allAvailableentityNamePlural,
    // draw);
    bodyBuilder.appendFormalLine(String.format("return new %s<%s>(%s, allAvailable%s, draw);",
        addTypeToImport(this.datatablesDataType).getSimpleTypeName(),
        this.entity.getSimpleTypeName(), this.entityPlural,
        StringUtils.capitalize(this.entityPlural)));

    // Generating returnType
    JavaType returnType =
        new JavaType(this.datatablesDataType.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
            Arrays.asList(this.entity));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            returnType, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "list" form method using Thymeleaf view response
   * type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getListFormMethod() {

    // If provided entity is readOnly or annotated controller is not a
    // Collection controller
    // create method will not be available
    if (this.type != ControllerType.COLLECTION) {
      return null;
    }

    // First of all, check if exists other method with the same
    // @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "", null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
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
        SpringEnumDetails.REQUEST_METHOD_GET, "", null, null,
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
   * This method provides the "create" form method using Thymeleaf view
   * response type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getCreateFormMethod() {

    // If provided entity is readOnly or annotated controller is not a
    // Collection controller
    // create method will not be available
    if (this.readOnly || this.type != ControllerType.COLLECTION) {
      return null;
    }

    // First of all, check if exists other method with the same
    // @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "/create-form", null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
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
        SpringEnumDetails.REQUEST_METHOD_GET, "/create-form", null, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // model.addAttribute(new Entity());
    bodyBuilder.appendFormalLine(String.format("model.addAttribute(new %s());",
        this.entity.getSimpleTypeName()));

    // return "path/create";
    bodyBuilder.appendFormalLine(String.format("return \"%s/create\";", getViewsPath()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "create" method using Thymeleaf view response
   * type
   *
   * @param serviceSaveMethod
   *            MethodMetadata
   *
   * @return MethodMetadata
   */
  private MethodMetadata getCreateMethod(MethodMetadata serviceSaveMethod) {

    // If provided entity is readOnly or annotated controller is not a
    // Collection controller
    // create method will not be available
    if (this.readOnly || this.type != ControllerType.COLLECTION) {
      return null;
    }

    // First of all, check if exists other method with the same
    // @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_POST, "", null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
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
        SpringEnumDetails.REQUEST_METHOD_POST, "", null, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (result.hasErrors()) {
    bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
    bodyBuilder.indent();

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
   * This method provides the "edit" form method using Thymeleaf view response
   * type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getEditFormMethod() {

    // If provided entity is readOnly or annotated controller is not a
    // Collection controller
    // create method will not be available
    if (this.readOnly || this.type != ControllerType.ITEM) {
      return null;
    }

    // First of all, check if exists other method with the same
    // @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET,
            String.format("/edit-form", getEntityField().getFieldName()), null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("editForm");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    AnnotationMetadataBuilder modelAttributeAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE);
    parameterTypes.add(new AnnotatedJavaType(this.entity, modelAttributeAnnotation.build()));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(getEntityField().getFieldName());
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET,
        String.format("/edit-form", getEntityField().getFieldName()), null, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return "path/create";
    bodyBuilder.appendFormalLine(String.format("return \"%s/edit\";", getViewsPath()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "update" method using Thymeleaf view response
   * type
   *
   * @param serviceSaveMethod
   *            MethodMetadata
   *
   * @return MethodMetadata
   */
  private MethodMetadata getUpdateMethod(MethodMetadata serviceSaveMethod) {

    // If provided entity is readOnly or annotated controller is not a
    // Collection controller
    // create method will not be available
    if (this.readOnly || this.type != ControllerType.ITEM) {
      return null;
    }

    // First of all, check if exists other method with the same
    // @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_PUT, null, null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
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
        SpringEnumDetails.REQUEST_METHOD_PUT, null, null, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // if (result.hasErrors()) {
    bodyBuilder.appendFormalLine("if (result.hasErrors()) {");
    bodyBuilder.indent();

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
   * This method provides the "delete" method using Thymeleaf view response
   * type
   *
   * @param serviceDeleteMethod
   *
   * @return MethodMetadata
   */
  private MethodMetadata getDeleteMethod(MethodMetadata serviceDeleteMethod) {

    // If provided entity is readOnly or annotated controller is not a
    // Collection controller
    // create method will not be available
    if (this.readOnly || this.type != ControllerType.ITEM) {
      return null;
    }

    // First of all, check if exists other method with the same
    // @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_DELETE, null, null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("delete");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

    AnnotationMetadataBuilder modelAttributeAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE);
    parameterTypes.add(new AnnotatedJavaType(this.entity, modelAttributeAnnotation.build()));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("id"));
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_DELETE, null, null, null,
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
   * This method provides a finder method using THYMELEAF response type
   *
   * @param finderMethod
   *
   * @return MethodMetadata
   */
  private MethodMetadata getFinderMethod(MethodMetadata finderMethod) {
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
      path = StringUtils.removeStart(methodName.getSymbolName(), "count");
    } else if (StringUtils.startsWith(methodName.getSymbolName(), "find")) {
      path = StringUtils.removeStart(methodName.getSymbolName(), "find");
    } else if (StringUtils.startsWith(methodName.getSymbolName(), "query")) {
      path = StringUtils.removeStart(methodName.getSymbolName(), "query");
    } else if (StringUtils.startsWith(methodName.getSymbolName(), "read")) {
      path = StringUtils.removeStart(methodName.getSymbolName(), "read");
    } else {
      path = methodName.getSymbolName();
    }
    path = StringUtils.uncapitalize(path);

    // Check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "/" + path, stringParameterNames, null,
            "application/vnd.datatables+json", "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Get parameters
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    StringBuffer finderParamsString = new StringBuffer();
    for (int i = 0; i < originalParameterTypes.size(); i++) {

      // Add @ModelAttribute for search param
      if (i == 0) {
        AnnotationMetadataBuilder requestParamAnnotation =
            new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE);
        requestParamAnnotation.addStringAttribute("value", originalParameterNames.get(i)
            .getSymbolName());
        parameterTypes.add(new AnnotatedJavaType(originalParameterTypes.get(i).getJavaType(),
            requestParamAnnotation.build()));
        parameterNames.add(originalParameterNames.get(i));
        finderParamsString.append(originalParameterNames.get(i).getSymbolName());
      } else if (originalParameterTypes.get(i).getJavaType().getSimpleTypeName()
          .equals("GlobalSearch")) {
        parameterTypes.add(originalParameterTypes.get(i));
        addTypeToImport(originalParameterTypes.get(i).getJavaType());
        parameterNames.add(originalParameterNames.get(i));

        // Build finder parameters String
        finderParamsString.append(", ".concat(originalParameterNames.get(i).getSymbolName()));
      }
    }

    // Add DatatablesPageable param
    parameterTypes.add(AnnotatedJavaType
        .convertFromJavaType(addTypeToImport(this.datatablesPageable)));
    parameterNames.add(new JavaSymbolName("pageable"));
    finderParamsString.append(", pageable");

    // Add additional 'draw' param
    AnnotationMetadataBuilder requestParamAnnotation =
        new AnnotationMetadataBuilder(addTypeToImport(SpringJavaType.REQUEST_PARAM));
    requestParamAnnotation.addStringAttribute("value", "draw");
    parameterTypes.add(new AnnotatedJavaType(JavaType.INT_OBJECT, requestParamAnnotation.build()));
    parameterNames.add(new JavaSymbolName("draw"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, "/" + path, stringParameterNames, null,
        "application/vnd.datatables+json", ""));

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
    String returnParameterName =
        StringUtils.uncapitalize(returnParameterTypes.get(0).getSimpleTypeName());
    bodyBuilder.newLine();
    if (StringUtils.isEmpty(returnTypeParamsString)) {
      bodyBuilder.appendFormalLine(String.format("%s %s = %s.%s(%s);", addTypeToImport(returnType)
          .getSimpleTypeName(), returnParameterName, getServiceField().getFieldName(), methodName,
          finderParamsString));
    } else {
      bodyBuilder.appendFormalLine(String.format("%s<%s> %s = %s.%s(%s);",
          addTypeToImport(returnType).getSimpleTypeName(), returnTypeParamsString,
          returnParameterName, getServiceField().getFieldName(), methodName, finderParamsString));
    }

    // long allAvailableEntity/Projection =
    // ENTITY_SERVICE_FIELD.COUNT_METHOD(formBean);
    bodyBuilder.newLine();
    bodyBuilder.appendFormalLine(String.format("long allAvailable%s = %s.%s(%s);",
        StringUtils.capitalize(Noun.pluralOf(returnParameterName, Locale.ENGLISH)),
        getServiceField().getFieldName(), "count".concat(StringUtils.capitalize(path)),
        parameterNames.get(0)));
    bodyBuilder.newLine();

    // return new DatatablesData<Entity/Projection>(entity/projection,
    // allAvailableEntity/Projection, draw);
    bodyBuilder.appendFormalLine(String.format("return new %s<%s>(%s, allAvailable%s, draw);",
        addTypeToImport(this.datatablesDataType).getSimpleTypeName(), returnTypeParamsString,
        returnParameterName,
        StringUtils.capitalize(Noun.pluralOf(returnParameterName, Locale.ENGLISH))));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            addTypeToImport(new JavaType(this.datatablesDataType.getFullyQualifiedTypeName(), 0,
                DataType.TYPE, null, returnParameterTypes)), parameterTypes, parameterNames,
            bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides a finder list method using THYMELEAF response type
   *
   * @param finderMethod
   * @return
   */
  private MethodMetadata getFinderListMethod(MethodMetadata finderMethod) {
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
    if (StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(), "count")) {
      path = StringUtils.removeStart(finderMethod.getMethodName().getSymbolName(), "count");
    } else if (StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(), "find")) {
      path = StringUtils.removeStart(finderMethod.getMethodName().getSymbolName(), "find");
    } else if (StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(), "query")) {
      path = StringUtils.removeStart(finderMethod.getMethodName().getSymbolName(), "query");
    } else if (StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(), "read")) {
      path = StringUtils.removeStart(finderMethod.getMethodName().getSymbolName(), "read");
    } else {
      path = methodName.getSymbolName();
    }
    path = StringUtils.uncapitalize(path);

    // Check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "/" + path, stringParameterNames, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Get parameters
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    // Add model parameter
    parameterTypes
        .add(AnnotatedJavaType.convertFromJavaType(addTypeToImport(SpringJavaType.MODEL)));
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, "/" + path, stringParameterNames, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // return "PATH_PREFIX/ENTITY_PLURAL/FINDER_NAMEList";
    String pathPrefix = "";
    if (StringUtils.isBlank(this.pathPrefix)) {
      pathPrefix = this.pathPrefix;
    } else {
      pathPrefix = this.pathPrefix.concat("/");
    }
    bodyBuilder.appendFormalLine(String.format("return \"%s%s/%sList\";",
        StringUtils.removeStart(pathPrefix, "/"), this.entityPlural, methodName.getSymbolName()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides a finder redirect method using THYMELEAF response
   * type
   *
   * @param finderMethod
   * @return
   */
  private MethodMetadata getFinderRedirectMethod(MethodMetadata finderMethod) {
    final List<AnnotatedJavaType> originalParameterTypes = finderMethod.getParameterTypes();

    // Get finder parameter names
    final List<JavaSymbolName> originalParameterNames = finderMethod.getParameterNames();
    List<String> stringParameterNames = new ArrayList<String>();
    for (JavaSymbolName parameterName : originalParameterNames) {
      stringParameterNames.add(parameterName.getSymbolName());
    }

    // Define methodName
    final JavaSymbolName methodName =
        new JavaSymbolName(finderMethod.getMethodName().getSymbolName().concat("Redirect"));

    // Define path
    String path = "";
    if (StringUtils.startsWith(methodName.getSymbolName(), "count")) {
      path = StringUtils.removeStart(methodName.getSymbolName(), "count");
    } else if (StringUtils.startsWith(methodName.getSymbolName(), "find")) {
      path = StringUtils.removeStart(methodName.getSymbolName(), "find");
    } else if (StringUtils.startsWith(methodName.getSymbolName(), "query")) {
      path = StringUtils.removeStart(methodName.getSymbolName(), "query");
    } else if (StringUtils.startsWith(methodName.getSymbolName(), "read")) {
      path = StringUtils.removeStart(methodName.getSymbolName(), "read");
    } else {
      path = methodName.getSymbolName();
    }
    path = StringUtils.uncapitalize(StringUtils.removeEnd(path, "Redirect"));

    // Check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_POST, "/" + path, stringParameterNames, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Get parameters
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    // Add form bean parameter
    AnnotationMetadataBuilder modelAttributeAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE);
    modelAttributeAnnotation.addStringAttribute("value", originalParameterNames.get(0)
        .getSymbolName());
    parameterTypes.add(new AnnotatedJavaType(originalParameterTypes.get(0).getJavaType(),
        modelAttributeAnnotation.build()));
    parameterNames.add(originalParameterNames.get(0));

    // Add redirect parameter
    parameterTypes.add(AnnotatedJavaType
        .convertFromJavaType(addTypeToImport(SpringJavaType.REDIRECT_ATTRIBUTES)));
    parameterNames.add(new JavaSymbolName("redirect"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_POST, "/" + path, stringParameterNames, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.newLine();

    // redirect.addFlashAttribute(entity/dtoSearch);
    bodyBuilder.appendFormalLine(String.format("redirect.addFlashAttribute(\"formBean\", %s);",
        parameterNames.get(0)));
    bodyBuilder.newLine();

    // return "redirect:PATH_PREFIX/ENTITY_PLURAL/FINDER_NAME";
    bodyBuilder.appendFormalLine(String.format("return \"redirect:%s/%s/search/%s\";",
        this.pathPrefix, this.entityPlural, path));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides a finder form method using THYMELEAF response type
   *
   * @param finderMethod
   * @return
   */
  private MethodMetadata getFinderFormMethod(MethodMetadata finderMethod) {
    // Get finder parameter names
    List<String> stringParameterNames = new ArrayList<String>();
    stringParameterNames.add("model");

    // Define methodName
    final JavaSymbolName methodName =
        new JavaSymbolName(finderMethod.getMethodName().getSymbolName().concat("Form"));

    // Define path
    String path = "";
    if (StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(), "count")) {
      path = StringUtils.removeStart(finderMethod.getMethodName().getSymbolName(), "count");
    } else if (StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(), "find")) {
      path = StringUtils.removeStart(finderMethod.getMethodName().getSymbolName(), "find");
    } else if (StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(), "query")) {
      path = StringUtils.removeStart(finderMethod.getMethodName().getSymbolName(), "query");
    } else if (StringUtils.startsWith(finderMethod.getMethodName().getSymbolName(), "read")) {
      path = StringUtils.removeStart(finderMethod.getMethodName().getSymbolName(), "read");
    } else {
      path = methodName.getSymbolName();
    }
    path = StringUtils.uncapitalize(path).concat("/search-form");

    // Check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "/" + path, stringParameterNames, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Get parameters
    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

    // Add model parameter
    parameterTypes
        .add(AnnotatedJavaType.convertFromJavaType(addTypeToImport(SpringJavaType.MODEL)));
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, "/" + path, stringParameterNames, null,
        SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE, ""));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
    bodyBuilder.newLine();

    // Entity/DTO search = new Entity/DTO();
    bodyBuilder.appendFormalLine(String.format("%1$s %2$s = new %1$s();",
        addTypeToImport(finderMethod.getParameterTypes().get(0).getJavaType()).getSimpleTypeName(),
        finderMethod.getParameterNames().get(0).getSymbolName()));
    bodyBuilder.newLine();

    // model.addAttribute("search", search);
    bodyBuilder.appendFormalLine(String.format("model.addAttribute(\"%1$s\", %1$s);", finderMethod
        .getParameterNames().get(0).getSymbolName()));
    bodyBuilder.newLine();

    // populateForm(model);
    bodyBuilder.appendFormalLine("populateForm(model);");
    bodyBuilder.newLine();

    // return "PATH_PREFIX/ENTITY_PLURAL/FINDER_NAMEForm";
    String pathPrefix = "";
    if (StringUtils.isBlank(this.pathPrefix)) {
      pathPrefix = this.pathPrefix;
    } else {
      pathPrefix = this.pathPrefix.concat("/");
    }
    bodyBuilder.appendFormalLine(String.format("return \"%s%s/%s\";",
        StringUtils.removeStart(pathPrefix, "/"), this.entityPlural, methodName.getSymbolName()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            JavaType.STRING, parameterTypes, parameterNames, bodyBuilder);
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
    if (this.readOnly || this.type != ControllerType.COLLECTION) {
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
   * Creates delete batch method
   *
   * @param serviceSaveMethod
   *            the MethodMetadata of entity's service save method
   * @return {@link MethodMetadata}
   */
  private MethodMetadata getDeleteBatchMethod(MethodMetadata serviceDeleteMethod) {

    // If provided entity is readOnly, create method is not available
    if (this.readOnly || this.type != ControllerType.COLLECTION) {
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
   * Creates update batch method
   *
   * @param serviceSaveMethod
   *            the MethodMetadata of entity's service save method
   * @return {@link MethodMetadata}
   */
  private MethodMetadata getUpdateBatchMethod(MethodMetadata serviceSaveMethod) {

    // If provided entity is readOnly, create method is not available
    if (this.readOnly || this.type != ControllerType.COLLECTION) {
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
   * This method provides the "show" method using Thymeleaf view response type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getShowMethod() {

    // If provided entity is readOnly or annotated controller is not a
    // Collection controller
    // create method will not be available
    if (this.readOnly || this.type != ControllerType.ITEM) {
      return null;
    }

    // First of all, check if exists other method with the same
    // @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, null, null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("show");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    AnnotationMetadataBuilder modelAttributeAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE);
    parameterTypes.add(new AnnotatedJavaType(this.entity, modelAttributeAnnotation.build()));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(getEntityField().getFieldName());
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, null, null, null,
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

    // populateFormats(model);
    bodyBuilder.appendFormalLine("populateFormats(model);");

    // Getting all enum types from provided entity
    MemberDetails entityDetails =
        getMemberDetails(getTypeLocationService().getTypeDetails(this.entity));
    List<FieldMetadata> fields = entityDetails.getFields();
    for (FieldMetadata field : fields) {
      if (isEnumType(field.getFieldType())) {
        // model.addAttribute("enumField",
        // Arrays.asList(Enum.values()));
        bodyBuilder.appendFormalLine(String.format(
            "model.addAttribute(\"%s\", %s.asList(%s.values()));", this.entityPlural,
            addTypeToImport(new JavaType("java.util.Arrays")).getSimpleTypeName(),
            addTypeToImport(field.getFieldType()).getSimpleTypeName()));
      }
    }

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PRIVATE, methodName,
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

    // Generating entity field name
    String fieldName =
        new JavaSymbolName(this.entity.getSimpleTypeName()).getSymbolNameUnCapitalisedFirstLetter();

    return new FieldMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName(fieldName), this.entity)
        .build();
  }

  /**
   * This method returns detail entity field included on controller
   *
   * @return
   */
  private FieldMetadata getDetailEntityField(JavaType detailEntity) {

    // Generating detail entity field name
    String fieldName =
        new JavaSymbolName(detailEntity.getSimpleTypeName())
            .getSymbolNameUnCapitalisedFirstLetter();

    return new FieldMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC,
        new ArrayList<AnnotationMetadataBuilder>(), new JavaSymbolName(fieldName), detailEntity)
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

  /**
   * This method returns service detail field included on controller
   *
   * @return
   */
  private FieldMetadata getServiceDetailField(JavaType detailService) {
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(this.controller.getDeclaredByMetadataId());
    final String controllerMetadataKey =
        ControllerMetadata.createIdentifier(this.controller.getType(), logicalPath);
    registerDependency(controllerMetadataKey, metadataIdentificationString);
    final ControllerMetadata controllerMetadata =
        (ControllerMetadata) getMetadataService().get(controllerMetadataKey);

    return controllerMetadata.getServiceDetailField(detailService);
  }

  /**
   * This method returns the final views path to be used
   *
   * @return
   */
  private String getViewsPath() {
    return this.controllerPath.startsWith("/") ? this.controllerPath.substring(1)
        : this.controllerPath;
  }

  /**
   * Returns the value of the mapped by attribute for OneToMany relations only
   * if it matches with the referenced side field name.
   *
   * @param entityFields
   * @param field
   * @param fieldNameOnReferenceSide
   * @return a String with field name on the reference side if matches with
   *         provided field on the owning side.
   */
  private String getMappedByField(List<FieldMetadata> entityFields, FieldMetadata field,
      String fieldNameOnReferenceSide) {
    String fieldName = null;
    for (FieldMetadata entityField : entityFields) {

      if (entityField.getFieldType().equals(field.getFieldType())) {
        AnnotationMetadata oneToManyAnnotation = entityField.getAnnotation(JpaJavaType.ONE_TO_MANY);
        if (oneToManyAnnotation != null
            && oneToManyAnnotation.getAttribute("mappedBy") != null
            && oneToManyAnnotation.getAttribute("mappedBy").getValue()
                .equals(fieldNameOnReferenceSide)) {
          fieldName = entityField.getFieldName().getSymbolName();
          break;
        }
        // TODO: Implement the same for ManyToMany relations
      }
    }
    return fieldName;
  }

  /**
   * This method registers a new type on types to import list and then returns
   * it.
   *
   * @param type
   * @return
   */
  private JavaType addTypeToImport(JavaType type) {
    typesToImport.add(type);
    return type;
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
        LOGGER.warning("Cannot load ControllerMVCService on ThymeleafMetadataProviderImpl.");
        return null;
      }
    } else {
      return controllerMVCService;
    }
  }

  /**
   * This method provides all detail methods using Thymeleaf response type
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

    MethodMetadata listDatatablesDetailMethod = getListDatatablesDetailMethod();
    if (listDatatablesDetailMethod != null) {
      detailMethods.add(listDatatablesDetailMethod);
    }

    return detailMethods;
  }

  /**
   * This method provides detail list method using Thymeleaf response type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getListDetailMethod() {

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
        new JavaSymbolName("list".concat(this.controllerDetailInfo.getEntity().getSimpleTypeName()));

    // Create PageableDefault annotation
    AnnotationMetadataBuilder pageableDefaultAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PAGEABLE_DEFAULT);

    String sortFieldName = "";
    MemberDetails entityDetails =
        getMemberDetails(getTypeLocationService().getTypeDetails(
            this.controllerDetailInfo.getEntity()));
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
    parameterTypes.add(new AnnotatedJavaType(this.controllerDetailInfo.getParentEntity(),
        modelAttributeAnnotation.build()));
    Validate.notNull(this.globalSearchType, "Couldn't find GlobalSearch in project.");
    parameterTypes.add(new AnnotatedJavaType(this.globalSearchType));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.PAGEABLE, pageableDefaultAnnotation
        .build()));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(StringUtils.uncapitalize(this.controllerDetailInfo
        .getParentEntity().getSimpleTypeName())));
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

    // Get finder method
    ClassOrInterfaceTypeDetails serviceDetails =
        getTypeLocationService().getTypeDetails(this.controllerDetailInfo.getService());

    final LogicalPath serviceLogicalPath =
        PhysicalTypeIdentifier.getPath(serviceDetails.getDeclaredByMetadataId());
    final String serviceMetadataKey =
        ServiceMetadata.createIdentifier(serviceDetails.getType(), serviceLogicalPath);
    final ServiceMetadata serviceMetadata =
        (ServiceMetadata) getMetadataService().get(serviceMetadataKey);

    // Get parent field
    FieldMetadata parentRelationField = null;
    MemberDetails memberDetails = getMemberDetails(this.controllerDetailInfo.getParentEntity());
    List<FieldMetadata> parentFields = memberDetails.getFields();
    for (FieldMetadata parentField : parentFields) {
      if (parentField.getFieldName().getSymbolName()
          .equals(this.controllerDetailInfo.getParentReferenceFieldName())) {
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
        "ERROR: '%s' must have a field related to '%s'", this.controllerDetailInfo
            .getParentEntity().getSimpleTypeName(), this.controllerDetailInfo.getEntity()
            .getSimpleTypeName()));

    // Generating returnType
    Map<FieldMetadata, MethodMetadata> referencedFieldsFindAllDefinedMethods =
        serviceMetadata.getReferencedFieldsFindAllDefinedMethods();
    AnnotationAttributeValue<Object> attributeMappedBy =
        parentRelationField.getAnnotation(JpaJavaType.ONE_TO_MANY).getAttribute("mappedBy");

    Validate.notNull(attributeMappedBy, String.format(
        "ERROR: The field '%s' of '%s' must have 'mappedBy' value", parentRelationField
            .getFieldName(), this.controllerDetailInfo.getParentEntity().getSimpleTypeName()));

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
            .uncapitalize(StringUtils.lowerCase(Noun.pluralOf(this.controllerDetailInfo.getEntity()
                .getSimpleTypeName(), Locale.ENGLISH))),
        getServiceDetailField(this.controllerDetailInfo.getService()).getFieldName()
            .getSymbolNameUnCapitalisedFirstLetter(), findByMethod.getMethodName(), StringUtils
            .uncapitalize(this.controllerDetailInfo.getParentEntity().getSimpleTypeName())));

    // return entityrelplural;
    bodyBuilder.appendFormalLine(String.format("return %s;", StringUtils.uncapitalize(StringUtils
        .lowerCase(Noun.pluralOf(this.controllerDetailInfo.getEntity().getSimpleTypeName(),
            Locale.ENGLISH)))));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            returnType, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);
    return methodBuilder.build();
  }

  /**
   * This method provides detail datatables list method using Thymeleaf
   * response type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getListDatatablesDetailMethod() {

    // First of all, check if exists other method with the same
    // @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, "", null, "", "application/vnd.datatables+json",
            "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName =
        new JavaSymbolName("list".concat(this.controllerDetailInfo.getEntity().getSimpleTypeName()));

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    AnnotationMetadataBuilder modelAttributeAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.MODEL_ATTRIBUTE);
    parameterTypes.add(new AnnotatedJavaType(this.controllerDetailInfo.getParentEntity(),
        modelAttributeAnnotation.build()));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(this.globalSearchType));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(this.datatablesPageable));
    AnnotationMetadataBuilder requestParamAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.REQUEST_PARAM);
    requestParamAnnotation.addStringAttribute("value", "draw");
    parameterTypes.add(new AnnotatedJavaType(JavaType.INT_OBJECT, requestParamAnnotation.build()));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName(StringUtils.uncapitalize(this.controllerDetailInfo
        .getParentEntity().getSimpleTypeName())));
    parameterNames.add(new JavaSymbolName("search"));
    parameterNames.add(new JavaSymbolName("pageable"));
    parameterNames.add(new JavaSymbolName("draw"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, "", null, "", "application/vnd.datatables+json", ""));

    // Adding @ResponseBody annotation
    AnnotationMetadataBuilder responseBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(responseBodyAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Page<ENTITYRELNAME> entityrelplural = listENTITYREL(entityName,
    // search, pageable);
    bodyBuilder.appendFormalLine(String.format("%s<%s> %s = list%s(%s, search, pageable);",
        addTypeToImport(SpringJavaType.PAGE).getSimpleTypeName(), this.controllerDetailInfo
            .getEntity().getSimpleTypeName(), StringUtils.lowerCase(Noun.pluralOf(
            this.controllerDetailInfo.getEntity().getSimpleTypeName(), Locale.ENGLISH)),
        this.controllerDetailInfo.getEntity().getSimpleTypeName(), StringUtils
            .uncapitalize(this.controllerDetailInfo.getParentEntity().getSimpleTypeName())));

    // Get finder method
    ClassOrInterfaceTypeDetails serviceDetails =
        getTypeLocationService().getTypeDetails(this.controllerDetailInfo.getService());

    final LogicalPath serviceLogicalPath =
        PhysicalTypeIdentifier.getPath(serviceDetails.getDeclaredByMetadataId());
    final String serviceMetadataKey =
        ServiceMetadata.createIdentifier(serviceDetails.getType(), serviceLogicalPath);
    final ServiceMetadata serviceMetadata =
        (ServiceMetadata) getMetadataService().get(serviceMetadataKey);

    // Get parent field
    FieldMetadata parentRelationField = null;
    MemberDetails memberDetails = getMemberDetails(this.controllerDetailInfo.getParentEntity());
    List<FieldMetadata> parentFields = memberDetails.getFields();
    for (FieldMetadata parentField : parentFields) {
      if (parentField.getFieldName().getSymbolName()
          .equals(this.controllerDetailInfo.getParentReferenceFieldName())) {
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
        "ERROR: '%s' must have a field related to '%s'", this.controllerDetailInfo
            .getParentEntity().getSimpleTypeName(), this.controllerDetailInfo.getEntity()
            .getSimpleTypeName()));

    // Generating returnType
    Map<FieldMetadata, MethodMetadata> referencedCountDefinedMethods =
        serviceMetadata.getCountByReferenceFieldDefinedMethod();
    AnnotationAttributeValue<Object> attributeMappedBy =
        parentRelationField.getAnnotation(JpaJavaType.ONE_TO_MANY).getAttribute("mappedBy");

    Validate.notNull(attributeMappedBy, String.format(
        "ERROR: The field '%s' of '%s' must have 'mappedBy' value", parentRelationField
            .getFieldName(), this.controllerDetailInfo.getParentEntity().getSimpleTypeName()));

    String mappedBy = (String) attributeMappedBy.getValue();
    MethodMetadata searchCountMethod = null;
    Iterator<Entry<FieldMetadata, MethodMetadata>> it =
        referencedCountDefinedMethods.entrySet().iterator();
    while (it.hasNext()) {
      Entry<FieldMetadata, MethodMetadata> countMethod = it.next();
      if (countMethod.getKey().getFieldName().getSymbolName().equals(mappedBy)) {
        searchCountMethod = countMethod.getValue();
        break;
      }
    }

    // long allAvailableEntityRelPlural =
    // entityRelNameService.countByENTITYNAMEPLURALContains(entityName);
    bodyBuilder.appendFormalLine(String.format("long allAvailable%s = %s.%s(%s);", Noun.pluralOf(
        this.controllerDetailInfo.getEntity().getSimpleTypeName(), Locale.ENGLISH),
        getServiceDetailField(this.controllerDetailInfo.getService()).getFieldName()
            .getSymbolNameUnCapitalisedFirstLetter(), searchCountMethod.getMethodName(),
        StringUtils.uncapitalize(this.controllerDetailInfo.getParentEntity().getSimpleTypeName())));

    // return new DatatablesData<ENTITYRELNAME>(entityrelplural,
    // allAvailableENTITYRELNAME, draw);
    bodyBuilder.appendFormalLine(String.format("return new %s<%s>(%s, allAvailable%s, draw);",
        addTypeToImport(this.datatablesDataType).getSimpleTypeName(), this.controllerDetailInfo
            .getEntity().getSimpleTypeName(), StringUtils.lowerCase(Noun.pluralOf(
            this.controllerDetailInfo.getEntity().getSimpleTypeName(), Locale.ENGLISH)), Noun
            .pluralOf(this.controllerDetailInfo.getEntity().getSimpleTypeName(), Locale.ENGLISH)));

    // Generating returnType
    JavaType returnType =
        new JavaType(this.datatablesDataType.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
            Arrays.asList(this.controllerDetailInfo.getEntity()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            returnType, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }
}
