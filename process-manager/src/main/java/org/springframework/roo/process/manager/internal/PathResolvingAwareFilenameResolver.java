package org.springframework.roo.process.manager.internal;

import java.io.File;

import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.file.undo.FilenameResolver;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.util.Assert;

/**
 * {@link FilenameResolver} that delegates to {@link PathResolver}.
 * 
 * <p>
 * Automatically used by {@link DefaultFileManager} where possible.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class PathResolvingAwareFilenameResolver implements FilenameResolver {

	private PathResolver pathResolver;
	
	public PathResolvingAwareFilenameResolver(PathResolver pathResolver) {
		Assert.notNull(pathResolver, "Path resolver required");
		this.pathResolver = pathResolver;
	}

	public String getMeaningfulName(File file) {
		Assert.notNull(file, "File required");
		return pathResolver.getFriendlyName(FileDetails.getCanonicalPath(file));
	}

}
