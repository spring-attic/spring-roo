package org.springframework.roo.rest.publisher;

import javax.ws.rs.Path;
import javax.ws.rs.core.Feature;
import javax.ws.rs.ext.Provider;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Track for all services and check if each one is annotated with {@link Path}, 
 * {@link Provider} or it implements {@link Feature}. If true this tracker
 * publish the resource automatically.
 * <p/>
 * If you want a resource won't be published set the property 
 * {@link AnyServiceFilter#PUBLISH} to {@code false}.
 * 
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 2.0
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ResourceTracker extends ServiceTracker {

  private final BundleContext bundleContext;
  private final JAXRSApplication jaxrsApplication;

  public ResourceTracker(BundleContext bc, JAXRSApplication jaxrsApp) {
    super(bc, new AnyServiceFilter(bc).getFilter(), null);
    this.bundleContext = bc;
    this.jaxrsApplication = jaxrsApp;
  }

  @Override
  public Object addingService(ServiceReference ref) {
    Object service = super.addingService(ref);
    return doAddService(ref, service);
  }

  /**
   * Check if given service is a JAX-RS resource and add it to the
   * JAX-RS application.
   * 
   * @param ref
   * @param service
   * @return
   */
  private Object doAddService(ServiceReference ref, Object service) {
    Object result;
    if (isResource(service)) {
      result = jaxrsApplication.addResource(ref);
    } else {
      bundleContext.ungetService(ref);
      result = null;
    }
    return result;
  }

  @Override
  public void removedService(ServiceReference ref, Object service) {
    // connector.removeResource( service );
    super.removedService(ref, service);
  }

  @Override
  public void modifiedService(ServiceReference ref, Object service) {
    // connector.removeResource( service );
    doAddService(ref, service);
  }

  /**
   * Check if the given service is a JAX-RS resource, that is, it is 
   * annotated with {@link Path}, {@link Provider} or it implements 
   * {@link Feature}
   * 
   * @param service
   * @return
   */
  private boolean isResource(Object service) {
    boolean result =
        service != null && (hasRegisterableAnnotation(service) || service instanceof Feature);
    return result;
  }

  /**
   * Check if the given service or any of the interfaces it implements
   * are annotated {@link Path} or {@link Provider}.
   * 
   * @param service
   * @return
   */
  private boolean hasRegisterableAnnotation(Object service) {
    boolean result = isRegisterableAnnotationPresent(service.getClass());
    if (!result) {
      Class<?>[] interfaces = service.getClass().getInterfaces();
      for (Class<?> type : interfaces) {
        result = result || isRegisterableAnnotationPresent(type);
      }
    }
    return result;
  }

  /**
   * Check if the given class is annotated with {@link Path} or 
   * {@link Provider}.
   * 
   * @param type class to check
   * @return true/false
   */
  private boolean isRegisterableAnnotationPresent(Class<?> type) {
    boolean result =
        type.isAnnotationPresent(Path.class) || type.isAnnotationPresent(Provider.class);
    return result;
  }
}
