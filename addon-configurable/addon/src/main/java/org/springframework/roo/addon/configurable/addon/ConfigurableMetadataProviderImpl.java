package org.springframework.roo.addon.configurable.addon;

import static org.springframework.roo.model.RooJavaType.ROO_CONFIGURABLE;

import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link ConfigurableMetadataProvider}.
 * 
 * @author Ben Alex
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 1.0
 */
@Component
@Service
public class ConfigurableMetadataProviderImpl extends AbstractItdMetadataProvider implements
    ConfigurableMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(ConfigurableMetadataProviderImpl.class);

  protected MetadataDependencyRegistryTracker registryTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}</li>
   * <li>Registers {@link RooJavaType#ROO_CONFIGURABLE} as additional 
   * JavaType that will trigger metadata registration.</li>
   * </ul>
   */
  @Override
  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, null,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_CONFIGURABLE);
  }

  /**
   * This service is being deactivated so unregister upstream-downstream 
   * dependencies, triggers, matchers and listeners.
   * 
   * @param context
   */
  protected void deactivate(final ComponentContext context) {
    MetadataDependencyRegistry registry = this.registryTracker.getService();
    registry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(),
        getProvidesType());
    this.registryTracker.close();

    removeMetadataTrigger(ROO_CONFIGURABLE);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return ConfigurableMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = ConfigurableMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = ConfigurableMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Configurable";
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
    return new ConfigurableMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata);
  }

  public String getProvidesType() {
    return ConfigurableMetadata.getMetadataIdentiferType();
  }
}
