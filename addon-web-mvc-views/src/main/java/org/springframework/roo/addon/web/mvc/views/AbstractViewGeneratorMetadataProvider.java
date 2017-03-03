package org.springframework.roo.addon.web.mvc.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.javabean.addon.JavaBeanMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.jpa.annotations.entity.JpaRelationType;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.FinderOperations;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.FinderOperationsImpl;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.addon.plural.addon.PluralService;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerAnnotationValues;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerLocator;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;
import org.springframework.roo.addon.web.mvc.controller.addon.DetailAnnotationValues;
import org.springframework.roo.addon.web.mvc.controller.addon.RelationInfoExtended;
import org.springframework.roo.addon.web.mvc.controller.addon.finder.SearchAnnotationValues;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.addon.web.mvc.i18n.I18nOperations;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.propfiles.manager.PropFilesManagerService;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

/**
 * This abstract class will be extended by MetadataProviders focused on view
 * generation. As a result, it will be possible that all MetadataProviders that
 * manages view generation follows the same steps and the same operations to do
 * it.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
@Component(componentAbstract = true)
public abstract class AbstractViewGeneratorMetadataProvider<T extends AbstractViewMetadata> extends
    AbstractMemberDiscoveringItdMetadataProvider {

  protected ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  private final List<JavaType> STANDAR_TYPES = Arrays.asList(JavaType.BOOLEAN_OBJECT,
      JavaType.STRING, JavaType.LONG_OBJECT, JavaType.INT_OBJECT, JavaType.FLOAT_OBJECT,
      JavaType.DOUBLE_OBJECT);

  private final List<JavaType> DATE_TIME_TYPES = Arrays.asList(JdkJavaType.DATE,
      JdkJavaType.CALENDAR);

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
    serviceInstaceManager.activate(this.context);
  }

  /**
   * This operation returns the MVCViewGenerationService that should be used to
   * generate views. Implements this operations in you views metadata providers
   * to be able to generate all necessary views.
   *
   * @return MVCViewGenerationService
   */
  protected abstract MVCViewGenerationService<T> getViewGenerationService();

  /**
   * This operations returns the necessary Metadata that will generate .aj file.
   * This operation is called from getMetadata operation to obtain the return
   * element.
   * 
   * @param itemController
   * @param compositionRelationOneToOne
   * @param entityIdentifierPlural
   * @param entityPlural
   * @param entityMetadata
   * @param serviceMetadata
   * @param controllerMetadata
   * @param governorPhysicalTypeMetadata
   * @param aspectName
   * @param metadataIdentificationString
   * @param collectionController
   * @param findersToAdd
   * @param formBeansEnumFields
   * @param formBeansDateTimeFields
   * @param detailItemController
   * @param detailCollectionController
   * @return ItdTypeDetailsProvidingMetadataItem
   */
  protected abstract T createMetadataInstance(String metadataIdentificationString,
      JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata,
      ControllerMetadata controllerMetadata, ServiceMetadata serviceMetadata,
      JpaEntityMetadata entityMetadata, String entityPlural, String entityIdentifierPlural,
      List<Pair<RelationInfo, JpaEntityMetadata>> compositionRelationOneToOne,
      JavaType itemController, JavaType collectionController, List<FieldMetadata> dateTimeFields,
      List<FieldMetadata> enumFields, Map<String, MethodMetadata> findersToAdd,
      Map<JavaType, List<FieldMetadata>> formBeansDateTimeFields,
      Map<JavaType, List<FieldMetadata>> formBeansEnumFields, JavaType detailsItemController,
      JavaType detailCollectionController);

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {

    // Use provided MVCViewGenerationService to generate views
    MVCViewGenerationService<T> viewGenerationService = getViewGenerationService();

    ClassOrInterfaceTypeDetails controllerDetail =
        governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();

    // Getting controller metadata
    final String controllerMetadataKey = ControllerMetadata.createIdentifier(controllerDetail);
    final ControllerMetadata controllerMetadata =
        (ControllerMetadata) getMetadataService().get(controllerMetadataKey);
    if (controllerMetadata == null) {
      return null;
    }

    // Getting entity and check if is a readOnly entity or not
    final JavaType entity = controllerMetadata.getEntity();

    JavaType viewType = viewGenerationService.getType();
    JavaType itemController = null;
    JavaType collectionController = null;
    final JavaPackage controllerPackage = controllerDetail.getType().getPackage();
    final String pathPrefix = controllerMetadata.getAnnotationValues().getPathPrefix();
    if (controllerMetadata.getType() != ControllerType.ITEM) {
      // Locate ItemController
      Collection<ClassOrInterfaceTypeDetails> itemControllers =
          getControllerLocator().getControllers(entity, ControllerType.ITEM, viewType);

      if (itemControllers.isEmpty()) {
        // We can't create metadata "Jet"
        return null;
      } else {
        // Get controller with the same package
        itemController =
            filterControllerByPackageAndPrefix(itemControllers, controllerPackage, pathPrefix);
        Validate.notNull(itemControllers,
            "ERROR: Can't find ITEM-type controller related to controller '%s'", controllerDetail
                .getType().getFullyQualifiedTypeName());
      }
    }

    if (controllerMetadata.getType() != ControllerType.COLLECTION) {
      // Locate ItemController
      Collection<ClassOrInterfaceTypeDetails> collectionControllers =
          getControllerLocator().getControllers(entity, ControllerType.COLLECTION, viewType);

      // Get controller with the same package
      collectionController =
          filterControllerByPackageAndPrefix(collectionControllers, controllerPackage, pathPrefix);
      Validate.notNull(collectionController,
          "ERROR: Can't find Collection-type controller related to controller '%s'",
          controllerDetail.getType().getFullyQualifiedTypeName());
    }

    JavaType detailItemController = null;
    JavaType detailCollectionController = null;
    if (controllerMetadata.getType() == ControllerType.DETAIL) {
      if (controllerMetadata.getLastDetailsInfo().type == JpaRelationType.AGGREGATION) {
        // Locate controller item which handles details elements
        JavaType detailEntity = controllerMetadata.getLastDetailEntity();
        Collection<ClassOrInterfaceTypeDetails> detailsControllersToCheck =
            getControllerLocator().getControllers(detailEntity, ControllerType.ITEM, viewType);

        for (ClassOrInterfaceTypeDetails controller : detailsControllersToCheck) {
          if (controllerPackage.equals(controller.getType().getPackage())) {
            detailItemController = controller.getType();
            break;
          }
        }
        Validate
            .notNull(
                detailItemController,
                "ERROR: Can't find Item-type controller for details entity '%s' related to controller '%s'",
                detailEntity.getFullyQualifiedTypeName(), controllerDetail.getType()
                    .getFullyQualifiedTypeName());

        // Locate controller collection which handles details elements
        detailsControllersToCheck =
            getControllerLocator()
                .getControllers(detailEntity, ControllerType.COLLECTION, viewType);

        for (ClassOrInterfaceTypeDetails controller : detailsControllersToCheck) {
          if (controllerPackage.equals(controller.getType().getPackage())) {
            detailCollectionController = controller.getType();
            break;
          }
        }
        Validate
            .notNull(
                detailItemController,
                "ERROR: Can't find Collection-type controller for details entity '%s' related to controller '%s'",
                detailEntity.getFullyQualifiedTypeName(), controllerDetail.getType()
                    .getFullyQualifiedTypeName());

      } else {
        // ** COMPOSITION **

        // Locate controller item which handles details elements
        Collection<ClassOrInterfaceTypeDetails> detailsControllersToCheck =
            getControllerLocator().getControllers(entity, ControllerType.DETAIL_ITEM, viewType);

        for (ClassOrInterfaceTypeDetails controller : detailsControllersToCheck) {
          if (controllerPackage.equals(controller.getType().getPackage())) {
            DetailAnnotationValues annotationValues = new DetailAnnotationValues(controller);
            if (controllerMetadata.getDetailAnnotaionFieldValue().equals(
                annotationValues.getRelationField())) {
              detailItemController = controller.getType();
            }
            break;
          }
        }
        Validate
            .notNull(
                detailItemController,
                "ERROR: Can't find Detail-Item-type controller for details entity '%s' related to controller '%s' (relation '%s')",
                entity.getFullyQualifiedTypeName(), controllerDetail.getType()
                    .getFullyQualifiedTypeName(), controllerMetadata.getDetailAnnotaionFieldValue());

      }
    }
    if (controllerMetadata.getType() == ControllerType.DETAIL_ITEM) {
      // Locate controller item which handles details elements
      Collection<ClassOrInterfaceTypeDetails> detailsControllersToCheck =
          getControllerLocator().getControllers(entity, ControllerType.DETAIL, viewType);

      for (ClassOrInterfaceTypeDetails controller : detailsControllersToCheck) {
        if (controllerPackage.equals(controller.getType().getPackage())) {
          DetailAnnotationValues annotationValues = new DetailAnnotationValues(controller);
          if (controllerMetadata.getDetailAnnotaionFieldValue().equals(
              annotationValues.getRelationField())) {
            detailCollectionController = controller.getType();
          }
          break;
        }
      }
      Validate
          .notNull(
              detailCollectionController,
              "ERROR: Can't find Detail-type controller for details entity '%s' related to controller '%s' (relation '%s')",
              entity.getFullyQualifiedTypeName(), controllerDetail.getType()
                  .getFullyQualifiedTypeName(), controllerMetadata.getDetailAnnotaionFieldValue());
    }

    Validate.notNull(entity, "ERROR: You should provide a valid entity for controller '%s'",
        controllerDetail.getType().getFullyQualifiedTypeName());

    final ClassOrInterfaceTypeDetails entityDetails =
        getTypeLocationService().getTypeDetails(entity);

    Validate.notNull(entityDetails, "ERROR: Can't load details of %s",
        entity.getFullyQualifiedTypeName());

    final JpaEntityMetadata entityMetadata =
        getMetadataService().get(JpaEntityMetadata.createIdentifier(entityDetails));

    Validate.notNull(entityMetadata, "ERROR: Can't get Jpa Entity metada of %s",
        entity.getFullyQualifiedTypeName());

    MemberDetails entityMemberDetails = getMemberDetails(entityDetails);

    // Get entity plural
    final String entityPlural = getPluralService().getPlural(entity);

    final String entityIdentifierPlural =
        getPluralService().getPlural(entityMetadata.getCurrentIndentifierField().getFieldName());

    // Getting service and its metadata
    final JavaType service = controllerMetadata.getService();

    ClassOrInterfaceTypeDetails serviceDetails = getTypeLocationService().getTypeDetails(service);

    final String serviceMetadataKey = ServiceMetadata.createIdentifier(serviceDetails);
    registerDependency(serviceMetadataKey, metadataIdentificationString);

    final ServiceMetadata serviceMetadata = getMetadataService().get(serviceMetadataKey);

    // Prepare information about ONE-TO-ONE relations
    final List<Pair<RelationInfo, JpaEntityMetadata>> compositionRelationOneToOne =
        new ArrayList<Pair<RelationInfo, JpaEntityMetadata>>();
    ClassOrInterfaceTypeDetails childEntityDetails;
    JpaEntityMetadata childEntityMetadata;
    for (RelationInfo info : entityMetadata.getRelationInfos().values()) {
      if (info.cardinality == Cardinality.ONE_TO_ONE) {
        childEntityDetails = getTypeLocationService().getTypeDetails(info.childType);
        childEntityMetadata =
            getMetadataService().get(JpaEntityMetadata.createIdentifier(childEntityDetails));
        compositionRelationOneToOne.add(Pair.of(info, childEntityMetadata));
      }
    }

    List<FieldMetadata> dateTimeFields;
    List<FieldMetadata> enumFields;
    if (controllerMetadata.getType() == ControllerType.DETAIL
        || controllerMetadata.getType() == ControllerType.DETAIL_ITEM) {

      ClassOrInterfaceTypeDetails childCid =
          getTypeLocationService().getTypeDetails(controllerMetadata.getLastDetailEntity());
      MemberDetails childMemberDetails = getMemberDetails(childCid);
      dateTimeFields = getDateTimeFields(childMemberDetails);
      enumFields = getEnumFields(childMemberDetails);

    } else {
      dateTimeFields = getDateTimeFields(entityMemberDetails);
      enumFields = getEnumFields(entityMemberDetails);
    }

    Map<String, MethodMetadata> findersToAdd = new HashMap<String, MethodMetadata>();
    Map<JavaType, List<FieldMetadata>> formBeansDateTimeFields =
        new HashMap<JavaType, List<FieldMetadata>>();
    Map<JavaType, List<FieldMetadata>> formBeansEnumFields =
        new HashMap<JavaType, List<FieldMetadata>>();

    // Getting annotated finders
    final SearchAnnotationValues searchAnnotationValues =
        new SearchAnnotationValues(governorPhysicalTypeMetadata);

    // Add finders only if controller is of search type
    Map<String, JavaType> finderReturnTypes = new HashMap<String, JavaType>();
    Map<String, JavaType> finderFormBeans = new HashMap<String, JavaType>();
    if (controllerMetadata.getType() == ControllerType.SEARCH && searchAnnotationValues != null
        && searchAnnotationValues.getFinders() != null) {
      List<String> finders =
          new ArrayList<String>(Arrays.asList(searchAnnotationValues.getFinders()));

      // Search indicated finders in its related service
      for (MethodMetadata serviceFinder : serviceMetadata.getFinders()) {
        String finderName = serviceFinder.getMethodName().getSymbolName();
        if (finders.contains(finderName)) {
          findersToAdd.put(finderName, serviceFinder);

          // FormBean parameters is always the first finder parameter
          JavaType formBean = serviceFinder.getParameterTypes().get(0).getJavaType();

          // Save the associated formBean to the current finder
          finderFormBeans.put(finderName, formBean);

          // Getting the returnType for this finder
          JavaType returnType = serviceFinder.getReturnType();

          // Save the associated returnType to the current finder
          finderReturnTypes.put(finderName, returnType);

          // Get dateTime and Enum of formBean
          MemberDetails formBeanDetails = getMemberDetails(formBean);

          List<FieldMetadata> formBeanDateTimeFields = getDateTimeFields(formBeanDetails);
          List<FieldMetadata> formBeanEnumFields = getEnumFields(formBeanDetails);

          if (!formBeanDateTimeFields.isEmpty()) {
            formBeansDateTimeFields.put(formBean, formBeanDateTimeFields);
          }

          if (!formBeanEnumFields.isEmpty()) {
            formBeansEnumFields.put(formBean, formBeanEnumFields);
          }

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

          finders.remove(finderName);
        }
      }

      // Check all finders have its service method
      if (!finders.isEmpty()) {
        throw new IllegalArgumentException(String.format(
            "ERROR: Service %s does not have these finder methods: %s ",
            service.getFullyQualifiedTypeName(), StringUtils.join(finders, ", ")));
      }
    }

    T viewMetadata =
        createMetadataInstance(metadataIdentificationString, aspectName,
            governorPhysicalTypeMetadata, controllerMetadata, serviceMetadata, entityMetadata,
            entityPlural, entityIdentifierPlural, compositionRelationOneToOne, itemController,
            collectionController, dateTimeFields, enumFields, findersToAdd,
            formBeansDateTimeFields, formBeansEnumFields, detailItemController,
            detailCollectionController);

    // Fill view context
    ViewContext ctx =
        viewGenerationService.createViewContext(controllerMetadata, entity, entityMetadata,
            viewMetadata);

    // Checking if Spring Security has been installed
    if (getProjectOperations().isFeatureInstalled(FeatureNames.SECURITY)) {
      ctx.setSecurityEnabled(true);
    }

    final String module = controllerDetail.getType().getModule();

    switch (controllerMetadata.getType()) {
      case COLLECTION:

        // Obtain the details controllers to use only them that includes "list" value in the
        // views parameter of @RooDetail annotation. If @RooDetail doesn't include views 
        // parameter, include it.
        List<T> detailsControllersForListView =
            getDetailsControllers(controllerMetadata, controllerPackage, entity, viewType, "list");

        // Add list view
        viewGenerationService.addListView(module, entityMetadata, entityMemberDetails,
            detailsControllersForListView, ctx);
        if (!entityMetadata.isReadOnly()) {
          // If not readOnly, add create view
          viewGenerationService.addCreateView(module, entityMetadata, entityMemberDetails, ctx);
        }

        break;
      case ITEM:

        // Obtain the details controllers to use only them that includes "show" value in the
        // views parameter of @RooDetail annotation.
        List<T> detailsControllersForShowView =
            getDetailsControllers(controllerMetadata, controllerPackage, entity, viewType, "show");

        // Add show view
        viewGenerationService.addShowView(module, entityMetadata, entityMemberDetails,
            detailsControllersForShowView, ctx);

        // Add showInline view
        viewGenerationService.addShowInlineView(module, entityMetadata, entityMemberDetails, ctx);

        if (!entityMetadata.isReadOnly()) {
          // If not readOnly, add update view
          viewGenerationService.addUpdateView(module, entityMetadata, entityMemberDetails, ctx);
        }
        break;
      case DETAIL:
        viewGenerationService.addDetailsViews(module, entityMetadata, entityMemberDetails,
            controllerMetadata, viewMetadata, ctx);

        // Add this metadata as upstream dependency for parent controllers
        // for updating views of parent controllers
        JavaType parentEntity = entityMetadata.getAnnotatedEntity();
        List<ClassOrInterfaceTypeDetails> parentControllers =
            new ArrayList<ClassOrInterfaceTypeDetails>();
        parentControllers.addAll(getControllerLocator().getControllers(parentEntity,
            ControllerType.COLLECTION, viewType));
        parentControllers.addAll(getControllerLocator().getControllers(parentEntity,
            ControllerType.ITEM, viewType));
        parentControllers.addAll(getControllerLocator().getControllers(parentEntity,
            ControllerType.SEARCH, viewType));
        for (ClassOrInterfaceTypeDetails parentController : parentControllers) {
          String viewMetadatIdentifier = createLocalIdentifier(parentController);
          registerDependency(metadataIdentificationString, viewMetadatIdentifier);
        }

        break;

      case DETAIL_ITEM:
        viewGenerationService.addDetailsItemViews(module, entityMetadata, entityMemberDetails,
            controllerMetadata, viewMetadata, ctx);

        RelationInfoExtended last = controllerMetadata.getLastDetailsInfo();
        ClassOrInterfaceTypeDetails childCid =
            getTypeLocationService().getTypeDetails(last.childType);

        MemberDetails detailMemberDetails = getMemberDetails(childCid);

        // Update i18n labels of detail entity
        Map<String, String> labels =
            viewGenerationService.getI18nLabels(detailMemberDetails, last.childType,
                last.childEntityMetadata, controllerMetadata, module, ctx);
        getI18nOperations().addOrUpdateLabels(module, labels);
        break;

      case SEARCH:
        // Check if this search controller have finders included
        // in @RooSearch annotation
        if (searchAnnotationValues != null && searchAnnotationValues.getFinders() != null) {
          List<String> finders =
              new ArrayList<String>(Arrays.asList(searchAnnotationValues.getFinders()));
          // Generating views for all finders
          for (String finderName : finders) {

            // Getting the formBean for this finder
            JavaType formBean = finderFormBeans.get(finderName);
            viewGenerationService.addFinderFormView(module, entityMetadata, viewMetadata, formBean,
                finderName, ctx);

            // Getting the returnType for this finder
            JavaType returnType = finderReturnTypes.get(finderName);
            if (!returnType.getParameters().isEmpty()) {
              returnType = returnType.getParameters().get(0);
            }

            // Obtain the details controllers to use only them that includes this finder value in the
            // views parameter of @RooDetail annotation.
            List<T> detailsControllersForFinderListView =
                getDetailsControllers(controllerMetadata, controllerPackage, entity, viewType,
                    finderName);

            viewGenerationService.addFinderListView(module, entityMetadata, entityMemberDetails,
                viewMetadata, formBean, returnType, finderName,
                detailsControllersForFinderListView, ctx);
          }
        }
        break;
      default:
        throw new IllegalArgumentException();
    }

    // Update menu view every time that new controller has been modified
    // TODO: Maybe, instead of modify all menu view, only new generated
    // controller should
    // be included on it. Must be fixed on future versions.
    viewGenerationService.updateMenuView(module, ctx);

    // Update i18n labels
    Map<String, String> labels =
        viewGenerationService.getI18nLabels(entityMemberDetails, entity, entityMetadata,
            controllerMetadata, module, ctx);
    getI18nOperations().addOrUpdateLabels(module, labels);

    // Add labels for child composite entity as well
    for (Pair<RelationInfo, JpaEntityMetadata> compositionRelation : compositionRelationOneToOne) {
      MemberDetails childMemberDetails =
          getMemberDetailsScanner().getMemberDetails(this.getClass().getName(),
              getTypeLocationService().getTypeDetails(compositionRelation.getKey().childType));
      Map<String, String> i18nLabels =
          viewGenerationService.getI18nLabels(childMemberDetails,
              compositionRelation.getKey().childType, compositionRelation.getValue(), null, module,
              ctx);
      getI18nOperations().addOrUpdateLabels(module, i18nLabels);
    }

    // Register dependency between JavaBeanMetadata and this one
    final String javaBeanMetadataKey = JavaBeanMetadata.createIdentifier(entityDetails);
    registerDependency(javaBeanMetadataKey, metadataIdentificationString);

    // Register dependency between JpaEntityMetadata and this one
    final String jpaEntityMetadataKey = JpaEntityMetadata.createIdentifier(entityDetails);
    registerDependency(jpaEntityMetadataKey, metadataIdentificationString);

    return viewMetadata;
  }

  /**
   * This method obtains the details controller that are available for an specific
   * view type.
   * 
   * @param controllerMetadata
   * @param controllerPackage
   * @param entity
   * @param responseType
   * @param viewType
   * @return
   */
  private List<T> getDetailsControllers(ControllerMetadata controllerMetadata,
      JavaPackage controllerPackage, JavaType entity, JavaType responseType, String viewType) {
    // Locate detail controllers related with this entity
    List<T> detailsControllers = new ArrayList<T>();
    Collection<ClassOrInterfaceTypeDetails> detailsControllersToCheck =
        getControllerLocator().getControllers(entity, ControllerType.DETAIL, responseType);
    List<T> found = new ArrayList<T>();
    for (ClassOrInterfaceTypeDetails controller : detailsControllersToCheck) {
      if (controllerPackage.equals(controller.getType().getPackage())) {
        // Get view metadata
        T detailViewMetadata = getMetadataService().get(createLocalIdentifier(controller));
        if (detailViewMetadata == null) {
          // Can't generate metadata YET!!
          return null;
        }

        // Check if the obtained detail controller should be included in this type view
        DetailAnnotationValues detailAnnotationValues = new DetailAnnotationValues(controller);
        String[] annotationViews = detailAnnotationValues.getViews();
        // Remember that if views parameter is not present, the detail will be included only
        // in list view
        if (annotationViews == null && viewType.equals("list")) {
          found.add(detailViewMetadata);
        } else if (annotationViews != null) {
          List<String> views = Arrays.asList(annotationViews);
          // If the @RooDetail annotation contains this type as view parameter, it should be
          // included
          if (views.contains(viewType)) {
            found.add(detailViewMetadata);
          }
        }

      }
    }
    if (!found.isEmpty()) {
      detailsControllers = Collections.unmodifiableList(found);
    }

    return detailsControllers;
  }

  private JavaType filterControllerByPackageAndPrefix(
      Collection<ClassOrInterfaceTypeDetails> itemControllers, JavaPackage controllerPackage,
      String pathPrefix) {
    for (ClassOrInterfaceTypeDetails controller : itemControllers) {
      ControllerAnnotationValues values = new ControllerAnnotationValues(controller);
      if (controllerPackage.equals(controller.getType().getPackage())) {
        if (StringUtils.isBlank(pathPrefix) && StringUtils.isBlank(values.getPathPrefix())) {
          return controller.getType();
        } else if (StringUtils.equals(pathPrefix, values.getPathPrefix())) {
          return controller.getType();
        }
      }
    }
    return null;
  }

  private List<FieldMetadata> getEnumFields(MemberDetails entityMemberDetails) {
    List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
    for (FieldMetadata field : entityMemberDetails.getFields()) {
      if (isEnumType(field)) {
        fields.add(field);
      }
    }
    return fields;
  }

  private List<FieldMetadata> getDateTimeFields(MemberDetails entityMemberDetails) {
    List<FieldMetadata> fields = new ArrayList<FieldMetadata>();
    for (FieldMetadata field : entityMemberDetails.getFields()) {
      if (DATE_TIME_TYPES.contains(field.getFieldType())) {
        fields.add(field);
      }
    }
    return fields;

  }

  /**
   * This method checks if the provided type is enum or not
   *
   * @param fieldType
   * @return
   */
  private boolean isEnumType(FieldMetadata field) {
    Validate.notNull(field, "Java type required");
    final JavaType fieldType = field.getFieldType();
    if (fieldType.isPrimitive()) {
      return false;
    }
    if (STANDAR_TYPES.contains(fieldType) || DATE_TIME_TYPES.contains(fieldType)) {
      return false;
    }
    if (field.getAnnotation(JpaJavaType.ENUMERATED) != null) {
      return true;
    }
    final ClassOrInterfaceTypeDetails javaTypeDetails =
        getTypeLocationService().getTypeDetails(fieldType);
    if (javaTypeDetails != null) {
      if (javaTypeDetails.getPhysicalTypeCategory().equals(PhysicalTypeCategory.ENUMERATION)) {
        return true;
      }
    }
    return false;
  }

  protected void registerDependency(final String upstreamDependency,
      final String downStreamDependency) {

    if (getMetadataDependencyRegistry() != null && StringUtils.isNotBlank(upstreamDependency)
        && StringUtils.isNotBlank(downStreamDependency)
        && !upstreamDependency.equals(downStreamDependency)) {
      getMetadataDependencyRegistry().registerDependency(upstreamDependency, downStreamDependency);
    }
  }

  protected ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

  protected PropFilesManagerService getPropFilesManager() {
    return serviceInstaceManager.getServiceInstance(this, PropFilesManagerService.class);
  }

  protected I18nOperations getI18nOperations() {
    return serviceInstaceManager.getServiceInstance(this, I18nOperations.class);
  }

  protected FinderOperationsImpl getFinderOperations() {
    return (FinderOperationsImpl) serviceInstaceManager.getServiceInstance(this,
        FinderOperations.class);
  }

  protected PluralService getPluralService() {
    return serviceInstaceManager.getServiceInstance(this, PluralService.class);
  }

  protected ControllerLocator getControllerLocator() {
    return serviceInstaceManager.getServiceInstance(this, ControllerLocator.class);
  }

}
