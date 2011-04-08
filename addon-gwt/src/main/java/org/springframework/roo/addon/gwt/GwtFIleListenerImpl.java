package org.springframework.roo.addon.gwt;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.roo.project.ProjectOperations;

/**
 * Implemetation of {@link GwtFileListener).
 * 
 * @author Alan Stewart
 * @since 1.1.3
 */
@Component(immediate = true)
@Service
public class GwtFIleListenerImpl implements GwtFileListener {
	@Reference private GwtConfigService gwtConfigService;
	@Reference private ProjectOperations projectOperations;
	private Boolean lastGaeState = null;

	public void onFileEvent(FileEvent fileEvent) {
		if (!projectOperations.isProjectAvailable()) {
			return;
		}

		if (lastGaeState != null && projectOperations.getProjectMetadata().isGaeEnabled() == lastGaeState) {
			return;
		}
		
		lastGaeState = projectOperations.getProjectMetadata().isGaeEnabled();

		if (fileEvent.getOperation().equals(FileOperation.UPDATED) && fileEvent.getFileDetails().getFile().getName().equals("pom.xml")) {
			gwtConfigService.updateConfiguration(false);
		}
	}
}
