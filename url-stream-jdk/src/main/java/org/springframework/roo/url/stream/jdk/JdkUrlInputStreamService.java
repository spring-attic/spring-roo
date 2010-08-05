package org.springframework.roo.url.stream.jdk;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.osgi.AbstractFlashingObject;
import org.springframework.roo.support.util.Assert;
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
public class JdkUrlInputStreamService extends AbstractFlashingObject implements UrlInputStreamService {

	public InputStream openConnection(URL httpUrl) throws IOException {
		URLConnection connection = httpUrl.openConnection();
		connection.setRequestProperty("user-agent", "roo-jdk-url-input-stream");
		return new ProgressIndicatingInputStream(connection);
	}
	
	private class ProgressIndicatingInputStream extends InputStream {
		private InputStream delegate;
		private float totalSize;
		private float readSoFar;
		private int lastPercentageIndicated = -1;
		private long lastNotified;
		private String text;
		
		public ProgressIndicatingInputStream(URLConnection connection) throws IOException {
			Assert.notNull(connection, "URL Connection required");
			this.totalSize = connection.getContentLength();
			this.delegate = connection.getInputStream();
			this.text = connection.getURL().getPath();
			if ("".equals(this.text)) {
				// fallback to the host name
				this.text = connection.getURL().getHost();
			} else {
				// we only want the filename
				int lastSlash = this.text.lastIndexOf("/");
				if (lastSlash > -1) {
					this.text = this.text.substring(lastSlash+1);
				}
			}
		}

		@Override
		public int read() throws IOException {
			readSoFar++;
			if (totalSize > 0) {
				// Total size is known
				int percentageDownloaded = Math.round((readSoFar / totalSize) * 100);
				if (System.currentTimeMillis() > (lastNotified + 1000)) {
					if (lastPercentageIndicated != percentageDownloaded) {
						flash(Level.FINE, "Downloaded " + percentageDownloaded + "% of " + text, MY_SLOT);
						lastPercentageIndicated = percentageDownloaded;
						lastNotified = System.currentTimeMillis();
					}
				}
			} else {
				// Total size is not known, rely on time-based updates instead
				if (System.currentTimeMillis() > (lastNotified + 1000)) {
					flash(Level.FINE, "Downloaded " + Math.round((readSoFar/1024)) + " kB of " + text, MY_SLOT);
					lastNotified = System.currentTimeMillis();
				}
			}
			
			int result = delegate.read();
			
			if (result == -1) {
				if (totalSize > 0) {
					flash(Level.FINE, "Downloaded 100% of " + text, MY_SLOT);
				} else {
					flash(Level.FINE, "Downloaded " + Math.round((readSoFar/1024)) + " kB of " + text, MY_SLOT);
				}
				flash(Level.FINE, "", MY_SLOT);
			}
			
			return result;
		}

		@Override
		public void close() throws IOException {
			flash(Level.FINE, "", MY_SLOT);
			delegate.close();
		}
	}


}

