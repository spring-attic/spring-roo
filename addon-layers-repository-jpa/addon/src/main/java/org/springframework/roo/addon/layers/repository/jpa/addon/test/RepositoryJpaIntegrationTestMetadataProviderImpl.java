package org.springframework.roo.addon.layers.repository.jpa.addon.test;

import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_JPA_INTEGRATION_TEST;

import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaMetadata;
import org.springframework.roo.addon.plural.addon.PluralService;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Implementation of {@link RepositoryJpaIntegrationTestMetadataProvider}.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class RepositoryJpaIntegrationTestMetadataProviderImpl extends
    AbstractMemberDiscoveringItdMetadataProvider implements
    RepositoryJpaIntegrationTestMetadataProvider {

  protected MetadataDependencyRegistryTracker registryTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_REPOSITORY_JPA_INTEGRATION_TEST} as 
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

    addMetadataTrigger(ROO_REPOSITORY_JPA_INTEGRATION_TEST);
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

    removeMetadataTrigger(ROO_REPOSITORY_JPA_INTEGRATION_TEST);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return RepositoryJpaIntegrationTestMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType =
        RepositoryJpaIntegrationTestMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path =
        RepositoryJpaIntegrationTestMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    return getLocalMid(itdTypeDetails);
  }

  @Override
  public String getItdUniquenessFilenameSuffix() {
    return "RepositoryJpaIntegrationTest";
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {

    final RepositoryJpaIntegrationTestAnnotationValues annotationValues =
        new RepositoryJpaIntegrationTestAnnotationValues(governorPhysicalTypeMetadata);

    // Find SpringJpaDataDetachableConfiguration class
    Set<JavaType> jpaConfigurationClasses =
        getTypeLocationService().findTypesWithAnnotation(
            RooJavaType.ROO_JPA_REPOSITORY_CONFIGURATION);
    Validate
        .isTrue(
            !jpaConfigurationClasses.isEmpty(),
            "Couldn't find the 'SpringDataJpaDetachableRepositoryConfiguration' on the project for '%s'",
            this.getClass().getName());
    JavaType jpaDetachableRepositoryClass = jpaConfigurationClasses.iterator().next();

    // Get repository metadata
    JavaType repositoryInterface = annotationValues.getTargetClass();
    ClassOrInterfaceTypeDetails repositoryDetails =
        getTypeLocationService().getTypeDetails(repositoryInterface);
    Validate.notNull(repositoryDetails.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA),
        "Couldn't find @RooJpaRepository in '%s'", repositoryInterface.getSimpleTypeName());
    String repositoryMetadataId = RepositoryJpaMetadata.createIdentifier(repositoryDetails);
    RepositoryJpaMetadata repositoryMetadata = getMetadataService().get(repositoryMetadataId);
    if (repositoryMetadata == null) {
      return null;
    }

    // Get entity identifier info
    JavaType entity = repositoryMetadata.getEntity();
    JavaType identifierType = getPersistenceMemberLocator().getIdentifierType(entity);
    JavaSymbolName identifierAccessorMethodName =
        getPersistenceMemberLocator().getIdentifierAccessor(entity).getMethodName();

    // Get entity plural
    String entityPlural = getPluralService().getPlural(entity);

    // Get repository default return type
    JavaType defaultReturnType = repositoryMetadata.getDefaultReturnType();

    return new RepositoryJpaIntegrationTestMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, annotationValues, jpaDetachableRepositoryClass,
        identifierType, identifierAccessorMethodName, entityPlural, entity, defaultReturnType);
  }

  public String getProvidesType() {
    return RepositoryJpaIntegrationTestMetadata.getMetadataIdentiferType();
  }

  protected PersistenceMemberLocator getPersistenceMemberLocator() {
    return this.serviceManager.getServiceInstance(this, PersistenceMemberLocator.class);
  }

  protected PluralService getPluralService() {
    return this.serviceManager.getServiceInstance(this, PluralService.class);
  }

}
