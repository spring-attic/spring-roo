package org.springframework.roo.url.stream;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.osgi.AbstractFlashingObject;
import org.springframework.roo.url.stream.UrlInputStreamService;

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
    public ProgressIndicatingInputStream(final HttpURLConnection connection) throws IOException {
      Validate.notNull(connection, "URL Connection required");
      totalSize = connection.getContentLength();
      delegate = connection.getInputStream();
      text = connection.getURL().getPath();
      if ("".equals(text)) {
        // Fall back to the host name
        text = connection.getURL().getHost();
      } else {
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
        final int percentageDownloaded = Math.round(readSoFar / totalSize * 100);
        if (System.currentTimeMillis() > lastNotified + 1000) {
          if (lastPercentageIndicated != percentageDownloaded) {
            flash(Level.FINE, "Downloaded " + percentageDownloaded + "% of " + text, MY_SLOT);
            lastPercentageIndicated = percentageDownloaded;
            lastNotified = System.currentTimeMillis();
          }
        }
      } else {
        // Total size is not known, rely on time-based updates instead
        if (System.currentTimeMillis() > lastNotified + 1000) {
          flash(Level.FINE, "Downloaded " + Math.round(readSoFar / 1024) + " kB of " + text,
              MY_SLOT);
          lastNotified = System.currentTimeMillis();
        }
      }

      final int result = delegate.read();
      if (result == -1) {
        if (totalSize > 0) {
          flash(Level.FINE, "Downloaded 100% of " + text, MY_SLOT);
        } else {
          flash(Level.FINE, "Downloaded " + Math.round(readSoFar / 1024) + " kB of " + text,
              MY_SLOT);
        }
        flash(Level.FINE, "", MY_SLOT);
      }

      return result;
    }
  }

  public String getUrlCannotBeOpenedMessage(final URL httpUrl) {
    return null;
  }

  public InputStream openConnection(final URL httpUrl) throws IOException {
    Validate.notNull(httpUrl, "HTTP URL is required");
    Validate.isTrue(httpUrl.getProtocol().equals("http"), "Only HTTP is supported (not %s)",
        httpUrl);

    final HttpURLConnection connection = prepareHttpUrlConnection(httpUrl);
    return new ProgressIndicatingInputStream(connection);
  }

  public HttpURLConnection prepareHttpUrlConnection(URL url) throws IOException {
    // Prepare proxy and proxy authentication
    Proxy proxy = setupProxy(url);
    Authenticator proxyAuthentication = setupProxyAuthentication(url, proxy);
    if (proxyAuthentication != null) {
      Authenticator.setDefault(proxyAuthentication);
    }

    HttpURLConnection connection =
        (HttpURLConnection) (proxy != null ? url.openConnection(proxy) : url.openConnection());
    connection.setConnectTimeout(5000);
    connection.setReadTimeout(5000);
    connection.setUseCaches(false);
    return connection;
  }

  public Proxy setupProxy(URL url) {
    List<Proxy> proxies = null;
    try {
      proxies = ProxySelector.getDefault().select(url.toURI());
    } catch (URISyntaxException e) {
      // this can't happen
    }

    if (proxies != null) {
      for (Proxy proxy : proxies) {
        if (proxy.type().equals(Proxy.Type.HTTP)) {
          return proxy;
        }
      }
    }
    return null;
  }

  public Authenticator setupProxyAuthentication(URL url, Proxy proxy) {
    return null;
  }
}
