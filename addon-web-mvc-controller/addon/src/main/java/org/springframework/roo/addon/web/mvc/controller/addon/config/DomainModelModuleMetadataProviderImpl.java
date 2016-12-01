package org.springframework.roo.addon.web.mvc.controller.addon.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Implementation of {@link DomainModelModuleMetadataProvider}.
 *
 * @author Jose Manuel Vivó
 * @since 2.0
 */
@Component
@Service
public class DomainModelModuleMetadataProviderImpl extends
    AbstractMemberDiscoveringItdMetadataProvider implements DomainModelModuleMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(DomainModelModuleMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToServiceMidMap =
      new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_DOMAIN_MODEL_MODULE} as additional
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

    addMetadataTrigger(RooJavaType.ROO_DOMAIN_MODEL_MODULE);
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

    removeMetadataTrigger(RooJavaType.ROO_DOMAIN_MODEL_MODULE);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return DomainModelModuleMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = DomainModelModuleMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = DomainModelModuleMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "DomainModelModule";
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

    AnnotationMetadata annotation =
        governorPhysicalTypeMetadata.getMemberHoldingTypeDetails().getAnnotation(
            RooJavaType.ROO_DOMAIN_MODEL_MODULE);

    Map<JavaType, JavaType> mixins = new HashMap<JavaType, JavaType>();

    Set<ClassOrInterfaceTypeDetails> allJsonMixin =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_JSON_MIXIN);

    JSONMixinAnnotationValues values;
    JavaType entity, previousMixin;
    ClassOrInterfaceTypeDetails entityDetails;
    for (ClassOrInterfaceTypeDetails mixin : allJsonMixin) {
      values = new JSONMixinAnnotationValues(mixin);

      entity = values.getEntity();

      entityDetails = getTypeLocationService().getTypeDetails(entity);

      Validate.notNull(entityDetails, "Can't get details of '%s' defined on '%s.@%s.entity'",
          entity.getFullyQualifiedTypeName(), mixin.getType(),
          RooJavaType.ROO_JSON_MIXIN.getSimpleTypeName());

      Validate
          .notNull(
              entityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY),
              "Class '%s' defined on '%s.@%s.entity' has no @%s annotation. Only JPA entities can set as mixin",
              entity.getFullyQualifiedTypeName(), mixin.getType(),
              RooJavaType.ROO_JSON_MIXIN.getSimpleTypeName());

      previousMixin = mixins.put(entity, mixin.getType());

      // Check than there isn't any previous mixin definition for current entity
      Validate.isTrue(previousMixin == null,
          "Found two classes annotates with @%s.entity = %s: %s and %s",
          RooJavaType.ROO_JSON_MIXIN.getSimpleTypeName(), entity.getFullyQualifiedTypeName(),
          mixin.getType(), previousMixin);

      // register metadata dependency
      registerDependency(JSONMixinMetadata.createIdentifier(mixin), metadataIdentificationString);


    }


    return new DomainModelModuleMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, mixins);
  }

  public String getProvidesType() {
    return DomainModelModuleMetadata.getMetadataIdentiferType();
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
}
