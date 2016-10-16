package org.springframework.roo.addon.layers.repository.jpa.addon;

import static org.springframework.roo.model.RooJavaType.ROO_READ_ONLY_REPOSITORY;
import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_JPA;
import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_JPA_CUSTOM;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.javabean.addon.JavaBeanMetadata;
import org.springframework.roo.addon.jpa.addon.JpaOperations;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepository;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
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
 * Implementation of {@link RepositoryJpaMetadataProvider}.
 *
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 1.2.0
 */
@Component
@Service
public class RepositoryJpaMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements RepositoryJpaMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(RepositoryJpaMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToRepositoryMidMap =
      new LinkedHashMap<JavaType, String>();
  private final Map<String, JavaType> repositoryMidToDomainTypeMap =
      new LinkedHashMap<String, JavaType>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_REPOSITORY_JPA} as additional
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

    addMetadataTrigger(ROO_REPOSITORY_JPA);

    this.keyDecoratorTracker =
        new CustomDataKeyDecoratorTracker(cContext.getBundleContext(), getClass(),
            new LayerTypeMatcher(ROO_REPOSITORY_JPA, new JavaSymbolName(
                RooJpaRepository.ENTITY_ATTRIBUTE)));
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

    removeMetadataTrigger(ROO_REPOSITORY_JPA);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return RepositoryJpaMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = RepositoryJpaMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = RepositoryJpaMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Jpa_Repository";
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    // Determine the governor for this ITD, and whether any metadata is even
    // hoping to hear about changes to that JavaType and its ITDs
    final JavaType governor = itdTypeDetails.getName();
    final String localMid = domainTypeToRepositoryMidMap.get(governor);
    if (localMid != null) {
      return localMid;
    }

    final MemberHoldingTypeDetails memberHoldingTypeDetails =
        getTypeLocationService().getTypeDetails(governor);
    if (memberHoldingTypeDetails != null) {
      for (final JavaType type : memberHoldingTypeDetails.getLayerEntities()) {
        final String localMidType = domainTypeToRepositoryMidMap.get(type);
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
    final RepositoryJpaAnnotationValues annotationValues =
        new RepositoryJpaAnnotationValues(governorPhysicalTypeMetadata);
    final JavaType domainType = annotationValues.getEntity();
    final List<FieldMetadata> identifiers =
        getPersistenceMemberLocator().getIdentifierFields(domainType);

    // Remember that this entity JavaType matches up with this metadata
    // identification string
    // Start by clearing any previous association
    final JavaType oldEntity = repositoryMidToDomainTypeMap.get(metadataIdentificationString);
    if (oldEntity != null) {
      domainTypeToRepositoryMidMap.remove(oldEntity);
    }
    domainTypeToRepositoryMidMap.put(domainType, metadataIdentificationString);
    repositoryMidToDomainTypeMap.put(metadataIdentificationString, domainType);

    // Getting associated entity
    JavaType entity = annotationValues.getEntity();

    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(entity);
    final String entityMetadataId =
        JpaEntityMetadata.createIdentifier(entityDetails.getType(),
            PhysicalTypeIdentifier.getPath(entityDetails.getDeclaredByMetadataId()));
    final JpaEntityMetadata entityMetadata =
        (JpaEntityMetadata) getMetadataService().get(entityMetadataId);

    if (entityMetadata == null) {
      return null;
    }

    // Getting java bean metadata
    final LogicalPath entityLogicalPath =
        PhysicalTypeIdentifier.getPath(entityDetails.getDeclaredByMetadataId());
    final String javaBeanMetadataKey =
        JavaBeanMetadata.createIdentifier(entityDetails.getType(), entityLogicalPath);

    // Create dependency between repository and java bean annotation
    registerDependency(javaBeanMetadataKey, metadataIdentificationString);

    // Register dependency between repositoryMetadata and jpaEntityMetadata
    registerDependency(entityMetadataId, metadataIdentificationString);

    // Check if related entity is setted as readOnly
    JavaType readOnlyRepository = null;
    if (entityMetadata.isReadOnly()) {
      // Getting ReadOnlyRepository interface annotated with
      // @RooReadOnlyRepository
      Set<ClassOrInterfaceTypeDetails> readOnlyRepositories =
          getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
              ROO_READ_ONLY_REPOSITORY);

      Validate
          .notEmpty(
              readOnlyRepositories,
              "ERROR: You should define a ReadOnlyRepository interface annotated with @RooReadOnlyRepository to be able to generate repositories of readOnly entities.");

      Iterator<ClassOrInterfaceTypeDetails> it = readOnlyRepositories.iterator();
      while (it.hasNext()) {
        ClassOrInterfaceTypeDetails readOnlyRepositoryDetails = it.next();
        readOnlyRepository = readOnlyRepositoryDetails.getType();
        break;
      }
    }

    List<JavaType> repositoryCustomList = new ArrayList<JavaType>();
    // Getting RepositoryCustom interface annotated with
    // @RooJpaRepositoryCustom
    Set<ClassOrInterfaceTypeDetails> customRepositories =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            ROO_REPOSITORY_JPA_CUSTOM);

    Iterator<ClassOrInterfaceTypeDetails> it = customRepositories.iterator();
    while (it.hasNext()) {
      ClassOrInterfaceTypeDetails customRepository = it.next();
      AnnotationMetadata annotation = customRepository.getAnnotation(ROO_REPOSITORY_JPA_CUSTOM);
      if (annotation.getAttribute("entity").getValue().equals(entity)) {
        repositoryCustomList.add(customRepository.getType());
      }
    }

    // Get field which entity is field part
    List<Pair<FieldMetadata, RelationInfo>> relationsAsChild =
        getJpaOperations().getFieldChildPartOfRelation(entityDetails);

    return new RepositoryJpaMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, annotationValues, identifiers, entityMetadata,
        readOnlyRepository, repositoryCustomList, relationsAsChild);
  }

  private JpaOperations getJpaOperations() {
    return getServiceManager().getServiceInstance(this, JpaOperations.class);
  }

  public String getProvidesType() {
    return RepositoryJpaMetadata.getMetadataIdentiferType();
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
}
