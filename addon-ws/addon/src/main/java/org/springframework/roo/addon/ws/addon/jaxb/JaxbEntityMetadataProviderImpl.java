package org.springframework.roo.addon.ws.addon.jaxb;

import static org.springframework.roo.model.RooJavaType.ROO_JAXB_ENTITY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.javabean.addon.JavaBeanMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.plural.addon.PluralService;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JpaJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

/**
 * Implementation of {@link JaxbEntityMetadataProvider}.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class JaxbEntityMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements JaxbEntityMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(JaxbEntityMetadataProviderImpl.class);

  private final Map<JavaType, String> domainTypeToServiceMidMap =
      new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();


  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_JAXB_ENTITY} as additional JavaType
   * that will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  @SuppressWarnings("unchecked")
  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    serviceInstaceManager.activate(this.context);
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_JAXB_ENTITY);
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

    removeMetadataTrigger(ROO_JAXB_ENTITY);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return JaxbEntityMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = JaxbEntityMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = JaxbEntityMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Jaxb_Entity";
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

    // Getting the annotated entity type
    JavaType annotatedEntity = governorPhysicalTypeMetadata.getType();

    // Getting the entity details
    ClassOrInterfaceTypeDetails entityDetails =
        getTypeLocationService().getTypeDetails(annotatedEntity);

    // Getting the plural
    String entityPlural = getPluralService().getPlural(entityDetails);

    // Getting JavaBean Metadata
    String javaBeanMetadataKey = JavaBeanMetadata.createIdentifier(entityDetails);
    JavaBeanMetadata javaBeanMetadata =
        (JavaBeanMetadata) getMetadataService().get(javaBeanMetadataKey);

    // Getting JpaEntity Metadata
    String jpaEntityMetadataKey = JpaEntityMetadata.createIdentifier(entityDetails);
    JpaEntityMetadata jpaEntityMetadata =
        (JpaEntityMetadata) getMetadataService().get(jpaEntityMetadataKey);

    // Getting the @OneToMany and @ManyToOne getters
    Map<String, String> entityNames = new HashMap<String, String>();
    List<MethodMetadata> oneToManyGetters = new ArrayList<MethodMetadata>();
    List<MethodMetadata> manyToOneGetters = new ArrayList<MethodMetadata>();
    for (FieldMetadata field : entityDetails.getDeclaredFields()) {
      // Getting getter for the oneToMany field
      MethodMetadata getter = javaBeanMetadata.getAccesorMethod(field);
      if (getter != null
          && (field.getAnnotation(JpaJavaType.ONE_TO_MANY) != null || field
              .getAnnotation(JpaJavaType.MANY_TO_MANY) != null)) {
        String getterTypeName =
            getter.getReturnType().getBaseType().getSimpleTypeName().toLowerCase();
        oneToManyGetters.add(getter);
        entityNames.put(getterTypeName, getPluralService().getPlural(getterTypeName));
      } else if (getter != null && field.getAnnotation(JpaJavaType.MANY_TO_ONE) != null) {
        String getterTypeName = getter.getReturnType().getSimpleTypeName().toLowerCase();
        manyToOneGetters.add(getter);
        entityNames.put(getterTypeName, getPluralService().getPlural(getterTypeName));
      }
    }

    // Getting the identifier accessor only if the annotated class doesn't have
    // parent
    MethodMetadata identifierAccessor = null;
    if (jpaEntityMetadata != null && jpaEntityMetadata.getParent() == null) {
      identifierAccessor = jpaEntityMetadata.getCurrentIdentifierAccessor();
    }

    return new JaxbEntityMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, getProjectOperations().getTopLevelPackage(""),
        annotatedEntity, entityPlural, identifierAccessor, oneToManyGetters, manyToOneGetters,
        entityNames);
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

  public String getProvidesType() {
    return JaxbEntityMetadata.getMetadataIdentiferType();
  }

  // OSGI Services
  protected ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

  protected PluralService getPluralService() {
    return serviceInstaceManager.getServiceInstance(this, PluralService.class);
  }

}
