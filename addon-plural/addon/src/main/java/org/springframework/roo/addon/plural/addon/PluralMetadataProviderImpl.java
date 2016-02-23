package org.springframework.roo.addon.plural.addon;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Implementation of {@link PluralMetadataProvider}.
 * <p/>
 * It's odd that this class extends {@link AbstractItdMetadataProvider}, as it
 * doesn't produce an ITD, it just provides a plural String via
 * {@link PluralMetadata#getPlural()}. We should probably refactor it.
 * 
 * @author Ben Alex
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 1.0
 */
@Component
@Service
public class PluralMetadataProviderImpl extends AbstractItdMetadataProvider implements
    PluralMetadataProvider {

  protected MetadataDependencyRegistryTracker registryTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * <li>Set ignore trigger annotations. It means that other MD providers 
   * that want to discover whether a type has finders can do so.</li>
   * </ul>
   */
  @Override
  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, null,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();
    setIgnoreTriggerAnnotations(true);
    setDependsOnGovernorBeingAClass(false);
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
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return PluralMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = PluralMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = PluralMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Plural";
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
    final PluralAnnotationValues pluralAnnotationValues =
        new PluralAnnotationValues(governorPhysicalTypeMetadata);
    return new PluralMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, pluralAnnotationValues);
  }

  public String getProvidesType() {
    return PluralMetadata.getMetadataIdentiferType();
  }
}
