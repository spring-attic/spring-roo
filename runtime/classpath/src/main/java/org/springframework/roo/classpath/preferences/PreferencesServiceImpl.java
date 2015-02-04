package org.springframework.roo.classpath.preferences;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

/**
 * The {@link PreferencesService} implementation.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class PreferencesServiceImpl implements PreferencesService {

    public Preferences getPreferencesFor(final Class<?> owningClass) {
        // Create the Preferences object, suppressing
        // "Created user preferences directory" messages if there is no Java
        // preferences directory
        // TODO Switch to UAA's PreferencesUtils (but must wait for UAA 1.0.3
        // due to bug in UAA 1.0.2 and earlier)
        final Logger l = Logger.getLogger("java.util.prefs");
        final Level original = l.getLevel();
        try {
            l.setLevel(Level.WARNING);
            return new Preferences(
                    java.util.prefs.Preferences.userNodeForPackage(owningClass));
        }
        finally {
            l.setLevel(original);
        }
    }
}