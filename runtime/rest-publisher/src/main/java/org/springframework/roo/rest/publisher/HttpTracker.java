package org.springframework.roo.rest.publisher;

import javax.servlet.ServletException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks for an HTTP service and configure the {@link #application}.
 * 
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 2.0
 */
public class HttpTracker extends ServiceTracker<HttpService, HttpService> {

  private final BundleContext bundleContext;
  private final JAXRSApplication application;

  public HttpTracker(BundleContext bc, JAXRSApplication application) {
    super(bc, HttpService.class.getName(), null);
    this.bundleContext = bc;
    this.application = application;
  }

  @Override
  public HttpService addingService(ServiceReference<HttpService> ref) {
    try {
      HttpService service = super.addingService(ref);
      HttpService active = application.addHttpService(service);

      // Only the 1sr HttpService is used to register REST resources, so
      // if a previous HttpService was registered it wasn't be added.
      // It that case we don't need to track the last HttpService
      if (service != active) {
        bundleContext.ungetService(ref);
      }
      return service;
    } catch (ServletException ex) {
      throw new RuntimeException(ex);
    } catch (NamespaceException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void removedService(ServiceReference<HttpService> ref, HttpService service) {
    application.removeHttpService(service);
  }
}
