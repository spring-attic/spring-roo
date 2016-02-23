package org.springframework.roo.rest.publisher;

import javax.ws.rs.Path;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Filter to get all Services.
 * <p/>
 * Use this filter a ServiceTracker is needed and the Service interface is not
 * known or you need to take the services annotated with an specific annotation.
 * 
 * @author Enrique Ruiz at DISID Corporation S.L.
 * @since 2.0
 */
public class AnyServiceFilter {

  /**
   * When registering a {@link Path} or @Provider annotated object as an OSGi service
   * the connector does publish this resource automatically. Anyway, in some
   * scenarios it's not wanted to publish those services. If you want a
   * resource not publish set this property as a service property with the
   * value <code>false</code>.
   */
  public static final String PUBLISH = "spring-roo.jaxrs.publish";

  /**
   * Filter to get all services except those that defines the property
   * {@link #PUBLISH} to false
   */
  static final String FILTER_STRING = "(&(objectClass=*)(!(".concat(PUBLISH).concat("=false)))");

  private final BundleContext context;

  /**
   * Creates a new filter.
   * 
   * @param context
   */
  public AnyServiceFilter(BundleContext context) {
    validateContext(context);
    this.context = context;
  }

  /**
   * Validate context.
   * 
   * @param context
   */
  private void validateContext(BundleContext context) {
    if (context == null) {
      throw new IllegalArgumentException("context must not be null");
    }
  }

  /**
   * Create the filter from the {@link #FILTER_STRING}.
   * 
   * @return
   */
  public Filter getFilter() {
    try {
      return context.createFilter(FILTER_STRING);
    } catch (InvalidSyntaxException willNotHappen) {
      throw new IllegalStateException(willNotHappen);
    }
  }
}
