package org.springframework.roo.addon.web.mvc.views;

import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
import org.springframework.roo.classpath.ModuleFeatureName;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link ViewOperations}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class ViewOperationsImpl implements ViewOperations {

  private static final Logger LOGGER = HandlerUtils.getLogger(ViewOperationsImpl.class);

  // ------------ OSGi component attributes ----------------
  private BundleContext context;

  private TypeLocationService typeLocationService;

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @Override
  public void setup(ControllerMVCResponseService responseType, Pom module) {
    // Check if provided module match with application modules features
    Validate.isTrue(getTypeLocationService()
        .hasModuleFeature(module, ModuleFeatureName.APPLICATION),
        "ERROR: Provided module doesn't match with application modules features. "
            + "Execute this operation again and provide a valid application module.");

    // Delegate on the selected response type to install
    // all necessary elements
    responseType.install(module);
  }

  /**
   * This method gets MVCViewGenerationService implementation that contains necessary operations
   * to install templates inside generated project.
   * 
   * @param type
   * @return
   */
  public MVCViewGenerationService getMVCViewGenerationService(String type) {
    try {
      ServiceReference<?>[] references =
          this.context.getAllServiceReferences(MVCViewGenerationService.class.getName(), null);

      for (ServiceReference<?> ref : references) {
        MVCViewGenerationService viewGenerationService =
            (MVCViewGenerationService) this.context.getService(ref);
        if (viewGenerationService.getName().equals(type)) {
          return viewGenerationService;
        }
      }

      return null;

    } catch (InvalidSyntaxException e) {
      LOGGER.warning("Cannot load MVCViewGenerationService on ViewCommands.");
      return null;
    }
  }

  // Get OSGi services

  public TypeLocationService getTypeLocationService() {
    if (typeLocationService == null) {
      // Get all Services implement TypeLocationService interface
      try {
        ServiceReference<?>[] references =
            this.context.getAllServiceReferences(TypeLocationService.class.getName(), null);

        for (ServiceReference<?> ref : references) {
          typeLocationService = (TypeLocationService) this.context.getService(ref);
          return typeLocationService;
        }

        return null;

      } catch (InvalidSyntaxException e) {
        LOGGER.warning("Cannot load TypeLocationService on ViewOperationsImpl.");
        return null;
      }
    } else {
      return typeLocationService;
    }
  }

}
