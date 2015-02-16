package org.springframework.roo.process.manager;

import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Shell;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Commands related to file system monitoring and process management.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
public class ProcessManagerCommands implements CommandMarker {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(ProcessManagerCommands.class);
	
    // ------------ OSGi component attributes ----------------
   	private BundleContext context;

    private ProcessManager processManager;
    private Shell shell;

    protected void activate(final ComponentContext context) {
    	this.context = context.getBundleContext();
    }

    @CliCommand(value = "development mode", help = "Switches the system into development mode (greater diagnostic information)")
    public String developmentMode(
            @CliOption(key = { "", "enabled" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "true", help = "Activates development mode") final boolean enabled) {
        
    	if(processManager == null){
    		processManager = getProcessManager();
    	}
    	
    	Validate.notNull(processManager, "ProcessManager is required");
    	
    	if(shell == null){
    		shell = getShell();
    	}
    	
    	Validate.notNull(shell, "Shell is required");
    	
    	processManager.setDevelopmentMode(enabled);
        shell.setDevelopmentMode(enabled);
        return "Development mode set to " + enabled;
    }

    @CliCommand(value = "poll now", help = "Perform a manual file system poll")
    public String poll() {
    	if(processManager == null){
    		processManager = getProcessManager();
    	}
    	
    	Validate.notNull(processManager, "ProcessManager is required");
    	
        final long originalSetting = processManager
                .getMinimumDelayBetweenPoll();
        try {
            processManager.setMinimumDelayBetweenPoll(1);
            processManager.timerBasedPoll();
        }
        finally {
            // Switch on manual polling again
            processManager.setMinimumDelayBetweenPoll(originalSetting);
        }
        return "Manual poll completed";
    }

    @CliCommand(value = "poll status", help = "Display file system polling information")
    public String pollingInfo() {
    	if(processManager == null){
    		processManager = getProcessManager();
    	}
    	
    	Validate.notNull(processManager, "ProcessManager is required");
    	
        final StringBuilder sb = new StringBuilder("File system polling ");
        final long duration = processManager.getLastPollDuration();
        if (duration == 0) {
            sb.append("never executed; ");
        }
        else {
            sb.append("last took ").append(duration).append(" ms; ");
        }
        final long minimum = processManager.getMinimumDelayBetweenPoll();
        if (minimum == 0) {
            sb.append("automatic polling is disabled");
        }
        else if (minimum < 0) {
            sb.append("auto-scaled polling is enabled");
        }
        else {
            sb.append("polling frequency has a minimum interval of ")
                    .append(minimum).append(" ms");
        }
        return sb.toString();
    }

    @CliCommand(value = "poll speed", help = "Changes the file system polling speed")
    public String pollingSpeed(
            @CliOption(key = { "", "ms" }, mandatory = true, help = "The number of milliseconds between each poll") final long minimumDelayBetweenPoll) {
    	if(processManager == null){
    		processManager = getProcessManager();
    	}
    	
    	Validate.notNull(processManager, "ProcessManager is required");
    	
    	processManager.setMinimumDelayBetweenPoll(minimumDelayBetweenPoll);
        return pollingInfo();
    }
    
    public ProcessManager getProcessManager(){
    	// Get all components implement ProcessManager interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(ProcessManager.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (ProcessManager) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load ProcessManager on ProcessManagerCommands.");
			return null;
		}
    }
    
    public Shell getShell(){
    	// Get all Shell implement Shell interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(Shell.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (Shell) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load Shell on ProcessManagerCommands.");
			return null;
		}
    }
}
