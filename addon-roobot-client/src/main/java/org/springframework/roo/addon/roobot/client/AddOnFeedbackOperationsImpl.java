package org.springframework.roo.addon.roobot.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.json.simple.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.roobot.client.model.Rating;
import org.springframework.roo.felix.BundleSymbolicName;
import org.springframework.roo.support.osgi.BundleFindingUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.uaa.UaaRegistrationService;
import org.springframework.roo.url.stream.UrlInputStreamService;

/**
 * Implementation of {@link AddOnFeedbackOperations}.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class AddOnFeedbackOperationsImpl implements AddOnFeedbackOperations {

	@Reference private UaaRegistrationService registrationService;
	@Reference private UrlInputStreamService urlInputStreamService;
	private BundleContext bundleContext;
	private static final Logger log = Logger.getLogger(AddOnFeedbackOperationsImpl.class.getName());
	
	protected void activate(ComponentContext context) {
		this.bundleContext = context.getBundleContext();
	}

	protected void deactivate(ComponentContext context) {
		this.bundleContext = null;
	}
	
	@SuppressWarnings("unchecked")
	public void feedbackBundle(BundleSymbolicName bsn, Rating rating, String comment) {
		Assert.notNull(bsn, "Bundle symbolic name required");
		Assert.notNull(rating, "Rating required");
		Assert.isTrue(comment == null || comment.length() <= 140, "Comment must be under 140 characters");
		if ("".equals(comment)) {
			comment = null;
		}
		
		// Figure out the HTTP URL we'll get "GET"ing in order to submit the user's feedback
		URL httpUrl;
		try {
			httpUrl = new URL(UaaRegistrationService.EMPTY_FILE_URL);
		} catch (MalformedURLException shouldNeverHappen) {
			throw new IllegalStateException(shouldNeverHappen);
		}
		
		// Fail early if we're not allowed GET this URL due to UAA restrictions
		String failureMessage = urlInputStreamService.getUrlCannotBeOpenedMessage(httpUrl);
		if (failureMessage != null) {
			log.warning(failureMessage);
			return;
		}
		
		// To get this far, there is no reason we shouldn't be able to store this user's feedback
		JSONObject o = new JSONObject();
		o.put("version", UaaRegistrationService.SPRING_ROO.getMajorVersion() + "." + UaaRegistrationService.SPRING_ROO.getMajorVersion() + "." + UaaRegistrationService.SPRING_ROO.getPatchVersion());
		o.put("type", "bundle_feedback");
		o.put("bsn", JSONObject.escape(bsn.getKey())); // a BSN shouldn't need escaping, but anyway...
		o.put("rating", rating.getKey());
		o.put("comment", comment == null ? "" : JSONObject.escape(comment));
		String customJson = o.toJSONString();
		
		// Register the feedback. Note we record all feedback against the BSN for the RooBot client add-on to assist simple server-side detection.
		// We do NOT record feedback against the BSN that is receiving the feedback (as the BSN receiving the feedback is stored inside the custom JSON).
		registrationService.registerBundleSymbolicNameUse(BundleFindingUtils.findFirstBundleForTypeName(bundleContext, AddOnRooBotOperations.class.getName()), customJson);
		
		// Push the feedback up to the server now if possible
		registrationService.requestTransmission();
		
		log.info("Thanks for sharing your feedback.");
	}

}