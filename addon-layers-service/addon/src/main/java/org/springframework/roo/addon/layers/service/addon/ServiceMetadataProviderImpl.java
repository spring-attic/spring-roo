package org.springframework.roo.addon.layers.service.addon;

import static org.springframework.roo.model.RooJavaType.ROO_SERVICE;

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
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaCustomMetadata;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaLocator;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaMetadata;
import org.springframework.roo.addon.layers.repository.jpa.addon.finder.parser.PartTree;
import org.springframework.roo.addon.layers.service.annotations.RooService;
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
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerTypeMatcher;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link ServiceMetadataProvider}.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ServiceMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements ServiceMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils.getLogger(ServiceMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToServiceMidMap =
      new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_SERVICE} as additional
   * JavaType that will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  @SuppressWarnings("unchecked")
  protected void activate(final ComponentContext cContext) {
    super.activate(cContext);
    super.setDependsOnGovernorBeingAClass(false);
    this.registryTracker =
        new MetadataDependencyRegistryTracker(cContext.getBundleContext(), this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_SERVICE);

    this.keyDecoratorTracker =
        new CustomDataKeyDecoratorTracker(cContext.getBundleContext(), getClass(),
            new LayerTypeMatcher(ROO_SERVICE, new JavaSymbolName(RooService.ENTITY_ATTRIBUTE)));
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

    removeMetadataTrigger(ROO_SERVICE);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return ServiceMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = ServiceMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = ServiceMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Service";
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
    final ServiceAnnotationValues annotationValues =
        new ServiceAnnotationValues(governorPhysicalTypeMetadata);

    // Getting entity
    JavaType entity = annotationValues.getEntity();

    Validate.notNull(entity, "ERROR: You should specify a valid entity on @RooService annotation");

    final JavaType identifierType = getPersistenceMemberLocator().getIdentifierType(entity);

    Validate.notNull(identifierType,
        "ERROR: You should specify a valid entity on @RooService annotation");


    final JpaEntityMetadata entityMetadata = getEntityMetadata(entity);
    if (entityMetadata == null) {
      return null;
    }
    registerDependency(entityMetadata.getId(), metadataIdentificationString);

    // Getting associated repository
    ClassOrInterfaceTypeDetails repositoryDetails = getRepositoryJpaLocator().getRepository(entity);

    // Check if we have a valid repository
    Validate
        .notNull(
            repositoryDetails,
            String
                .format(
                    "ERROR: You must generate some @RooJpaRepository for entity '%s' to be able to generate services",
                    entity.getSimpleTypeName()));

    // Get repository metadata
    final String repositoryMetadataKey = RepositoryJpaMetadata.createIdentifier(repositoryDetails);
    registerDependency(repositoryMetadataKey, metadataIdentificationString);
    final RepositoryJpaMetadata repositoryMetadata =
        getMetadataService().get(repositoryMetadataKey);

    List<MethodMetadata> finders = new ArrayList<MethodMetadata>();
    List<MethodMetadata> countMethods = new ArrayList<MethodMetadata>();

    if (repositoryMetadata == null) {
      // Can't generate metadata yet
      return null;
    }


    // Add dependencies between modules
    for (MethodMetadata finder : repositoryMetadata.getFindersGenerated()) {

      // Add to service finders list
      finders.add(finder);

      registerDependencyModulesOfFinder(governorPhysicalTypeMetadata, finder);
    }

    // Get count methods
    countMethods.addAll(repositoryMetadata.getCountMethods());

    // Get finders and its associated count method from repository metadata
    Map<JavaSymbolName, MethodMetadata> repositoryFindersAndCounts =
        repositoryMetadata.getFinderMethodsAndCounts();

    // Getting methods declared on related RepositoryJpaCustomMetadata
    final JavaType customRepository = repositoryMetadata.getCustomRepository();

    final ClassOrInterfaceTypeDetails customRepositoryDetails =
        getTypeLocationService().getTypeDetails(customRepository);
    final String customRepositoryMetadataKey =
        RepositoryJpaCustomMetadata.createIdentifier(customRepositoryDetails);
    final RepositoryJpaCustomMetadata repositoryCustomMetadata =
        getMetadataService().get(customRepositoryMetadataKey);

    // Get finders and its associated count method from custom repository metadata
    Map<JavaSymbolName, MethodMetadata> repositoryCustomFindersAndCounts =
        repositoryCustomMetadata.getFinderMethodsAndCounts();

    // Check if we have a valid custom repository
    Validate
        .notNull(
            repositoryCustomMetadata,
            String
                .format(
                    "ERROR: Can't found a class @RooJpaRepositoryCustom for entity '%s' to be able to generate services",
                    entity.getSimpleTypeName()));
    registerDependency(customRepositoryMetadataKey, metadataIdentificationString);


    final Map<FieldMetadata, MethodMetadata> countByReferencedFieldMethods =
        new HashMap<FieldMetadata, MethodMetadata>(
            repositoryMetadata.getCountMethodByReferencedFields());

    // Add custom finders to finders list and add dependencies between modules
    for (Pair<MethodMetadata, PartTree> finderInfo : repositoryCustomMetadata
        .getCustomFinderMethods()) {

      // Add to service finders list
      finders.add(finderInfo.getKey());

      registerDependencyModulesOfFinder(governorPhysicalTypeMetadata, finderInfo.getKey());
    }

    // Add custom count methods to count method list
    for (Pair<MethodMetadata, PartTree> countInfo : repositoryCustomMetadata
        .getCustomCountMethods()) {
      countMethods.add(countInfo.getKey());
    }

    // Get related entities metadata
    final Map<JavaType, JpaEntityMetadata> relatedEntities =
        new HashMap<JavaType, JpaEntityMetadata>();

    // As parent
    JavaType childEntity;
    JpaEntityMetadata childEntityMetadata;
    for (RelationInfo info : entityMetadata.getRelationInfos().values()) {
      childEntity = info.fieldMetadata.getFieldType().getBaseType();
      if (relatedEntities.containsKey(childEntity)) {
        continue;
      }
      childEntityMetadata = getEntityMetadata(childEntity);
      if (childEntityMetadata == null) {
        // We need child metadata. Return null waiting next metadata iteration
        return null;
      }
      registerDependency(childEntityMetadata.getId(), metadataIdentificationString);
      relatedEntities.put(childEntity, childEntityMetadata);
    }

    // As child
    JavaType parentEntity;
    JpaEntityMetadata parentEntityMetadata;
    for (FieldMetadata fieldAsChild : entityMetadata.getRelationsAsChild().values()) {
      parentEntity = fieldAsChild.getFieldType().getBaseType();
      if (relatedEntities.containsKey(parentEntity)) {
        continue;
      }
      parentEntityMetadata = getEntityMetadata(parentEntity);
      if (parentEntityMetadata == null) {
        // We need parent metadata. Return null waiting next metadata iteration
        return null;
      }
      registerDependency(parentEntityMetadata.getId(), metadataIdentificationString);
      relatedEntities.put(parentEntity, parentEntityMetadata);
    }

    return new ServiceMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, entity, identifierType, entityMetadata, repositoryMetadata,
        finders, repositoryCustomMetadata.getCurrentFindAllGlobalSearchMethod(),
        repositoryCustomMetadata.getReferencedFieldsFindAllMethods(),
        countByReferencedFieldMethods, countMethods, relatedEntities, repositoryFindersAndCounts,
        repositoryCustomFindersAndCounts);
  }

  private void registerDependencyModulesOfFinder(
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, MethodMetadata finder) {
    // Add dependencies between modules
    List<JavaType> types = new ArrayList<JavaType>();
    types.add(finder.getReturnType());
    types.addAll(finder.getReturnType().getParameters());

    for (AnnotatedJavaType parameter : finder.getParameterTypes()) {
      types.add(parameter.getJavaType());
      types.addAll(parameter.getJavaType().getParameters());
    }

    for (JavaType parameter : types) {
      getTypeLocationService().addModuleDependency(
          governorPhysicalTypeMetadata.getType().getModule(), parameter);
    }
  }

  /**
   * Gets {@link JpaEntityMetadata} from a entity
   *
   * @param entity
   * @return
   */
  private JpaEntityMetadata getEntityMetadata(JavaType entity) {
    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(entity);
    return getMetadataService().get(JpaEntityMetadata.createIdentifier(entityDetails));
  }

  private RepositoryJpaLocator getRepositoryJpaLocator() {
    return getServiceManager().getServiceInstance(this, RepositoryJpaLocator.class);
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
    return ServiceMetadata.getMetadataIdentiferType();
  }
}
