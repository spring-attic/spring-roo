package org.springframework.roo.metadata.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataLogger;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.support.util.Assert;

/**
 * Default implementation of {@link MetadataDependencyRegistry}.
 * 
 * <p>
 * This implementation is not thread safe. It should only be accessed by a single thread at a time.
 * This is enforced by the process manager semantics, so we avoid the cost of re-synchronization here.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component
@Service
public final class DefaultMetadataDependencyRegistry implements MetadataDependencyRegistry {
	@Reference private MetadataLogger metadataLogger;
	/** key: upstream dependency; value: list<downstream dependencies> */
	private Map<String, Set<String>> upstreamKeyed = new HashMap<String, Set<String>>();
	/** key: downstream dependency; value: list<upstream dependencies> */
	private Map<String, Set<String>> downstreamKeyed = new HashMap<String, Set<String>>();
	private MetadataService metadataService;
	private Set<MetadataNotificationListener> listeners = new HashSet<MetadataNotificationListener>();
	
	public void registerDependency(String upstreamDependency, String downstreamDependency) {
		Assert.isTrue(isValidDependency(upstreamDependency, downstreamDependency), "Invalid dependency between upstream '" + upstreamDependency + "' and downstream '" + downstreamDependency + "'");
		
		// Maintain the upstream-keyed map
		Set<String> downstream = upstreamKeyed.get(upstreamDependency);
		if (downstream == null) {
			downstream = new HashSet<String>();
			upstreamKeyed.put(upstreamDependency, downstream);
		}
		downstream.add(downstreamDependency);
		
		// Maintain the downstream-keyed map
		Set<String> upstream = downstreamKeyed.get(downstreamDependency);
		if (upstream == null) {
			upstream = new HashSet<String>();
			downstreamKeyed.put(downstreamDependency, upstream);
		}
		upstream.add(upstreamDependency);
	}

	public void deregisterDependencies(String downstreamDependency) {
		Assert.isTrue(MetadataIdentificationUtils.isValid(downstreamDependency), "Downstream dependency is an invalid metadata identification string ('" + downstreamDependency + "')");
		
		// Acquire the keys to delete
		Set<String> upstream = downstreamKeyed.get(downstreamDependency);
		if (upstream == null) {
			return;
		}
		
		Set<String> upstreamToDelete = new HashSet<String>(upstream);
		
		// Delete them normally
		for (String deleteUpstream : upstreamToDelete) {
			deregisterDependency(deleteUpstream, downstreamDependency);
		}
	}

	public void deregisterDependency(String upstreamDependency, String downstreamDependency) {
		Assert.isTrue(MetadataIdentificationUtils.isValid(upstreamDependency), "Upstream dependency is an invalid metadata identification string ('" + upstreamDependency + "')");
		Assert.isTrue(MetadataIdentificationUtils.isValid(downstreamDependency), "Downstream dependency is an invalid metadata identification string ('" + downstreamDependency + "')");
		
		// Maintain the upstream-keyed map, if it even exists
		Set<String> downstream = upstreamKeyed.get(upstreamDependency);
		if (downstream != null) {
			downstream.remove(downstreamDependency);
		}
		
		// Maintain the downstream-keyed map, if it even exists
		Set<String> upstream = downstreamKeyed.get(downstreamDependency);
		if (upstream != null) {
			upstream.remove(upstreamDependency);
		}
	}

	public Set<String> getDownstream(String upstreamDependency) {
		Assert.isTrue(MetadataIdentificationUtils.isValid(upstreamDependency), "Upstream dependency is an invalid metadata identification string ('" + upstreamDependency + "')");
		
		Set<String> downstream = upstreamKeyed.get(upstreamDependency);
		if (downstream == null) {
			return new HashSet<String>();
		}
		
		return Collections.unmodifiableSet(new CopyOnWriteArraySet<String>(downstream));
	}
	
	public Set<String> getUpstream(String downstreamDependency) {
		Assert.isTrue(MetadataIdentificationUtils.isValid(downstreamDependency), "Downstream dependency is an invalid metadata identification string ('" + downstreamDependency + "')");
		
		Set<String> upstream = downstreamKeyed.get(downstreamDependency);
		if (upstream == null) {
			return new HashSet<String>();
		}
		
		return Collections.unmodifiableSet(upstream);
	}

	public boolean isValidDependency(String upstreamDependency, String downstreamDependency) {
		Assert.isTrue(MetadataIdentificationUtils.isValid(upstreamDependency), "Upstream dependency is an invalid metadata identification string ('" + upstreamDependency + "')");
		Assert.isTrue(MetadataIdentificationUtils.isValid(downstreamDependency), "Downstream dependency is an invalid metadata identification string ('" + downstreamDependency + "')");
		Assert.isTrue(!upstreamDependency.equals(downstreamDependency), "Upstream dependency cannot be the same as the downstream dependency ('" + upstreamDependency + "')");
		
		// The simplest possible outcome is the relationship already exists, so quickly return in that case
		Set<String> downstream = upstreamKeyed.get(upstreamDependency);
		if (downstream != null && downstream.contains(downstreamDependency)) {
			return true;
		}
		// Don't need the variable anymore, as we don't care about the other downstream dependencies
		downstream = null;
		
		// Need to walk the upstream dependency's parent dependency graph, verifying no presence of the proposed downstream dependency
		
		// Need to build a set representing every eventual upstream dependency of the indicated upstream dependency
		Set<String> allUpstreams = new HashSet<String>();
		buildSetOfAllUpstreamDependencies(allUpstreams, upstreamDependency);
		
		// The dependency is valid if none of the upstreams depend on the proposed downstream
		return !allUpstreams.contains(downstreamDependency);
	}
	
	private void buildSetOfAllUpstreamDependencies(Set<String> results, String downstreamDependency) {
		Set<String> upstreams = downstreamKeyed.get(downstreamDependency);
		if (upstreams == null) {
			return;
		}
		
		for (String upstream : upstreams) {
			results.add(upstream);
			buildSetOfAllUpstreamDependencies(results, upstream);
		}
	}

	public void addNotificationListener(MetadataNotificationListener listener) {
		Assert.notNull(listener, "Metadata notification listener required");
		
		if (listener instanceof MetadataService) {
			Assert.isTrue(metadataService == null, "Cannot register more than one MetadataListener");
			this.metadataService = (MetadataService) listener;
			return;
		}
		
		this.listeners.add(listener);
	}

	public void removeNotificationListener(MetadataNotificationListener listener) {
		Assert.notNull(listener, "Metadata notification listener required");
		
		if (listener instanceof MetadataService && listener.equals(this.metadataService)) {
			this.metadataService = null;
			return;
		}
		
		this.listeners.remove(listener);
	}

	public void notifyDownstream(String upstreamDependency) {
		try {
			metadataLogger.startEvent();
			
			if (metadataService != null) {
				// First dispatch the fine-grained, instance-specific dependencies.
				Set<String> notifiedDownstreams = new HashSet<String>();
				for (String downstream : getDownstream(upstreamDependency)) {
					if (metadataLogger.getTraceLevel() > 0) {
						metadataLogger.log(upstreamDependency + " -> " + downstream);
					}
					// No need to ensure upstreamDependency is different from downstream, as that's taken care of in the isValidDependency() method
					try {
						String responsibleClass = MetadataIdentificationUtils.getMetadataClass(downstream);
						metadataLogger.startTimer(responsibleClass);
						metadataService.notify(upstreamDependency, downstream);
					} finally {
						metadataLogger.stopTimer();
					}
					notifiedDownstreams.add(downstream);
				}
				
				// Next dispatch the coarse-grained, class-specific dependencies.
				// We only do it if the upstream is not class specific, as otherwise we'd have handled class-specific dispatch in previous loop 
				if (!MetadataIdentificationUtils.isIdentifyingClass(upstreamDependency)) {
					String asClass = MetadataIdentificationUtils.create(MetadataIdentificationUtils.getMetadataClass(upstreamDependency));
					for (String downstream : getDownstream(asClass)) {
						// We don't notify a downstream if it had a direct instance-specific dependency and was already notified in previous loop
						// We also don't notify if upstream is the same as downstream, as it doesn't make sense to notify yourself of an event
						// (such a condition is only possible if an instance registered to receive class-specific notifications and that instance
						// caused an event to fire)
						if (!notifiedDownstreams.contains(downstream) && !upstreamDependency.equals(downstream)) {
							if (metadataLogger.getTraceLevel() > 0) {
								metadataLogger.log(upstreamDependency + " -> " + downstream + " [via class]");
							}
							try {
								String responsibleClass = MetadataIdentificationUtils.getMetadataClass(downstream);
								metadataLogger.startTimer(responsibleClass);
								metadataService.notify(upstreamDependency, downstream);
							} finally {
								metadataLogger.stopTimer();
							}
						}
					}
				}
				
				notifiedDownstreams = null;
			}
			
			// Finally dispatch the general-purpose additional listeners
			for (MetadataNotificationListener listener : listeners) {
				if (metadataLogger.getTraceLevel() > 1) {
					metadataLogger.log(upstreamDependency + " -> " + upstreamDependency + " [" + listener.getClass().getSimpleName() + "]");
				}
				try {
					String responsibleClass = listener.getClass().getName();
					metadataLogger.startTimer(responsibleClass);
					listener.notify(upstreamDependency, null);
				} finally {
					metadataLogger.stopTimer();
				}
			}
		} finally {
			metadataLogger.stopEvent();
		}
	}
}
