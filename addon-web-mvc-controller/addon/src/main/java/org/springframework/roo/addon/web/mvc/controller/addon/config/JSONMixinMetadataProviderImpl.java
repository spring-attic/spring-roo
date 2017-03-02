package org.springframework.roo.addon.web.mvc.controller.addon.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
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
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link JSONMixinMetadataProvider}.
 *
 * @author Jose Manuel Vivó
 * @since 2.0
 */
@Component
@Service
public class JSONMixinMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements JSONMixinMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(JSONMixinMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToServiceMidMap =
      new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_JSON_MIXIN} as additional
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

    addMetadataTrigger(RooJavaType.ROO_JSON_MIXIN);
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

    removeMetadataTrigger(RooJavaType.ROO_JSON_MIXIN);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return JSONMixinMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = JSONMixinMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = JSONMixinMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "JSONMixin";
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

    final JSONMixinAnnotationValues values =
        new JSONMixinAnnotationValues(governorPhysicalTypeMetadata);
    final JavaType mixinType = governorPhysicalTypeMetadata.getType();

    final JavaType entity = values.getEntity();

    final ClassOrInterfaceTypeDetails entityDetails =
        getTypeLocationService().getTypeDetails(entity);

    Validate.notNull(entityDetails, "Can't get details of '%s' defined on '%s.@%s.entity'",
        entity.getFullyQualifiedTypeName(), mixinType,
        RooJavaType.ROO_JSON_MIXIN.getSimpleTypeName());

    Validate
        .notNull(
            entityDetails.getAnnotation(RooJavaType.ROO_JPA_ENTITY),
            "Class '%s' defined on '%s.@%s.entity' has no @%s annotation. Only JPA entities can set as mixin",
            entity.getFullyQualifiedTypeName(), mixinType,
            RooJavaType.ROO_JSON_MIXIN.getSimpleTypeName());

    final String entityId = JpaEntityMetadata.createIdentifier(entityDetails);
    final JpaEntityMetadata entityMetadata = getMetadataService().get(entityId);

    if (entityMetadata == null) {
      // not ready for this metadata yet
      return null;
    }
    // register metadata dependency
    registerDependency(entityId, metadataIdentificationString);

    // Register dependency with DomainModelModule
    Set<ClassOrInterfaceTypeDetails> domainModelModuleDetails =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_DOMAIN_MODEL_MODULE);
    if (!domainModelModuleDetails.isEmpty()) {
      String domainModelModuleMetadataId =
          DomainModelModuleMetadata.createIdentifier(domainModelModuleDetails.iterator().next());
      registerDependency(metadataIdentificationString, domainModelModuleMetadataId);
    }

    Map<FieldMetadata, JavaType> jsonDeserializerByEntity =
        new TreeMap<FieldMetadata, JavaType>(FieldMetadata.COMPARATOR_BY_NAME);
    for (FieldMetadata field : entityMetadata.getRelationsAsChild().values()) {
      if (isAnyToOneRelation(field)) {

        JavaType parentEntity = field.getFieldType();
        JavaType entityDeserializer = getEntityDeserializerFor(parentEntity);
        Validate.notNull(entityDeserializer,
            "Can't locate class with @%s.entity=%s required for %s entity Json Mixin (%s)",
            RooJavaType.ROO_DESERIALIZER, parentEntity, entity.getFullyQualifiedTypeName(),
            mixinType.getFullyQualifiedTypeName());
        jsonDeserializerByEntity.put(field, entityDeserializer);
      }
    }

    return new JSONMixinMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, values, entityMetadata, jsonDeserializerByEntity);

  }

  private JavaType getEntityDeserializerFor(JavaType entity) {
    Set<ClassOrInterfaceTypeDetails> deserializers =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_DESERIALIZER);

    for (ClassOrInterfaceTypeDetails deserializer : deserializers) {
      AnnotationMetadata annotation = deserializer.getAnnotation(RooJavaType.ROO_DESERIALIZER);
      AnnotationAttributeValue<JavaType> annotationValue = annotation.getAttribute("entity");
      if (entity.equals(annotationValue.getValue())) {
        return deserializer.getType();
      }
    }
    return null;
  }

  /**
   * Return true if field is annotated with @OneToOne or @ManyToOne JPA annotation
   *
   * @param field
   * @return
   */
  private boolean isAnyToOneRelation(FieldMetadata field) {
    return field.getAnnotation(JpaJavaType.MANY_TO_ONE) != null
        || field.getAnnotation(JpaJavaType.ONE_TO_ONE) != null;
  }

  public String getProvidesType() {
    return JSONMixinMetadata.getMetadataIdentiferType();
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
