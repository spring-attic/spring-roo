package org.springframework.roo.metadata.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataLogger;
import org.springframework.roo.metadata.MetadataNotificationListener;
import org.springframework.roo.metadata.MetadataService;

/**
 * Default implementation of {@link MetadataDependencyRegistry}.
 * <p>
 * This implementation is not thread safe. It should only be accessed by a
 * single thread at a time. This is enforced by the process manager semantics,
 * so we avoid the cost of re-synchronization here.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class DefaultMetadataDependencyRegistry implements
        MetadataDependencyRegistry {
    /** key: downstream dependency; value: list<upstream dependencies> */
    private final Map<String, Set<String>> downstreamKeyed = new HashMap<String, Set<String>>();
    private final Set<MetadataNotificationListener> listeners = new HashSet<MetadataNotificationListener>();
    @Reference private MetadataLogger metadataLogger;
    private MetadataService metadataService;
    /** key: upstream dependency; value: list<downstream dependencies> */
    private final Map<String, Set<String>> upstreamKeyed = new HashMap<String, Set<String>>();

    public void addNotificationListener(
            final MetadataNotificationListener listener) {
        Validate.notNull(listener, "Metadata notification listener required");

        if (listener instanceof MetadataService) {
            Validate.isTrue(metadataService == null,
                    "Cannot register more than one MetadataListener");
            metadataService = (MetadataService) listener;
            return;
        }

        listeners.add(listener);
    }

    private void buildSetOfAllUpstreamDependencies(final Set<String> results,
            final String downstreamDependency) {
        final Set<String> upstreams = downstreamKeyed.get(downstreamDependency);
        if (upstreams == null) {
            return;
        }

        for (final String upstream : upstreams) {
            results.add(upstream);
            buildSetOfAllUpstreamDependencies(results, upstream);
        }
    }

    public void deregisterDependencies(final String downstreamDependency) {
        Validate.isTrue(
                MetadataIdentificationUtils.isValid(downstreamDependency),
                "Downstream dependency is an invalid metadata identification string ('%s')",
                downstreamDependency);

        // Acquire the keys to delete
        final Set<String> upstream = downstreamKeyed.get(downstreamDependency);
        if (upstream == null) {
            return;
        }

        final Set<String> upstreamToDelete = new HashSet<String>(upstream);

        // Delete them normally
        for (final String deleteUpstream : upstreamToDelete) {
            deregisterDependency(deleteUpstream, downstreamDependency);
        }
    }

    public void deregisterDependency(final String upstreamDependency,
            final String downstreamDependency) {
        Validate.isTrue(
                MetadataIdentificationUtils.isValid(upstreamDependency),
                "Upstream dependency is an invalid metadata identification string ('%s')",
                upstreamDependency);
        Validate.isTrue(
                MetadataIdentificationUtils.isValid(downstreamDependency),
                "Downstream dependency is an invalid metadata identification string ('%s')",
                downstreamDependency);

        // Maintain the upstream-keyed map, if it even exists
        final Set<String> downstream = upstreamKeyed.get(upstreamDependency);
        if (downstream != null) {
            downstream.remove(downstreamDependency);
        }

        // Maintain the downstream-keyed map, if it even exists
        final Set<String> upstream = downstreamKeyed.get(downstreamDependency);
        if (upstream != null) {
            upstream.remove(upstreamDependency);
        }
    }

    public Set<String> getDownstream(final String upstreamDependency) {
        Validate.isTrue(
                MetadataIdentificationUtils.isValid(upstreamDependency),
                "Upstream dependency is an invalid metadata identification string ('%s')",
                upstreamDependency);

        final Set<String> downstream = upstreamKeyed.get(upstreamDependency);
        if (downstream == null) {
            return new HashSet<String>();
        }

        return Collections.unmodifiableSet(new CopyOnWriteArraySet<String>(
                downstream));
    }

    public Set<String> getUpstream(final String downstreamDependency) {
        Validate.isTrue(
                MetadataIdentificationUtils.isValid(downstreamDependency),
                "Downstream dependency is an invalid metadata identification string ('%s')",
                downstreamDependency);

        final Set<String> upstream = downstreamKeyed.get(downstreamDependency);
        if (upstream == null) {
            return new HashSet<String>();
        }

        return Collections.unmodifiableSet(upstream);
    }

    public boolean isValidDependency(final String upstreamDependency,
            final String downstreamDependency) {
        Validate.isTrue(
                MetadataIdentificationUtils.isValid(upstreamDependency),
                "Upstream dependency is an invalid metadata identification string ('%s')",
                upstreamDependency);
        Validate.isTrue(
                MetadataIdentificationUtils.isValid(downstreamDependency),
                "Downstream dependency is an invalid metadata identification string ('%s')",
                downstreamDependency);
        Validate.isTrue(
                !upstreamDependency.equals(downstreamDependency),
                "Upstream dependency cannot be the same as the downstream dependency ('%s')",
                downstreamDependency);

        // The simplest possible outcome is the relationship already exists, so
        // quickly return in that case
        Set<String> downstream = upstreamKeyed.get(upstreamDependency);
        if (downstream != null && downstream.contains(downstreamDependency)) {
            return true;
        }
        // Don't need the variable anymore, as we don't care about the other
        // downstream dependencies
        downstream = null;

        // Need to walk the upstream dependency's parent dependency graph,
        // verifying no presence of the proposed downstream dependency

        // Need to build a set representing every eventual upstream dependency
        // of the indicated upstream dependency
        final Set<String> allUpstreams = new HashSet<String>();
        buildSetOfAllUpstreamDependencies(allUpstreams, upstreamDependency);

        // The dependency is valid if none of the upstreams depend on the
        // proposed downstream
        return !allUpstreams.contains(downstreamDependency);
    }

    public void notifyDownstream(final String upstreamDependency) {
        try {
            metadataLogger.startEvent();

            if (metadataService != null) {
                // First dispatch the fine-grained, instance-specific
                // dependencies.
                Set<String> notifiedDownstreams = new HashSet<String>();
                for (final String downstream : getDownstream(upstreamDependency)) {
                    if (metadataLogger.getTraceLevel() > 0) {
                        metadataLogger.log(upstreamDependency + " -> "
                                + downstream);
                    }
                    // No need to ensure upstreamDependency is different from
                    // downstream, as that's taken care of in the
                    // isValidDependency() method
                    try {
                        final String responsibleClass = MetadataIdentificationUtils
                                .getMetadataClass(downstream);
                        metadataLogger.startTimer(responsibleClass);
                        metadataService.notify(upstreamDependency, downstream);
                    }
                    finally {
                        metadataLogger.stopTimer();
                    }
                    notifiedDownstreams.add(downstream);
                }

                // Next dispatch the coarse-grained, class-specific
                // dependencies.
                // We only do it if the upstream is not class specific, as
                // otherwise we'd have handled class-specific dispatch in
                // previous loop
                if (!MetadataIdentificationUtils
                        .isIdentifyingClass(upstreamDependency)) {
                    final String asClass = MetadataIdentificationUtils
                            .getMetadataClassId(upstreamDependency);
                    for (final String downstream : getDownstream(asClass)) {
                        // We don't notify a downstream if it had a direct
                        // instance-specific dependency and was already notified
                        // in previous loop
                        // We also don't notify if upstream is the same as
                        // downstream, as it doesn't make sense to notify
                        // yourself of an event
                        // (such a condition is only possible if an instance
                        // registered to receive class-specific notifications
                        // and that instance
                        // caused an event to fire)
                        if (!notifiedDownstreams.contains(downstream)
                                && !upstreamDependency.equals(downstream)) {
                            if (metadataLogger.getTraceLevel() > 0) {
                                metadataLogger.log(upstreamDependency + " -> "
                                        + downstream + " [via class]");
                            }
                            try {
                                final String responsibleClass = MetadataIdentificationUtils
                                        .getMetadataClass(downstream);
                                metadataLogger.startTimer(responsibleClass);
                                metadataService.notify(upstreamDependency,
                                        downstream);
                            }
                            finally {
                                metadataLogger.stopTimer();
                            }
                        }
                    }
                }

                notifiedDownstreams = null;
            }

            // Finally dispatch the general-purpose additional listeners
            for (final MetadataNotificationListener listener : listeners) {
                if (metadataLogger.getTraceLevel() > 1) {
                    metadataLogger.log(upstreamDependency + " -> "
                            + upstreamDependency + " ["
                            + listener.getClass().getSimpleName() + "]");
                }
                try {
                    final String responsibleClass = listener.getClass()
                            .getName();
                    metadataLogger.startTimer(responsibleClass);
                    listener.notify(upstreamDependency, null);
                }
                finally {
                    metadataLogger.stopTimer();
                }
            }
        }
        finally {
            metadataLogger.stopEvent();
        }
    }

    public void registerDependency(final String upstreamDependency,
            final String downstreamDependency) {
        Validate.isTrue(
                isValidDependency(upstreamDependency, downstreamDependency),
                "Invalid dependency between upstream '%s' and downstream '%s'",
                upstreamDependency, downstreamDependency);

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

    public void removeNotificationListener(
            final MetadataNotificationListener listener) {
        Validate.notNull(listener, "Metadata notification listener required");

        if (listener instanceof MetadataService
                && listener.equals(metadataService)) {
            metadataService = null;
            return;
        }

        listeners.remove(listener);
    }
}
