package org.springframework.roo.addon.web.mvc.thymeleaf.addon.test;

import static org.springframework.roo.model.RooJavaType.ROO_THYMELEAF_CONTROLLER_INTEGRATION_TEST;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.jpa.addon.entity.factories.JpaEntityFactoryLocator;
import org.springframework.roo.addon.layers.service.addon.ServiceLocator;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Implementation of {@link ThymeleafControllerIntegrationTestMetadataProvider}.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class ThymeleafControllerIntegrationTestMetadataProviderImpl extends
    AbstractMemberDiscoveringItdMetadataProvider implements
    ThymeleafControllerIntegrationTestMetadataProvider {

  protected MetadataDependencyRegistryTracker registryTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_THYMELEAF_CONTROLLER_INTEGRATION_TEST} as 
   * additional JavaType that will trigger metadata registration.</li>
   * </ul>
   */
  @Override
  protected void activate(final ComponentContext cContext) {
    super.activate(cContext);
    this.registryTracker =
        new MetadataDependencyRegistryTracker(cContext.getBundleContext(), this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_THYMELEAF_CONTROLLER_INTEGRATION_TEST);
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

    removeMetadataTrigger(ROO_THYMELEAF_CONTROLLER_INTEGRATION_TEST);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return ThymeleafControllerIntegrationTestMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType =
        ThymeleafControllerIntegrationTestMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path =
        ThymeleafControllerIntegrationTestMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    return getLocalMid(itdTypeDetails);
  }

  @Override
  public String getItdUniquenessFilenameSuffix() {
    return "ThymeleafControllerIntegrationTest";
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {

    final ThymeleafControllerIntegrationTestAnnotationValues annotationValues =
        new ThymeleafControllerIntegrationTestAnnotationValues(governorPhysicalTypeMetadata);

    // Get JSON controller target class
    final JavaType jsonController = annotationValues.getTargetClass();

    // Get the controller managed entity
    ClassOrInterfaceTypeDetails controllerCid =
        getTypeLocationService().getTypeDetails(jsonController);
    AnnotationMetadata rooControllerAnnotation =
        controllerCid.getAnnotation(RooJavaType.ROO_CONTROLLER);
    Validate.notNull(rooControllerAnnotation,
        "The target class must be annotated with @RooController.");
    Validate.notNull(rooControllerAnnotation.getAttribute("entity"),
        "The @RooController must have an 'entity' attribute, targeting managed entity.");
    final JavaType managedEntity =
        (JavaType) rooControllerAnnotation.getAttribute("entity").getValue();

    // Get the entity metadata
    String jpaEntityIdentifier =
        JpaEntityMetadata.createIdentifier(getTypeLocationService().getTypeDetails(managedEntity));
    JpaEntityMetadata entityMetadata = getMetadataService().get(jpaEntityIdentifier);
    if (entityMetadata == null) {
      return null;
    }

    // Get child related entities
    Collection<RelationInfo> relatioInfos = entityMetadata.getRelationInfos().values();
    List<JavaType> relatedEntities = new ArrayList<JavaType>();
    // First, add managed entity
    relatedEntities.add(managedEntity);
    for (RelationInfo relationInfo : relatioInfos) {
      JavaType entity = relationInfo.childType;
      if (!relatedEntities.contains(entity)) {
        relatedEntities.add(entity);
      }
    }

    // Get the entity factory of managed entity
    final JavaType entityFactory =
        getJpaEntityFactoryLocator().getFirstJpaEntityFactoryForEntity(managedEntity);

    // Get the services related to managed entity
    List<JavaType> relatedServices = new ArrayList<JavaType>();
    for (JavaType entity : relatedEntities) {
      final JavaType service = getServiceLocator().getFirstService(entity).getType();
      Validate.notNull(service, "Couldn't find service of %s in %s", entity.getSimpleTypeName(),
          this.getClass().getName());
      if (!relatedServices.contains(service)) {
        relatedServices.add(service);
      }
    }

    return new ThymeleafControllerIntegrationTestMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, annotationValues, jsonController, managedEntity,
        entityFactory, relatedServices);
  }

  public String getProvidesType() {
    return ThymeleafControllerIntegrationTestMetadata.getMetadataIdentiferType();
  }

  protected JpaEntityFactoryLocator getJpaEntityFactoryLocator() {
    return this.serviceManager.getServiceInstance(this, JpaEntityFactoryLocator.class);
  }

  protected ServiceLocator getServiceLocator() {
    return this.serviceManager.getServiceInstance(this, ServiceLocator.class);
  }

}
