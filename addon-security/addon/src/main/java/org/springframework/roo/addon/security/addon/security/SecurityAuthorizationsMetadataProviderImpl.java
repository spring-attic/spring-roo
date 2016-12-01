package org.springframework.roo.addon.security.addon.security;

import static org.springframework.roo.model.RooJavaType.ROO_SECURITY_AUTHORIZATIONS;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecoratorTracker;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.NestedAnnotationAttributeValue;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Implementation of {@link SecurityAuthorizationsMetadataProvider}.
 *
 * @author Manuel Iborra
 * @since 2.0
 */
@Component
@Service
public class SecurityAuthorizationsMetadataProviderImpl extends
    AbstractMemberDiscoveringItdMetadataProvider implements SecurityAuthorizationsMetadataProvider {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(SecurityAuthorizationsMetadataProviderImpl.class);

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

    addMetadataTrigger(ROO_SECURITY_AUTHORIZATIONS);
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

    removeMetadataTrigger(ROO_SECURITY_AUTHORIZATIONS);

    CustomDataKeyDecorator keyDecorator = this.keyDecoratorTracker.getService();
    keyDecorator.unregisterMatchers(getClass());
    this.keyDecoratorTracker.close();
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return SecurityAuthorizationsMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType =
        SecurityAuthorizationsMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = SecurityAuthorizationsMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  public String getItdUniquenessFilenameSuffix() {
    return "Security_Authorizations";
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
    JavaType annotatedService = governorPhysicalTypeMetadata.getType();

    // Getting the service details
    MemberDetails serviceDetails =
        getMemberDetailsScanner().getMemberDetails(getClass().getName(),
            getTypeLocationService().getTypeDetails(annotatedService));

    Map<MethodMetadata, String> preAuthorizationMethods =
        new LinkedHashMap<MethodMetadata, String>();

    // Get methods defined in each annotation @RooSecurityAuthorization
    AnnotationMetadata annotationAuthorizations =
        serviceDetails.getAnnotation(RooJavaType.ROO_SECURITY_AUTHORIZATIONS);
    AnnotationAttributeValue<?> attributeAuthorizations =
        annotationAuthorizations.getAttribute("authorizations");
    List<?> values = (List<?>) attributeAuthorizations.getValue();

    if (values != null && !values.isEmpty()) {
      Iterator<?> valuesIt = values.iterator();
      while (valuesIt.hasNext()) {
        NestedAnnotationAttributeValue authorizationAnnotation =
            (NestedAnnotationAttributeValue) valuesIt.next();

        // Get attributes from the annotation @RooSecurityAuthorization
        String methodName =
            (String) authorizationAnnotation.getValue().getAttribute("method").getValue();
        List<?> methodParameters =
            (List<?>) authorizationAnnotation.getValue().getAttribute("parameters").getValue();

        List<MethodMetadata> methods = serviceDetails.getMethods(new JavaSymbolName(methodName));

        for (MethodMetadata method : methods) {

          // check the parameters to get corresponding method
          if (checkParameters(method, methodParameters)) {

            String roles = null;
            if (authorizationAnnotation.getValue().getAttribute("roles") != null) {
              roles = (String) authorizationAnnotation.getValue().getAttribute("roles").getValue();
            }

            String usernames = null;
            if (authorizationAnnotation.getValue().getAttribute("usernames") != null) {
              usernames =
                  (String) authorizationAnnotation.getValue().getAttribute("usernames").getValue();
            }

            // Create the content based on defined roles and
            // usernames to include it in the security annotation
            String secAnnotationValue =
                getSecurityOperations().getSpringSecurityAnnotationValue(roles, usernames);

            preAuthorizationMethods.put(method, secAnnotationValue);
            break;
          }
        }
      }
    }

    return new SecurityAuthorizationsMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, preAuthorizationMethods);

  }

  /**
   * Check that the parameters of the method are equals of parameters list
   *
   * @param method Method to check
   * @param methodParametersToCompare Parameters to compare
   * @return true if are equals, false otherwise
   */
  private boolean checkParameters(MethodMetadata method, List<?> methodParametersToCompare) {
    boolean parametersAreEquals = true;
    List<AnnotatedJavaType> parameterTypes = method.getParameterTypes();

    if (methodParametersToCompare.size() != parameterTypes.size()) {
      parametersAreEquals = false;
    } else {
      for (int i = 0; i < methodParametersToCompare.size(); i++) {
        ClassAttributeValue methodParameterToCompare =
            (ClassAttributeValue) methodParametersToCompare.get(i);
        AnnotatedJavaType parameterJavaType = parameterTypes.get(i);
        if (!methodParameterToCompare.getValue().getSimpleTypeName()
            .equals(parameterJavaType.getJavaType().getSimpleTypeName())) {
          parametersAreEquals = false;
          break;
        }
      }
    }
    return parametersAreEquals;
  }

  public String getProvidesType() {
    return SecurityAuthorizationsMetadata.getMetadataIdentiferType();
  }

  // OSGI Services
  protected ProjectOperations getProjectOperations() {
    return serviceInstaceManager.getServiceInstance(this, ProjectOperations.class);
  }

  protected SecurityOperations getSecurityOperations() {
    return serviceInstaceManager.getServiceInstance(this, SecurityOperations.class);
  }

  protected MemberDetailsScanner getMemberDetailsScanner() {
    return serviceInstaceManager.getServiceInstance(this, MemberDetailsScanner.class);
  }


}
