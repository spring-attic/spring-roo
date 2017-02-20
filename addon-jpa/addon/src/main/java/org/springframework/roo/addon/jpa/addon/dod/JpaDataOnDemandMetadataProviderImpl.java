package org.springframework.roo.addon.jpa.addon.dod;

import static org.springframework.roo.model.RooJavaType.ROO_JPA_DATA_ON_DEMAND;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.addon.ConfigurableMetadataProvider;
import org.springframework.roo.addon.jpa.addon.dod.configuration.JpaDataOnDemandConfigurationMetadata;
import org.springframework.roo.addon.jpa.addon.entity.factories.JpaEntityFactoryMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProviderTracker;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

/**
 * Implementation of {@link JpaDataOnDemandMetadataProvider}.
 *
 * @author Ben Alex
 * @author Greg Turnquist
 * @author Andrew Swan
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 1.0
 */
@Component
@Service
public class JpaDataOnDemandMetadataProviderImpl extends
    AbstractMemberDiscoveringItdMetadataProvider implements JpaDataOnDemandMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(JpaDataOnDemandMetadataProviderImpl.class);

  private static final String FLUSH_METHOD = CustomDataKeys.FLUSH_METHOD.name();

  private LayerService layerService;
  private final Map<String, JavaType> dodMidToEntityMap = new LinkedHashMap<String, JavaType>();
  private final Map<JavaType, String> entityToDodMidMap = new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected ItdTriggerBasedMetadataProviderTracker configurableMetadataProviderTracker = null;

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}</li>
   * <li>Create and open the {@link ItdTriggerBasedMetadataProviderTracker}
   * to track for {@link ConfigurableMetadataProvider} service.</li>
   * <li>Registers {@link RooJavaType#ROO_JPA_DATA_ON_DEMAND} as additional
   * JavaType that will trigger metadata registration.</li>
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

    // DOD classes are @Configurable because they may need DI of other DOD
    // classes that provide M:1 relationships
    this.configurableMetadataProviderTracker =
        new ItdTriggerBasedMetadataProviderTracker(localContext,
            ConfigurableMetadataProvider.class, ROO_JPA_DATA_ON_DEMAND);
    this.configurableMetadataProviderTracker.open();

    addMetadataTrigger(ROO_JPA_DATA_ON_DEMAND);

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

    ItdTriggerBasedMetadataProvider metadataProvider =
        this.configurableMetadataProviderTracker.getService();
    metadataProvider.removeMetadataTrigger(ROO_JPA_DATA_ON_DEMAND);
    this.configurableMetadataProviderTracker.close();

    removeMetadataTrigger(ROO_JPA_DATA_ON_DEMAND);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return JpaDataOnDemandMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = JpaDataOnDemandMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = JpaDataOnDemandMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "JpaDataOnDemand";
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    // Determine the governor for this ITD, and whether any DOD metadata is
    // even hoping to hear about changes to that JavaType and its ITDs
    final JavaType governor = itdTypeDetails.getName();

    for (final JavaType type : itdTypeDetails.getGovernor().getLayerEntities()) {
      final String localMidType = entityToDodMidMap.get(type);
      if (localMidType != null) {
        return localMidType;
      }
    }

    final String localMid = entityToDodMidMap.get(governor);
    if (localMid == null) {
      // No DOD is interested in this JavaType, so let's move on
      return null;
    }

    // We have some DOD metadata, so let's check if we care if any methods
    // match our requirements
    for (final MethodMetadata method : itdTypeDetails.getDeclaredMethods()) {
      if (BeanInfoUtils.isMutatorMethod(method)) {
        // A DOD cares about the JavaType, and an ITD offers a method
        // likely of interest, so let's formally trigger it to run.
        // Note that it will re-scan and discover this ITD, and register
        // a direct dependency on it for the future.
        return localMid;
      }
    }

    return null;
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String dodMetadataId,
      final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final String itdFilename) {

    if (layerService == null) {
      layerService = getLayerService();
    }
    Validate.notNull(layerService, "LayerService is required");

    // We need to parse the annotation, which we expect to be present
    final JpaDataOnDemandAnnotationValues annotationValues =
        new JpaDataOnDemandAnnotationValues(governorPhysicalTypeMetadata);
    final JavaType entity = annotationValues.getEntity();
    if (!annotationValues.isAnnotationFound() || entity == null) {
      return null;
    }

    // Remember that this entity JavaType matches up with this DOD's
    // metadata identification string
    // Start by clearing the previous association
    final JavaType oldEntity = dodMidToEntityMap.get(dodMetadataId);
    if (oldEntity != null) {
      entityToDodMidMap.remove(oldEntity);
    }
    entityToDodMidMap.put(annotationValues.getEntity(), dodMetadataId);
    dodMidToEntityMap.put(dodMetadataId, annotationValues.getEntity());

    final JavaType identifierType = getPersistenceMemberLocator().getIdentifierType(entity);
    if (identifierType == null) {
      return null;
    }

    final MemberDetails memberDetails = getMemberDetails(entity);
    if (memberDetails == null) {
      return null;
    }

    // Get associated factory class (factory for current associated entity)
    Set<ClassOrInterfaceTypeDetails> entityFactoryClasses =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_JPA_ENTITY_FACTORY);
    JpaEntityFactoryMetadata entityFactoryMetadata = null;
    for (ClassOrInterfaceTypeDetails cid : entityFactoryClasses) {
      if (((JavaType) cid.getAnnotation(RooJavaType.ROO_JPA_ENTITY_FACTORY).getAttribute("entity")
          .getValue()).equals(entity)) {
        String entityFactoryIdentifier = JpaEntityFactoryMetadata.createIdentifier(cid);

        // Register dependency between EntityFactoryMetadata and DataOnDemandMetadata
        registerDependency(entityFactoryIdentifier, dodMetadataId);

        // Obtain related entity EntityFactoryMetadata
        entityFactoryMetadata = getMetadataService().get(entityFactoryIdentifier);
        break;
      }
    }
    if (entityFactoryMetadata == null) {
      return null;
    }

    // Register JpaDataOndDemandConfiguration as downstream dependency
    Set<ClassOrInterfaceTypeDetails> dataOnDemandConfigurationClasses =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_JPA_DATA_ON_DEMAND_CONFIGURATION);
    if (dataOnDemandConfigurationClasses.isEmpty()) {
      return null;
    }
    ClassOrInterfaceTypeDetails jpaDataOnDemandConfigurationDetails =
        dataOnDemandConfigurationClasses.iterator().next();
    String dodConfigurationId =
        JpaDataOnDemandConfigurationMetadata.createIdentifier(jpaDataOnDemandConfigurationDetails);
    registerDependency(dodMetadataId, dodConfigurationId);

    return new JpaDataOnDemandMetadata(dodMetadataId, aspectName, governorPhysicalTypeMetadata,
        annotationValues, entityFactoryMetadata);
  }

  public String getProvidesType() {
    return JpaDataOnDemandMetadata.getMetadataIdentiferType();
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

  protected LayerService getLayerService() {
    return serviceInstaceManager.getServiceInstance(this, LayerService.class);
  }

}
