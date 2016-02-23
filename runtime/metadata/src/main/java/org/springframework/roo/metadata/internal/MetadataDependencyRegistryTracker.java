package org.springframework.roo.metadata.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.roo.metadata.MetadataDependency;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataNotificationListener;

/**
 * {@link ServiceTracker} to obtain a MetadataDependencyRegistry from the
 * service registry.
 * 
 * @author Enrique Ruiz at DISID Corporation S.L.
 */
public class MetadataDependencyRegistryTracker extends
    ServiceTracker<MetadataDependencyRegistry, MetadataDependencyRegistry> {

  private MetadataNotificationListener listener;
  private MetadataDependency[] dependencies;

  /**
   * Create new ServiceTracker to obtain a MetadataDependencyRegistry and
   * register the given metadata dependency.
   * 
   * @param bc Bundle execution context
   * @param notificationListener Registers an additional instance to receive
   *            MetadataNotificationListener events. It could be null.
   * @param upstreamDependency the upstream dependency (required; eg metadata
   *            representing a disk file)
   * @param downstreamDependency the downstream dependency (required; eg
   *            metadata representing a Java type)
   */
  public MetadataDependencyRegistryTracker(BundleContext bc,
      MetadataNotificationListener notificationListener) {
    this(bc, notificationListener, (MetadataDependency[]) null);
  }

  /**
   * Create new ServiceTracker to obtain a MetadataDependencyRegistry and
   * register the given metadata dependency.
   * 
   * @param bc Bundle execution context
   * @param notificationListener Registers an additional instance to receive
   *            MetadataNotificationListener events. It could be null.
   * @param upstreamDependency the upstream dependency (required; eg metadata
   *            representing a disk file)
   * @param downstreamDependency the downstream dependency (required; eg
   *            metadata representing a Java type)
   */
  public MetadataDependencyRegistryTracker(BundleContext bc,
      MetadataNotificationListener notificationListener, String upstreamDependency,
      String downstreamDependency) {
    this(bc, notificationListener, new MetadataDependency(upstreamDependency, downstreamDependency));
  }

  /**
   * Create new ServiceTracker to obtain a MetadataDependencyRegistry and
   * register the given metadata dependencies.
   * 
   * @param bc Bundle execution context
   * @param notificationListener Registers an additional instance to receive
   *            MetadataNotificationListener events. It could be null.
   * @param dependencies the upstream-downstream dependencies to be registered
   *            (required)
   */
  public MetadataDependencyRegistryTracker(BundleContext bc,
      MetadataNotificationListener notificationListener, MetadataDependency... dependencies) {
    super(bc, MetadataDependencyRegistry.class.getName(), null);
    this.listener = notificationListener;
    this.dependencies = dependencies;
  }

  /**
   * Register the metadata {@link #dependencies} in the given 
   * {@link MetadataDependencyRegistry} service.
   * <p/>
   * Moreover if {@link #listener} is not null registers an additional 
   * instance to send {@link MetadataNotificationListener} events to that 
   * {@link #listener}.
   * 
   * @param ref MetadataDependencyRegistry service reference 
   */
  @Override
  public MetadataDependencyRegistry addingService(ServiceReference<MetadataDependencyRegistry> ref) {
    MetadataDependencyRegistry registry = super.addingService(ref);
    if (this.listener != null) {
      registry.addNotificationListener(this.listener);
    }
    if (this.dependencies != null) {
      for (MetadataDependency dependency : this.dependencies) {
        registry.registerDependency(dependency.getUpstreamDependency(),
            dependency.getDownstreamDependency());
      }
    }
    return registry;
  }

  @Override
  public void modifiedService(ServiceReference<MetadataDependencyRegistry> reference,
      MetadataDependencyRegistry service) {
    super.modifiedService(reference, service);
  }

  /**
   * The {@link MetadataDependencyRegistry} service has been removed, so
   * unregister the metadata {@link #dependencies}.
   */
  @Override
  public void removedService(ServiceReference<MetadataDependencyRegistry> ref,
      MetadataDependencyRegistry registry) {
    if (this.listener != null) {
      registry.removeNotificationListener(this.listener);
    }
    if (this.dependencies != null) {
      for (MetadataDependency dependency : this.dependencies) {
        registry.deregisterDependency(dependency.getUpstreamDependency(),
            dependency.getDownstreamDependency());
      }
    }
    super.removedService(ref, registry);
  }
}
