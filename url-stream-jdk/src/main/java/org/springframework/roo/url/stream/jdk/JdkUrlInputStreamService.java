package org.springframework.roo.url.stream.jdk;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.osgi.AbstractFlashingObject;
import org.springframework.roo.url.stream.UrlInputStreamService;
import org.springframework.roo.url.stream.UrlInputStreamUtils;
import org.springframework.uaa.client.ProxyService;
import org.springframework.uaa.client.UaaService;

/**
 * Simple implementation of {@link UrlInputStreamService} that uses the JDK.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class JdkUrlInputStreamService extends AbstractFlashingObject implements
        UrlInputStreamService {

    private class ProgressIndicatingInputStream extends InputStream {
        private final InputStream delegate;
        private long lastNotified;
        private int lastPercentageIndicated = -1;
        private float readSoFar;
        private String text;
        private final float totalSize;

        /**
         * Constructor
         * 
         * @param connection
         * @throws IOException
         */
        public ProgressIndicatingInputStream(final HttpURLConnection connection)
                throws IOException {
            Validate.notNull(connection, "URL Connection required");
            totalSize = connection.getContentLength();
            delegate = connection.getInputStream();
            text = connection.getURL().getPath();
            if ("".equals(text)) {
                // Fall back to the host name
                text = connection.getURL().getHost();
            }
            else {
                // We only want the filename
                final int lastSlash = text.lastIndexOf("/");
                if (lastSlash > -1) {
                    text = text.substring(lastSlash + 1);
                }
            }
        }

        @Override
        public void close() throws IOException {
            flash(Level.FINE, "", MY_SLOT);
            delegate.close();
        }

        @Override
        public int read() throws IOException {
            readSoFar++;
            if (totalSize > 0) {
                // Total size is known
                final int percentageDownloaded = Math.round(readSoFar
                        / totalSize * 100);
                if (System.currentTimeMillis() > lastNotified + 1000) {
                    if (lastPercentageIndicated != percentageDownloaded) {
                        flash(Level.FINE, "Downloaded " + percentageDownloaded
                                + "% of " + text, MY_SLOT);
                        lastPercentageIndicated = percentageDownloaded;
                        lastNotified = System.currentTimeMillis();
                    }
                }
            }
            else {
                // Total size is not known, rely on time-based updates instead
                if (System.currentTimeMillis() > lastNotified + 1000) {
                    flash(Level.FINE,
                            "Downloaded " + Math.round(readSoFar / 1024)
                                    + " kB of " + text, MY_SLOT);
                    lastNotified = System.currentTimeMillis();
                }
            }

            final int result = delegate.read();
            if (result == -1) {
                if (totalSize > 0) {
                    flash(Level.FINE, "Downloaded 100% of " + text, MY_SLOT);
                }
                else {
                    flash(Level.FINE,
                            "Downloaded " + Math.round(readSoFar / 1024)
                                    + " kB of " + text, MY_SLOT);
                }
                flash(Level.FINE, "", MY_SLOT);
            }

            return result;
        }
    }

    @Reference private ProxyService proxyService;
    @Reference private UaaService uaaService;

    public String getUrlCannotBeOpenedMessage(final URL httpUrl) {
        if (uaaService.isCommunicationRestricted(httpUrl)) {
            if (!uaaService.isUaaTermsOfUseAccepted()) {
                return UrlInputStreamUtils.SETUP_UAA_REQUIRED;
            }
        }
        // No reason it shouldn't work
        return null;
    }

    public InputStream openConnection(final URL httpUrl) throws IOException {
        Validate.notNull(httpUrl, "HTTP URL is required");
        Validate.isTrue(httpUrl.getProtocol().equals("http"),
                "Only HTTP is supported (not " + httpUrl + ")");

        // Fail if we're banned from accessing this domain
        Validate.isTrue(getUrlCannotBeOpenedMessage(httpUrl) == null,
                UrlInputStreamUtils.SETUP_UAA_REQUIRED);
        final HttpURLConnection connection = proxyService
                .prepareHttpUrlConnection(httpUrl);
        return new ProgressIndicatingInputStream(connection);
    }
}