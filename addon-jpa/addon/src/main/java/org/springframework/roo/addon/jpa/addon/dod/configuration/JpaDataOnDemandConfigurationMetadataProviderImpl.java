package org.springframework.roo.addon.jpa.addon.dod.configuration;

import static org.springframework.roo.model.RooJavaType.ROO_JPA_DATA_ON_DEMAND_CONFIGURATION;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.addon.ConfigurableMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProviderTracker;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

/**
 * Implementation of {@link JpaDataOnDemandConfigurationMetadataProvider}.
 *
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class JpaDataOnDemandConfigurationMetadataProviderImpl extends
    AbstractMemberDiscoveringItdMetadataProvider implements
    JpaDataOnDemandConfigurationMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(JpaDataOnDemandConfigurationMetadataProviderImpl.class);

  protected MetadataDependencyRegistryTracker registryTracker = null;

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}</li>
   * <li>Create and open the {@link ItdTriggerBasedMetadataProviderTracker}
   * to track for {@link ConfigurableMetadataProvider} service.</li>
   * <li>Registers {@link RooJavaType#ROO_JPA_DATA_ON_DEMAND_CONFIGURATION} as 
   * additional JavaType that will trigger metadata registration.</li>
   * </ul>
   */
  @Override
  protected void activate(final ComponentContext cContext) {
    super.activate(cContext);
    BundleContext localContext = cContext.getBundleContext();
    this.registryTracker =
        new MetadataDependencyRegistryTracker(localContext, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_JPA_DATA_ON_DEMAND_CONFIGURATION);

    serviceInstaceManager.activate(cContext.getBundleContext());
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

    removeMetadataTrigger(ROO_JPA_DATA_ON_DEMAND_CONFIGURATION);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return JpaDataOnDemandConfigurationMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType =
        JpaDataOnDemandConfigurationMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path =
        JpaDataOnDemandConfigurationMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "JpaDataOnDemandConfiguration";
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    return getLocalMid(itdTypeDetails);
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String dodMetadataId,
      final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final String itdFilename) {

    SortedSet<JavaType> dataOnDemandTypes = new TreeSet<JavaType>();
    dataOnDemandTypes.addAll(getTypeLocationService().findTypesWithAnnotation(
        RooJavaType.ROO_JPA_DATA_ON_DEMAND));

    return new JpaDataOnDemandConfigurationMetadata(dodMetadataId, aspectName,
        governorPhysicalTypeMetadata, dataOnDemandTypes);
  }

  public String getProvidesType() {
    return JpaDataOnDemandConfigurationMetadata.getMetadataIdentiferType();
  }

}
