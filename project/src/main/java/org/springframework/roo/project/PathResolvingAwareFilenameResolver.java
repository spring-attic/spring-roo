package org.springframework.roo.project;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.file.undo.FilenameResolver;
import org.springframework.roo.support.util.Assert;

import java.io.File;

/**
 * {@link FilenameResolver} that delegates to {@link PathResolver}.
 *
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component(immediate = true)
@Service
public class PathResolvingAwareFilenameResolver implements FilenameResolver {
	@Reference private PathResolver pathResolver;

	public String getMeaningfulName(File file) {
		Assert.notNull(file, "File required");
		return pathResolver.getFriendlyName(FileDetails.getCanonicalPath(file));
	}
}
