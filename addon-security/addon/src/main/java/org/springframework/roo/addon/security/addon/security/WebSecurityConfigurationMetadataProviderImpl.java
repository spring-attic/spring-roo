package org.springframework.roo.addon.security.addon.security;

import static org.springframework.roo.model.RooJavaType.ROO_WEB_SECURITY_CONFIGURATION;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.addon.web.mvc.views.MVCViewGenerationService;
import org.springframework.roo.addon.web.mvc.views.ViewContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Implementation of {@link WebSecurityConfigurationMetadataProvider}.
 * <p/>
 * 
 * @author Sergio Clares
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class WebSecurityConfigurationMetadataProviderImpl extends
    AbstractMemberDiscoveringItdMetadataProvider implements
    WebSecurityConfigurationMetadataProvider {

  protected MetadataDependencyRegistryTracker registryTracker = null;
  private static final JavaType ROO_AUTHENTICATION_AUDITOR_AWARE = new JavaType(
      "org.springframework.roo.addon.security.annotations.RooAuthenticationAuditorAware");

  private List<ControllerMVCResponseService> controllerMvcResponseServices =
      new ArrayList<ControllerMVCResponseService>();

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_SECURITY_CONFIGURATION} as additional 
   * JavaType that will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    super.setDependsOnGovernorBeingAClass(false);
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_WEB_SECURITY_CONFIGURATION);
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

    removeMetadataTrigger(ROO_WEB_SECURITY_CONFIGURATION);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return WebSecurityConfigurationMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType =
        WebSecurityConfigurationMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = WebSecurityConfigurationMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    return getLocalMid(itdTypeDetails);
  }

  @Override
  public String getItdUniquenessFilenameSuffix() {
    return "WebSecurityConfiguration";
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {

    ClassOrInterfaceTypeDetails webSecurityConfiguration =
        governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();

    final WebSecurityConfigurationAnnotationValues annotationValues =
        new WebSecurityConfigurationAnnotationValues(governorPhysicalTypeMetadata);

    // Get AuthenticationAuditorAware JavaType if exists
    Set<JavaType> authenticationClasses =
        getTypeLocationService().findTypesWithAnnotation(ROO_AUTHENTICATION_AUDITOR_AWARE);

    JavaType authenticationType = null;
    if (authenticationClasses.size() > 0) {
      for (JavaType authenticationClass : authenticationClasses) {
        if (authenticationClass.getModule() != null
            && authenticationClass.getModule().equals(
                governorPhysicalTypeMetadata.getType().getModule())) {
          authenticationType = authenticationClass;
          break;
        } else if (authenticationClasses.size() == 1) {

          // Project is single module. Pick single authentication class
          authenticationType = authenticationClass;
        }
      }
    }

    // Looking for an installed view provider and delegating on it to 
    // generate login view
    generateLoginPage(webSecurityConfiguration.getType().getModule());


    return new WebSecurityConfigurationMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, authenticationType, annotationValues);
  }

  /**
   * Method that obtains all installed MVCViewGenerationService and delegates on them
   * to generate login view.
   * 
   * If exists more than one view provider, different login pages will be generated.
   * 
   */
  private void generateLoginPage(String moduleName) {
    for (ControllerMVCResponseService responseService : getAllControllerMVCResponseService()) {

      // Check if current view provider has been installed on the project
      if (responseService.isInstalledInModule(moduleName)) {
        try {
          // Get all Services implement MVCViewGenerationService interface
          ServiceReference<?>[] references =
              this.context.getAllServiceReferences(MVCViewGenerationService.class.getName(), null);

          for (ServiceReference<?> ref : references) {
            MVCViewGenerationService viewGenerationService =
                (MVCViewGenerationService) this.context.getService(ref);
            if (viewGenerationService.getName().equals(responseService.getName())) {
              viewGenerationService.addLoginView(moduleName, new ViewContext());
            }
          }

        } catch (InvalidSyntaxException e) {
          LOGGER.warning("ERROR: Exception trying to generate login page.");
          return;
        }
      }
    }

  }

  /**
   * Method that obtains all controllerMVCResponseServices
   * 
   * @return List with all registered implementations of ControllerMVCResponseService. 
   *         Doesn't matter if they're installed or not.
   */
  public List<ControllerMVCResponseService> getAllControllerMVCResponseService() {
    // Get all Services implement ControllerMVCResponseService interface
    if (controllerMvcResponseServices.isEmpty()) {
      try {
        ServiceReference<?>[] references =
            this.context
                .getAllServiceReferences(ControllerMVCResponseService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          ControllerMVCResponseService responseService =
              (ControllerMVCResponseService) this.context.getService(ref);
          controllerMvcResponseServices.add(responseService);
        }

        return controllerMvcResponseServices;

      } catch (InvalidSyntaxException e) {
        LOGGER
            .warning("Cannot load controllerMvcResponseServices on WebSecurityConfigurationMetadataProvider.");
        return null;
      }
    } else {
      return controllerMvcResponseServices;
    }
  }

  public String getProvidesType() {
    return WebSecurityConfigurationMetadata.getMetadataIdentiferType();
  }

}
