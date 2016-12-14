package org.springframework.roo.addon.web.mvc.thymeleaf.addon.link.factory;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * = LinkFactoryMetadataProviderImpl
 * 
 * Implementation of {@link LinkFactoryMetadataProvider}.
 *
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class LinkFactoryMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements LinkFactoryMetadataProvider {

  protected MetadataDependencyRegistryTracker registryTracker = null;

  /**
   * This service is being activated so setup it:
   * 
   * * Create and open the MetadataDependencyRegistryTracker.
   * * Create and open the CustomDataKeyDecoratorTracker.
   * * Registers RooJavaType.ROO_LINK_FACTORY as additional JavaType that 
   * will trigger metadata registration.
   * * Set ensure the governor type details represent a class.
   *
   */
  @Override
  @SuppressWarnings("unchecked")
  protected void activate(final ComponentContext cContext) {
    super.activate(cContext);
    this.registryTracker =
        new MetadataDependencyRegistryTracker(cContext.getBundleContext(), this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(RooJavaType.ROO_LINK_FACTORY);
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

    removeMetadataTrigger(RooJavaType.ROO_LINK_FACTORY);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return LinkFactoryMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = LinkFactoryMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = LinkFactoryMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    return getLocalMid(itdTypeDetails);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "LinkFactory";
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {

    // Get controller JavaType from @RooLinkFactory
    LinkFactoryAnnotationValues annotationValues =
        new LinkFactoryAnnotationValues(governorPhysicalTypeMetadata);
    JavaType controller = annotationValues.getController();

    // Check if it is a valid controller
    ClassOrInterfaceTypeDetails controllerDetails =
        getTypeLocationService().getTypeDetails(controller);
    Validate.isTrue(controllerDetails.getAnnotation(RooJavaType.ROO_CONTROLLER) != null,
        "Error creating controller %s methods to generate URL's.",
        controller.getFullyQualifiedTypeName());

    // Get ControllerType
    String controllerMetadataKey = ControllerMetadata.createIdentifier(controllerDetails);
    final ControllerMetadata controllerMetadata = getMetadataService().get(controllerMetadataKey);

    return new LinkFactoryMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, controller, controllerMetadata);
  }

  public String getProvidesType() {
    return LinkFactoryMetadata.getMetadataIdentiferType();
  }

}
