package org.springframework.roo.uaa;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.metadata.MetadataLogger;
import org.springframework.roo.metadata.MetadataTimingStatistic;
import org.springframework.roo.support.osgi.BundleFindingUtils;

/**
 * Regularly polls {@link MetadataLogger#getTimings()} and incorporates all
 * timings into UAA feature use statistics.
 * 
 * @author Ben Alex
 * @since 1.1.1
 */
@Component(enabled = true)
public class MetadataPollingUaaRegistrationFacility {

    private class MetadataTimerTask extends TimerTask {
        @Override
        public void run() {
            // Try..catch used to avoid unexpected problems terminating the
            // timer thread
            try {
                // Deal with modules being used via the add-on infrastructure
                for (final MetadataTimingStatistic stat : metadataLogger
                        .getTimings()) {
                    final String typeName = stat.getName();
                    String bundleSymbolicName = typeToBsnMap.get(typeName);
                    if (bundleSymbolicName == null) {
                        // Try to look it up and cache the outcome
                        bundleSymbolicName = BundleFindingUtils
                                .findFirstBundleForTypeName(bundleContext,
                                        typeName);
                        if (bundleSymbolicName == null) {
                            bundleSymbolicName = NOT_FOUND;
                        }
                        // Cache to avoid the lookup cost in the future
                        typeToBsnMap.put(typeName, bundleSymbolicName);
                    }

                    if (NOT_FOUND.equals(bundleSymbolicName)) {
                        continue;
                    }

                    // Only notify the UAA service if we haven't previously told
                    // it about this BSN (UAA service handles buffering
                    // internally)
                    if (!previouslyNotifiedBsns.contains(bundleSymbolicName)) {
                        // UaaRegistrationService deals with determining if the
                        // BSN is public (non-public BSNs are not registered)
                        uaaRegistrationService.registerBundleSymbolicNameUse(
                                bundleSymbolicName, null);
                        previouslyNotifiedBsns.add(bundleSymbolicName);
                    }

                }
            }
            catch (final RuntimeException ignored) {
            }
        }
    }

    private static final String NOT_FOUND = "___NOT_FOUND___";
    private BundleContext bundleContext;
    @Reference private MetadataLogger metadataLogger;
    private final Set<String> previouslyNotifiedBsns = new HashSet<String>();
    private final Timer timer = new Timer();
    private final Map<String, String> typeToBsnMap = new HashMap<String, String>();

    @Reference private UaaRegistrationService uaaRegistrationService;

    protected void activate(final ComponentContext context) {
        bundleContext = context.getBundleContext();
        timer.scheduleAtFixedRate(new MetadataTimerTask(), 0, 5 * 1000);
    }

    protected void deactivate(final ComponentContext context) {
        timer.cancel();
    }
}
