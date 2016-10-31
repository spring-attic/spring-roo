package org.springframework.roo.addon.web.mvc.views;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.javabean.addon.JavaBeanMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.FinderOperations;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.FinderOperationsImpl;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.addon.plural.addon.PluralService;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerLocator;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.addon.web.mvc.i18n.I18nOperations;
import org.springframework.roo.addon.web.mvc.i18n.I18nOperationsImpl;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * This abstract class will be extended by MetadataProviders focused on
 * view generation.
 *
 * As a result, it will be possible that all MetadataProviders that manages
 * view generation follows the same steps and the same operations to do it.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
@Component(componentAbstract = true)
public abstract class AbstractViewGeneratorMetadataProvider extends
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
   * This operation returns the MVCViewGenerationService that should be used
   * to generate views.
   *
   * Implements this operations in you views metadata providers to be able to
   * generate all necessary views.
   *
   * @return MVCViewGenerationService
   */
  protected abstract MVCViewGenerationService getViewGenerationService();

  /**
   * This operations returns the necessary Metadata that will generate .aj file.
   *
   * This operation is called from getMetadata operation to obtain the return
   * element.
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
   *
   * @return ItdTypeDetailsProvidingMetadataItem
   */
  protected abstract ItdTypeDetailsProvidingMetadataItem createMetadataInstance(
      String metadataIdentificationString, JavaType aspectName,
      PhysicalTypeMetadata governorPhysicalTypeMetadata, ControllerMetadata controllerMetadata,
      ServiceMetadata serviceMetadata, JpaEntityMetadata entityMetadata, String entityPlural,
      String entityIdentifierPlural,
      List<Pair<RelationInfo, JpaEntityMetadata>> compositionRelationOneToOne,
      JavaType itemController, JavaType collectionController, List<FieldMetadata> dateTimeFields,
      List<FieldMetadata> enumFields);

  protected void fillContext(ViewContext ctx) {
    // To be overridden if needed
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {

    // Use provided MVCViewGenerationService to generate views
    MVCViewGenerationService viewGenerationService = getViewGenerationService();

    ClassOrInterfaceTypeDetails controllerDetail =
        governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();

    // Getting controller metadata
    final String controllerMetadataKey = ControllerMetadata.createIdentifier(controllerDetail);
    final ControllerMetadata controllerMetadata =
        (ControllerMetadata) getMetadataService().get(controllerMetadataKey);

    // Getting entity and check if is a readOnly entity or not
    final JavaType entity = controllerMetadata.getEntity();

    JavaType viewType = viewGenerationService.getType();
    JavaType itemController = null;
    JavaType collectionController = null;
    if (controllerMetadata.getType() != ControllerType.ITEM) {
      // Locate ItemController
      Collection<ClassOrInterfaceTypeDetails> itemControllers =
          getControllerLocator().getControllers(entity, ControllerType.ITEM, viewType);

      if (itemControllers.isEmpty()) {
        // We can't create metadata "Jet"
        return null;
      } else {
        // Get controller with the same package
        JavaPackage controllerPackage = controllerDetail.getType().getPackage();
        for (ClassOrInterfaceTypeDetails controller : itemControllers) {
          if (controllerPackage.equals(controller.getType().getPackage())) {
            itemController = controller.getType();
            break;
          }
        }
        Validate.notNull(itemControllers,
            "ERROR: Can't find ITEM-type controller related to controller '%s'", controllerDetail
                .getType().getFullyQualifiedTypeName());
      }
    }

    if (controllerMetadata.getType() != ControllerType.COLLECTION) {
      // Locate ItemController
      Collection<ClassOrInterfaceTypeDetails> collectionControllers =
          getControllerLocator().getControllers(entity, ControllerType.COLLECTION, viewType);

      if (collectionControllers.isEmpty()) {
        // We can't create metadata "Jet"
        return null;
      } else {
        // Get controller with the same package
        JavaPackage controllerPackage = controllerDetail.getType().getPackage();
        for (ClassOrInterfaceTypeDetails controller : collectionControllers) {
          if (controllerPackage.equals(controller.getType().getPackage())) {
            collectionController = controller.getType();
            break;
          }
        }
        Validate.notNull(collectionController,
            "ERROR: Can't find ITEM-type controller related to controller '%s'", controllerDetail
                .getType().getFullyQualifiedTypeName());
      }
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

    //  TODO Finders
    List<MethodMetadata> finders = null;



    // Fill view context
    ViewContext ctx = new ViewContext();
    ctx.setControllerPath(controllerMetadata.getPath());
    ctx.setProjectName(getProjectOperations().getProjectName(""));
    ctx.setVersion(getProjectOperations().getPomFromModuleName("").getVersion());
    ctx.setEntityName(entity.getSimpleTypeName());
    ctx.setModelAttribute(StringUtils.uncapitalize(entity.getSimpleTypeName()));
    ctx.setModelAttributeName(StringUtils.uncapitalize(entity.getSimpleTypeName()));
    ctx.setIdentifierField(entityMetadata.getCurrentIndentifierField().getFieldName()
        .getSymbolName());

    // Checking if Spring Security has been installed
    if (getProjectOperations().isFeatureInstalled(FeatureNames.SECURITY)) {
      ctx.setSecurityEnabled(true);
    }

    // Call Abstract Method fillContext to fill context with view provider custom implementation
    fillContext(ctx);


    final String module = controllerDetail.getType().getModule();
    // Add list view
    viewGenerationService.addListView(module, entityMemberDetails, ctx);

    // Add show view
    viewGenerationService.addShowView(module, entityMemberDetails, ctx);

    if (!entityMetadata.isReadOnly()) {
      // If not readOnly, add create view
      viewGenerationService.addCreateView(module, entityMemberDetails, ctx);

      // If not readOnly, add update view
      viewGenerationService.addUpdateView(module, entityMemberDetails, ctx);
    }

    // Add finder views
    /* TODO
    if (finders != null) {
      for (MethodMetadata finderMethod : finders) {

        // For each finder, create form and list view exposing only finder params
        // from form bean object
        JavaType formBean = finderMethod.getParameterTypes().get(0).getJavaType();
        List<FieldMetadata> fieldsToAdd = new ArrayList<FieldMetadata>();

        // Check if finder form bean is a DTO or the entity
        if (getTypeLocationService().getTypeDetails(formBean) != null
            && getTypeLocationService().getTypeDetails(formBean).getAnnotation(RooJavaType.ROO_DTO) == null) {
          formBean = entity;

          // Register dependency between DTO JavaBeanMetadata and this one
          final LogicalPath logicalPath =
              PhysicalTypeIdentifier.getPath(getTypeLocationService().getTypeDetails(formBean)
                  .getDeclaredByMetadataId());
          final String javaBeanMetadataKey =
              JavaBeanMetadata.createIdentifier(getTypeLocationService().getTypeDetails(formBean)
                  .getType(), logicalPath);
          registerDependency(javaBeanMetadataKey, metadataIdentificationString);

        }

        // Add formBean to viewContext
        ctx.addExtraParameter("formBean", "formBean");

        // Use method from FinderOperationsImpl to fill maps
        Map<JavaType, Map<String, String>> typesFieldMaps =
            new HashMap<JavaType, Map<String, String>>();
        Map<JavaType, Map<String, FieldMetadata>> typeFieldMetadataMap =
            new HashMap<JavaType, Map<String, FieldMetadata>>();
        Map<JavaSymbolName, List<FinderParameter>> finderParametersMap =
            new HashMap<JavaSymbolName, List<FinderParameter>>();
        getFinderOperations().buildFormBeanFieldNamesMap(entity, formBean, typesFieldMaps,
            typeFieldMetadataMap, finderMethod.getMethodName(), finderParametersMap);

        // Get finder parameters for each finder method and FieldMetadata for each finder param
        List<FinderParameter> finderParameters =
            finderParametersMap.get(finderMethod.getMethodName());
        Map<String, FieldMetadata> formBeanFields = typeFieldMetadataMap.get(formBean);

        for (FinderParameter finderParam : finderParameters) {
          fieldsToAdd.add(formBeanFields.get(finderParam.getName().getSymbolName()));
        }

        viewGenerationService.addFinderFormView(module, entityMemberDetails, finderMethod
            .getMethodName().getSymbolName(), fieldsToAdd, ctx);

        // If return type is a projection, use its details
        ClassOrInterfaceTypeDetails returnTypeDetails =
            getTypeLocationService().getTypeDetails(
                finderMethod.getReturnType().getParameters().get(0));
        if (returnTypeDetails != null
            && returnTypeDetails.getAnnotation(RooJavaType.ROO_ENTITY_PROJECTION) != null) {
          viewGenerationService.addFinderListView(module, getMemberDetails(returnTypeDetails),
              finderMethod.getMethodName().getSymbolName(), ctx);

          // Register dependency between projection JavaBeanMetadata and this one
          final LogicalPath logicalPath =
              PhysicalTypeIdentifier.getPath(getTypeLocationService().getTypeDetails(
                  returnTypeDetails.getType()).getDeclaredByMetadataId());
          final String javaBeanMetadataKey =
              JavaBeanMetadata.createIdentifier(
                  getTypeLocationService().getTypeDetails(returnTypeDetails.getType()).getType(),
                  logicalPath);
          registerDependency(javaBeanMetadataKey, metadataIdentificationString);

        } else {
          viewGenerationService.addFinderListView(module, entityMemberDetails, finderMethod
              .getMethodName().getSymbolName(), ctx);
        }
      }
    }
    */

    // Update menu view every time that new controller has been modified
    // TODO: Maybe, instead of modify all menu view, only new generated controller should
    // be included on it. Must be fixed on future versions.
    viewGenerationService.updateMenuView(module, ctx);

    // Update i18n labels
    getI18nOperationsImpl().updateI18n(entityMemberDetails, entity, module);

    // Register dependency between JavaBeanMetadata and this one
    final String javaBeanMetadataKey = JavaBeanMetadata.createIdentifier(entityDetails);
    registerDependency(javaBeanMetadataKey, metadataIdentificationString);

    List<FieldMetadata> dateTimeFields = getDateTimeFields(entityMemberDetails);
    List<FieldMetadata> enumFields = getEnumFields(entityMemberDetails);

    return createMetadataInstance(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, controllerMetadata, serviceMetadata, entityMetadata,
        entityPlural, entityIdentifierPlural, compositionRelationOneToOne, itemController,
        collectionController, dateTimeFields, enumFields);
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

    if (getMetadataDependencyRegistry() != null
        && StringUtils.isNotBlank(upstreamDependency)
        && StringUtils.isNotBlank(downStreamDependency)
        && !upstreamDependency.equals(downStreamDependency)
        && !MetadataIdentificationUtils.getMetadataClass(downStreamDependency).equals(
            MetadataIdentificationUtils.getMetadataClass(upstreamDependency))) {
      getMetadataDependencyRegistry().registerDependency(upstreamDependency, downStreamDependency);
    }
  }

  protected ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

  protected PropFilesManagerService getPropFilesManager() {
    return serviceInstaceManager.getServiceInstance(this, PropFilesManagerService.class);
  }

  protected I18nOperationsImpl getI18nOperationsImpl() {
    return (I18nOperationsImpl) serviceInstaceManager
        .getServiceInstance(this, I18nOperations.class);
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
