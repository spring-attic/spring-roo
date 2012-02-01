package org.springframework.roo.addon.cloud.foundry;

import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.preferences.Preferences;
import org.springframework.roo.classpath.preferences.PreferencesService;

/**
 * The user's cloud-related preferences.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class CloudPreferences {

    private static final String CHARSET_NAME = "UTF-8";
    private static final String CLOUD_FOUNDRY_KEY = "Cloud Foundry Prefs";
    private static final String DELIMITER = "|";
    private static final String DELIMITER_REGEX = "\\|"; // i.e. a pipe
    private static final String ROO_KEY = "Roo == Java + Productivity";

    private final Preferences preferences;

    /**
     * Constructor
     * 
     * @param preferencesService the service from which to load the preferences
     *            (required)
     */
    public CloudPreferences(final PreferencesService preferencesService) {
        preferences = preferencesService
                .getPreferencesFor(CloudFoundrySessionImpl.class);
    }

    /**
     * Clears any stored {@link CloudCredentials}.
     */
    public void clearStoredLoginDetails() {
        preferences.putByteArray(CLOUD_FOUNDRY_KEY, new byte[0]);
        preferences.flush();
    }

    /**
     * Encrypts or decrypts the given input, according to the given
     * <code>opmode</code>
     * 
     * @param input the bytes to operate upon (required)
     * @param opmode the operation to perform, see the {@link Cipher} class for
     *            suitable constants
     * @return a non-<code>null</code> array
     */
    private byte[] crypt(final byte[] input, final int opmode) {
        final Cipher cipher = getCipher(opmode);
        try {
            return cipher.doFinal(input);
        }
        catch (final GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Decodes the given encoded (but decrypted) preferences string into a set
     * of {@link CloudCredentials}.
     * 
     * @param encodedEntries the encoded string to convert (can be blank)
     * @return a non-<code>null</code> set
     */
    private Set<CloudCredentials> decodeLoginPrefEntries(
            final String encodedEntries) {
        if (StringUtils.isBlank(encodedEntries)) {
            return Collections.emptySet();
        }
        final Set<CloudCredentials> set = new HashSet<CloudCredentials>();
        for (final String encodedEntry : encodedEntries.split(DELIMITER_REGEX)) {
            set.add(CloudCredentials.decode(encodedEntry));
        }
        return set;
    }

    /**
     * Flushes these preferences to the persistent store
     * 
     * @see Preferences#flush()
     */
    public void flush() {
        preferences.flush();
    }

    private Cipher getCipher(final int opmode) {
        try {
            final DESKeySpec keySpec = new DESKeySpec(
                    ROO_KEY.getBytes(CHARSET_NAME));
            final SecretKeyFactory keyFactory = SecretKeyFactory
                    .getInstance("DES");
            final SecretKey skey = keyFactory.generateSecret(keySpec);
            final Cipher cipher = Cipher.getInstance("DES");
            cipher.init(opmode, skey);
            return cipher;
        }
        catch (final InvalidKeySpecException e) {
            throw new IllegalStateException(e);
        }
        catch (final NoSuchPaddingException e) {
            throw new IllegalStateException(e);
        }
        catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        catch (final InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
        catch (final UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns any stored {@link CloudCredentials}
     * 
     * @return a non-<code>null</code> set
     */
    private Set<CloudCredentials> getStoredCredentials() {
        final byte[] encodedPrefs = preferences.getByteArray(CLOUD_FOUNDRY_KEY);
        if (encodedPrefs.length == 0) {
            return Collections.emptySet();
        }
        final byte[] decryptedPrefs = crypt(encodedPrefs, DECRYPT_MODE);

        try {
            return decodeLoginPrefEntries(new String(decryptedPrefs,
                    CHARSET_NAME));
        }
        catch (final UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns any stored credentials having the given URL.
     * 
     * @param url the URL to match upon (can be <code>null</code>)
     * @return a non-<code>null</code> list, empty if the given URL is
     *         <code>null</code>
     */
    public List<CloudCredentials> getStoredCredentialsForUrl(final String url) {
        final List<CloudCredentials> matches = new ArrayList<CloudCredentials>();
        if (url != null) {
            for (final CloudCredentials cloudCredentials : getStoredCredentials()) {
                if (url.equals(cloudCredentials.getUrl())) {
                    matches.add(cloudCredentials);
                }
            }
        }
        return matches;
    }

    /**
     * Returns the email addresses of any stored credentials
     * 
     * @return a non-<code>null</code> list with no duplicates
     */
    public List<String> getStoredEmails() {
        final Set<String> storedEmails = new LinkedHashSet<String>();
        for (final CloudCredentials storedCredentials : getStoredCredentials()) {
            storedEmails.add(storedCredentials.getEmail());
        }
        return new ArrayList<String>(storedEmails);
    }

    /**
     * Returns the email addresses of any stored credentials with the given URL
     * 
     * @param cloudControllerUrl the URL to match on (can be blank)
     * @return a non-<code>null</code> list with no duplicates; empty if the
     *         given URL is blank
     */
    public List<String> getStoredEmails(final String cloudControllerUrl) {
        final Set<String> storedEmails = new LinkedHashSet<String>();
        if (StringUtils.isNotBlank(cloudControllerUrl)) {
            for (final CloudCredentials storedCredentials : getStoredCredentials()) {
                if (cloudControllerUrl.equals(storedCredentials.getUrl())) {
                    storedEmails.add(storedCredentials.getEmail());
                }
            }
        }
        return new ArrayList<String>(storedEmails);
    }

    /**
     * Returns the stored password for the given URL and email address
     * 
     * @param cloudControllerUrl
     * @param email
     * @return <code>null</code> if there isn't one
     */
    public String getStoredPassword(final String cloudControllerUrl,
            final String email) {
        for (final CloudCredentials storedCredential : getStoredCredentials()) {
            if (storedCredential.isSameAccount(cloudControllerUrl, email)) {
                return storedCredential.getPassword();
            }
        }
        return null;
    }

    /**
     * Returns the URLs of any stored credentials
     * 
     * @return a non-<code>null</code> list with no duplicates
     */
    public List<String> getStoredUrls() {
        final Set<String> storedUrls = new LinkedHashSet<String>();
        for (final CloudCredentials storedCredentials : getStoredCredentials()) {
            storedUrls.add(storedCredentials.getUrl());
        }
        return new ArrayList<String>(storedUrls);
    }

    /**
     * Stores the given credentials along with any previously stored ones
     * 
     * @param newCredentials the credentials to store (required, must be valid)
     */
    public void storeCredentials(final CloudCredentials newCredentials) {
        Validate.isTrue(newCredentials.isValid(),
                "Cannot store invalid credentials");
        // The credentials to write are the existing valid ones...
        final Collection<String> entries = new LinkedHashSet<String>();
        for (final CloudCredentials storedCredentials : getStoredCredentials()) {
            if (storedCredentials.isValid()) {
                entries.add(storedCredentials.encode());
            }
        }
        // ...plus the given ones
        entries.add(newCredentials.encode());

        // Write them
        try {
            final byte[] encodedEntries = StringUtils.join(entries, DELIMITER)
                    .getBytes(CHARSET_NAME);
            final byte[] encryptedEntries = crypt(encodedEntries, ENCRYPT_MODE);
            preferences.putByteArray(CLOUD_FOUNDRY_KEY, encryptedEntries);
        }
        catch (final UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
