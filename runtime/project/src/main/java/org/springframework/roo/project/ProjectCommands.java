package org.springframework.roo.project;

import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.ProcessManager;
import org.springframework.roo.project.packaging.JarPackaging;
import org.springframework.roo.project.packaging.PackagingProvider;
import org.springframework.roo.project.providers.ProjectManagerProviderId;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Commands related to file system monitoring and process management.
 * 
 * @author Ben Alex
 * @author Juan Carlos García
 * @since 1.1
 */
@Component
@Service
public class ProjectCommands implements CommandMarker {
	
	private static final String DEVELOPMENT_MODE_COMMAND = "development mode";
	private static final String PROJECT_SETUP_COMMAND = "project setup";
	private static final String PROJECT_SCAN_SPEED_COMMAND = "project scan speed";
	private static final String PROJECT_SCAN_STATUS_COMMAND = "project scan status";
	private static final String PROJECT_SCAN_NOW_COMMAND = "project scan now";

	protected final static Logger LOGGER = HandlerUtils.getLogger(ProjectCommands.class);
	
    // ------------ OSGi component attributes ----------------
   	private BundleContext context;

    private ProcessManager processManager;
    private Shell shell;
    private ProjectService projectService;

    protected void activate(final ComponentContext context) {
    	this.context = context.getBundleContext();
    }
    
    @CliAvailabilityIndicator(PROJECT_SETUP_COMMAND)
    public boolean isCreateProjectAvailable() {
        return getProjectService().isCreateProjectAvailable();
    }
    
    @CliAvailabilityIndicator({PROJECT_SCAN_SPEED_COMMAND, PROJECT_SCAN_STATUS_COMMAND,
    	PROJECT_SCAN_NOW_COMMAND})
    public boolean isProjecScanAvailable() {
        return getProjectService().isFocusedProjectAvailable();
    }
    
    @CliCommand(value = PROJECT_SETUP_COMMAND, help = "Creates a new Maven project")
    public void createProject(
            @CliOption(key = { "", "topLevelPackage" }, mandatory = true, optionContext = "update", help = "The uppermost package name (this becomes the groupId and also the '~' value when using Roo's shell)") final JavaPackage topLevelPackage,
            @CliOption(key = "provider", mandatory = true, help = "Provider to use on project generation") ProjectManagerProviderId provider,
            @CliOption(key = "projectName", help = "The name of the project (last segment of package name used as default)") final String projectName,
            @CliOption(key = "java", help = "Forces a particular major version of Java to be used (will be auto-detected if unspecified; specify 5 or 6 or 7 only)") final Integer majorJavaVersion,
            @CliOption(key = "parent", help = "The Maven coordinates of the parent POM, in the form \"groupId:artifactId:version\"") final GAV parentPom,
            @CliOption(key = "packaging", help = "The Maven packaging of this project", unspecifiedDefaultValue = JarPackaging.NAME) final PackagingProvider packaging) {

        getProjectService().createProject(topLevelPackage, projectName,
                majorJavaVersion, packaging, provider);
    }
    
    @CliCommand(value = DEVELOPMENT_MODE_COMMAND, help = "Switches the system into development mode (greater diagnostic information)")
    public String developmentMode(
            @CliOption(key = { "", "enabled" }, mandatory = false, specifiedDefaultValue = "true", unspecifiedDefaultValue = "true", help = "Activates development mode") final boolean enabled) {
    	getProcessManager().setDevelopmentMode(enabled);
        getShell().setDevelopmentMode(enabled);
        return "Development mode set to " + enabled;
    }

    @CliCommand(value = PROJECT_SCAN_NOW_COMMAND, help = "Perform a manual file system scan")
    public String scan() {
        final long originalSetting = getProcessManager()
                .getMinimumDelayBetweenScan();
        try {
            getProcessManager().setMinimumDelayBetweenScan(1);
            getProcessManager().timerBasedScan();
        }
        finally {
            // Switch on manual scan again
        	getProcessManager().setMinimumDelayBetweenScan(originalSetting);
        }
        return "Manual scan completed";
    }

    @CliCommand(value = PROJECT_SCAN_STATUS_COMMAND, help = "Display file system scanning information")
    public String scanningInfo() {
    	
        final StringBuilder sb = new StringBuilder("File system scanning ");
        final long duration = getProcessManager().getLastScanDuration();
        if (duration == 0) {
            sb.append("never executed; ");
        }
        else {
            sb.append("last took ").append(duration).append(" ms; ");
        }
        final long minimum = getProcessManager().getMinimumDelayBetweenScan();
        if (minimum == 0) {
            sb.append("automatic scanning is disabled");
        }
        else if (minimum < 0) {
            sb.append("auto-scaled scanning is enabled");
        }
        else {
            sb.append("scanning frequency has a minimum interval of ")
                    .append(minimum).append(" ms");
        }
        return sb.toString();
    }

    @CliCommand(value = PROJECT_SCAN_SPEED_COMMAND, help = "Changes the file system scanning speed")
    public String scanningSpeed(
            @CliOption(key = { "", "ms" }, mandatory = true, help = "The number of milliseconds between each scan") final long minimumDelayBetweenScan) {
    	getProcessManager().setMinimumDelayBetweenScan(minimumDelayBetweenScan);
        return scanningInfo();
    }
    
    public ProcessManager getProcessManager(){
    	if(processManager == null){
    		// Get all components implement ProcessManager interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(ProcessManager.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				processManager = (ProcessManager) this.context.getService(ref);
    				return processManager;
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load ProcessManager on ProjectCommands.");
    			return null;
    		}
    	}else{
    		return processManager;
    	}
    }
    
    public Shell getShell(){
    	if(shell == null){
    		// Get all Shell implement Shell interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(Shell.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				shell = (Shell) this.context.getService(ref);
    				return shell;
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load Shell on ProjectCommands.");
    			return null;
    		}
    		
    	}else{
    		return shell;
    	}
    }
    
    public ProjectService getProjectService() {
        if (projectService == null) {
            // Get all Services implement ProjectService interface
            try {
                ServiceReference<?>[] references = this.context
                        .getAllServiceReferences(
                        		ProjectService.class.getName(), null);

                for (ServiceReference<?> ref : references) {
                    return (ProjectService) this.context.getService(ref);
                }

                return null;

            }
            catch (InvalidSyntaxException e) {
                LOGGER.warning("Cannot load ProjectService on ProjectCommands.");
                return null;
            }
        }
        else {
            return projectService;
        }
    }
}
