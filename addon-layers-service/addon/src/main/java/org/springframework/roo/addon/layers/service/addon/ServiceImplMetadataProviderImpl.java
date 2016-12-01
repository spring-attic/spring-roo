package org.springframework.roo.addon.layers.service.addon;

import static org.springframework.roo.model.RooJavaType.ROO_SERVICE_IMPL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.addon.JpaOperations;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.jpa.annotations.entity.JpaRelationType;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaLocator;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaMetadata;
import org.springframework.roo.addon.layers.service.annotations.RooServiceImpl;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerTypeMatcher;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link ServiceImplMetadataProvider}.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ServiceImplMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements ServiceImplMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(ServiceImplMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToServiceMidMap =
      new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_SERVICE_IMPL} as additional
   * JavaType that will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  @SuppressWarnings("unchecked")
  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_SERVICE_IMPL);

    this.keyDecoratorTracker =
        new CustomDataKeyDecoratorTracker(context, getClass(), new LayerTypeMatcher(
            ROO_SERVICE_IMPL, new JavaSymbolName(RooServiceImpl.SERVICE_ATTRIBUTE)));
    this.keyDecoratorTracker.open();
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

    removeMetadataTrigger(ROO_SERVICE_IMPL);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return ServiceImplMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = ServiceImplMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = ServiceImplMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Service_Impl";
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    // Determine the governor for this ITD, and whether any metadata is even
    // hoping to hear about changes to that JavaType and its ITDs
    final JavaType governor = itdTypeDetails.getName();
    final String localMid = domainTypeToServiceMidMap.get(governor);
    if (localMid != null) {
      return localMid;
    }

    final MemberHoldingTypeDetails memberHoldingTypeDetails =
        getTypeLocationService().getTypeDetails(governor);
    if (memberHoldingTypeDetails != null) {
      for (final JavaType type : memberHoldingTypeDetails.getLayerEntities()) {
        final String localMidType = domainTypeToServiceMidMap.get(type);
        if (localMidType != null) {
          return localMidType;
        }
      }
    }
    return null;
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
    final ServiceImplAnnotationValues annotationValues =
        new ServiceImplAnnotationValues(governorPhysicalTypeMetadata);

    // Getting service interface
    JavaType serviceInterface = annotationValues.getService();

    ClassOrInterfaceTypeDetails serviceInterfaceDetails =
        getTypeLocationService().getTypeDetails(serviceInterface);
    final ServiceMetadata serviceMetadata =
        getServiceMetadata(metadataIdentificationString, serviceInterfaceDetails);

    AnnotationMetadata serviceAnnotation =
        serviceInterfaceDetails.getAnnotation(RooJavaType.ROO_SERVICE);

    Validate.notNull(serviceAnnotation,
        "ERROR: Provided service should be annotated with @RooService");

    JavaType entity = (JavaType) serviceAnnotation.getAttribute("entity").getValue();

    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(entity);
    final String entityMetadataId =
        JpaEntityMetadata.createIdentifier(entityDetails.getType(),
            PhysicalTypeIdentifier.getPath(entityDetails.getDeclaredByMetadataId()));
    final JpaEntityMetadata entityMetadata =
        (JpaEntityMetadata) getMetadataService().get(entityMetadataId);

    // Add dependencies between modules
    if (serviceMetadata != null) {
      for (MethodMetadata method : serviceMetadata.getAllMethods()) {
        List<JavaType> types = new ArrayList<JavaType>();
        types.add(method.getReturnType());
        types.addAll(method.getReturnType().getParameters());

        for (AnnotatedJavaType parameter : method.getParameterTypes()) {
          types.add(AnnotatedJavaType.convertFromAnnotatedJavaType(parameter));
          types.addAll(AnnotatedJavaType.convertFromAnnotatedJavaType(parameter).getParameters());
        }

        for (JavaType parameter : types) {
          getTypeLocationService().addModuleDependency(
              governorPhysicalTypeMetadata.getType().getModule(), parameter);
        }
      }
    }

    // Getting associated repository
    ClassOrInterfaceTypeDetails repositoryDetails = getRepositoryJpaLocator().getRepository(entity);
    final String repositoryMetadataId =
        RepositoryJpaMetadata.createIdentifier(repositoryDetails.getType(),
            PhysicalTypeIdentifier.getPath(repositoryDetails.getDeclaredByMetadataId()));
    final RepositoryJpaMetadata repositoryMetadata =
        (RepositoryJpaMetadata) getMetadataService().get(repositoryMetadataId);

    // Locate related services API types required by relations
    Map<JavaType, ServiceMetadata> requiredServicesByEntity =
        new HashMap<JavaType, ServiceMetadata>();
    ClassOrInterfaceTypeDetails relatedService;
    for (RelationInfo info : entityMetadata.getRelationInfos().values()) {
      if (info.cardinality != Cardinality.ONE_TO_ONE
          && !requiredServicesByEntity.containsKey(info.childType)) {
        relatedService = getServiceLocator().getFirstService(info.childType);
        if (relatedService == null) {
          // Exit without metadata. This should be called in next metadata iteration
          return null;
        }
        requiredServicesByEntity.put(info.childType,
            getServiceMetadata(metadataIdentificationString, relatedService));
      }
    }

    // Get child relations info
    List<Pair<FieldMetadata, RelationInfo>> childRelationsInfo =
        getJpaOperations().getFieldChildPartOfRelation(entityDetails);

    return new ServiceImplMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, serviceInterface, repositoryDetails.getType(),
        repositoryMetadata, entity, entityMetadata, serviceMetadata, requiredServicesByEntity,
        childRelationsInfo);
  }

  /**
   * Returns {@link ServiceMetadata} of _serviceInterfaceDetails_
   *
   * Also register metadata dependencies
   *
   * @param metadataIdentificationString
   * @param serviceInterfaceDetails
   * @return
   */
  private ServiceMetadata getServiceMetadata(final String metadataIdentificationString,
      ClassOrInterfaceTypeDetails serviceInterfaceDetails) {
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(serviceInterfaceDetails.getDeclaredByMetadataId());
    final String serviceMetadataKey =
        ServiceMetadata.createIdentifier(serviceInterfaceDetails.getType(), logicalPath);
    registerDependency(serviceMetadataKey, metadataIdentificationString);
    final ServiceMetadata serviceMetadata =
        (ServiceMetadata) getMetadataService().get(serviceMetadataKey);
    return serviceMetadata;
  }

  private RepositoryJpaLocator getRepositoryJpaLocator() {
    return getServiceManager().getServiceInstance(this, RepositoryJpaLocator.class);
  }

  private ServiceLocator getServiceLocator() {
    return getServiceManager().getServiceInstance(this, ServiceLocator.class);
  }

  private JpaOperations getJpaOperations() {
    return getServiceManager().getServiceInstance(this, JpaOperations.class);
  }

  private void registerDependency(final String upstreamDependency, final String downStreamDependency) {

    if (getMetadataDependencyRegistry() != null
        && StringUtils.isNotBlank(upstreamDependency)
        && StringUtils.isNotBlank(downStreamDependency)
        && !upstreamDependency.equals(downStreamDependency)
        && !MetadataIdentificationUtils.getMetadataClass(downStreamDependency).equals(
            MetadataIdentificationUtils.getMetadataClass(upstreamDependency))) {
      getMetadataDependencyRegistry().registerDependency(upstreamDependency, downStreamDependency);
    }
  }

  public String getProvidesType() {
    return ServiceImplMetadata.getMetadataIdentiferType();
  }
}
