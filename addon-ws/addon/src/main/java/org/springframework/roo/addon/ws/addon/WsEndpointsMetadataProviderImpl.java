package org.springframework.roo.addon.ws.addon;

import static org.springframework.roo.model.RooJavaType.ROO_WS_ENDPOINTS;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

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
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

/**
 * Implementation of {@link WsEndpointsMetadataProvider}.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class WsEndpointsMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements WsEndpointsMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(WsEndpointsMetadataProviderImpl.class);

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
   * <li>Registers {@link RooJavaType#ROO_CONTROLLER} as additional JavaType
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

    addMetadataTrigger(ROO_WS_ENDPOINTS);
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

    removeMetadataTrigger(ROO_WS_ENDPOINTS);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return WsEndpointsMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = WsEndpointsMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = WsEndpointsMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "WS_Endpoints";
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

    // Getting annotated class details
    ClassOrInterfaceTypeDetails cid = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
    AnnotationMetadata wsEndpointsAnnotation = cid.getAnnotation(ROO_WS_ENDPOINTS);

    // Getting endpoints from annotation
    AnnotationAttributeValue<?> currentEndpoints = wsEndpointsAnnotation.getAttribute("endpoints");

    Validate.notNull(currentEndpoints,
        "ERROR: You must provide a valid endpoint list to be able to register endpoints.");

    List<?> values = (List<?>) currentEndpoints.getValue();

    Iterator<?> valuesIt = values.iterator();

    Map<JavaType, JavaType> endpointsAndSeis = new TreeMap<JavaType, JavaType>();
    Map<JavaType, JavaType> endpointsAndServices = new TreeMap<JavaType, JavaType>();

    while (valuesIt.hasNext()) {
      ClassAttributeValue endpointAttr = (ClassAttributeValue) valuesIt.next();
      JavaType endpoint = endpointAttr.getValue();

      // Getting endpoint details
      ClassOrInterfaceTypeDetails endpointDetails =
          getTypeLocationService().getTypeDetails(endpoint);

      // Use SeiImplMetadata to obtain necessary information about the endpoint
      final String endpointMetadataId =
          SeiImplMetadata.createIdentifier(endpoint,
              PhysicalTypeIdentifier.getPath(endpointDetails.getDeclaredByMetadataId()));
      final SeiImplMetadata endpointMetadata =
          (SeiImplMetadata) getMetadataService().get(endpointMetadataId);
      JavaType sei = endpointMetadata.getSei();
      JavaType service = endpointMetadata.getService();

      // Saving the related sei
      endpointsAndSeis.put(endpoint, sei);
      // Saving the related service
      endpointsAndServices.put(endpoint, service);

    }

    // Getting profile from annotation
    String profile = "";
    AnnotationAttributeValue<String> profileAttr = wsEndpointsAnnotation.getAttribute("profile");
    if (profileAttr != null && StringUtils.isNotEmpty(profileAttr.getValue())) {
      profile = profileAttr.getValue();
    }


    return new WsEndpointsMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, endpointsAndSeis, endpointsAndServices, profile);
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
    return WsEndpointsMetadata.getMetadataIdentiferType();
  }

  // OSGI Services

}
