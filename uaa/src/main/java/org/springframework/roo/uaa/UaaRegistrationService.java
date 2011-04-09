package org.springframework.roo.uaa;

import org.springframework.uaa.client.UaaService;
import org.springframework.uaa.client.VersionHelper;
import org.springframework.uaa.client.protobuf.UaaClient.Product;

/**
 * Provides an API for any other Roo modules or add-ons to use to record UAA data.
 * 
 * <p>
 * This API ensures UAA conventions used by Roo are observed.
 * 
 * <p>
 * Implementations should perform the initial registration of the {@link #SPRING_ROO} product.
 * 
 * <p>
 * Implementations are required to buffer all notifications until such time as the {@link UaaService}
 * reaches a privacy level where they can be successfully persisted. An implementation can rely on
 * an invocation of {@link #flushIfPossible()} or attempt to write notifications on a subsequent
 * call to a standard registration method. Implementations are therefore not required to establish
 * a thread to handle flushing themselves, although they should make a final attempt on
 * component deactivation.
 * 
 * @author Ben Alex
 * @since 1.1.1
 *
 */
public interface UaaRegistrationService {

	/**
	 * A HTTP URL of an "empty file" that add-ons can request if they wish to eagerly perform a UAA upload.
	 */
	public static final String EMPTY_FILE_URL = "http://spring-roo-repository.springsource.org/empty_file.html";
	
	/**
	 * Static representation of the Spring Roo product that should be used by any modules
	 * requiring a product representation.
	 */
	public static final Product SPRING_ROO = VersionHelper.getProductFromManifest(UaaRegistrationServiceImpl.class, "Spring Roo");

	/**
	 * Registers a new "feature use" within UAA. This method requires every feature to be a bundle
	 * symbolic name. This method permits (but does not require) the presentation of UTF-8 encoded
	 * custom JSON that will be stored as feature_data in the resulting UAA payload.
	 * 
	 * <p>
	 * This method may be invoked without determining if the bundle symbolic name is public or not.
	 * This determination will be automatically made by implementations. Non-public bundle symbolic
	 * names will not be used. 
	 * 
	 * @param bundleSymbolicName a BSN to register the use of (required)
	 * @param customJson an optional JSON payload (can be null or an empty string if required)
	 */
	void registerBundleSymbolicNameUse(String bundleSymbolicName, String customJson);
	
	/**
	 * Registers a new "project" within UAA against the presented product. Note that
	 * UAA will use SHA-256 encoding for the project ID and it is never stored or transmitted in 
	 * a non-hashed form.
	 * 
	 * <p>
	 * A product is mandatory because a caller requiring a fallback product may use
	 * {@link #SPRING_ROO}. A project ID is mandatory because if low-level product information is
	 * available, this indicates a project configuration of some description is also available and
	 * therefore a project ID should also be available.
	 * 
	 * @param product the product (required)
	 * @param projectId the project name to register (required)
	 */
	void registerProject(Product product, String projectId);
	
	/**
	 * Indicates to attempt to flush the buffered notifications. If the {@link UaaService} is at a
	 * privacy level where it will accept registrations, the buffered notifications should be sent
	 * to the service. If the privacy level does not support this, the buffer should be preserved.
	 */
	void flushIfPossible();
	
	/**
	 * Attempts to transmit the data immediately to the server. This will only occur if the privacy
	 * level is acceptable and the {@link UaaService} is capable of transmission. Note this method
	 * should very rarely be necessary. It is only useful if an immediate transmission is desirable
	 * for some special reason (eg UAA is being used to convey user contributions to the server).
	 */
	void requestTransmission();
}
