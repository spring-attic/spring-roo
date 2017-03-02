package org.springframework.roo.addon.web.mvc.controller.addon.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.javabean.addon.JavaBeanMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.layers.service.addon.ServiceLocator;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link EntityDeserializerMetadataProvider}.
 *
 * @author Jose Manuel Vivó
 * @since 2.0
 */
@Component
@Service
public class EntityDeserializerMetadataProviderImpl extends
    AbstractMemberDiscoveringItdMetadataProvider implements EntityDeserializerMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(EntityDeserializerMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToServiceMidMap =
      new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_DESERIALIZER} as additional
   * JavaType that will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  protected void activate(final ComponentContext cContext) {
    super.activate(cContext);
    context = cContext.getBundleContext();
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(RooJavaType.ROO_DESERIALIZER);
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

    removeMetadataTrigger(RooJavaType.ROO_DESERIALIZER);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return EntityDeserializerMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = EntityDeserializerMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = EntityDeserializerMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "EntityDeserializer";
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

    final EntityDeserializerAnnotationValues values =
        new EntityDeserializerAnnotationValues(governorPhysicalTypeMetadata);
    final JavaType deserializerType = governorPhysicalTypeMetadata.getType();

    final JavaType entity = values.getEntity();

    final ClassOrInterfaceTypeDetails entityDetails =
        getTypeLocationService().getTypeDetails(entity);

    Validate.notNull(entityDetails, "Can't get details of '%s' defined on '%s.@%s.entity'",
        entity.getFullyQualifiedTypeName(), deserializerType,
        RooJavaType.ROO_DESERIALIZER.getSimpleTypeName());

    Validate
        .notNull(
            entityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY),
            "Class '%s' defined on '%s.@%s.entity' has no @%s annotation. Only JPA entities can set as mixin",
            entity.getFullyQualifiedTypeName(), deserializerType,
            RooJavaType.ROO_DESERIALIZER.getSimpleTypeName());

    // Register JpaEntityMetadata dependency
    final String entityId = JpaEntityMetadata.createIdentifier(entityDetails);
    final JpaEntityMetadata entityMetadata = getMetadataService().get(entityId);
    if (entityMetadata == null) {
      // not ready to this metadata yet
      return null;
    }
    registerDependency(entityId, metadataIdentificationString);

    // Register JavaBeanMetadata dependency
    final String javaBeanId = JavaBeanMetadata.createIdentifier(entityDetails);
    final JavaBeanMetadata javaBeanMetadata = getMetadataService().get(javaBeanId);
    if (javaBeanMetadata == null) {
      // not ready to this metadata yet
      return null;
    }
    registerDependency(javaBeanId, metadataIdentificationString);

    // Register ServiceMetadata dependency
    ClassOrInterfaceTypeDetails serviceDetails = getServiceLocator().getService(entity);
    String serviceMetadataId = ServiceMetadata.createIdentifier(serviceDetails);
    ServiceMetadata serviceMetadata = getMetadataService().get(serviceMetadataId);
    if (serviceMetadata == null) {
      // not ready to this metadata yet
      return null;
    }


    return new EntityDeserializerMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, values, entityMetadata, serviceMetadata);
  }

  public String getProvidesType() {
    return EntityDeserializerMetadata.getMetadataIdentiferType();
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

  private ServiceLocator getServiceLocator() {
    return this.getServiceManager().getServiceInstance(this, ServiceLocator.class);
  }
}
