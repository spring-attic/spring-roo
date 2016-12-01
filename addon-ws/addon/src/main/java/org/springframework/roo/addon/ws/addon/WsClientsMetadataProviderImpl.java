package org.springframework.roo.addon.ws.addon;

import static org.springframework.roo.model.RooJavaType.ROO_WS_CLIENTS;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.ws.annotations.SoapBindingType;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.NestedAnnotationAttributeValue;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

/**
 * Implementation of {@link WsClientsMetadataProvider}.
 *
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class WsClientsMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements WsClientsMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(WsClientsMetadataProviderImpl.class);

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

    addMetadataTrigger(ROO_WS_CLIENTS);
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

    removeMetadataTrigger(ROO_WS_CLIENTS);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return WsClientsMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = WsClientsMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = WsClientsMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "WS_Clients";
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

    RooWsClientsAnnotationValues annotationValues =
        new RooWsClientsAnnotationValues(governorPhysicalTypeMetadata);

    // Getting @RooWsClients annotation
    ClassOrInterfaceTypeDetails cid = governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
    AnnotationMetadata wsClientsAnnotation = cid.getAnnotation(ROO_WS_CLIENTS);

    // Getting profile from annotation
    String profile = annotationValues.getProfile();

    // Getting endpoints from annotation
    List<WsClientEndpoint> endpoints = new ArrayList<WsClientEndpoint>();
    AnnotationAttributeValue<?> currentEndpoints = wsClientsAnnotation.getAttribute("endpoints");

    if (currentEndpoints != null) {
      List<?> values = (List<?>) currentEndpoints.getValue();

      Iterator<?> valuesIt = values.iterator();

      while (valuesIt.hasNext()) {
        NestedAnnotationAttributeValue wsClientAnnotation =
            (NestedAnnotationAttributeValue) valuesIt.next();

        if (wsClientAnnotation.getValue() != null
            && wsClientAnnotation.getValue().getAttribute("endpoint") != null) {
          // Get endpoint name
          String endpointName = null;
          if (wsClientAnnotation.getValue().getAttribute("endpoint").getValue() instanceof String) {
            endpointName =
                (String) wsClientAnnotation.getValue().getAttribute("endpoint").getValue();
          }
          Validate.notNull(endpointName, "'endpoint' attribute in @RooWsClient must be a String");
          // Get endpoint nameSpace
          String endpointNameSpace = null;
          if (wsClientAnnotation.getValue().getAttribute("targetNamespace").getValue() instanceof String) {
            endpointNameSpace =
                (String) wsClientAnnotation.getValue().getAttribute("targetNamespace").getValue();
          }
          Validate.notNull(endpointName,
              "'targetNamespace' attribute in @RooWsClient must be a String");
          // Get endpoint binding type
          EnumDetails endpointBindingType = null;
          if (wsClientAnnotation.getValue().getAttribute("binding").getValue() instanceof EnumDetails) {
            endpointBindingType =
                (EnumDetails) wsClientAnnotation.getValue().getAttribute("binding").getValue();
          }
          Validate.notNull(endpointBindingType,
              "'binding' attribute in @RooWsClient must be a SoapBindingType");

          endpoints.add(new WsClientEndpoint(endpointName, endpointNameSpace, endpointBindingType));
        }

      }

    }

    return new WsClientsMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, endpoints, profile);
  }

  public String getProvidesType() {
    return WsClientsMetadata.getMetadataIdentiferType();
  }

  // OSGI Services

}
