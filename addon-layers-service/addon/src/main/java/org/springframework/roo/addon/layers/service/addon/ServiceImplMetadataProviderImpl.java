package org.springframework.roo.addon.layers.service.addon;

import static org.springframework.roo.model.RooJavaType.ROO_SERVICE;
import static org.springframework.roo.model.RooJavaType.ROO_SERVICE_IMPL;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.layers.service.annotations.RooServiceImpl;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerTypeMatcher;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
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
    super.setDependsOnGovernorBeingAClass(false);
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

    // Validate that contains service interface
    Validate.notNull(serviceInterface,
        "ERROR: You need to specify service interface to be implemented.");

    ClassOrInterfaceTypeDetails interfaceTypeDetails =
        getTypeLocationService().getTypeDetails(serviceInterface);

    // Getting related entity
    AnnotationMetadata serviceAnnotation = interfaceTypeDetails.getAnnotation(ROO_SERVICE);

    Validate.notNull(serviceAnnotation,
        "ERROR: Service interface should be annotated with @RooService");

    AnnotationAttributeValue<JavaType> relatedEntity = serviceAnnotation.getAttribute("entity");

    Validate.notNull(relatedEntity,
        "ERROR: @RooService annotation should has a reference to managed entity");

    JavaType entity = relatedEntity.getValue();

    ClassOrInterfaceTypeDetails entityDetails = getTypeLocationService().getTypeDetails(entity);
    AnnotationMetadata entityAnnotation = entityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY);

    Validate.notNull(entityAnnotation,
        "ERROR: Related entity should be annotated with @RooJpaEntity");

    // Getting entity identifier type
    final JavaType identifierType = getPersistenceMemberLocator().getIdentifierType(entity);

    Validate.notNull(identifierType,
        "ERROR: Related entity should define a field annotated with @Id");

    // Check if related entity is readOnly or not
    AnnotationAttributeValue<Boolean> readOnlyAttribute = entityAnnotation.getAttribute("readOnly");

    boolean readOnly = false;
    if (readOnlyAttribute != null && readOnlyAttribute.getValue()) {
      readOnly = true;
    }

    // Getting related repository to current entity
    Set<ClassOrInterfaceTypeDetails> repositories =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_REPOSITORY_JPA);

    ClassOrInterfaceTypeDetails repositoryDetails = null;
    for (ClassOrInterfaceTypeDetails repository : repositories) {
      AnnotationAttributeValue<JavaType> entityAttr =
          repository.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA).getAttribute("entity");

      if (entityAttr.getValue().equals(entity)) {
        repositoryDetails = repository;
      }
    }

    return new ServiceImplMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, serviceInterface, repositoryDetails, entity, identifierType,
        readOnly);
  }

  public String getProvidesType() {
    return ServiceImplMetadata.getMetadataIdentiferType();
  }
}
