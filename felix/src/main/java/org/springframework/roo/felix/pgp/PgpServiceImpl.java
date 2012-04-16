package org.springframework.roo.felix.pgp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.support.osgi.OSGiUtils;
import org.springframework.roo.url.stream.UrlInputStreamService;

/**
 * Default implementation of {@link PgpService}.
 * <p>
 * Stores the user's PGP information in the
 * <code>~/.spring_roo_pgp.bpg<code> file. Every key in this
 * file is considered trusted by the user. Expiration times of keys are ignored. Default keys that
 * ship with Roo are added to this file automatically when the file is not present on disk.
 * 
 * <p>
 * This implementation will only verify "detached armored signatures". Produce such a file via
 * "gpg --armor --detach-sign file_to_sign.ext".
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class PgpServiceImpl implements PgpService {

    private static final int BUFFER_SIZE = 1024;
    private static String defaultKeyServerUrl = "http://keyserver.ubuntu.com/pks/lookup?op=get&search=";
    // private static String defaultKeyServerUrl =
    // "http://pgp.mit.edu/pks/lookup?op=get&search=";

    private static final File ROO_PGP_FILE = FileUtils.getFile(
            FileUtils.getUserDirectory(), ".spring_roo_pgp.bpg");

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private boolean automaticTrust;
    private BundleContext context;
    private final SortedSet<PgpKeyId> discoveredKeyIds = new TreeSet<PgpKeyId>();
    @Reference private UrlInputStreamService urlInputStreamService;

    public SortedSet<PgpKeyId> getDiscoveredKeyIds() {
        return Collections.unmodifiableSortedSet(discoveredKeyIds);
    }

    public URL getKeyServerUrlToRetrieveKeyInformation(final PgpKeyId keyId) {
        Validate.notNull(keyId, "Key ID required");
        final URL keyUrl = getKeyServerUrlToRetrieveKeyId(keyId);
        try {
            final URL keyIndexUrl = new URL(keyUrl.getProtocol() + "://"
                    + keyUrl.getAuthority() + keyUrl.getPath()
                    + "?fingerprint=on&op=index&search=");
            return new URL(keyIndexUrl.toString() + keyId);
        }
        catch (final MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getKeyStorePhysicalLocation() {
        try {
            return ROO_PGP_FILE.getCanonicalPath();
        }
        catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public PGPPublicKeyRing getPublicKey(final InputStream in) {
        Object obj;
        try {
            final PGPObjectFactory pgpFact = new PGPObjectFactory(
                    PGPUtil.getDecoderStream(in));
            obj = pgpFact.nextObject();
        }
        catch (final Exception e) {
            throw new IllegalStateException(e);
        }

        if (obj instanceof PGPPublicKeyRing) {
            final PGPPublicKeyRing keyRing = (PGPPublicKeyRing) obj;
            rememberKey(keyRing);
            return keyRing;
        }

        throw new IllegalStateException("Pblic key not available");
    }

    public PGPPublicKeyRing getPublicKey(final PgpKeyId keyId) {
        Validate.notNull(keyId, "Key ID required");
        InputStream in = null;
        try {
            final URL lookup = getKeyServerUrlToRetrieveKeyId(keyId);
            in = urlInputStreamService.openConnection(lookup);
            return getPublicKey(in);
        }
        catch (final Exception e) {
            throw new IllegalStateException("Public key ID '" + keyId
                    + "' not available from key server", e);
        }
        finally {
            IOUtils.closeQuietly(in);
        }
    }

    @SuppressWarnings("unchecked")
    public List<PGPPublicKeyRing> getTrustedKeys() {
        if (!ROO_PGP_FILE.exists()) {
            return new ArrayList<PGPPublicKeyRing>();
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(ROO_PGP_FILE);
            final PGPPublicKeyRingCollection pubRings = new PGPPublicKeyRingCollection(
                    PGPUtil.getDecoderStream(fis));
            final Iterator<PGPPublicKeyRing> rIt = pubRings.getKeyRings();
            final List<PGPPublicKeyRing> result = new ArrayList<PGPPublicKeyRing>();
            while (rIt.hasNext()) {
                final PGPPublicKeyRing pgpPub = rIt.next();
                rememberKey(pgpPub);
                result.add(pgpPub);
            }
            return result;
        }
        catch (final Exception e) {
            throw new IllegalArgumentException(
                    "Unable to get trusted keys",
                    ObjectUtils.defaultIfNull(ExceptionUtils.getRootCause(e), e));
        }
        finally {
            IOUtils.closeQuietly(fis);
        }
    }

    public boolean isAutomaticTrust() {
        return automaticTrust;
    }

    public boolean isResourceSignedBySignature(final InputStream resource,
            InputStream signature) {
        PGPPublicKey publicKey = null;
        PGPSignature pgpSignature = null;

        try {
            if (!(signature instanceof ArmoredInputStream)) {
                signature = new ArmoredInputStream(signature);
            }

            pgpSignature = isSignatureAcceptable(signature).getPgpSignature();
            final PGPPublicKeyRing keyRing = getPublicKey(new PgpKeyId(
                    pgpSignature));
            rememberKey(keyRing);
            publicKey = keyRing.getPublicKey();

            Validate.notNull(publicKey,
                    "Could not obtain public key for signer key ID '%s'",
                    pgpSignature);

            pgpSignature.initVerify(publicKey, "BC");

            // Now verify the signed content
            final byte[] buff = new byte[BUFFER_SIZE];
            int chunk;
            do {
                chunk = resource.read(buff);
                if (chunk > 0) {
                    pgpSignature.update(buff, 0, chunk);
                }
            } while (chunk >= 0);

            return pgpSignature.verify();
        }
        catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public SignatureDecision isSignatureAcceptable(final InputStream signature)
            throws IOException {
        Validate.notNull(signature, "Signature input stream required");
        PGPObjectFactory factory = new PGPObjectFactory(
                PGPUtil.getDecoderStream(signature));
        final Object obj = factory.nextObject();
        Validate.notNull(obj, "Unable to retrieve signature from stream");

        PGPSignatureList p3;
        if (obj instanceof PGPCompressedData) {
            try {
                factory = new PGPObjectFactory(
                        ((PGPCompressedData) obj).getDataStream());
            }
            catch (final Exception e) {
                throw new IllegalStateException(e);
            }
            p3 = (PGPSignatureList) factory.nextObject();
        }
        else {
            p3 = (PGPSignatureList) obj;
        }

        final PGPSignature pgpSignature = p3.get(0);
        Validate.notNull(pgpSignature,
                "Unable to retrieve signature from stream");

        final PgpKeyId keyIdInHex = new PgpKeyId(pgpSignature);

        // Special case where we directly store the key ID, as we know it's
        // valid
        discoveredKeyIds.add(keyIdInHex);

        boolean signatureAcceptable = false;

        // Loop to see if the user trusts this key
        for (final PGPPublicKeyRing keyRing : getTrustedKeys()) {
            final PgpKeyId candidate = new PgpKeyId(keyRing.getPublicKey());
            if (candidate.equals(keyIdInHex)) {
                signatureAcceptable = true;
                break;
            }
        }

        if (!signatureAcceptable && automaticTrust) {
            // We don't approve of this signature, but the user has told us it's
            // OK
            trust(keyIdInHex);
            signatureAcceptable = true;
        }

        return new SignatureDecision(pgpSignature, keyIdInHex,
                signatureAcceptable);
    }

    public SortedMap<PgpKeyId, String> refresh() {
        final SortedMap<PgpKeyId, String> result = new TreeMap<PgpKeyId, String>();
        // Get the keys we currently trust
        final List<PGPPublicKeyRing> trusted = getTrustedKeys();

        // Build a new list of our refreshed keys
        final List<PGPPublicKeyRing> stillTrusted = new ArrayList<PGPPublicKeyRing>();

        // Locate the element to remove (we need to record it so the method can
        // return it)
        for (final PGPPublicKeyRing candidate : trusted) {
            final PGPPublicKey firstKey = candidate.getPublicKey();
            final PgpKeyId candidateKeyId = new PgpKeyId(firstKey);
            // Try to refresh
            PGPPublicKeyRing newKeyRing;
            try {
                newKeyRing = getPublicKey(candidateKeyId);
            }
            catch (final Exception e) {
                // Can't retrieve, so keep the old one for now
                stillTrusted.add(candidate);
                result.put(candidateKeyId,
                        "WARNING: Retained original (download issue)");
                continue;
            }
            // Do not store if the first key is revoked
            if (newKeyRing.getPublicKey().isRevoked()) {
                result.put(candidateKeyId,
                        "WARNING: Key revoked, so removed from trust list");
            }
            else {
                stillTrusted.add(newKeyRing);
                result.put(candidateKeyId, "SUCCESS");
            }
        }

        // Write back to disk
        OutputStream fos = null;
        try {
            final PGPPublicKeyRingCollection newCollection = new PGPPublicKeyRingCollection(
                    stillTrusted);
            fos = new FileOutputStream(ROO_PGP_FILE);
            newCollection.encode(fos);
        }
        catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        finally {
            IOUtils.closeQuietly(fos);
        }

        return result;
    }

    public void setAutomaticTrust(final boolean automaticTrust) {
        this.automaticTrust = automaticTrust;
    }

    public PGPPublicKeyRing trust(final PgpKeyId keyId) {
        Validate.notNull(keyId, "Key ID required");
        final PGPPublicKeyRing keyRing = getPublicKey(keyId);
        return trust(keyRing);
    }

    @SuppressWarnings("unchecked")
    public PGPPublicKeyRing untrust(final PgpKeyId keyId) {
        Validate.notNull(keyId, "Key ID required");
        // Get the keys we currently trust
        final List<PGPPublicKeyRing> trusted = getTrustedKeys();

        // Build a new list of keys we'll continue to trust after this method
        // ends
        final List<PGPPublicKeyRing> stillTrusted = new ArrayList<PGPPublicKeyRing>();

        // Locate the element to remove (we need to record it so the method can
        // return it)
        PGPPublicKeyRing removed = null;
        for (final PGPPublicKeyRing candidate : trusted) {
            boolean stillTrust = true;
            final Iterator<PGPPublicKey> it = candidate.getPublicKeys();
            while (it.hasNext()) {
                final PGPPublicKey pgpKey = it.next();
                final PgpKeyId candidateKeyId = new PgpKeyId(pgpKey);
                if (removed == null && candidateKeyId.equals(keyId)) {
                    stillTrust = false;
                    removed = candidate;
                    break;
                }
            }
            if (stillTrust) {
                stillTrusted.add(candidate);
            }
        }

        Validate.notNull(removed,
                "The public key ID '%s' is not currently trusted", keyId);

        // Write back to disk
        OutputStream fos = null;
        try {
            final PGPPublicKeyRingCollection newCollection = new PGPPublicKeyRingCollection(
                    stillTrusted);
            fos = new FileOutputStream(ROO_PGP_FILE);
            newCollection.encode(fos);
        }
        catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        finally {
            IOUtils.closeQuietly(fos);
        }
        return removed;
    }

    protected void activate(final ComponentContext context) {
        this.context = context.getBundleContext();
        final String keyserver = context.getBundleContext().getProperty(
                "pgp.keyserver.url");
        if (StringUtils.isNotBlank(keyserver)) {
            defaultKeyServerUrl = keyserver;
        }
        trustDefaultKeysIfRequired();
        // Seed the discovered keys database
        getTrustedKeys();
    }

    protected void trustDefaultKeysIfRequired() {
        // Setup default keys we trust automatically
        trustDefaultKeys();
    }

    /**
     * Obtains a URL that should allow the download of the specified public key.
     * <p>
     * The key server may not contain the specified public key if it has never
     * been uploaded.
     * 
     * @param keyId hex-encoded key ID to download (required)
     * @return the URL (never null)
     */
    private URL getKeyServerUrlToRetrieveKeyId(final PgpKeyId keyId) {
        try {
            return new URL(defaultKeyServerUrl + keyId);
        }
        catch (final MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Simply stores the key ID in {@link #discoveredKeyIds} for future
     * reference of all Key IDs we've come across. This method uses a
     * {@link PGPPublicKeyRing} to ensure the input is actually a valid key,
     * plus locating any key IDs that have signed the key.
     * <p>
     * Please note {@link #discoveredKeyIds} is not used for any key functions
     * of this class. It is simply for user interface convenience.
     * 
     * @param keyRing the key ID to store (required)
     */
    @SuppressWarnings("unchecked")
    private void rememberKey(final PGPPublicKeyRing keyRing) {
        final PGPPublicKey key = keyRing.getPublicKey();
        if (key != null) {
            final PgpKeyId keyId = new PgpKeyId(key);
            discoveredKeyIds.add(keyId);
            final Iterator<String> userIdIterator = key.getUserIDs();
            while (userIdIterator.hasNext()) {
                final String userId = userIdIterator.next();
                final Iterator<PGPSignature> signatureIterator = key
                        .getSignaturesForID(userId);
                while (signatureIterator.hasNext()) {
                    final PGPSignature signature = signatureIterator.next();
                    final PgpKeyId signatureKeyId = new PgpKeyId(signature);
                    discoveredKeyIds.add(signatureKeyId);
                }
            }
        }
    }

    private PGPPublicKeyRing trust(final PGPPublicKeyRing keyRing) {
        rememberKey(keyRing);

        // Get the keys we currently trust
        final List<PGPPublicKeyRing> trusted = getTrustedKeys();

        // Do not store if the first key is revoked
        Validate.validState(
                !keyRing.getPublicKey().isRevoked(),
                "The public key ID '%s' has been revoked and cannot be trusted",
                new PgpKeyId(keyRing.getPublicKey()));

        // trust it and write back to disk
        trusted.add(keyRing);
        OutputStream fos = null;
        try {
            final PGPPublicKeyRingCollection newCollection = new PGPPublicKeyRingCollection(
                    trusted);
            fos = new FileOutputStream(ROO_PGP_FILE);
            newCollection.encode(fos);
        }
        catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        finally {
            IOUtils.closeQuietly(fos);
        }
        return keyRing;
    }

    private void trustDefaultKeys() {
        // Get the URIs of all PGP keystore files within installed OSGi bundles
        final List<URL> urls = new ArrayList<URL>(
                OSGiUtils.findEntriesByPattern(context,
                        "/org/springframework/roo/felix/pgp/*.asc"));
        Collections.sort(urls, new Comparator<URL>() {
            public int compare(final URL url1, final URL url2) {
                return url1.toExternalForm().compareTo(url2.toExternalForm());
            }
        });

        // Trust each one
        for (final URL url : urls) {
            InputStream inputStream = null;
            try {
                inputStream = url.openStream();
                trust(getPublicKey(inputStream));
            }
            catch (final IOException ignored) {
            }
            finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
    }
}
