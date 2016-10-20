package org.springframework.roo.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.osgi.ServiceInstaceManager;

/**
 * Allows a {@link MetadataProvider} or other class to track hash codes of
 * {@link MetadataItem}s and only invoke
 * {@link MetadataDependencyRegistry#notifyDownstream(String)} if there has been
 * an actual change since the last notification.
 * <p>
 * IMPORTANT: Before subclassing this class, ensure the {@link MetadataItem}s
 * that you will be presenting are all of the same type AND they provide a
 * reliable {@link Object#hashCode()} method. Failure to observe this
 * requirement will result in erroneous notifications.
 *
 * @author Ben Alex
 * @since 1.1
 */
@Component(componentAbstract = true)
public abstract class AbstractHashCodeTrackingMetadataNotifier {

  protected final static Logger LOGGER = HandlerUtils
      .getLogger(AbstractHashCodeTrackingMetadataNotifier.class);

  // ------------ OSGi component attributes ----------------
  /**
   * @deprecated this property should be _private_ and set by {@link #activate(ComponentContext)}
   *        method
   */
  public BundleContext context;

  protected ServiceInstaceManager serviceManager = null;

  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    serviceManager = new ServiceInstaceManager();
    this.serviceManager.activate(this.context);
  }

  private final Map<String, Integer> hashes = new HashMap<String, Integer>();

  /**
   * Notifies downstream dependencies of a change if and only if the passed
   * metadata item has a different hash code than the existing metadata item.
   * This is aimed at reducing needless notifications if nothing has actually
   * changed since the last notification.
   *
   * @param metadataItem the potentially-updated metadata item (required; must
   *            be a metadata item of the same class as all other items
   *            presented to this class)
   */
  protected void notifyIfRequired(final MetadataItem metadataItem) {

    final String instanceId = MetadataIdentificationUtils.getMetadataInstance(metadataItem.getId());
    final Integer existing = hashes.get(instanceId);
    final int newHash = metadataItem.hashCode();
    if (existing != null && newHash == existing) {
      // No need to notify
      return;
    }
    // To get this far, we need to notify and replace/add the metadata
    // item's hash for future reference
    hashes.put(instanceId, newHash);

    // Eagerly insert into the cache to so any recursive gets for this
    // metadata item will be returned successfully
    getMetadataService().put(metadataItem);

    if (getMetadataDependencyRegistry() != null) {
      getMetadataDependencyRegistry().notifyDownstream(metadataItem.getId());
    }
  }

  /**
   *
   * @deprecated this method should be removed as {@link #serviceManager}
   *    should be created and activated thru {@link #activate(ComponentContext)}
   *    method of this class. Child classes should call super.activate in its activate
   *    method
   */
  public ServiceInstaceManager getServiceManager() {
    // TODO this method should be removed this
    if (serviceManager == null) {
      serviceManager = new ServiceInstaceManager();
      serviceManager.activate(context);
    }
    return serviceManager;
  }

  public MetadataDependencyRegistry getMetadataDependencyRegistry() {
    return getServiceManager().getServiceInstance(this, MetadataDependencyRegistry.class);
  }

  public MetadataService getMetadataService() {
    return getServiceManager().getServiceInstance(this, MetadataService.class);
  }

}
