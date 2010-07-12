package org.springframework.roo.url.stream.jdk;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.url.stream.UrlInputStreamService;

/**
 * Simple implementation of {@link UrlInputStreamService} that uses the JDK.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
@Component
@Service
public class JdkUrlInputStreamService implements UrlInputStreamService {

	public InputStream openConnection(URL httpUrl) throws IOException {
		URLConnection conn = httpUrl.openConnection();
		conn.setRequestProperty("user-agent", "roo-jdk-url-input-stream");
		return conn.getInputStream();
	}
	
}

