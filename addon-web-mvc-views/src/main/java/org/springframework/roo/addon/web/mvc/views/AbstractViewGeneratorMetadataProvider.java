package org.springframework.roo.addon.web.mvc.views;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.addon.finder.addon.parser.FinderMethod;
import org.springframework.roo.addon.web.mvc.i18n.I18nOperations;
import org.springframework.roo.addon.web.mvc.i18n.I18nOperationsImpl;
import org.springframework.roo.addon.javabean.addon.JavaBeanMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.propfiles.manager.PropFilesManagerService;

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
  public boolean readOnly;
  public JavaType service;
  public List<FinderMethod> finders;
  public String controllerPath;
  public JavaType identifierType;
  public MethodMetadata identifierAccessor;

  private ProjectOperations projectOperations;
  private PropFilesManagerService propFilesManagerService;
  private I18nOperationsImpl i18nOperationsImpl;

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
    this.service = (JavaType) controllerAnnotation.getAttribute("service").getValue();

    // Getting path
    this.controllerPath = (String) controllerAnnotation.getAttribute("path").getValue();

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

  public List<FinderMethod> getFinders() {
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

}
