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
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.osgi.AbstractFlashingObject;

/**
 * Allows monitoring of {@link ProcessManager} for development mode users.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.1
 */
@Service
@Component(immediate=true)
public class ProcessManagerDiagnosticsListener extends AbstractFlashingObject implements ProcessManagerStatusListener, CommandMarker {

	@Reference private ProcessManagerStatusProvider processManagerStatusProvider;
	private boolean isDebug = false;
	
	protected void activate(ComponentContext context) {
		processManagerStatusProvider.addProcessManagerStatusListener(this);
		isDebug = System.getProperty("roo-args") != null && isDevelopmentMode();
	}

	protected void deactivate(ComponentContext context) {
		processManagerStatusProvider.removeProcessManagerStatusListener(this);
	}

	public void onProcessManagerStatusChange(ProcessManagerStatus oldStatus, ProcessManagerStatus newStatus) {
		if (isDebug) {
			flash(Level.FINE, newStatus.name(), MY_SLOT);
		}
	}
	
	@CliCommand(value="process manager debug", help="Indicates if process manager debugging is desired")
	public void processManagerDebug (@CliOption(key={"","enabled"}, mandatory=false, specifiedDefaultValue="true", unspecifiedDefaultValue="true", help="Activates debug mode") boolean debug) {
		this.isDebug = debug;
	}

}
