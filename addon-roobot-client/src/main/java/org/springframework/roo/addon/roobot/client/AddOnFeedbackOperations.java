package org.springframework.roo.addon.roobot.client;

import org.springframework.roo.addon.roobot.client.model.Rating;
import org.springframework.roo.felix.BundleSymbolicName;

/**
 * Provides the operations support for add-on feedback.
 * 
 * <p>
 * We need this in a separate interface to avoid a circular dependency between the UAA support mechanism and {@link AddOnFeedbackOperations}.
 *
 * @author Ben Alex
 * @since 1.1.1
 */
public interface AddOnFeedbackOperations {
	
	/**
	 * Provide feedback on the {@link BundleSymbolicName}.
	 * 
	 * @param bsn the bundle symbolic name (required)
	 * @param rating the rating given (required)
	 * @param comment the comment (can be null if no comment was offered)
	 */
	void feedbackBundle(BundleSymbolicName bsn, Rating rating, String comment);
}