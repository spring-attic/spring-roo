package org.springframework.roo.project;

import java.io.File;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.file.undo.FilenameResolver;
import org.springframework.roo.support.util.FileUtils;

/**
 * {@link FilenameResolver} that delegates to {@link PathResolver}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public class PathResolvingAwareFilenameResolver implements FilenameResolver {

    @Reference private PathResolver pathResolver;

    public String getMeaningfulName(final File file) {
        Validate.notNull(file, "File required");
        return pathResolver.getFriendlyName(FileUtils.getCanonicalPath(file));
    }
}
