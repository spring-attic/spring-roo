package org.springframework.roo.classpath.preferences;

import java.io.UnsupportedEncodingException;
import java.util.prefs.BackingStoreException;

import org.apache.commons.lang3.Validate;

/**
 * A node in the user's tree of preferences.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class Preferences {

    private final java.util.prefs.Preferences preferences; // "Delegate" pattern

    /**
     * Constructor for delegating to a Java Preferences instance.
     * 
     * @param preferences the preferences to read and write (required)
     */
    public Preferences(final java.util.prefs.Preferences preferences) {
        Validate.notNull(preferences, "Delegate preferences are required");
        this.preferences = preferences;
    }

    /**
     * Flushes any changes in these preferences to the persistent storage.
     * 
     * @throws IllegalStateException if there was a problem
     */
    public void flush() {
        try {
            preferences.flush();
        }
        catch (final BackingStoreException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the byte array with the given key, or if none, an empty array.
     * 
     * @param key the key whose value to retrieve (can be <code>null</code>)
     * @return <code>null</code> iff a <code>null</code> key is given
     */
    public byte[] getByteArray(final String key) {
        return getByteArray(key, null, "ignored");
    }

    /**
     * Returns the byte array with the given key, or if none, the given default
     * value.
     * 
     * @param key the key whose value to retrieve (can be <code>null</code>)
     * @param defaultValue can be <code>null</code> to default to an empty array
     * @param characterSetName the name of the character set into which to
     *            encode the given default value
     * @return <code>null</code> iff a <code>null</code> key is given
     * @throws UnsupportedOperationException if the given character set is not
     *             supported
     */
    public byte[] getByteArray(final String key, final String defaultValue,
            final String characterSetName) {
        if (key == null) {
            return null;
        }
        try {
            final byte[] defaultBytes = defaultValue == null ? new byte[0]
                    : defaultValue.getBytes(characterSetName);
            return preferences.getByteArray(key, defaultBytes);
        }
        catch (final UnsupportedEncodingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    /**
     * Returns the int with the given key, or if none, the given default value.
     * 
     * @param key the key whose value to retrieve (can be <code>null</code>)
     * @param defaultValue see above
     */
    public int getInt(final String key, final int defaultValue) {
        return preferences.getInt(key, defaultValue);
    }

    /**
     * Adds the given byte array under the given key.
     * 
     * @param key
     * @param value
     */
    public void putByteArray(final String key, final byte[] value) {
        preferences.putByteArray(key, value);
    }

    /**
     * Adds the given int value under the given key.
     * 
     * @param key
     * @param value
     */
    public void putInt(final String key, final int value) {
        preferences.putInt(key, value);
    }
}
