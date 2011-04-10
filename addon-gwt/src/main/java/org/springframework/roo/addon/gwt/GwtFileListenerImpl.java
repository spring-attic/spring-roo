package org.springframework.roo.addon.gwt;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.project.ProjectOperations;

/**
 * Implementation of {@link GwtFileListener).
 * 
 * @author Alan Stewart
 * @since 1.1.3
 */
@Component(immediate = true)
@Service
public class GwtFileListenerImpl implements GwtFileListener {
	@Reference private GwtConfigService gwtConfigService;
	@Reference private ProjectOperations projectOperations;

	public void onFileEvent(FileEvent fileEvent) {
		if (!projectOperations.isProjectAvailable() || !projectOperations.getProjectMetadata().isGaeEnabled()) {
			return;
		}

		if (fileEvent.getOperation().equals(FileOperation.UPDATED) && fileEvent.getFileDetails().getFile().getName().equals("pom.xml") && projectOperations.getProjectMetadata().isGwtEnabled()) {
			gwtConfigService.updateConfiguration(false);
		}
	}
}
