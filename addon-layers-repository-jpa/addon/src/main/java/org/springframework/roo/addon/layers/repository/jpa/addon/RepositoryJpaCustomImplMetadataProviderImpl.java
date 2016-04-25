package org.springframework.roo.addon.layers.repository.jpa.addon;

import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_JPA_CUSTOM;
import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_JPA_CUSTOM_IMPL;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.javabean.addon.JavaBeanMetadata;
import org.springframework.roo.addon.layers.repository.jpa.annotations.RooJpaRepositoryCustomImpl;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
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
 * Implementation of {@link RepositoryJpaCustomImplMetadataProvider}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class RepositoryJpaCustomImplMetadataProviderImpl extends
    AbstractMemberDiscoveringItdMetadataProvider implements RepositoryJpaCustomImplMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(RepositoryJpaCustomImplMetadataProviderImpl.class);

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
   * <li>Registers {@link RooJavaType#ROO_REPOSITORY_JPA_CUSTOM_IMPL} as additional 
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

    addMetadataTrigger(ROO_REPOSITORY_JPA_CUSTOM_IMPL);

    this.keyDecoratorTracker =
        new CustomDataKeyDecoratorTracker(context, getClass(), new LayerTypeMatcher(
            ROO_REPOSITORY_JPA_CUSTOM_IMPL, new JavaSymbolName(
                RooJpaRepositoryCustomImpl.REPOSITORY_ATTRIBUTE)));
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

    removeMetadataTrigger(ROO_REPOSITORY_JPA_CUSTOM_IMPL);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return RepositoryJpaCustomImplMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType =
        RepositoryJpaCustomImplMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = RepositoryJpaCustomImplMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Jpa_Repository_Impl";
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
    final RepositoryJpaCustomImplAnnotationValues annotationValues =
        new RepositoryJpaCustomImplAnnotationValues(governorPhysicalTypeMetadata);

    // Getting repository custom
    JavaType repositoryCustom = annotationValues.getRepository();

    // Validate that contains repository interface
    Validate.notNull(repositoryCustom,
        "ERROR: You need to specify interface repository to be implemented.");

    ClassOrInterfaceTypeDetails repositoryDetails =
        getTypeLocationService().getTypeDetails(repositoryCustom);

    AnnotationMetadata repositoryCustomAnnotation =
        repositoryDetails.getAnnotation(ROO_REPOSITORY_JPA_CUSTOM);

    Validate.notNull(repositoryCustomAnnotation,
        "ERROR: Repository interface should be annotated with @RooJpaRepositoryCustom");

    AnnotationAttributeValue<JavaType> entityAttribute =
        repositoryCustomAnnotation.getAttribute("entity");

    Validate
        .notNull(entityAttribute,
            "ERROR: Repository interface should be contain an entity on @RooJpaRepositoryCustom annotation");

    ClassOrInterfaceTypeDetails entityDetails =
        getTypeLocationService().getTypeDetails(entityAttribute.getValue());

    // Getting repository metadata
    final LogicalPath logicalPath =
        PhysicalTypeIdentifier.getPath(repositoryDetails.getDeclaredByMetadataId());
    final String repositoryMetadataKey =
        RepositoryJpaCustomMetadata.createIdentifier(repositoryDetails.getType(), logicalPath);
    final RepositoryJpaCustomMetadata repositoryCustomMetadata =
        (RepositoryJpaCustomMetadata) getMetadataService().get(repositoryMetadataKey);

    // Getting java bean metadata
    final LogicalPath entityLogicalPath =
        PhysicalTypeIdentifier.getPath(entityDetails.getDeclaredByMetadataId());
    final String javaBeanMetadataKey =
        JavaBeanMetadata.createIdentifier(entityDetails.getType(), entityLogicalPath);

    // Create dependency between repository and java bean annotations
    registerDependency(javaBeanMetadataKey, metadataIdentificationString);


    // Getting entity properties for findAll method
    ClassOrInterfaceTypeDetails searchResultDetails =
        getTypeLocationService().getTypeDetails(repositoryCustomMetadata.getSearchResult());

    final int maxFields = 5;
    List<FieldMetadata> validFields = new ArrayList<FieldMetadata>();
    for (FieldMetadata field : getMemberDetails(searchResultDetails).getFields()) {

      // Exclude non-simple fields
      if (!field.getFieldType().isCommonCollectionType()
          && getTypeLocationService().getTypeDetails(field.getFieldType()) == null) {
        validFields.add(field);

        if (validFields.size() == maxFields) {
          break;
        }
      }
    }

    return new RepositoryJpaCustomImplMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, annotationValues, entityAttribute.getValue(), validFields,
        repositoryCustomMetadata.getFindAllGlobalSearchMethod());
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
    return RepositoryJpaCustomImplMetadata.getMetadataIdentiferType();
  }
}
