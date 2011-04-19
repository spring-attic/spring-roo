package org.springframework.roo.felix;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.url.stream.UrlInputStreamService;

/**
 * Processes <code>httppgp://</code> URLs. Does not handle HTTPS URLs.
 * 
 * <p>
 * This implementation offers two main features:
 * 
 * <ul>
 * <li>It delegates the downloading process to {@link UrlInputStreamService} so that an
 * alternate implementation can be added that may offer more advanced capabilities or
 * configuration (eg as available from a hosting IDE)</li>
 * <li>It downloads an .asc file (computed by the original URL + ".asc") and verifies the
 * signature and that the user trusts the signing key (the .asc must be a detached armored
 * signature, as produced via "gpg --armor --detach-sign file_to_sign.ext")</li>
 * </li>
 * </ul>
 * 
 * <p>
 * As such this module simplifies security management and proxy server compatibility for
 * Spring Roo.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component(immediate = true)
@Service
public class HttpPgpUrlStreamHandlerServiceImpl extends AbstractURLStreamHandlerService implements HttpPgpUrlStreamHandlerService {
	@Reference private UrlInputStreamService urlInputStreamService;
	@Reference private PgpService pgpService;
	private static final Logger logger = HandlerUtils.getLogger(HttpPgpUrlStreamHandlerServiceImpl.class);

	protected void activate(ComponentContext context) {
		Hashtable<String,String> dict = new Hashtable<String,String>();
		dict.put(URLConstants.URL_HANDLER_PROTOCOL, "httppgp");
		context.getBundleContext().registerService(URLStreamHandlerService.class.getName(), this, dict);
	}
	
	@Override
	public URLConnection openConnection(URL u) throws IOException {
		// Convert httppgp:// URL into a standard http:// URL
		URL resourceUrl = new URL(u.toExternalForm().replace("httppgp", "http"));
		// Add .asc to the end of the standard resource URL
		URL ascUrl = new URL(resourceUrl.toExternalForm() + ".asc");

		// Start with the ASC file, as if this is for an untrusted key, there's no point download the larger resource
		File ascUrlFile = File.createTempFile("roo_asc", null);
		ascUrlFile.deleteOnExit();

		try {
			FileCopyUtils.copy(urlInputStreamService.openConnection(ascUrl), new FileOutputStream(ascUrlFile));
		} catch (IOException ioe) {
			// This is not considered fatal; it is likely the ASC isn't available, so we will continue
			ascUrlFile.delete();
		}

		// Abort if a signature wasn't downloaded (this is a httppgp:// URL after all, so it should be available)
		Assert.isTrue(ascUrlFile.exists(), "Signature verification file is not available at '" + ascUrl.toExternalForm() + "'; continuing");

		// Decide if this signature file is well-formed and of a key ID that is trusted by the user
		SignatureDecision decision = pgpService.isSignatureAcceptable(new FileInputStream(ascUrlFile));
		if (!decision.isSignatureAcceptable()) {
			logger.log(Level.SEVERE, "Download URL '" + resourceUrl.toExternalForm() + "' failed");
			logger.log(Level.SEVERE, "This resource was signed with PGP key ID '" + decision.getSignatureAsHex() + "', which is not currently trusted");
			logger.log(Level.SEVERE, "Use 'pgp key view' to view this key, 'pgp trust' to trust it, or 'pgp automatic trust' to trust any keys");
			throw new IOException("Download URL '" + resourceUrl.toExternalForm() + "' has untrusted PGP signature " + JdkDelegatingLogListener.DO_NOT_LOG);
		}
		
		// logger.log(Level.FINE, "Download URL '" + resourceUrl.toExternalForm() + "' signature uses acceptable PGP key ID '" + decision.getSignatureAsHex() + "'");

		// So far so good. Next we need the actual resource to ensure the ASC file really did sign it
		File resourceFile = File.createTempFile("roo_resource", null);
		resourceFile.deleteOnExit();
		FileCopyUtils.copy(urlInputStreamService.openConnection(resourceUrl), new FileOutputStream(resourceFile));

		Assert.isTrue(pgpService.isResourceSignedBySignature(new FileInputStream(resourceFile), new FileInputStream(ascUrlFile)), "PGP signature illegal for URL '" + resourceUrl.toExternalForm() + "'");

		// Excellent it worked! We don't need the ASC file anymore, so get rid of it
		ascUrlFile.delete();

		// logger.log(Level.FINE, "Download URL '" + resourceUrl.toExternalForm() + "' was correctly signed by key");

		return resourceFile.toURI().toURL().openConnection();
	}
}
