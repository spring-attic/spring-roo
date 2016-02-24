package org.springframework.roo.classpath.customdata.taggers;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.roo.model.CustomDataAccessor;

/**
 * {@link ServiceTracker} to obtain a CustomDataKeyDecorator from 
 * the service registry.
 *  
 * @author Enrique Ruiz at DISID Corporation S.L.
 */
@SuppressWarnings("rawtypes")
public class CustomDataKeyDecoratorTracker extends
    ServiceTracker<CustomDataKeyDecorator, CustomDataKeyDecorator> {

  private final Matcher<? extends CustomDataAccessor>[] matchers;
  private final Class clazz;

  /**
   * Create new ServiceTracker to obtain a CustomDataKeyDecorator.   
   * 
   * @param bc
   * @param clazz the name of the class registering the matcher (required)
   * @param matcher the matchers to register (required)
   */
  public CustomDataKeyDecoratorTracker(BundleContext bc, Class clazz,
      Matcher<? extends CustomDataAccessor>... matchers) {
    super(bc, CustomDataKeyDecorator.class.getName(), null);
    this.matchers = matchers;
    this.clazz = clazz;
  }

  /**
   * Register the {@link #matchers} in the given {@link CustomDataKeyDecorator}
   * service reference. 
   * 
   * @param ref service provider reference 
   */
  @Override
  public CustomDataKeyDecorator addingService(ServiceReference<CustomDataKeyDecorator> ref) {
    CustomDataKeyDecorator keyDecorator = super.addingService(ref);
    keyDecorator.registerMatchers(this.clazz, this.matchers);
    return keyDecorator;
  }

  /**
   * The {@link CustomDataKeyDecorator} service has been removed, so
   * unregister the matchers.
   */
  @Override
  public void removedService(ServiceReference<CustomDataKeyDecorator> ref,
      CustomDataKeyDecorator keyDecorator) {
    keyDecorator.unregisterMatchers(this.clazz);
    super.removedService(ref, keyDecorator);
  }
}
