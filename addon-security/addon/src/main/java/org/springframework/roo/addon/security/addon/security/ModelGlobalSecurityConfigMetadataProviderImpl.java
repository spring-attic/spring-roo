package org.springframework.roo.addon.security.addon.security;

import static org.springframework.roo.model.RooJavaType.ROO_MODEL_GLOBAL_SECURITY_CONFIG;

import java.util.ArrayList;
import java.util.List;

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
import org.springframework.roo.project.ProjectOperations;

/**
 * Implementation of {@link ModelGlobalSecurityConfigMetadataProvider}.
 * <p/>
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ModelGlobalSecurityConfigMetadataProviderImpl extends
    AbstractMemberDiscoveringItdMetadataProvider implements
    ModelGlobalSecurityConfigMetadataProvider {

  protected MetadataDependencyRegistryTracker registryTracker = null;

  private List<ControllerMVCResponseService> controllerMvcResponseServices =
      new ArrayList<ControllerMVCResponseService>();

  private ProjectOperations projectOperations;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_MODEL_GLOBAL_SECURITY_CONFIG} as additional 
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

    addMetadataTrigger(ROO_MODEL_GLOBAL_SECURITY_CONFIG);
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

    removeMetadataTrigger(ROO_MODEL_GLOBAL_SECURITY_CONFIG);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return ModelGlobalSecurityConfigMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType =
        ModelGlobalSecurityConfigMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path =
        ModelGlobalSecurityConfigMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    return getLocalMid(itdTypeDetails);
  }

  @Override
  public String getItdUniquenessFilenameSuffix() {
    return "ModelGlobalSecurityConfig";
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {

    ClassOrInterfaceTypeDetails globalSecurityConfiguration =
        governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();

    final ModelGlobalSecurityConfigAnnotationValues annotationValues =
        new ModelGlobalSecurityConfigAnnotationValues(governorPhysicalTypeMetadata);

    return new ModelGlobalSecurityConfigMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, annotationValues);
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
              ViewContext ctx = new ViewContext();
              ctx.setProjectName(getProjectOperations().getProjectName(""));
              viewGenerationService.addLoginView(moduleName, ctx);
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
            .warning("Cannot load controllerMvcResponseServices on ModelGlobalSecurityConfigMetadataProviderImpl.");
        return null;
      }
    } else {
      return controllerMvcResponseServices;
    }
  }

  public ProjectOperations getProjectOperations() {
    // Get all Services implement ProjectOperations interface
    if (projectOperations == null) {
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          ProjectOperations responseService = (ProjectOperations) this.context.getService(ref);
          projectOperations = responseService;
          return responseService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER
            .warning("Cannot load ProjectOperations on ModelGlobalSecurityConfigMetadataProviderImpl.");
        return null;
      }
    } else {
      return projectOperations;
    }
  }

  public String getProvidesType() {
    return ModelGlobalSecurityConfigMetadata.getMetadataIdentiferType();
  }

}
