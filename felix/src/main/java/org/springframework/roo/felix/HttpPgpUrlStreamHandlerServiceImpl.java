package org.springframework.roo.felix;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;
import org.springframework.roo.felix.pgp.PgpService;
import org.springframework.roo.felix.pgp.SignatureDecision;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.url.stream.UrlInputStreamService;

/**
 * Processes <code>httppgp://</code> URLs. Does not handle HTTPS URLs.
 * <p>
 * This implementation offers two main features:
 * <ul>
 * <li>It delegates the downloading process to {@link UrlInputStreamService} so
 * that an alternate implementation can be added that may offer more advanced
 * capabilities or configuration (eg as available from a hosting IDE)</li>
 * <li>It downloads an .asc file (computed by the original URL + ".asc") and
 * verifies the signature and that the user trusts the signing key (the .asc
 * must be a detached armored signature, as produced via
 * "gpg --armor --detach-sign file_to_sign.ext")</li> </li>
 * </ul>
 * <p>
 * As such this module simplifies security management and proxy server
 * compatibility for Spring Roo.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class HttpPgpUrlStreamHandlerServiceImpl extends
        AbstractURLStreamHandlerService implements
        HttpPgpUrlStreamHandlerService {

    private static final Logger LOGGER = HandlerUtils
            .getLogger(HttpPgpUrlStreamHandlerServiceImpl.class);

    @Reference private PgpService pgpService;
    @Reference private UrlInputStreamService urlInputStreamService;

    protected void activate(final ComponentContext context) {
        final Hashtable<String, String> dict = new Hashtable<String, String>();
        dict.put(URLConstants.URL_HANDLER_PROTOCOL, "httppgp");
        context.getBundleContext().registerService(
                URLStreamHandlerService.class.getName(), this, dict);
    }

    @Override
    public URLConnection openConnection(final URL u) throws IOException {
        // Convert httppgp:// URL into a standard http:// URL
        final URL resourceUrl = new URL(u.toExternalForm().replace("httppgp",
                "http"));
        // Add .asc to the end of the standard resource URL
        final URL ascUrl = new URL(resourceUrl.toExternalForm() + ".asc");

        // Start with the ASC file, as if this is for an untrusted key, there's
        // no point download the larger resource
        final File ascUrlFile = File.createTempFile("roo_asc", null);
        ascUrlFile.deleteOnExit();

        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(ascUrlFile);
            inputStream = urlInputStreamService.openConnection(ascUrl);
            IOUtils.copy(inputStream, outputStream);
        }
        catch (final IOException ioe) {
            // This is not considered fatal; it is likely the ASC isn't
            // available, so we will continue
            ascUrlFile.delete();
        }
        finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
        }

        // Abort if a signature wasn't downloaded (this is a httppgp:// URL
        // after all, so it should be available)
        Validate.isTrue(
                ascUrlFile.exists(),
                "Signature verification file is not available at '"
                        + ascUrl.toExternalForm() + "'; continuing");

        // Decide if this signature file is well-formed and of a key ID that is
        // trusted by the user
        InputStream resource = null;
        InputStream signature = null;
        try {
            signature = new FileInputStream(ascUrlFile);
            final SignatureDecision decision = pgpService
                    .isSignatureAcceptable(signature);
            if (!decision.isSignatureAcceptable()) {
                LOGGER.log(Level.SEVERE,
                        "Download URL '" + resourceUrl.toExternalForm()
                                + "' failed");
                LOGGER.log(
                        Level.SEVERE,
                        "This resource was signed with PGP key ID '"
                                + decision.getSignatureAsHex()
                                + "', which is not currently trusted");
                LOGGER.log(
                        Level.SEVERE,
                        "Use 'pgp key view' to view this key, 'pgp trust' to trust it, or 'pgp automatic trust' to trust any keys");
                throw new IOException("Download URL '"
                        + resourceUrl.toExternalForm()
                        + "' has untrusted PGP signature "
                        + JdkDelegatingLogListener.DO_NOT_LOG);
            }

            // So far so good. Next we need the actual resource to ensure the
            // ASC file really did sign it
            final File resourceFile = File.createTempFile("roo_resource", null);
            resourceFile.deleteOnExit();

            inputStream = urlInputStreamService.openConnection(resourceUrl);
            outputStream = new FileOutputStream(resourceFile);
            IOUtils.copy(inputStream, outputStream);

            resource = new FileInputStream(resourceFile);
            signature = new FileInputStream(ascUrlFile);
            Validate.isTrue(
                    pgpService.isResourceSignedBySignature(resource, signature),
                    "PGP signature illegal for URL '"
                            + resourceUrl.toExternalForm() + "'");

            // Excellent it worked! We don't need the ASC file anymore, so get
            // rid of it
            ascUrlFile.delete();

            return resourceFile.toURI().toURL().openConnection();
        }
        finally {
            IOUtils.closeQuietly(resource);
            IOUtils.closeQuietly(signature);
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
        }
    }
}
