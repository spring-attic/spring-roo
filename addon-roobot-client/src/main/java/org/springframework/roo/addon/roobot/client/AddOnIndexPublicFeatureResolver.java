package org.springframework.roo.addon.roobot.client;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.uaa.PublicFeatureResolver;

/**
 * Resolves public features by reference to the RooBot add-on index.
 * <p>
 * Any item in the RooBot add-on index is considered a public feature. In
 * addition, any item starting with "org.springframework.roo" is considered a
 * public feature. All other items are considered private features.
 * 
 * @author Ben Alex
 * @since 1.1.1
 */
@Component
@Service
public class AddOnIndexPublicFeatureResolver implements PublicFeatureResolver {

    @Reference AddOnRooBotOperations rooBotOperations;

    public boolean isPublic(final String bundleSymbolicNameOrTypeName) {
        if (bundleSymbolicNameOrTypeName.startsWith("org.springframework.roo")) {
            return true;
        }
        for (final String bsn : rooBotOperations.getAddOnCache(false).keySet()) {
            if (bundleSymbolicNameOrTypeName.startsWith(bsn)) {
                return true;
            }
        }
        return false;
    }
}
