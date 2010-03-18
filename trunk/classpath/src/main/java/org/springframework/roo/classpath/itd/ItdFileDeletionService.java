package org.springframework.roo.classpath.itd;

import java.io.File;

import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.file.monitor.FileMonitorService;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;

/**
 * Listens for {@link FileMetadata} and automatically deletes any ITD that should not exist
 * due to the non-existence of the {@link PhysicalTypeMetadata} that the ITD would have been
 * introduced into.
 * 
 * <p>
 * This service will only delete files matching the syntax *_Roo_*.aj, and then only if the
 * leftmost wildcard represents a filename that does not have a .java file in the same directory.
 * For example, if the file src/main/java/com/foo/Bar_Roo_Hello.aj was detected as existing,
 * it will be deleted unless src/main/java/com/foo/Bar.java also presently exists.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public class ItdFileDeletionService implements FileEventListener {

	private static String ANT_PATH_ALL_ITD_SOURCE = "**" + File.separator + "*_Roo_*.aj";
	private static String ANT_PATH_ALL_JAVA_SOURCE = "**" + File.separator + "*.java";

	static {
		if ("/".equals(File.separator)) {
			// This is a *nix box and thus starts all paths with a slash (ROO-34)
			ANT_PATH_ALL_ITD_SOURCE = File.separator + ANT_PATH_ALL_ITD_SOURCE;
			ANT_PATH_ALL_JAVA_SOURCE = File.separator + ANT_PATH_ALL_JAVA_SOURCE;
		}
	}
	
	private FileManager fileManager;

	public ItdFileDeletionService(FileMonitorService fileMonitorService, FileManager fileManager) {
		Assert.notNull(fileMonitorService, "File monitor service required");
		Assert.notNull(fileManager, "File manager required");
		this.fileManager = fileManager;
		fileMonitorService.addFileEventListener(this);
	}

	public void onFileEvent(FileEvent fileEvent) {
		Assert.notNull(fileEvent, "File event required");
		if (fileEvent.getFileDetails().matchesAntPath(ANT_PATH_ALL_ITD_SOURCE)) {
			// It's a ROO ITD, but check it really exists
			if (!fileEvent.getFileDetails().getFile().exists()) {
				return;
			}
			// It exists, so compute the governor filename
			String path = fileEvent.getFileDetails().getCanonicalPath();
			int lastIndex = path.lastIndexOf("_Roo_");
			String governorName = path.substring(0, lastIndex) + ".java";
			if (!new File(governorName).exists()) {
				// We just checked the disk, and the governor does not exist, so blow away the ITD
				fileManager.delete(fileEvent.getFileDetails().getCanonicalPath());
			}
		} else if (fileEvent.getFileDetails().matchesAntPath(ANT_PATH_ALL_JAVA_SOURCE)) {
			// It's a Java file, but we only cleanup on deletion in this case
			if (!fileEvent.getFileDetails().getFile().exists()) {
				// Java file was deleted, so let's get rid of any ITDs that are laying around
				String governorName = fileEvent.getFileDetails().getCanonicalPath();
				int lastIndex = governorName.lastIndexOf(".java");
				String itdAntPath = governorName.substring(0, lastIndex) + "_Roo_*.aj";
				for (FileDetails itd : fileManager.findMatchingAntPath(itdAntPath)) {
					String itdCanonicalPath = itd.getCanonicalPath();
					if (new File(itdCanonicalPath).exists()) {
						fileManager.delete(itdCanonicalPath);
					}
				}
			}
		}
	}
		
}
