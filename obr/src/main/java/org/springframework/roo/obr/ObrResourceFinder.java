package org.springframework.roo.obr;

import java.util.List;

import org.osgi.service.obr.Resource;

/**
 * Obtains access to Apache OBR repositories.
 *
 * <p>
 * Centralised via this interface to facilitate caching and eager downloading for the entire
 * Spring Roo system.
 *
 * @author Ben Alex
 * @since 1.1
 */
public interface ObrResourceFinder {

	/**
	 * Obtains all known resources from all known repositories, if possible.
	 * 
	 * <p>
	 * Returning null means the resources could not presently be downloaded for some reason (eg
	 * bad internet connection). Returning an empty list means the resources were downloaded but
	 * there simply aren't any resources in any OBR repositories (or no OBR repositories).
	 * 
	 * @return the known resources or null if none are presently available
	 */
	List<Resource> getKnownResources();
	
	/**
	 * Indicates how many OBR repositories are registered.
	 * 
	 * <p>
	 * Null is returned if we have not yet completed a download. Zero is returned if there are
	 * none registered. A positive integer is returned if some are registered and we have
	 * downloaded their contents. 
	 * 
	 * @return zero or above, or null if the download has not completed
	 */
	Integer getRepositoryCount();
}
