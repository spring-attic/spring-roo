package org.springframework.roo.addon.jpa.addon.entity.factories;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.EMBEDDED_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.EMBEDDED_ID_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MANY_TO_MANY_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MANY_TO_ONE_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.ONE_TO_MANY_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.ONE_TO_ONE_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSISTENT_TYPE;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.TRANSIENT_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.VERSION_FIELD;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ENTITY_FACTORY;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.addon.ConfigurableMetadataProvider;
import org.springframework.roo.addon.jpa.addon.dod.JpaDataOnDemandMetadata;
import org.springframework.roo.addon.jpa.addon.dod.EmbeddedHolder;
import org.springframework.roo.addon.jpa.addon.dod.EmbeddedIdHolder;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProviderTracker;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.shell.NaturalOrderComparator;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

/**
 * Implementation of {@link JpaEntityFactoryMetadataProvider}.
 *
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class JpaEntityFactoryMetadataProviderImpl extends
    AbstractMemberDiscoveringItdMetadataProvider implements JpaEntityFactoryMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(JpaEntityFactoryMetadataProviderImpl.class);

  protected MetadataDependencyRegistryTracker registryTracker = null;

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}</li>
   * <li>Create and open the {@link ItdTriggerBasedMetadataProviderTracker}
   * to track for {@link ConfigurableMetadataProvider} service.</li>
   * <li>Registers {@link RooJavaType#ROO_JPA_DATA_ON_DEMAND_CONFIGURATION} as 
   * additional JavaType that will trigger metadata registration.</li>
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

    addMetadataTrigger(ROO_JPA_ENTITY_FACTORY);

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

    removeMetadataTrigger(ROO_JPA_ENTITY_FACTORY);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return JpaEntityFactoryMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = JpaEntityFactoryMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = JpaEntityFactoryMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "JpaEntityFactory";
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    return getLocalMid(itdTypeDetails);
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String entityFactoryMetadata,
      final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata,
      final String itdFilename) {

    // Get related entity
    final JavaType entity =
        (JavaType) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails()
            .getAnnotation(ROO_JPA_ENTITY_FACTORY).getAttribute("entity").getValue();
    if (entity == null) {
      return null;
    }

    final MemberDetails memberDetails = getMemberDetails(entity);
    if (memberDetails == null) {
      return null;
    }

    final MemberHoldingTypeDetails persistenceMemberHoldingTypeDetails =
        MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(memberDetails,
            PERSISTENT_TYPE);
    if (persistenceMemberHoldingTypeDetails == null) {
      return null;
    }

    // We need to be informed if our dependent metadata changes
    getMetadataDependencyRegistry().registerDependency(
        persistenceMemberHoldingTypeDetails.getDeclaredByMetadataId(), entityFactoryMetadata);

    final JavaType identifierType = getPersistenceMemberLocator().getIdentifierType(entity);
    if (identifierType == null) {
      return null;
    }

    // Identify all the fields we care about on the entity
    final Map<FieldMetadata, JpaEntityFactoryMetadata> locatedFields =
        getLocatedFields(memberDetails, entityFactoryMetadata);

    // Get the list of embedded metadata holders - may be an empty list if
    // no embedded identifier exists
    final List<EmbeddedHolder> embeddedHolders =
        getEmbeddedHolders(memberDetails, entityFactoryMetadata);

    // Get the embedded identifier metadata holder - may be null if no
    // embedded identifier exists
    final EmbeddedIdHolder embeddedIdHolder =
        getEmbeddedIdHolder(memberDetails, entityFactoryMetadata);

    Set<ClassOrInterfaceTypeDetails> entityFactoryClasses =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_JPA_ENTITY_FACTORY);

    return new JpaEntityFactoryMetadata(entityFactoryMetadata, aspectName,
        governorPhysicalTypeMetadata, entity, locatedFields, embeddedHolders, entityFactoryClasses,
        embeddedIdHolder);
  }

  public String getProvidesType() {
    return JpaEntityFactoryMetadata.getMetadataIdentiferType();
  }

  private Map<FieldMetadata, JpaEntityFactoryMetadata> getLocatedFields(
      final MemberDetails memberDetails, final String dodMetadataId) {
    final Map<FieldMetadata, JpaEntityFactoryMetadata> locatedFields =
        new LinkedHashMap<FieldMetadata, JpaEntityFactoryMetadata>();
    final Iterable<ClassOrInterfaceTypeDetails> entityFactoryTypes =
        getTypeLocationService()
            .findClassesOrInterfaceDetailsWithAnnotation(ROO_JPA_ENTITY_FACTORY);

    final List<MethodMetadata> mutatorMethods = memberDetails.getMethods();
    // To avoid unnecessary rewriting of the DoD ITD we sort the mutators by
    // method name to provide a consistent ordering
    Collections.sort(mutatorMethods, new NaturalOrderComparator<MethodMetadata>() {
      @Override
      protected String stringify(final MethodMetadata object) {
        return object.getMethodName().getSymbolName();
      }
    });

    for (final MethodMetadata method : mutatorMethods) {
      if (!BeanInfoUtils.isMutatorMethod(method)) {
        continue;
      }

      final FieldMetadata field = BeanInfoUtils.getFieldForJavaBeanMethod(memberDetails, method);
      if (field == null) {
        continue;
      }

      // Track any changes to the mutator method (eg it goes away)
      getMetadataDependencyRegistry().registerDependency(method.getDeclaredByMetadataId(),
          dodMetadataId);

      final Set<Object> fieldCustomDataKeys = field.getCustomData().keySet();

      // Never include id or version fields (they shouldn't normally have
      // a mutator anyway, but the user might have added one), or embedded
      // types
      if (fieldCustomDataKeys.contains(IDENTIFIER_FIELD)
          || fieldCustomDataKeys.contains(EMBEDDED_ID_FIELD)
          || fieldCustomDataKeys.contains(EMBEDDED_FIELD)
          || fieldCustomDataKeys.contains(VERSION_FIELD)) {
        continue;
      }

      // Never include persistence transient fields
      if (fieldCustomDataKeys.contains(TRANSIENT_FIELD)) {
        continue;
      }

      // Never include any sort of collection; user has to make such
      // entities by hand
      if (field.getFieldType().isCommonCollectionType()
          || fieldCustomDataKeys.contains(ONE_TO_MANY_FIELD)
          || fieldCustomDataKeys.contains(MANY_TO_MANY_FIELD)) {
        continue;
      }

      // Look up collaborating metadata
      final JpaEntityFactoryMetadata collaboratingMetadata =
          locateCollaboratingMetadata(dodMetadataId, field, entityFactoryTypes);
      locatedFields.put(field, collaboratingMetadata);
    }

    return locatedFields;
  }

  private EmbeddedIdHolder getEmbeddedIdHolder(final MemberDetails memberDetails,
      final String metadataIdentificationString) {
    final List<FieldMetadata> idFields = new ArrayList<FieldMetadata>();
    final List<FieldMetadata> fields =
        MemberFindingUtils.getFieldsWithTag(memberDetails, EMBEDDED_ID_FIELD);
    if (fields.isEmpty()) {
      return null;
    }
    final FieldMetadata embeddedIdField = fields.get(0);
    final MemberDetails identifierMemberDetails = getMemberDetails(embeddedIdField.getFieldType());
    if (identifierMemberDetails == null) {
      return null;
    }

    for (final FieldMetadata field : identifierMemberDetails.getFields()) {
      if (!(Modifier.isStatic(field.getModifier()) || Modifier.isFinal(field.getModifier()) || Modifier
          .isTransient(field.getModifier()))) {
        getMetadataDependencyRegistry().registerDependency(field.getDeclaredByMetadataId(),
            metadataIdentificationString);
        idFields.add(field);
      }
    }

    return new EmbeddedIdHolder(embeddedIdField, idFields);
  }

  /**
   * Returns the {@link JpaEntityFactoryMetadata} for the entity that's the target
   * of the given reference field.
   *
   * @param entityFactoryMetadataId
   * @param field
   * @param entityFactoryTypes
   * @return <code>null</code> if it's not an n:1 or 1:1 field, or the DoD
   *         metadata is simply not available
   */
  private JpaEntityFactoryMetadata locateCollaboratingMetadata(
      final String entityFactoryMetadataId, final FieldMetadata field,
      final Iterable<ClassOrInterfaceTypeDetails> entityFactoryTypes) {
    if (!(field.getCustomData().keySet().contains(MANY_TO_ONE_FIELD) || field.getCustomData()
        .keySet().contains(ONE_TO_ONE_FIELD))) {
      return null;
    }

    final String otherEntityFactoryMetadataId =
        getEntityFactoryMetadataId(field.getFieldType(), entityFactoryTypes);

    if (otherEntityFactoryMetadataId == null
        || otherEntityFactoryMetadataId.equals(entityFactoryMetadataId)) {
      // No DoD for this field's type, or it's a self-reference
      return null;
    }

    // Make this DoD depend on the related entity (not its Dod, otherwise
    // we get a circular MD dependency)
    registerDependencyUponType(entityFactoryMetadataId, field.getFieldType());

    return (JpaEntityFactoryMetadata) getMetadataService().get(otherEntityFactoryMetadataId);
  }

  private String getEntityFactoryMetadataId(final JavaType javaType,
      final Iterable<ClassOrInterfaceTypeDetails> entityFactoryTypes) {
    for (final ClassOrInterfaceTypeDetails cid : entityFactoryTypes) {
      final AnnotationMetadata entityFactoryAnnotation = cid.getAnnotation(ROO_JPA_ENTITY_FACTORY);
      final AnnotationAttributeValue<JavaType> entityAttribute =
          entityFactoryAnnotation.getAttribute("entity");
      if (entityAttribute != null && entityAttribute.getValue().equals(javaType)) {
        // Found the DoD type for the given field's type
        return JpaDataOnDemandMetadata.createIdentifier(cid.getName(),
            PhysicalTypeIdentifier.getPath(cid.getDeclaredByMetadataId()));
      }
    }
    return null;
  }

  private void registerDependencyUponType(final String entityFactoryMetadata, final JavaType type) {
    final String fieldPhysicalTypeId = getTypeLocationService().getPhysicalTypeIdentifier(type);
    getMetadataDependencyRegistry().registerDependency(fieldPhysicalTypeId, entityFactoryMetadata);
  }

  private List<EmbeddedHolder> getEmbeddedHolders(final MemberDetails memberDetails,
      final String metadataIdentificationString) {
    final List<EmbeddedHolder> embeddedHolders = new ArrayList<EmbeddedHolder>();

    final List<FieldMetadata> embeddedFields =
        MemberFindingUtils.getFieldsWithTag(memberDetails, EMBEDDED_FIELD);
    if (embeddedFields.isEmpty()) {
      return embeddedHolders;
    }

    for (final FieldMetadata embeddedField : embeddedFields) {
      final MemberDetails embeddedMemberDetails = getMemberDetails(embeddedField.getFieldType());
      if (embeddedMemberDetails == null) {
        continue;
      }

      final List<FieldMetadata> fields = new ArrayList<FieldMetadata>();

      for (final FieldMetadata field : embeddedMemberDetails.getFields()) {
        if (!(Modifier.isStatic(field.getModifier()) || Modifier.isFinal(field.getModifier()) || Modifier
            .isTransient(field.getModifier()))) {
          getMetadataDependencyRegistry().registerDependency(field.getDeclaredByMetadataId(),
              metadataIdentificationString);
          fields.add(field);
        }
      }
      embeddedHolders.add(new EmbeddedHolder(embeddedField, fields));
    }

    return embeddedHolders;
  }

}
