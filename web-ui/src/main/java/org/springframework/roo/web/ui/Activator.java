package org.springframework.roo.web.ui;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.web.ui.controllers.RedirectServlet;

/**
 * Registers static resources of the Spring Roo Estearn Grey UI web application
 * into the URI namespace.
 * 
 * @author Juan Carlos García
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 2.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Activator implements BundleActivator {

  /** The URI namespace at which the container servlet is registered */
  private static final String CONTEXT_PATH = "/eastern-grey-ui";

  private ServiceTracker httpTracker;

  private static final Logger LOGGER = HandlerUtils.getLogger(Activator.class);

  /**
   * Open the HTTP service tracker to get the HTTP service to register the
   * static resources of the Spring Roo Estearn Grey UI web app.
   * 
   * @param bundleContext
   */
  @Override
  public void start(BundleContext context) throws Exception {
    httpTracker = new ServiceTracker(context, HttpService.class.getName(), null) {

      public void removedService(ServiceReference reference, Object service) {
        // HTTP service is no longer available, unregister our
        // servlet...
        try {
          ((HttpService) service).unregister("/");
        } catch (IllegalArgumentException exception) {
          // Ignore; servlet registration probably failed earlier
          // on...
          LOGGER.log(Level.FINE, "Servlet registration probably failed earlier.");
        }
      }

      public Object addingService(ServiceReference reference) {
        // HTTP service is available, register our servlet...
        HttpService httpService = (HttpService) this.context.getService(reference);
        try {
          // All the request to / will be redirected to /index.html
          httpService.registerServlet("/", new RedirectServlet(CONTEXT_PATH.concat("/index.html")),
              null, null);

          // Map the CONTEXT_PATH to the the "ui" directory, so all
          // the static contents in the "ui" directory will be
          // served when you connect to CONTEXT_PATH.
          // Note the "ui" directory and files are inside the
          // generated JAR of the bundle.
          httpService.registerResources(CONTEXT_PATH, "ui", null);

          LOGGER.log(Level.INFO, "Spring Roo Eastern Grey UI started at 'http://localhost:9191/'");
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, "Unabled to connect to 'http://localhost:9191/'", ex);
        }
        return httpService;
      }
    };
    // start tracking all HTTP services...
    httpTracker.open();
  }

  /**
   * Close the {@link #httpTracker}.
   * 
   * @param bundleContext
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    // stop tracking all HTTP services...
    httpTracker.close();
  }
}
