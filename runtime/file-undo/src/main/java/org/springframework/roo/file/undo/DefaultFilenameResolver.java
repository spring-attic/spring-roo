package org.springframework.roo.file.undo;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.Validate;

/**
 * Default {@link FilenameResolver} that simply returns canonical file paths.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class DefaultFilenameResolver implements FilenameResolver {

    public String getMeaningfulName(final File file) {
        Validate.notNull(file, "File required");
        try {
            return file.getCanonicalPath();
        }
        catch (final IOException ioe) {
            throw new IllegalStateException("Could not resolve filename for '"
                    + file + "'", ioe);
        }
    }
}
