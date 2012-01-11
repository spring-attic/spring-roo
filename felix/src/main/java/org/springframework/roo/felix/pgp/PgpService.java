package org.springframework.roo.felix.pgp;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;

import org.bouncycastle.openpgp.PGPPublicKeyRing;

/**
 * Provides a central location for all PGP key store and file verification
 * activities.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public interface PgpService {

    /**
     * Provides a way of discovered all Key IDs that have been encountered by
     * the service since it started. This is mostly useful for the user
     * interface building tab completion commands etc.
     * 
     * @return an unmodifiable list of the Key IDs (never null, but may be
     *         empty)
     */
    SortedSet<PgpKeyId> getDiscoveredKeyIds();

    /**
     * Obtains a URL that should allow a human-friendly display of key
     * properties.
     * <p>
     * The key server may not contain the specified public key if it has never
     * been uploaded.
     * 
     * @param keyId hex-encoded key ID to display (required)
     * @return the URL (never null)
     */
    URL getKeyServerUrlToRetrieveKeyInformation(PgpKeyId keyId);

    /**
     * @return the canonical file path to the key store (never null, although
     *         the file may not exist)
     */
    String getKeyStorePhysicalLocation();

    /**
     * Attempts to download the specified key ID.
     * <p>
     * This method requires internet access to complete.
     * 
     * @param keyId hex-encoded key ID to download (required)
     * @return the key (never null, but an exception is thrown if the key is
     *         unavailable)
     */
    PGPPublicKeyRing getPublicKey(PgpKeyId keyId);

    /**
     * Obtains all of the keys presently trusted by the user.
     * <p>
     * Does not require internet access.
     * 
     * @return the list of keys (may have zero elements, but will never be null)
     */
    List<PGPPublicKeyRing> getTrustedKeys();

    /**
     * Indicates if the service automatically trusts new keys.
     * 
     * @return true if auto-trust is active
     */
    boolean isAutomaticTrust();

    /**
     * Indicates if this resource has been signed by the presented ASC file.
     * This does not make any decision whether the key used in the ASC is valid
     * or not (use {@link #isSignatureAcceptable(InputStream)} for this
     * instead).
     * <p>
     * This method does not require internet access.
     * 
     * @param resource the resource that was presented (required)
     * @param signature the ASC signature that was presented (required)
     * @return true if this signature file verified this resource, false
     *         otherwise
     */
    boolean isResourceSignedBySignature(InputStream resource,
            InputStream signature) throws IOException;

    /**
     * Indicates if the signature is acceptable or not based on the presentation
     * of an ASC file. This will determine if the ASC is valid and the key used
     * to produce it is trusted. It does not verify a resource was actually
     * signed using the ASC (use
     * {@link #isResourceSignedBySignature(InputStream, InputStream)} for this
     * instead). In practical terms this method will throw an exception if
     * something is wrong with the signature (eg it is corrupted). If the method
     * returns an object, it means the ASC signature was valid (although the key
     * which signed it may not be trusted).
     * <p>
     * This method requires internet access if the automatic trust mode is on
     * and it is necessary to add a new key during processing. In no other case
     * is internet access required.
     * 
     * @param signature the ASC signature that was presented (required)
     * @return the decision (never null)
     */
    SignatureDecision isSignatureAcceptable(InputStream signature)
            throws IOException;

    /**
     * Instructs the implementation to refresh all keys it current trusts. This
     * is to identify keys that may have been revoked, which will automatically
     * be untrusted.
     * <p>
     * This method requires internet access to complete. If a download fails,
     * the key trust will be retained (but of course not refreshed). The outcome
     * of each refresh request is included in the returned object.
     * 
     * @return a map where the keys are the hexadecimal key IDs and the values
     *         are the status of the update (never returns null)
     */
    SortedMap<PgpKeyId, String> refresh();

    /**
     * Directs the service to automatically trust new keys it encounters.
     * 
     * @param automaticTrust the new value
     */
    void setAutomaticTrust(boolean automaticTrust);

    /**
     * Trusts a new key ID (refreshing the existing key ID if it is already
     * trusted).
     * <p>
     * This method requires internet access to complete.
     * 
     * @param keyId hex-encoded key ID to trust (required)
     * @return the key information now trusted (as refreshed from the server)
     */
    PGPPublicKeyRing trust(PgpKeyId keyId);

    /**
     * Untrusts an existing key ID (method will throw an exception if the key
     * isn't currently trusted).
     * <p>
     * This method does not require internet access.
     * 
     * @param keyId hex-encoded key ID to untrust (required)
     * @return the key information that is no longer trusted (as last cached;
     *         never returns null)
     */
    PGPPublicKeyRing untrust(PgpKeyId keyId);

}
