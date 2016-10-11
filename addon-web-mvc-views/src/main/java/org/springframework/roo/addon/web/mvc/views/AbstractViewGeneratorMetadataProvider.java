package org.springframework.roo.addon.web.mvc.views;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.jvnet.inflector.Noun;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.addon.finder.addon.FinderOperations;
import org.springframework.roo.addon.finder.addon.FinderOperationsImpl;
import org.springframework.roo.addon.finder.addon.parser.FinderParameter;
import org.springframework.roo.addon.javabean.addon.JavaBeanMetadata;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.addon.web.mvc.controller.addon.finder.SearchAnnotationValues;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.addon.web.mvc.i18n.I18nOperations;
import org.springframework.roo.addon.web.mvc.i18n.I18nOperationsImpl;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.propfiles.manager.PropFilesManagerService;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

  public String metadataIdentificationString;
  public JavaType aspectName;
  public PhysicalTypeMetadata governorPhysicalTypeMetadata;
  public String itdFilename;

  public ClassOrInterfaceTypeDetails controller;
  public JavaType entity;
  public ControllerType type;
  public boolean readOnly;
  public JavaType service;
  public List<MethodMetadata> finders;
  public String controllerPath;
  public JavaType identifierType;
  public MethodMetadata identifierAccessor;

  private ProjectOperations projectOperations;
  private PropFilesManagerService propFilesManagerService;
  private I18nOperationsImpl i18nOperationsImpl;
  private FinderOperationsImpl finderOperationsImpl;

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
   *
   * @return ItdTypeDetailsProvidingMetadataItem
   */
  protected abstract ItdTypeDetailsProvidingMetadataItem createMetadataInstance();

  protected void fillContext(ViewContext ctx) {
    // To be overridden if needed
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {

    this.metadataIdentificationString = metadataIdentificationString;
    this.aspectName = aspectName;
    this.governorPhysicalTypeMetadata = governorPhysicalTypeMetadata;
    this.itdFilename = itdFilename;

    // Save annotated controller
    this.controller = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();

    // Getting @RooController annotation
    AnnotationMetadata controllerAnnotation = controller.getAnnotation(RooJavaType.ROO_CONTROLLER);

    // Validate that provided controller has @RooController annotation
    Validate.notNull(controllerAnnotation,
        "ERROR: Provided controller has not been annotated with @RooController annotation");

    // Getting entity and check if is a readOnly entity or not
    this.entity = (JavaType) controllerAnnotation.getAttribute("entity").getValue();
    AnnotationMetadata entityAnnotation =
        getTypeLocationService().getTypeDetails(this.entity).getAnnotation(
            RooJavaType.ROO_JPA_ENTITY);

    Validate.notNull(entityAnnotation, "ERROR: Entity should be annotated with @RooJpaEntity");

    Validate.notNull(controllerAnnotation.getAttribute("type"),
        "@RooController annotation should have 'type' attribute.");
    this.type =
        ControllerType.getControllerType(((EnumDetails) controllerAnnotation.getAttribute("type")
            .getValue()).getField().getSymbolName());

    // Getting identifierField
    List<FieldMetadata> identifierField = getPersistenceMemberLocator().getIdentifierFields(entity);

    // Getting entity details
    MemberDetails entityDetails = getMemberDetails(entity);

    this.readOnly = false;
    if (entityAnnotation.getAttribute("readOnly") != null) {
      this.readOnly = (Boolean) entityAnnotation.getAttribute("readOnly").getValue();
    }

    // Getting identifierType
    this.identifierType = getPersistenceMemberLocator().getIdentifierType(entity);
    this.identifierAccessor = getPersistenceMemberLocator().getIdentifierAccessor(entity);

    // Getting service
    Set<ClassOrInterfaceTypeDetails> services =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_SERVICE);
    Iterator<ClassOrInterfaceTypeDetails> itServices = services.iterator();

    while (itServices.hasNext()) {
      ClassOrInterfaceTypeDetails existingService = itServices.next();
      AnnotationAttributeValue<Object> entityAttr =
          existingService.getAnnotation(RooJavaType.ROO_SERVICE).getAttribute("entity");
      if (entityAttr != null && entityAttr.getValue().equals(entity)) {
        this.service = existingService.getType();

        ClassOrInterfaceTypeDetails serviceDetails =
            getTypeLocationService().getTypeDetails(this.service);

        final LogicalPath logicalPath =
            PhysicalTypeIdentifier.getPath(serviceDetails.getDeclaredByMetadataId());
        final String serviceMetadataKey =
            ServiceMetadata.createIdentifier(serviceDetails.getType(), logicalPath);
        final ServiceMetadata serviceMetadata =
            (ServiceMetadata) getMetadataService().get(serviceMetadataKey);

        // Get only those service finders exposed to views
        this.finders = new ArrayList<MethodMetadata>();
        this.finders = serviceMetadata.getFinders();
      }
    }

    // Getting controller finders
    final SearchAnnotationValues annotationValues =
        new SearchAnnotationValues(governorPhysicalTypeMetadata);
    List<String> finderNames = new ArrayList<String>();

    // Add finder names from @RooSearch to filter those exposed to views
    if (annotationValues != null && annotationValues.getFinders() != null) {
      finderNames = Arrays.asList(annotationValues.getFinders());

      // Get only those finders exposed to views
      if (this.finders != null && !this.finders.isEmpty()) {
        List<MethodMetadata> finderMethods = new ArrayList<MethodMetadata>();
        for (MethodMetadata finder : this.finders) {
          if (finderNames.contains(finder.getMethodName().getSymbolName())) {
            finderMethods.add(finder);
          }
        }

        // Fill finders variable only with exposed finder methods
        this.finders = finderMethods;
      }
    } else {

      // Entity hasn't exposed finders
      this.finders = new ArrayList<MethodMetadata>();
    }

    // Getting pathPrefix
    AnnotationAttributeValue<Object> pathPrefixAttr =
        controllerAnnotation.getAttribute("pathPrefix");
    String pathPrefix = "";
    if (pathPrefixAttr != null) {
      pathPrefix = StringUtils.lowerCase((String) pathPrefixAttr.getValue());
    }
    // Generate path
    String path =
        "/".concat(StringUtils.lowerCase(Noun.pluralOf(entity.getSimpleTypeName(), Locale.ENGLISH)));
    if (StringUtils.isNotEmpty(pathPrefix)) {
      if (!pathPrefix.startsWith("/")) {
        pathPrefix = "/".concat(pathPrefix);
      }
      path = pathPrefix.concat(path);
    }
    this.controllerPath = path;

    // Fill view context
    ViewContext ctx = new ViewContext();
    ctx.setControllerPath(controllerPath);
    ctx.setProjectName(getProjectOperations().getProjectName(""));
    ctx.setVersion(getProjectOperations().getPomFromModuleName("").getVersion());
    ctx.setEntityName(entity.getSimpleTypeName());
    ctx.setModelAttribute(getEntityField().getFieldName().getSymbolName());
    ctx.setModelAttributeName(StringUtils.uncapitalize(entity.getSimpleTypeName()));
    ctx.setIdentifierField(identifierField.get(0).getFieldName().getSymbolName());
    fillContext(ctx);

    // Use provided MVCViewGenerationService to generate views
    MVCViewGenerationService viewGenerationService = getViewGenerationService();

    // Add list view
    viewGenerationService.addListView(this.controller.getType().getModule(), entityDetails, ctx);

    // Add show view
    viewGenerationService.addShowView(this.controller.getType().getModule(), entityDetails, ctx);

    if (!readOnly) {
      // If not readOnly, add create view
      viewGenerationService
          .addCreateView(this.controller.getType().getModule(), entityDetails, ctx);

      // If not readOnly, add update view
      viewGenerationService
          .addUpdateView(this.controller.getType().getModule(), entityDetails, ctx);
    }

    // Add finder views
    if (this.finders != null) {
      for (MethodMetadata finderMethod : this.finders) {

        // For each finder, create form and list view exposing only finder params
        // from form bean object
        JavaType formBean = finderMethod.getParameterTypes().get(0).getJavaType();
        List<FieldMetadata> fieldsToAdd = new ArrayList<FieldMetadata>();

        // Check if finder form bean is a DTO or the entity
        if (getTypeLocationService().getTypeDetails(formBean) != null
            && getTypeLocationService().getTypeDetails(formBean).getAnnotation(RooJavaType.ROO_DTO) == null) {
          formBean = this.entity;

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
        getFinderOperations().buildFormBeanFieldNamesMap(this.entity, formBean, typesFieldMaps,
            typeFieldMetadataMap, finderMethod.getMethodName(), finderParametersMap);

        // Get finder parameters for each finder method and FieldMetadata for each finder param
        List<FinderParameter> finderParameters =
            finderParametersMap.get(finderMethod.getMethodName());
        Map<String, FieldMetadata> formBeanFields = typeFieldMetadataMap.get(formBean);

        for (FinderParameter finderParam : finderParameters) {
          fieldsToAdd.add(formBeanFields.get(finderParam.getName().getSymbolName()));
        }

        viewGenerationService.addFinderFormView(this.controller.getType().getModule(),
            entityDetails, finderMethod.getMethodName().getSymbolName(), fieldsToAdd, ctx);

        // If return type is a projection, use its details
        ClassOrInterfaceTypeDetails returnTypeDetails =
            getTypeLocationService().getTypeDetails(
                finderMethod.getReturnType().getParameters().get(0));
        if (returnTypeDetails != null
            && returnTypeDetails.getAnnotation(RooJavaType.ROO_ENTITY_PROJECTION) != null) {
          viewGenerationService.addFinderListView(this.controller.getType().getModule(),
              getMemberDetails(returnTypeDetails), finderMethod.getMethodName().getSymbolName(),
              ctx);

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
          viewGenerationService.addFinderListView(this.controller.getType().getModule(),
              entityDetails, finderMethod.getMethodName().getSymbolName(), ctx);
        }
      }
    }

    // Update menu view every time that new controller has been modified
    // TODO: Maybe, instead of modify all menu view, only new generated controller should
    // be included on it. Must be fixed on future versions.
    viewGenerationService.updateMenuView(this.controller.getType().getModule(), ctx);

    // Update i18n labels
    getI18nOperationsImpl().updateI18n(entityDetails, this.entity,
        this.controller.getType().getModule());

    // Register dependency between JavaBeanMetadata and this one
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(getTypeLocationService().getTypeDetails(entity)
            .getDeclaredByMetadataId());
    final String javaBeanMetadataKey =
        JavaBeanMetadata.createIdentifier(
            getTypeLocationService().getTypeDetails(entity).getType(), logicalPath);
    registerDependency(javaBeanMetadataKey, metadataIdentificationString);

    return createMetadataInstance();
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

  public ClassOrInterfaceTypeDetails getController() {
    return this.controller;
  }

  public boolean isReadOnly() {
    return this.readOnly;
  }

  public JavaType getEntity() {
    return this.entity;
  }

  public String getControllerpath() {
    return this.controllerPath;
  }

  public JavaType getIdentifierType() {
    return this.identifierType;
  }

  public MethodMetadata getIdentifierAccessor() {
    return this.identifierAccessor;
  }

  public JavaType getService() {
    return this.service;
  }

  public List<MethodMetadata> getFinders() {
    return this.finders;
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

  public ProjectOperations getProjectOperations() {
    if (projectOperations == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          projectOperations = (ProjectOperations) this.context.getService(ref);
          return projectOperations;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on AbstractViewGeneratorMetadataProvider.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }

  public PropFilesManagerService getPropFilesManager() {
    if (propFilesManagerService == null) {
      // Get all Services implement PropFileOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(PropFilesManagerService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          propFilesManagerService = (PropFilesManagerService) this.context.getService(ref);
          return propFilesManagerService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER
            .warning("Cannot load PropFilesManagerService on AbstractViewGeneratorMetadataProvider.");
        return null;
      }
    } else {
      return propFilesManagerService;
    }
  }

  public I18nOperationsImpl getI18nOperationsImpl() {
    if (i18nOperationsImpl == null) {
      // Get all Services implement ProjectOperations interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(I18nOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          i18nOperationsImpl = (I18nOperationsImpl) this.context.getService(ref);
          return i18nOperationsImpl;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load ProjectOperations on AbstractViewGeneratorMetadataProvider.");
        return null;
      }
    } else {
      return i18nOperationsImpl;
    }
  }

  public FinderOperationsImpl getFinderOperations() {
    if (finderOperationsImpl == null) {
      // Get all Services implement DtoOperations interface
      try {
        ServiceReference<?>[] references =
            context.getAllServiceReferences(FinderOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          return (FinderOperationsImpl) context.getService(ref);
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER
            .warning("Cannot load FinderOperationsImpl on RepositoryJpaCustomImplMetadataProviderImpl.");
        return null;
      }
    } else {
      return this.finderOperationsImpl;
    }
  }

}
