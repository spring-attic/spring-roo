package org.springframework.roo.classpath.preferences;

/**
 * Manages user preferences. Use this interface instead of the Java
 * {@link java.util.prefs.Preferences} API in order to minimise coupling, both
 * for increased testability and to allow for alternative implementations.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public interface PreferencesService {

    /**
     * Returns the user's preferences for the package in which the given class
     * resides.
     * 
     * @param owningClass the class for whose package to retrieve the user's
     *            preferences (required)
     * @return a non-<code>null</code> instance
     * @see Preferences#userNodeForPackage(Class)
     */
    Preferences getPreferencesFor(Class<?> owningClass);
}