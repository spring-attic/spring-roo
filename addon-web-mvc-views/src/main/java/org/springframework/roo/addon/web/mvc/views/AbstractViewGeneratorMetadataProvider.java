package org.springframework.roo.addon.web.mvc.views;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.addon.finder.addon.parser.FinderMethod;
import org.springframework.roo.addon.javabean.addon.JavaBeanMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;

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
    fillContext(ctx);

    // Use provided MVCViewGenerationService to generate views
    MVCViewGenerationService viewGenerationService = getViewGenerationService();

    // Add list view
    viewGenerationService.addListView(entityDetails, ctx);

    // Add show view
    viewGenerationService.addShowView(entityDetails, ctx);

    if (!readOnly) {
      // If not readOnly, add create view
      viewGenerationService.addCreateView(entityDetails, ctx);

      // If not readOnly, add update view
      viewGenerationService.addUpdateView(entityDetails, ctx);
    }

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

}
