package org.springframework.roo.addon.layers.repository.jpa.addon;

import static org.springframework.roo.model.RooJavaType.ROO_JPA_REPOSITORY_CONFIGURATION;

import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

/**
 * = RepositoryJpaConfigurationMetadataProviderImpl
 * 
 * Implementation of {@link RepositoryJpaConfigurationMetadataProvider}.
 *
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class RepositoryJpaConfigurationMetadataProviderImpl extends
    AbstractMemberDiscoveringItdMetadataProvider implements
    RepositoryJpaConfigurationMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(RepositoryJpaConfigurationMetadataProviderImpl.class);

  protected MetadataDependencyRegistryTracker registryTracker = null;

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_JPA_REPOSITORY_CONFIGURATION} as additional
   * JavaType that will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  protected void activate(final ComponentContext cContext) {
    super.activate(cContext);
    super.setDependsOnGovernorBeingAClass(true);
    serviceInstaceManager.activate(cContext.getBundleContext());
    this.registryTracker =
        new MetadataDependencyRegistryTracker(cContext.getBundleContext(), this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_JPA_REPOSITORY_CONFIGURATION);
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

    removeMetadataTrigger(ROO_JPA_REPOSITORY_CONFIGURATION);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return RepositoryJpaConfigurationMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType =
        RepositoryJpaConfigurationMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path =
        RepositoryJpaConfigurationMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Jpa_Repository_Configuration";
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    return getLocalMid(itdTypeDetails);
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {

    // Get application main class
    Set<JavaType> applicationTypes =
        getTypeLocationService().findTypesWithAnnotation(SpringJavaType.SPRING_BOOT_APPLICATION);
    String repositoryConfigurationModuleName = governorPhysicalTypeMetadata.getType().getModule();
    JavaType applicationMainType = null;
    for (JavaType applicationType : applicationTypes) {
      if (applicationType.getModule().equals(repositoryConfigurationModuleName)) {
        applicationMainType = applicationType;
      }
    }
    Validate.notNull(applicationMainType, "Unable to find a main application class on module %s",
        repositoryConfigurationModuleName);

    // Check if security is installed in module
    boolean isSpringletsSecurityEnabled =
        getProjectOperations()
            .isFeatureInstalled(applicationMainType.getModule(), "SPRINGLETS_JPA");

    return new RepositoryJpaConfigurationMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, applicationMainType, isSpringletsSecurityEnabled);
  }

  @Override
  public String getProvidesType() {
    return RepositoryJpaConfigurationMetadata.getMetadataIdentiferType();
  }

  protected ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

}
