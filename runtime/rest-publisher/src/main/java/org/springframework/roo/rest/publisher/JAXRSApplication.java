package org.springframework.roo.rest.publisher;

import javax.json.stream.JsonGenerator;
import javax.servlet.ServletException;

import org.glassfish.jersey.jsonp.JsonProcessingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * {@link JAXRSApplication} that tracks and registers JAX-RS resources.
 * 
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 2.0
 */
public class JAXRSApplication {

  private BundleContext bc;
  private HttpService httpService;
  private ResourceConfig resourceConfig;
  private ResourceTracker resourceTracker = null;
  private ServletContainer servletContainer;
  private String contextPath;

  /**
   * Create REST Application and the default Jersey {@link ResourceConfig
   * https://jersey.java.net/apidocs/2.6/jersey/org/glassfish/jersey/server/
   * ResourceConfig.html}.
   * 
   * @param bundleContext
   */
  public JAXRSApplication(BundleContext bundleContext, String cpath) {
    this.bc = bundleContext;
    this.contextPath = cpath;
    this.resourceConfig =
        new ResourceConfig().register(JsonProcessingFeature.class).property(
            JsonGenerator.PRETTY_PRINTING, true);
  }

  /**
   * Set the HTTP service in which register the servlet container that will
   * host the JAX-RS resources.
   * <p/>
   * After get an HTTP service, start the resource tracker to track
   * for REST services.
   * 
   * @param service
   * @return
   * @throws ServletException
   * @throws NamespaceException
   */
  public HttpService addHttpService(HttpService service) throws ServletException,
      NamespaceException {

    // 1st registered HttpService will win
    if (this.httpService == null) {
      this.servletContainer = new ServletContainer(this.resourceConfig);
      this.httpService = service;
      this.httpService.registerServlet(this.contextPath, servletContainer, null, null);

      // After get an HTTP service, start the resource tracker to track
      // for REST resources
      this.resourceTracker = new ResourceTracker(this.bc, this);
      this.resourceTracker.open();

      return service;
    } else {
      return this.httpService;
    }
  }

  /**
   * Register the given JAX-RS service in the {@link JAXRS#servletContainer}.
   * 
   * @param ref JAX-RS service
   * @return registered service
   */
  public Object addResource(ServiceReference<Object> ref) {
    Object resource = this.bc.getService(ref);

    // How to add Resources at runtime in Jersey
    // http://stackoverflow.com/questions/27959594/can-we-add-resource-path-at-runtime-in-jersey#answer-28891762

    ResourceConfig freshConfig = new ResourceConfig(this.resourceConfig);
    freshConfig.registerInstances(resource);

    this.resourceConfig = freshConfig;
    this.servletContainer.reload(freshConfig);

    return resource;
  }

  /**
   * Stop the HTTP service
   * .
   * @param service
   */
  public void removeHttpService(HttpService service) {

    // stop if given service is the holded HttpService only
    if (this.httpService == service) {
      stop();
    }
  }

  /**
   * Stopping the bundle, so clean the resources and trackers.
   */
  public void stop() {
    if (this.resourceTracker != null) {
      this.resourceTracker.close();
    }
    if (this.servletContainer != null) {
      this.servletContainer.destroy();
    }
    if (this.httpService != null) {
      this.httpService.unregister(this.contextPath);
    }
    this.servletContainer = null;
    this.httpService = null;
  }
}
