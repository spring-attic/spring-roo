package org.springframework.roo.metadata;

import java.util.Set;

/**
 * Registers the dependencies between different metadata identification strings.
 * 
 * <p>
 * Metadata is free to depend on other metadata, including both metadata classes and metadata
 * instances being depended on or depending on other metadata classes and metadata instances.
 * In other words, metadata dependency graphs are not limited to only metadata instances.
 * 
 * <p>
 * For performance and memory efficiency reasons, only metadata identification strings are
 * stored by the {@link MetadataDependencyRegistry}. In particular, {@link MetadataItem} instances
 * are definitely not stored internally - which is especially applicable given they are immutable.
 * 
 * <p>
 * From a logical perspective, an item of metadata can "depend on" some other piece of metadata.
 * For example, an item of metadata representing the members appearing within a Java source file
 * would "depend on" the metadata that represents the physical source file on disk. An item of
 * metadata can also be "depended on" by other pieces of metadata. If the relationship just
 * exemplified was declared, the metadata representing the physical source file on disk would be
 * "depended on" by the Java member metadata. We use the terms "upstream" and "downstream" to
 * refer to these dependencies. In our example we would say that Java member metadata is
 * downstream of the physical source file on disk metadata. We would also say that the physical
 * source file on disk is upstream of the Java member metadata.
 * 
 * <p>
 * In terms of notifications, the {@link MetadataDependencyRegistry} maintains a reference to the
 * {@link MetadataService}, and delivers notifications to the {@link MetadataService} via its
 * extension of the {@link MetadataNotificationListener} interface. Note that a {@link MetadataService}
 * may subsequently deliver notifications to {@link MetadataProvider}s using the semantics defined in
 * the contract for {@link MetadataService}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public interface MetadataDependencyRegistry {
	
	/**
	 * Indicates whether the indicated downstream dependency is legally permitted to depend
	 * on the indicated upstream dependency. Specifically, the {@link MetadataDependencyRegistry}
	 * is required to verify the upstream dependency (and its dependencies) do not already
	 * depend on the downstream dependency (or any other dependency that itself depends on the
	 * downstream dependency). Good metadata design should prevent metadata from ever invoking
	 * this method and receiving a false response, but we like to be robust in ensuring the
	 * {@link MetadataDependencyRegistry} never can represent a circular dependency graph. 
	 * 
	 * <p>
	 * Both arguments must return true if presented to {@link MetadataIdentificationUtils#isValid(String)}.
	 * 
	 * @param upstreamDependency the upstream dependency (required; eg metadata representing a disk file)
	 * @param downstreamDependency the downstream dependency (required; eg metadata representing a Java type)
	 * @return true if the dependency relationship is legal
	 */
	boolean isValidDependency(String upstreamDependency, String downstreamDependency);
	
	/**
	 * Registers a dependency between two items of metadata.
	 * 
	 * <p>
	 * The two items of metadata will be presented to {@link #isValidDependency(String, String)}. If this
	 * method returns false, an exception will be generated.
	 * 
	 * @param upstreamDependency the upstream dependency (required; eg metadata representing a disk file)
	 * @param downstreamDependency the downstream dependency (required; eg metadata representing a Java type)
	 */
	void registerDependency(String upstreamDependency, String downstreamDependency);
	
	/**
	 * Registers an additional instance to receive {@link MetadataNotificationListener} events. Note that
	 * these events are guaranteed to be delivered after the {@link MetadataService} has received them.
	 * 
	 * <p>
	 * Attempting to register a {@link MetadataService} using this method will result in an exception.
	 * 
	 * @param listener to also receive all notifications (required)
	 */
	void addNotificationListener(MetadataNotificationListener listener);
	
	/**
	 * De-register an additional instance to receive {@link MetadataNotificationListener} events. If the
	 * listener was never registered in the first place, the method simply returns.
	 * 
	 * @param listener to no longer receive notifications (required)
	 */
	void removeNotificationListener(MetadataNotificationListener listener);
	
	/**
	 * Removes a dependency between two items of metadata.
	 * 
	 * <p>
	 * Both arguments must return true if presented to {@link MetadataIdentificationUtils#isValid(String)}.
	 * 
	 * <p>
	 * If the dependency was never registered in the first place, this method simply returns.
	 * 
	 * @param upstreamDependency the upstream dependency (required)
	 * @param downstreamDependency the downstream dependency (required)
	 */
	void deregisterDependency(String upstreamDependency, String downstreamDependency);
	
	/**
	 * Removes all upstream dependencies that were previously registered for the specified downstream dependency.
	 * This is useful if rebuilding the downstream dependency metadata, and just want to clear all old
	 * dependency information from the registry.
	 *  
	 * <p>
	 * The dependency must return true if presented to {@link MetadataIdentificationUtils#isValid(String)}.
	 * 
	 * <p>
	 * If the dependency was never registered in the first place, this method simply returns.
	 *  
	 * @param downstreamDependency the downstream dependency (required)
	 */
	void deregisterDependencies(String downstreamDependency);
	
	/**
	 * Causes the immediate downstream dependencies of the indicated metadata item to be notified the
	 * upstream metadata item is publishing a notification.
	 * 
	 * <p>
	 * The upstream dependency must return true if presented to {@link MetadataIdentificationUtils#isValid(String)}.
	 * However, the upstream dependency need not have been registered with this registry in advance (in
	 * which case the method will not notify any downstream dependencies, as none are known).
	 * 
	 * <p>
	 * Notifications are delivered to the {@link MetadataService} initially, followed by all 
	 * {@link MetadataNotificationListener}s registered against the instance.
	 * 
	 * @param upstreamDependency that is generating the notification (required).
	 */
	void notifyDownstream(String upstreamDependency);
	
	/**
	 * Obtains the list of the immediate downstream dependencies of the indicated metadata item.
	 * 
	 * <p>
	 * The upstream dependency must return true if presented to {@link MetadataIdentificationUtils#isValid(String)}.
	 * However, the upstream dependency need not have been registered with this registry in advance (in
	 * which case the method will not return any downstream dependencies, as none are known).
	 * 
	 * @param upstreamDependency to find the immediate downstream items for (required)
	 * @return an immutable set of dependencies (never null, but the set may be empty)
	 */
	Set<String> getDownstream(String upstreamDependency);
	
	/**
	 * Obtains a list of the immediate upstream dependencies of the indicated metadata item.
	 * 
	 * <p>
	 * The downstream dependency must return true if presented to {@link MetadataIdentificationUtils#isValid(String)}.
	 * 
	 * @param downstreamDependency to find the immediate upstream items for (required)
	 * @return an immutable set of dependencies (never null, but the set may be empty)
	 */
	Set<String> getUpstream(String downstreamDependency);
}
