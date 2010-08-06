package org.springframework.roo.process.manager.internal;

import java.util.logging.Level;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.process.manager.event.ProcessManagerStatus;
import org.springframework.roo.process.manager.event.ProcessManagerStatusListener;
import org.springframework.roo.process.manager.event.ProcessManagerStatusProvider;
import org.springframework.roo.shell.osgi.AbstractFlashingObject;

/**
 * Allows monitoring of {@link ProcessManager} for development mode users.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Service
@Component(immediate=true)
public class ProcessManagerDiagnosticsListener extends AbstractFlashingObject implements ProcessManagerStatusListener {

	@Reference private ProcessManagerStatusProvider processManagerStatusProvider;
	
	protected void activate(ComponentContext context) {
		processManagerStatusProvider.addProcessManagerStatusListener(this);
	}

	protected void deactivate(ComponentContext context) {
		processManagerStatusProvider.removeProcessManagerStatusListener(this);
	}

	
	public void onProcessManagerStatusChange(ProcessManagerStatus oldStatus, ProcessManagerStatus newStatus) {
		if (isDevelopmentMode()) {
			flash(Level.FINE, newStatus.name(), MY_SLOT);
		}
	}

}
