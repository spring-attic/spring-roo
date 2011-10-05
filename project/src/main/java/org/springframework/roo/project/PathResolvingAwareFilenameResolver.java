package org.springframework.roo.project;

import java.io.File;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.file.undo.FilenameResolver;
import org.springframework.roo.support.util.Assert;

/**
 * {@link FilenameResolver} that delegates to {@link PathResolver}.
 *
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public class PathResolvingAwareFilenameResolver implements FilenameResolver {

	// Fields
	@Reference private PathResolver pathResolver;

	public String getMeaningfulName(final File file) {
		Assert.notNull(file, "File required");
		return pathResolver.getFriendlyName(FileDetails.getCanonicalPath(file));
	}
}
