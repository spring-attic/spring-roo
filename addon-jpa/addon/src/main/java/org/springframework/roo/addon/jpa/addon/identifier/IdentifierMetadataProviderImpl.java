package org.springframework.roo.addon.jpa.addon.identifier;

import static org.springframework.roo.model.RooJavaType.ROO_IDENTIFIER;

import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.addon.ConfigurableMetadataProvider;
import org.springframework.roo.addon.javabean.addon.SerializableMetadataProvider;
import org.springframework.roo.addon.jpa.addon.AbstractIdentifierServiceAwareMetadataProvider;
import org.springframework.roo.addon.plural.addon.PluralMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProviderTracker;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link IdentifierMetadataProvider}.
 * 
 * @author Alan Stewart
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 1.1
 */
@Component
@Service
public class IdentifierMetadataProviderImpl extends AbstractIdentifierServiceAwareMetadataProvider
    implements IdentifierMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(IdentifierMetadataProviderImpl.class);

  private ProjectOperations projectOperations;

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected ItdTriggerBasedMetadataProviderTracker configurableMetadataProviderTracker = null;
  protected ItdTriggerBasedMetadataProviderTracker serializableMetadataProviderTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open one {@link ItdTriggerBasedMetadataProviderTracker} 
   * for each {@link ConfigurableMetadataProvider} and {@link SerializableMetadataProvider}.</li>
   * <li>Registers {@link RooJavaType#ROO_IDENTIFIER} as additional 
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
    addMetadataTrigger(ROO_IDENTIFIER);

    this.configurableMetadataProviderTracker =
        new ItdTriggerBasedMetadataProviderTracker(context, ConfigurableMetadataProvider.class,
            ROO_IDENTIFIER);
    this.configurableMetadataProviderTracker.open();

    this.serializableMetadataProviderTracker =
        new ItdTriggerBasedMetadataProviderTracker(context, SerializableMetadataProvider.class,
            ROO_IDENTIFIER);
    this.serializableMetadataProviderTracker.open();
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
    removeMetadataTrigger(ROO_IDENTIFIER);

    ItdTriggerBasedMetadataProvider metadataProvider =
        this.configurableMetadataProviderTracker.getService();
    metadataProvider.removeMetadataTrigger(ROO_IDENTIFIER);
    this.configurableMetadataProviderTracker.close();

    metadataProvider = this.serializableMetadataProviderTracker.getService();
    metadataProvider.removeMetadataTrigger(ROO_IDENTIFIER);
    this.serializableMetadataProviderTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return IdentifierMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = IdentifierMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = IdentifierMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Identifier";
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {

    if (projectOperations == null) {
      projectOperations = getProjectOperations();
    }
    Validate.notNull(projectOperations, "ProjectOperations is required");

    final IdentifierAnnotationValues annotationValues =
        new IdentifierAnnotationValues(governorPhysicalTypeMetadata);
    if (!annotationValues.isAnnotationFound()) {
      return null;
    }

    // We know governor type details are non-null and can be safely cast
    final JavaType javaType = IdentifierMetadata.getJavaType(metadataIdentificationString);
    final List<Identifier> identifierServiceResult = getIdentifiersForType(javaType);

    final LogicalPath path =
        PhysicalTypeIdentifierNamingUtils.getPath(metadataIdentificationString);
    if (projectOperations.isProjectAvailable(path.getModule())) {
      // If the project itself changes, we want a chance to refresh this
      // item
      getMetadataDependencyRegistry().registerDependency(
          ProjectMetadata.getProjectIdentifier(path.getModule()), metadataIdentificationString);
    }

    return new IdentifierMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, annotationValues, identifierServiceResult);
  }

  public String getProvidesType() {
    return IdentifierMetadata.getMetadataIdentifierType();
  }

  protected ProjectOperations getProjectOperations() {
    // Get all Services implement ProjectOperations interface
    try {
      ServiceReference<?>[] references =
          context.getAllServiceReferences(ProjectOperations.class.getName(), null);

      for (ServiceReference<?> ref : references) {
        return (ProjectOperations) context.getService(ref);
      }

      return null;

    } catch (InvalidSyntaxException e) {
      LOGGER.warning("Cannot load ProjectOperations on IdentifierMetadataProviderImpl.");
      return null;
    }
  }
}
