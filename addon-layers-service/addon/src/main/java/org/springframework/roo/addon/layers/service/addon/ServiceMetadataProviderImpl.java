package org.springframework.roo.addon.layers.service.addon;

import static org.springframework.roo.model.RooJavaType.ROO_SERVICE;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.finder.addon.FinderMetadata;
import org.springframework.roo.addon.finder.addon.parser.FinderMethod;
import org.springframework.roo.addon.finder.addon.parser.FinderParameter;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaCustomMetadata;
import org.springframework.roo.addon.layers.repository.jpa.addon.RepositoryJpaMetadata;
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
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
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
    context = cContext.getBundleContext();
    super.setDependsOnGovernorBeingAClass(false);
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_SERVICE);

    this.keyDecoratorTracker =
        new CustomDataKeyDecoratorTracker(context, getClass(), new LayerTypeMatcher(ROO_SERVICE,
            new JavaSymbolName(RooService.ENTITY_ATTRIBUTE)));
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

    // Check if current entity is annotated with @RooJpaEntity
    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(entity);

    AnnotationMetadata entityAnnotation = entityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY);
    Validate
        .notNull(entityAnnotation,
            "ERROR: You should specify an entity annotated with @RooJpaEntity on @RooService annotation");

    // Check if readOnly
    AnnotationAttributeValue<Boolean> readOnlyAttribute = entityAnnotation.getAttribute("readOnly");

    boolean readOnly = false;
    if (readOnlyAttribute != null && readOnlyAttribute.getValue()) {
      readOnly = true;
    }

    // Getting associated repository
    ClassOrInterfaceTypeDetails repositoryDetails = null;
    Set<ClassOrInterfaceTypeDetails> repositories =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_REPOSITORY_JPA);
    for (ClassOrInterfaceTypeDetails repo : repositories) {
      AnnotationAttributeValue<JavaType> entityAttr =
          repo.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA).getAttribute("entity");
      if (entityAttr != null && entityAttr.getValue().equals(entity)) {
        repositoryDetails = repo;
      }
    }

    // Check if we have a valid repository
    Validate
        .notNull(
            repositoryDetails,
            String
                .format(
                    "ERROR: You must generate some @RooJpaRepository for entity '%s' to be able to generate services",
                    entity.getSimpleTypeName()));

    // Getting finders to be included on current service
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(repositoryDetails.getDeclaredByMetadataId());
    final String finderMetadataKey =
        FinderMetadata.createIdentifier(repositoryDetails.getType(), logicalPath);
    registerDependency(finderMetadataKey, metadataIdentificationString);
    final FinderMetadata finderMetadata =
        (FinderMetadata) getMetadataService().get(finderMetadataKey);

    List<FinderMethod> finders = new ArrayList<FinderMethod>();
    if (finderMetadata != null) {
      finders = finderMetadata.getFinders();

      // Add dependencies between modules
      for (FinderMethod finder : finders) {
        List<JavaType> types = new ArrayList<JavaType>();
        types.add(finder.getReturnType());
        types.addAll(finder.getReturnType().getParameters());

        for (FinderParameter parameter : finder.getParameters()) {
          types.add(parameter.getType());
          types.addAll(parameter.getType().getParameters());
        }

        for (JavaType parameter : types) {
          getTypeLocationService().addModuleDependency(
              governorPhysicalTypeMetadata.getType().getModule(), parameter);
        }
      }
    }

    // Getting methods declared on related RepositoryJpaCustomMetadata
    ClassOrInterfaceTypeDetails customRepositoryDetails = null;
    Set<ClassOrInterfaceTypeDetails> customRepositories =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_REPOSITORY_JPA_CUSTOM);
    for (ClassOrInterfaceTypeDetails customRepo : customRepositories) {
      AnnotationAttributeValue<JavaType> entityAttr =
          customRepo.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA_CUSTOM).getAttribute("entity");
      if (entityAttr != null && entityAttr.getValue().equals(entity)) {
        customRepositoryDetails = customRepo;
      }
    }

    // Check if we have a valid repository
    Validate
        .notNull(
            repositoryDetails,
            String
                .format(
                    "ERROR: A valid @RooJpaCustomRepository for entity '%s' doesn't exists or has been deleted",
                    entity.getSimpleTypeName()));

    // Getting finders to be included on current service
    final LogicalPath customLogicalPath =
        PhysicalTypeIdentifier.getPath(customRepositoryDetails.getDeclaredByMetadataId());
    final String customRepositoryMetadataKey =
        RepositoryJpaCustomMetadata.createIdentifier(customRepositoryDetails.getType(),
            customLogicalPath);
    registerDependency(customRepositoryMetadataKey, metadataIdentificationString);
    final RepositoryJpaCustomMetadata repositoryCustomMetadata =
        (RepositoryJpaCustomMetadata) getMetadataService().get(customRepositoryMetadataKey);

    final LogicalPath repositoryLogicalPath =
        PhysicalTypeIdentifier.getPath(repositoryDetails.getDeclaredByMetadataId());
    final String repositoryMetadataKey =
        RepositoryJpaMetadata.createIdentifier(repositoryDetails.getType(), repositoryLogicalPath);
    registerDependency(repositoryMetadataKey, metadataIdentificationString);
    final RepositoryJpaMetadata repositoryMetadata =
        (RepositoryJpaMetadata) getMetadataService().get(repositoryMetadataKey);

    Map<FieldMetadata, MethodMetadata> countByReferencedFieldMethods = null;
    if (repositoryMetadata != null) {
      countByReferencedFieldMethods = repositoryMetadata.getCountMethodByReferencedFields();
    }

    return new ServiceMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, entity, identifierType, readOnly, finders,
        repositoryCustomMetadata.getFindAllGlobalSearchMethod(),
        repositoryCustomMetadata.getReferencedFieldsFindAllMethods(), countByReferencedFieldMethods);
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
