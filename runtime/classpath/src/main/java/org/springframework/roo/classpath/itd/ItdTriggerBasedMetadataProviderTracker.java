package org.springframework.roo.classpath.itd;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.roo.model.JavaType;

/**
 * {@link ServiceTracker} to obtain an ItdTriggerBasedMetadataProvider from 
 * the service registry.
 *  
 * @author Enrique Ruiz at DISID Corporation S.L.
 */
public class ItdTriggerBasedMetadataProviderTracker extends
    ServiceTracker<ItdTriggerBasedMetadataProvider, ItdTriggerBasedMetadataProvider> {

  private final JavaType javaType;

  /**
   * Create new ServiceTracker to obtain any ItdTriggerBasedMetadataProvider.   
   * 
   * @param bc Bundle execution context
   * @param javaType the type-level annotation to detect that will cause
   *            metadata creation (required)
   */
  public ItdTriggerBasedMetadataProviderTracker(BundleContext bc, JavaType javaType) {
    this(bc, ItdTriggerBasedMetadataProvider.class, javaType);
  }

  /**
   * Create new ServiceTracker to obtain the service subclass of 
   * ItdTriggerBasedMetadataProvider.
   * 
   * @param bc Bundle execution context
   * @param clazz the class name of the
   *            {@link ItdTriggerBasedMetadataProvider} to be tracked.
   * @param javaType the type-level annotation to detect that will cause
   *            metadata creation (required)
   */
  public ItdTriggerBasedMetadataProviderTracker(BundleContext bc,
      Class<? extends ItdTriggerBasedMetadataProvider> clazz, JavaType javaType) {
    super(bc, clazz.getName(), null);
    this.javaType = javaType;
  }

  /**
   * Register a trigger that causes the given service provider will generate 
   * metadata if the {@link #javaType} annotation is present. 
   * 
   * @param ref service provider reference 
   */
  @Override
  public ItdTriggerBasedMetadataProvider addingService(
      ServiceReference<ItdTriggerBasedMetadataProvider> ref) {
    ItdTriggerBasedMetadataProvider metadataProvider = super.addingService(ref);
    metadataProvider.addMetadataTrigger(this.javaType);
    return metadataProvider;
  }

  /**
   * The {@link ItdTriggerBasedMetadataProvider} service has been removed, so
   * unregister the trigger of the {@link #javaType}.
   */
  @Override
  public void removedService(ServiceReference<ItdTriggerBasedMetadataProvider> ref,
      ItdTriggerBasedMetadataProvider metadataProvider) {
    metadataProvider.removeMetadataTrigger(this.javaType);
    super.removedService(ref, metadataProvider);
  }
}
