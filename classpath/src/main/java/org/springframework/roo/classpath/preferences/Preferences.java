package org.springframework.roo.classpath.preferences;

import java.io.UnsupportedEncodingException;
import java.util.prefs.BackingStoreException;

import org.springframework.roo.support.util.Assert;

/**
 * A node in the user's tree of preferences.
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class Preferences {

	// Fields
	private final java.util.prefs.Preferences preferences;	// "Delegate" pattern

	/**
	 * Constructor for delegating to a Java Preferences instance.
	 *
	 * @param preferences the preferences to read and write (required)
	 */
	public Preferences(final java.util.prefs.Preferences preferences) {
		Assert.notNull(preferences, "Delegate preferences are required");
		this.preferences = preferences;
	}
	
	/**
	 * Flushes any changes in these preferences to the persistent storage.
	 */
	public void flush() {
		try {
			preferences.flush();
		} catch (final BackingStoreException e) {
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
	 * Returns the byte array with the given key, or if none, the given default value.
	 * 
	 * @param key the key whose value to retrieve (can be <code>null</code>)
	 * @param defaultValue can be <code>null</code> to default to an empty array
	 * @param characterSetName the name of the character set into which to encode the given default value
	 * @return <code>null</code> iff a <code>null</code> key is given
	 * @throws UnsupportedOperationException if the given character set is not supported
	 */
	public byte[] getByteArray(final String key, final String defaultValue, final String characterSetName) {
		if (key == null) {
			return null;
		}
		try {
			final byte[] defaultBytes = defaultValue == null ? new byte[0] : defaultValue.getBytes(characterSetName);
			return this.preferences.getByteArray(key, defaultBytes);
		} catch (UnsupportedEncodingException e) {
			throw new UnsupportedOperationException(e);
		} 
	}

	/**
	 * Adds the given byte array under the given key.
	 * 
	 * @param key
	 * @param value
	 */
	public void putByteArray(final String key, final byte[] value) {
		this.preferences.putByteArray(key, value);
	}
}
