package org.springframework.roo.addon.web.mvc.thymeleaf.addon;

import static org.springframework.roo.model.RooJavaType.ROO_THYMELEAF;

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
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;
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
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
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
    ClassOrInterfaceTypeDetails serviceDetails =
        getTypeLocationService().getTypeDetails(getService());

    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(serviceDetails.getDeclaredByMetadataId());
    final String serviceMetadataKey =
        ServiceMetadata.createIdentifier(serviceDetails.getType(), logicalPath);
    final ServiceMetadata serviceMetadata =
        (ServiceMetadata) getMetadataService().get(serviceMetadataKey);

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

    // Getting methods from related service
    MethodMetadata serviceSaveMethod = serviceMetadata.getSaveMethod();
    MethodMetadata serviceDeleteMethod = serviceMetadata.getDeleteMethod();
    MethodMetadata serviceFindAllGlobalSearchMethod =
        serviceMetadata.getFindAllGlobalSearchMethod();
    MethodMetadata serviceCountMethod = serviceMetadata.getCountMethod();

    return new ThymeleafMetadata(metadataIdentificationString, this.aspectName,
        this.governorPhysicalTypeMetadata, getListFormMethod(),
        getListJSONMethod(serviceFindAllGlobalSearchMethod),
        getListDatatablesJSONMethod(serviceCountMethod), getCreateFormMethod(),
        getCreateMethod(serviceSaveMethod), getEditFormMethod(),
        getUpdateMethod(serviceSaveMethod), getDeleteMethod(serviceDeleteMethod),
        getDeleteJSONMethod(serviceDeleteMethod), getShowMethod(), getDetailsMethods(),
        getPopulateFormMethod(), getPopulateFormatsMethod(), isReadOnly(), typesToImport);
  }

  /**
   * This method provides populateFormats method that allows to configure date time
   * format for every entity
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

    // Check if exists other addDateTimeFormatPatterns method in this controller
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
            // model.addAttribute("field_date_format", DateTimeFormat.patternForStyle("M-", LocaleContextHolder.getLocale()));
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
   * This method provides a list of methods that will be used to list
   * relations between entities.
   *
   * @return
   */
  private List<MethodMetadata> getDetailsMethods() {

    List<MethodMetadata> detailsMethods = new ArrayList<MethodMetadata>();
    List<JavaSymbolName> detailMethodNames = new ArrayList<JavaSymbolName>();

    // Getting all defined services.
    Set<ClassOrInterfaceTypeDetails> allDefinedServices =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_SERVICE);

    // Getting entityDetails
    MemberDetails entityDetails = getMemberDetails(getEntity());

    // Getting all fields defined on current entity
    List<FieldMetadata> entityFields = entityDetails.getFields();

    for (FieldMetadata field : entityFields) {

      // If entity has some Set or List field, maybe is necessary to generate details method
      if (field.getFieldType().getFullyQualifiedTypeName().equals(Set.class.getName())
          || field.getFieldType().getFullyQualifiedTypeName().equals(List.class.getName())) {

        // Getting inner type
        JavaType detailType = field.getFieldType().getBaseType();

        // Check if provided inner type is an entity annotated with @RooJPAEntity
        if (detailType != null
            && getTypeLocationService().getTypeDetails(detailType) != null
            && getTypeLocationService().getTypeDetails(detailType).getAnnotation(
                RooJavaType.ROO_JPA_ENTITY) != null) {

          // Getting service that manages detail type
          for (ClassOrInterfaceTypeDetails detailService : allDefinedServices) {

            AnnotationAttributeValue<JavaType> entityServiceAttr =
                detailService.getAnnotation(RooJavaType.ROO_SERVICE).getAttribute("entity");
            if (entityServiceAttr != null && entityServiceAttr.getValue().equals(detailType)) {

              // Getting count method for current detailType
              MethodMetadata countMethod = null;
              MethodMetadata findAllByReferencedFieldMethod = null;
              final LogicalPath logicalPath =
                  PhysicalTypeIdentifier.getPath(detailService.getDeclaredByMetadataId());
              final String detailServiceMetadataKey =
                  ServiceMetadata.createIdentifier(detailService.getType(), logicalPath);
              final ServiceMetadata detailServiceMetadata =
                  (ServiceMetadata) getMetadataService().get(detailServiceMetadataKey);

              Map<FieldMetadata, MethodMetadata> countMethods =
                  detailServiceMetadata.getCountByReferenceFieldDefinedMethod();

              // First, check if we'll need field mappings
              boolean fieldMappings = false;
              int fieldMappingsCounter = 0;
              for (Entry<FieldMetadata, MethodMetadata> method : countMethods.entrySet()) {
                if (method.getKey().getFieldType().equals(this.entity)) {
                  fieldMappingsCounter++;
                }
              }
              if (fieldMappingsCounter > 1) {
                fieldMappings = true;
              }

              for (Entry<FieldMetadata, MethodMetadata> method : countMethods.entrySet()) {
                if (method.getKey().getFieldType().equals(this.entity)) {
                  countMethod = method.getValue();

                  // Get field name on the reference side
                  String fieldNameOnReferenceSide = method.getKey().getFieldName().getSymbolName();

                  // If referenced entity has more than one field of this type mapped, we need the mappedBy attribute
                  String fieldName = null;
                  if (fieldMappings) {
                    fieldName = getMappedByField(entityFields, field, fieldNameOnReferenceSide);
                  } else {
                    fieldName = field.getFieldName().getSymbolName();
                  }

                  MethodMetadata listDetailsDatatablesMethod =
                      getListDetailsDatatablesMethod(fieldName, detailType,
                          detailService.getType(), countMethod);
                  if (listDetailsDatatablesMethod != null
                      && !detailMethodNames.contains(listDetailsDatatablesMethod.getMethodName())) {
                    detailsMethods.add(listDetailsDatatablesMethod);
                    detailMethodNames.add(listDetailsDatatablesMethod.getMethodName());
                  }
                }
              }

              if (countMethod == null) {
                continue;
              }

              Map<FieldMetadata, MethodMetadata> findAllReferencedFieldMethods =
                  detailServiceMetadata.getReferencedFieldsFindAllDefinedMethods();

              for (Entry<FieldMetadata, MethodMetadata> method : findAllReferencedFieldMethods
                  .entrySet()) {
                if (method.getKey().getFieldType().equals(this.entity)) {
                  findAllByReferencedFieldMethod = method.getValue();

                  // Get field name on the reference side
                  String fieldNameOnReferenceSide = method.getKey().getFieldName().getSymbolName();

                  // If entity has 2 reference fields of same type, we need the mappedBy attribute
                  String fieldName = null;
                  if (fieldMappings) {
                    fieldName = getMappedByField(entityFields, field, fieldNameOnReferenceSide);
                  } else {
                    fieldName = field.getFieldName().getSymbolName();
                  }

                  MethodMetadata listDetailsMethod =
                      getListDetailsMethod(fieldName, detailType, detailService.getType(),
                          findAllByReferencedFieldMethod);
                  if (listDetailsMethod != null
                      && !detailMethodNames.contains(listDetailsMethod.getMethodName())) {
                    detailsMethods.add(listDetailsMethod);
                    detailMethodNames.add(listDetailsMethod.getMethodName());
                  }
                }
              }

              if (findAllByReferencedFieldMethod == null) {
                continue;
              }
            }
          }
        }
      }
    }

    return detailsMethods;
  }

  /**
   * This method generates listDetailsMethod that will be used
   * to display relations between entities
   *
   * @param fieldName
   * @param detailType
   * @param detailService
   * @param findAllByReferencedFieldMethod
   * @return
   */
  private MethodMetadata getListDetailsMethod(String fieldName, JavaType detailType,
      JavaType detailService, MethodMetadata findAllByReferencedFieldMethod) {

    // Calculate method path value
    // Getting identifier Fields
    List<FieldMetadata> identifierFields =
        getPersistenceMemberLocator().getIdentifierFields(detailType);
    if (identifierFields.isEmpty()) {
      return null;
    }


    String listDetailsPath =
        String.format("/{%s}/%s/", identifierFields.get(0).getFieldName().getSymbolName(),
            fieldName.toLowerCase());

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(this.controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, listDetailsPath, null, null,
            SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName =
        new JavaSymbolName(String.format("list%s", StringUtils.capitalize(fieldName)));

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    AnnotationMetadataBuilder pathVariableAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVariableAnnotation.addStringAttribute("value", "id");
    parameterTypes.add(new AnnotatedJavaType(entity, pathVariableAnnotation.build()));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(this.globalSearchType));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.PAGEABLE));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("id"));
    parameterNames.add(new JavaSymbolName("search"));
    parameterNames.add(new JavaSymbolName("pageable"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, listDetailsPath, null, null,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE, ""));

    // Adding @ResponseBody annotation
    AnnotationMetadataBuilder responseBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(responseBodyAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Page<Entity> entityField = detailService.FIND_ALL_METHOD(id, search, pageable);
    bodyBuilder.appendFormalLine(String.format("%s<%s> %s = %s.%s(id, search, pageable);",
        addTypeToImport(SpringJavaType.PAGE).getSimpleTypeName(), addTypeToImport(detailType)
            .getSimpleTypeName(), getDetailEntityField(detailType).getFieldName().getSymbolName(),
        getServiceDetailField(detailService).getFieldName().getSymbolName(),
        findAllByReferencedFieldMethod.getMethodName().getSymbolName()));

    // return entityField;
    bodyBuilder.appendFormalLine(String.format("return %s;", getDetailEntityField(detailType)
        .getFieldName()));


    // Generating returnType
    JavaType returnType =
        new JavaType(SpringJavaType.PAGE.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
            Arrays.asList(detailType));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            returnType, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method generates listDetailsDatatablesMethod that will be used
   * to display relations between entities using DTT component
   *
   * @param fieldName
   * @param detailType
   * @param detailService
   * @param countMethod
   * @return
   */
  private MethodMetadata getListDetailsDatatablesMethod(String fieldName, JavaType detailType,
      JavaType detailService, MethodMetadata countMethod) {

    // Calculate method path value
    // Getting identifier Fields
    List<FieldMetadata> identifierFields =
        getPersistenceMemberLocator().getIdentifierFields(detailType);
    if (identifierFields.isEmpty()) {
      return null;
    }

    MethodMetadata identifierAccessor =
        getPersistenceMemberLocator().getIdentifierAccessor(detailType);

    String listDetailsPath =
        String.format("/{%s}/%s/", identifierFields.get(0).getFieldName().getSymbolName(),
            fieldName.toLowerCase());

    // First of all, check if exists other method with the same @RequesMapping to generate
    MethodMetadata existingMVCMethod =
        getControllerMVCService().getMVCMethodByRequestMapping(this.controller.getType(),
            SpringEnumDetails.REQUEST_METHOD_GET, listDetailsPath, null, null,
            "application/vnd.datatables+json", "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName =
        new JavaSymbolName(String.format("list%sDetail", StringUtils.capitalize(fieldName)));

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    AnnotationMetadataBuilder pathVariableAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVariableAnnotation.addStringAttribute("value", "id");
    parameterTypes.add(new AnnotatedJavaType(entity, pathVariableAnnotation.build()));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(this.globalSearchType));
    parameterTypes.add(AnnotatedJavaType.convertFromJavaType(SpringJavaType.PAGEABLE));
    AnnotationMetadataBuilder requestParamAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.REQUEST_PARAM);
    requestParamAnnotation.addStringAttribute("value", "draw");
    parameterTypes.add(new AnnotatedJavaType(JavaType.INT_OBJECT, requestParamAnnotation.build()));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("id"));
    parameterNames.add(new JavaSymbolName("search"));
    parameterNames.add(new JavaSymbolName("pageable"));
    parameterNames.add(new JavaSymbolName("draw"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET, listDetailsPath, null, null,
        "application/vnd.datatables+json", ""));

    // Adding @ResponseBody annotation
    AnnotationMetadataBuilder responseBodyAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY);
    annotations.add(responseBodyAnnotation);

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // Page<Entity> entityField = listEntity(id, search, pageable);
    bodyBuilder.appendFormalLine(String.format("%s<%s> %s = list%s(id, search, pageable);",
        addTypeToImport(SpringJavaType.PAGE).getSimpleTypeName(), addTypeToImport(detailType)
            .getSimpleTypeName(), getDetailEntityField(detailType).getFieldName().getSymbolName(),
        StringUtils.capitalize(fieldName)));

    // long allAvailableEntityDetails = detailService.countByMasterId(id.getId());
    bodyBuilder.appendFormalLine(String.format("long allAvailable%sDetails = %s.%s(%s.%s());",
        detailType.getSimpleTypeName(), getServiceDetailField(detailService).getFieldName()
            .getSymbolName(), countMethod.getMethodName().getSymbolName(), identifierFields.get(0)
            .getFieldName().getSymbolName(), identifierAccessor.getMethodName()));

    // return new DatatablesData<OrderDetailInfo>(orderDetails, allAvailableOrderDetails, draw);
    bodyBuilder.appendFormalLine(String.format(
        "return new %s<%s>(%s, allAvailable%sDetails, draw);", addTypeToImport(datatablesDataType)
            .getSimpleTypeName(), addTypeToImport(detailType).getSimpleTypeName(),
        getDetailEntityField(detailType).getFieldName().getSymbolName(), detailType
            .getSimpleTypeName()));


    // Generating returnType
    JavaType returnType =
        new JavaType(datatablesDataType.getFullyQualifiedTypeName(), 0, DataType.TYPE, null,
            Arrays.asList(detailType));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            returnType, parameterTypes, parameterNames, bodyBuilder);
    methodBuilder.setAnnotations(annotations);

    return methodBuilder.build();
  }

  /**
   * This method provides the "list" JSON method using JSON
   * response type and returns Page element
   *
   * @param serviceFindAllGlobalSearchMethod
   *
   * @return MethodMetadata
   */
  private MethodMetadata getListJSONMethod(MethodMetadata serviceFindAllGlobalSearchMethod) {

    // First of all, check if exists other method with the same @RequesMapping to generate
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

    // Page<Entity> entityField = serviceField.findAll(search, pageable);
    bodyBuilder.appendFormalLine(String.format("%s<%s> %s = %s.%s(search, pageable);",
        addTypeToImport(SpringJavaType.PAGE).getSimpleTypeName(), addTypeToImport(this.entity)
            .getSimpleTypeName(), getEntityField().getFieldName(),
        getServiceField().getFieldName(), serviceFindAllGlobalSearchMethod.getMethodName()));

    // return entityField;
    bodyBuilder.appendFormalLine(String.format("return %s;", getEntityField().getFieldName()));


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
   * This method provides the "list" Datatables JSON method  using JSON
   * response type and returns Datatables element
   *
   * @param serviceCountMethod
   *
   * @return MethodMetadata
   */
  private MethodMetadata getListDatatablesJSONMethod(MethodMetadata serviceCountMethod) {

    // First of all, check if exists other method with the same @RequesMapping to generate
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

    // Page<Entity> entityField = list(search, pageable);
    bodyBuilder.appendFormalLine(String.format("%s<%s> %s = list(search, pageable);",
        addTypeToImport(SpringJavaType.PAGE).getSimpleTypeName(), addTypeToImport(this.entity)
            .getSimpleTypeName(), getEntityField().getFieldName()));

    // long allAvailableEntity = serviceField.count();
    bodyBuilder.appendFormalLine(String.format("long allAvailable%s = %s.%s();",
        this.entity.getSimpleTypeName(), getServiceField().getFieldName(),
        serviceCountMethod.getMethodName()));

    // return new DatatablesData<Entity>(entityField, allAvailableEntity, draw);
    bodyBuilder.appendFormalLine(String.format("return new %s<%s>(%s, allAvailable%s, draw);",
        addTypeToImport(this.datatablesDataType).getSimpleTypeName(),
        this.entity.getSimpleTypeName(), getEntityField().getFieldName(),
        this.entity.getSimpleTypeName()));

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
   * This method provides the "list" form method  using Thymeleaf view
   * response type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getListFormMethod() {

    // First of all, check if exists other method with the same @RequesMapping to generate
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
   * This method provides the "create" form method  using Thymeleaf view
   * response type
   *
   * @return MethodMetadata
   */
  private MethodMetadata getCreateFormMethod() {

    // First of all, check if exists other method with the same @RequesMapping to generate
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
            String.format("/{%s}/edit-form", getEntityField().getFieldName()), null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("editForm");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    final List<AnnotationAttributeValue<?>> annotationAttributes =
        new ArrayList<AnnotationAttributeValue<?>>();
    annotationAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), getEntityField()
        .getFieldName().getSymbolName()));
    parameterTypes.add(new AnnotatedJavaType(this.entity, new AnnotationMetadataBuilder(
        SpringJavaType.PATH_VARIABLE, annotationAttributes).build()));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(getEntityField().getFieldName());
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET,
        String.format("/{%s}/edit-form", getEntityField().getFieldName()), null, null,
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
            String.format("/{%s}", getEntityField().getFieldName()), null, null,
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
        SpringEnumDetails.REQUEST_METHOD_PUT,
        String.format("/{%s}", getEntityField().getFieldName()), null, null,
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
            SpringEnumDetails.REQUEST_METHOD_DELETE, "/{id}", null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
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
        SpringEnumDetails.REQUEST_METHOD_DELETE, "/{id}", null, null,
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
   * This method provides the "delete" method using JSPON response type
   *
   * @param serviceDeleteMethod
   *
   * @return MethodMetadata
   */
  private MethodMetadata getDeleteJSONMethod(MethodMetadata serviceDeleteMethod) {

    // First of all, check if exists other method with the same @RequesMapping to generate
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

    AnnotationMetadataBuilder pathVariableAnnotation =
        new AnnotationMetadataBuilder(SpringJavaType.PATH_VARIABLE);
    pathVariableAnnotation.addStringAttribute("value", "id");

    parameterTypes.add(new AnnotatedJavaType(this.identifierType, pathVariableAnnotation.build()));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(new JavaSymbolName("id"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_DELETE, "/{id}", null, null,
        SpringEnumDetails.MEDIA_TYPE_APPLICATION_JSON_VALUE, ""));

    // Adding @ResponseBody annotation
    annotations.add(new AnnotationMetadataBuilder(SpringJavaType.RESPONSE_BODY));

    // Generate body
    InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

    // entityService.DELETE_METHOD(id);
    bodyBuilder.appendFormalLine(String.format("%s.%s(id);", getServiceField().getFieldName(),
        serviceDeleteMethod.getMethodName()));

    // return new ResponseEntity(HttpStatus.OK);
    bodyBuilder.appendFormalLine(String.format("return new ResponseEntity(%s.OK);",
        addTypeToImport(SpringJavaType.HTTP_STATUS).getSimpleTypeName()));

    MethodMetadataBuilder methodBuilder =
        new MethodMetadataBuilder(this.metadataIdentificationString, Modifier.PUBLIC, methodName,
            SpringJavaType.RESPONSE_ENTITY, parameterTypes, parameterNames, bodyBuilder);
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
            String.format("/{%s}", getEntityField().getFieldName()), null, null,
            SpringEnumDetails.MEDIA_TYPE_TEXT_HTML_VALUE.toString(), "");
    if (existingMVCMethod != null
        && !existingMVCMethod.getDeclaredByMetadataId().equals(this.metadataIdentificationString)) {
      return existingMVCMethod;
    }

    // Define methodName
    final JavaSymbolName methodName = new JavaSymbolName("show");

    List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
    final List<AnnotationAttributeValue<?>> annotationAttributes =
        new ArrayList<AnnotationAttributeValue<?>>();
    annotationAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), getEntityField()
        .getFieldName().getSymbolName()));
    parameterTypes.add(new AnnotatedJavaType(this.entity, new AnnotationMetadataBuilder(
        SpringJavaType.PATH_VARIABLE, annotationAttributes).build()));
    parameterTypes.add(new AnnotatedJavaType(SpringJavaType.MODEL));

    final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
    parameterNames.add(getEntityField().getFieldName());
    parameterNames.add(new JavaSymbolName("model"));

    // Adding annotations
    final List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

    // Adding @RequestMapping annotation
    annotations.add(getControllerMVCService().getRequestMappingAnnotation(
        SpringEnumDetails.REQUEST_METHOD_GET,
        String.format("/{%s}", getEntityField().getFieldName()), null, null,
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
        // model.addAttribute("enumField", Arrays.asList(Enum.values()));
        bodyBuilder.appendFormalLine(String.format(
            "model.addAttribute(\"%s\", %s.asList(%s.values()));",
            Noun.pluralOf(field.getFieldName().getSymbolName(), Locale.ENGLISH),
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
   * Returns the value of the mapped by attribute for OneToMany relations
   * only if it matches with the referenced side field name.
   *
   * @param entityFields
   * @param field
   * @param fieldNameOnReferenceSide
   * @return a String with field name on the reference side if matches
   * with provided field on the owning side.
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
}
