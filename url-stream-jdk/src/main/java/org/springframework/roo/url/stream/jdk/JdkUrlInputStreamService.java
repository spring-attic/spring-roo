package org.springframework.roo.url.stream.jdk;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.osgi.AbstractFlashingObject;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.url.stream.UrlInputStreamService;
import org.springframework.roo.url.stream.UrlInputStreamUtils;
import org.springframework.uaa.client.UaaService;
import org.springframework.uaa.client.protobuf.UaaClient.Privacy.PrivacyLevel;

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

	@Reference private UaaService uaaService;
	
	public InputStream openConnection(URL httpUrl) throws IOException {
		Assert.notNull(httpUrl, "HTTP URL is required");
		Assert.isTrue(httpUrl.getProtocol().equals("http"), "Only HTTP is supported (not " + httpUrl + ")");
		
		// Fail if we're banned from accessing this domain
		Assert.isTrue(getUrlCannotBeOpenedMessage(httpUrl) == null, UrlInputStreamUtils.SETUP_UAA_REQUIRED);
		HttpURLConnection connection = (HttpURLConnection) httpUrl.openConnection();
		
		// Use UAA (we know UAA Terms of Use have been accepted by this point)
		if (UrlInputStreamUtils.isVMwareDomain(httpUrl)) {
			// Only send UAA header to VMware domains
			connection.setRequestProperty("user-agent", uaaService.toHttpUserAgentHeaderValue());
		}
		
		return new ProgressIndicatingInputStream(connection);
	}
	
	public String getUrlCannotBeOpenedMessage(URL httpUrl) {
		if (UrlInputStreamUtils.isVMwareDomain(httpUrl)) {
			if (uaaService.getPrivacyLevel() == PrivacyLevel.UNDECIDED_TOU || uaaService.getPrivacyLevel() == PrivacyLevel.DECLINE_TOU) {
				return UrlInputStreamUtils.SETUP_UAA_REQUIRED;
			}
		}
		// No reason it shouldn't work
		return null;
	}

	private class ProgressIndicatingInputStream extends InputStream {
		private InputStream delegate;
		private float totalSize;
		private float readSoFar;
		private int lastPercentageIndicated = -1;
		private long lastNotified;
		private String text;
		
		public ProgressIndicatingInputStream(HttpURLConnection connection) throws IOException {
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
			
			// Handle the response code
			if (connection.getResponseCode() == 200) {
				uaaService.clearIfPossible();
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

