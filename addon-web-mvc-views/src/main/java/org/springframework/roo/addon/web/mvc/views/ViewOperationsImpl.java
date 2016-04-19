package org.springframework.roo.addon.web.mvc.views;

import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.addon.responses.ControllerMVCResponseService;
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

  protected void activate(final ComponentContext context) {
    this.context = context.getBundleContext();
  }

  @Override
  public void setup(ControllerMVCResponseService responseType, Pom module) {
    // Delegate on the selected response type to install
    // all necessary elements
    responseType.install(module);
  }

}
