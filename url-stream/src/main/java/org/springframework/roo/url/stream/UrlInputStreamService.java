package org.springframework.roo.url.stream;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Provides an {@link InputStream} for a given HTTP {@link URL}.
 * 
 * <p>
 * This implementation can be used to provide an alternative mechanism to download
 * resources. It can, for instance, populate proxy details, delegate to a download
 * agent provided by a host framework (like an IDE) and augment the headers of
 * the HTTP request.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
public interface UrlInputStreamService {
	
	/**
	 * Opens an input stream to the specified connection. The input stream
	 * represents the resource (no headers).
	 * 
	 * @param httpUrl to open (HTTP only, never HTTPS or another protocol)
	 * @return the input stream (implementation may not return null)
	 */
	InputStream openConnection(URL httpUrl) throws IOException;
	
	/**
	 * Returns a reason a URL cannot be opened, or null if there is no known reason
	 * why the URL wouldn't be able to be opened if {@link #openConnection(URL)} was
	 * invoked. The returned reasons should be formatted in a user-friendly manner
	 * for direct display to Roo users.
	 * 
	 * <p>
	 * The purpose of this method is to allow restrictions to be placed on the availability
	 * of URLs. For example, if the user has indicated offline operation is needed, or
	 * if the user needs to complete an enabling step such as terms of use acceptance.
	 * 
	 * @param httpUrl desired to open (HTTP only, never HTTPS or another protocol)
	 * @return null if URL can probably be opened, or a message why that URL is unavailable
	 */
	String getUrlCannotBeOpenedMessage(URL httpUrl);
}
