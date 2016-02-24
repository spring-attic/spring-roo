package org.springframework.roo.rest.publisher;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Creates a {@link JAXRSApplication} that tracks and registers JAX-RS
 * resources.
 * 
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 2.0
 */
public class Activator implements BundleActivator {

  /** The URI namespace at which the container servlet is registered */
  private static final String CONTEXT_PATH = "/rs-api";

  private HttpTracker httpTracker = null;
  private JAXRSApplication jaxrsApplication = null;

  /**
   * Creates the {@link JAXRSApplication} and open the HTTP service tracker to
   * configure the application with the tracked HTTP service.
   * 
   * @param bundleContext
   */
  @Override
  public synchronized void start(BundleContext bundleContext) throws Exception {
    this.jaxrsApplication = new JAXRSApplication(bundleContext, CONTEXT_PATH);

    this.httpTracker = new HttpTracker(bundleContext, this.jaxrsApplication);
    this.httpTracker.open();
  }

  /**
   * Stops the {@link JAXRSApplication} and close the {@link #httpTracker}.
   * 
   * @param bundleContext
   */
  @Override
  public synchronized void stop(BundleContext bundleContext) throws Exception {
    this.jaxrsApplication.stop();
    this.httpTracker.close();
  }
}
