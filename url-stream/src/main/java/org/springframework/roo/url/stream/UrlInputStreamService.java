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
}
