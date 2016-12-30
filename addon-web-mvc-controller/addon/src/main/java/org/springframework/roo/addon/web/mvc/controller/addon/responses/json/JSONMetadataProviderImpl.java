package org.springframework.roo.addon.web.mvc.controller.addon.responses.json;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.addon.jpa.annotations.entity.JpaRelationType;
import org.springframework.roo.addon.layers.service.addon.ServiceMetadata;
import org.springframework.roo.addon.plural.addon.PluralService;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerLocator;
import org.springframework.roo.addon.web.mvc.controller.addon.ControllerMetadata;
import org.springframework.roo.addon.web.mvc.controller.addon.finder.SearchAnnotationValues;
import org.springframework.roo.addon.web.mvc.controller.annotations.ControllerType;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Implementation of {@link JSONMetadataProvider}.
 *
 * @author Juan Carlos García
 * @author Paula Navarro
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class JSONMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements JSONMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils.getLogger(JSONMetadataProviderImpl.class);

  private ServiceInstaceManager serviceInstaceManager = new ServiceInstaceManager();

  private final Map<JavaType, String> domainTypeToServiceMidMap =
      new LinkedHashMap<JavaType, String>();

  protected MetadataDependencyRegistryTracker registryTracker = null;
  protected CustomDataKeyDecoratorTracker keyDecoratorTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Create and open the {@link CustomDataKeyDecoratorTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_JSON} as additional JavaType that
   * will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  protected void activate(final ComponentContext cContext) {
    super.activate(cContext);
    context = cContext.getBundleContext();
    serviceInstaceManager.activate(this.context);
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(RooJavaType.ROO_JSON);
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

    removeMetadataTrigger(RooJavaType.ROO_JSON);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return JSONMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = JSONMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = JSONMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "JSON";
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

    ClassOrInterfaceTypeDetails controllerDetail =
        governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();

    // Getting controller metadata
    final String controllerMetadataKey = ControllerMetadata.createIdentifier(controllerDetail);
    final ControllerMetadata controllerMetadata =
        (ControllerMetadata) getMetadataService().get(controllerMetadataKey);

    // This metadata is not available yet.
    if (controllerMetadata == null) {
      return null;
    }

    // Getting entity and check if is a readOnly entity or not
    final JavaType entity = controllerMetadata.getEntity();

    JavaType itemController = null;
    if (controllerMetadata.getType() != ControllerType.ITEM) {
      // Locate ItemController
      Collection<ClassOrInterfaceTypeDetails> itemControllers =
          getControllerLocator().getControllers(entity, ControllerType.ITEM, RooJavaType.ROO_JSON);

      if (itemControllers.isEmpty()) {
        // We can't create metadata "Jet"
        return null;
      } else {
        // Get controller with the same package
        JavaPackage controllerPackage = controllerDetail.getType().getPackage();
        for (ClassOrInterfaceTypeDetails controller : itemControllers) {
          if (controllerPackage.equals(controller.getType().getPackage())) {
            itemController = controller.getType();
            break;
          }
        }
        Validate.notNull(itemController,
            "ERROR: Can't find ITEM-type controller related to controller '%s'", controllerDetail
                .getType().getFullyQualifiedTypeName());
      }
    }

    Validate.notNull(entity, "ERROR: You should provide a valid entity for controller '%s'",
        controllerDetail.getType().getFullyQualifiedTypeName());

    final ClassOrInterfaceTypeDetails entityDetails =
        getTypeLocationService().getTypeDetails(entity);

    Validate.notNull(entityDetails, "ERROR: Can't load details of %s",
        entity.getFullyQualifiedTypeName());


    final JpaEntityMetadata entityMetadata =
        getMetadataService().get(JpaEntityMetadata.createIdentifier(entityDetails));

    Validate.notNull(entityMetadata, "ERROR: Can't get Jpa Entity metada of %s",
        entity.getFullyQualifiedTypeName());

    // Get entity plural
    final String entityPlural = getPluralService().getPlural(entity);

    final String entityIdentifierPlural =
        getPluralService().getPlural(entityMetadata.getCurrentIndentifierField().getFieldName());

    // Getting service and its metadata
    final JavaType service = controllerMetadata.getService();

    ClassOrInterfaceTypeDetails serviceDetails = getTypeLocationService().getTypeDetails(service);

    final String serviceMetadataKey = ServiceMetadata.createIdentifier(serviceDetails);
    registerDependency(serviceMetadataKey, metadataIdentificationString);

    final ServiceMetadata serviceMetadata = getMetadataService().get(serviceMetadataKey);


    // Prepare information about ONE-TO-ONE relations
    final List<Pair<RelationInfo, JpaEntityMetadata>> compositionRelationOneToOne =
        new ArrayList<Pair<RelationInfo, JpaEntityMetadata>>();
    ClassOrInterfaceTypeDetails childEntityDetails;
    JpaEntityMetadata childEntityMetadata;
    for (RelationInfo info : entityMetadata.getRelationInfos().values()) {
      if (info.cardinality == Cardinality.ONE_TO_ONE && info.type == JpaRelationType.COMPOSITION) {
        childEntityDetails = getTypeLocationService().getTypeDetails(info.childType);
        childEntityMetadata =
            getMetadataService().get(JpaEntityMetadata.createIdentifier(childEntityDetails));
        compositionRelationOneToOne.add(Pair.of(info, childEntityMetadata));
      }
    }

    Map<String, MethodMetadata> findersToAdd = new HashMap<String, MethodMetadata>();

    // Getting annotated finders
    final SearchAnnotationValues searchAnnotationValues =
        new SearchAnnotationValues(governorPhysicalTypeMetadata);

    // Add finders only if controller is of search type
    if (controllerMetadata.getType() == ControllerType.SEARCH && searchAnnotationValues != null
        && searchAnnotationValues.getFinders() != null) {
      List<String> finders =
          new ArrayList<String>(Arrays.asList(searchAnnotationValues.getFinders()));

      // Search indicated finders in its related service
      for (MethodMetadata serviceFinder : serviceMetadata.getFinders()) {
        String finderName = serviceFinder.getMethodName().getSymbolName();
        if (finders.contains(finderName)) {
          findersToAdd.put(finderName, serviceFinder);

          // Add dependencies between modules
          List<JavaType> types = new ArrayList<JavaType>();
          types.add(serviceFinder.getReturnType());
          types.addAll(serviceFinder.getReturnType().getParameters());

          for (AnnotatedJavaType parameter : serviceFinder.getParameterTypes()) {
            types.add(parameter.getJavaType());
            types.addAll(parameter.getJavaType().getParameters());
          }

          for (JavaType parameter : types) {
            getTypeLocationService().addModuleDependency(
                governorPhysicalTypeMetadata.getType().getModule(), parameter);
          }

          finders.remove(finderName);
        }
      }

      // Check all finders have its service method
      if (!finders.isEmpty()) {
        throw new IllegalArgumentException(String.format(
            "ERROR: Service %s does not have these finder methods: %s ",
            service.getFullyQualifiedTypeName(), StringUtils.join(finders, ", ")));
      }
    }

    return new JSONMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata,
        controllerMetadata, serviceMetadata, entityMetadata, entityPlural, entityIdentifierPlural,
        compositionRelationOneToOne, itemController, findersToAdd);

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
    return JSONMetadata.getMetadataIdentiferType();
  }


  // OSGI Services
  private PluralService getPluralService() {
    return serviceInstaceManager.getServiceInstance(this, PluralService.class);
  }

  private ControllerLocator getControllerLocator() {
    return serviceInstaceManager.getServiceInstance(this, ControllerLocator.class);
  }

}
