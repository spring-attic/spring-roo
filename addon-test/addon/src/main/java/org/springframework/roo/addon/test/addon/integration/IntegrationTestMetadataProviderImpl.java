package org.springframework.roo.addon.test.addon.integration;

import static org.springframework.roo.model.RooJavaType.ROO_DATA_ON_DEMAND;
import static org.springframework.roo.model.RooJavaType.ROO_INTEGRATION_TEST;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.addon.ConfigurableMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTriggerBasedMetadataProviderTracker;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Implementation of {@link IntegrationTestMetadataProvider}.
 *
 * @author Ben Alex
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @author Juan Carlos García
 * @author Manuel Iborra
 * @since 1.0
 */
@Component
@Service
public class IntegrationTestMetadataProviderImpl extends AbstractItdMetadataProvider implements
    IntegrationTestMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(IntegrationTestMetadataProviderImpl.class);

  protected MetadataDependencyRegistryTracker registryTracker = null;
  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link ItdTriggerBasedMetadataProviderTracker} to
   * track for {@link ConfigurableMetadataProvider} service.</li>
   * <li>Registers {@link RooJavaType#ROO_INTEGRATION_TEST} as additional
   * JavaType that will trigger metadata registration.</li>
   * </ul>
   */
  @Override
  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    this.serviceInstaceManager.activate(this.context);

    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_INTEGRATION_TEST);
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

    removeMetadataTrigger(ROO_INTEGRATION_TEST);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return IntegrationTestMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = IntegrationTestMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = IntegrationTestMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "IT";
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {

    // Get repository
    IntegrationTestAnnotationValues integrationTestValues =
        new IntegrationTestAnnotationValues(governorPhysicalTypeMetadata);
    JavaType repository = integrationTestValues.getSource();

    // Obtain target type complete details
    final ClassOrInterfaceTypeDetails cidRepository =
        getTypeLocationService().getTypeDetails(repository);

    // Get entity related
    AnnotationMetadata annotationRepository =
        cidRepository.getAnnotation(RooJavaType.ROO_REPOSITORY_JPA);
    AnnotationAttributeValue<Object> entityAttribute = annotationRepository.getAttribute("entity");
    JavaType entity = (JavaType) entityAttribute.getValue();

    // Get DataOnDemand
    final JavaType dataOnDemandType = getDataOnDemandType(entity, governorPhysicalTypeMetadata);

    Validate.notNull(dataOnDemandType, "DataOnDemand of entity %s is necessary.",
        entity.getSimpleTypeName());

    List<MethodMetadata> methods = getAllMethods(repository);

    return new IntegrationTestMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, repository, dataOnDemandType, methods);
  }

  /**
   * Get all methods of JavaType and the classes that it extends
   *
   * @param type JavaType on which obtaining the methods
   * @return list of methods
   */
  private List<MethodMetadata> getAllMethods(JavaType type) {
    List<MethodMetadata> methodsList = new ArrayList<MethodMetadata>();
    final ClassOrInterfaceTypeDetails cid = getTypeLocationService().getTypeDetails(type);
    if (cid != null) {
      final MemberDetails memberDetails =
          getMemberDetailsScanner().getMemberDetails(this.getClass().getName(), cid);

      List<MemberHoldingTypeDetails> details = memberDetails.getDetails();
      for (MemberHoldingTypeDetails detail : details) {

        // Set methods of extended JavaTypes
        List<JavaType> extendsTypes = detail.getExtendsTypes();
        for (JavaType extendJavaType : extendsTypes) {
          for (MethodMetadata method : getAllMethods(extendJavaType)) {
            if (!methodsList.contains(method)) {
              methodsList.add(method);
            }
          }
        }
      }

      // Set methods of actual JavaType
      for (MethodMetadata method : memberDetails.getMethods()) {
        if (!methodsList.contains(method)) {
          methodsList.add(method);
        }
      }
    }
    return methodsList;
  }

  /**
   * Returns the {@link JavaType} for the given entity's "data on demand"
   * class.
   *
   * @param entity
   *            the entity for which to get the DoD type
   * @return a non-<code>null</code> type (which may or may not exist yet)
   */
  private JavaType getDataOnDemandType(final JavaType entity,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata) {

    for (final ClassOrInterfaceTypeDetails dodType : getTypeLocationService()
        .findClassesOrInterfaceDetailsWithAnnotation(ROO_DATA_ON_DEMAND)) {
      final AnnotationMetadata dodAnnotation =
          MemberFindingUtils.getFirstAnnotation(dodType, ROO_DATA_ON_DEMAND);
      if (dodAnnotation != null
          && dodAnnotation.getAttribute("entity").getValue().equals(entity)
          && governorPhysicalTypeMetadata.getType().getModule()
              .equals(dodType.getType().getModule())) {
        return dodType.getName();
      }
    }
    return null;
  }

  public String getProvidesType() {
    return IntegrationTestMetadata.getMetadataIdentiferType();
  }

}
