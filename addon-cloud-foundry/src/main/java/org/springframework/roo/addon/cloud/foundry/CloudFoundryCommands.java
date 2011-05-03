package org.springframework.roo.addon.cloud.foundry;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.cloud.foundry.model.CloudApp;
import org.springframework.roo.addon.cloud.foundry.model.CloudAppMemoryOption;
import org.springframework.roo.addon.cloud.foundry.model.CloudControllerUrl;
import org.springframework.roo.addon.cloud.foundry.model.CloudDeployableFile;
import org.springframework.roo.addon.cloud.foundry.model.CloudFile;
import org.springframework.roo.addon.cloud.foundry.model.CloudLoginEmail;
import org.springframework.roo.addon.cloud.foundry.model.CloudUri;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

import com.vmware.appcloud.client.CloudService;
import com.vmware.appcloud.client.ServiceConfiguration;

/**
 * Commands for addon-cloud foundry.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
@Component
@Service
public class CloudFoundryCommands implements CommandMarker {
	@Reference private CloudFoundryOperations cloudFoundryOperations;

	@CliCommand(value = "cloud foundry info", help = "Information")
	public void info() {
		cloudFoundryOperations.info();
	}

	@CliCommand(value = "cloud foundry login", help = "Login")
	public void login(
		@CliOption(key = "email", mandatory = false, help = "The user's email address") CloudLoginEmail loginEmail,
		@CliOption(key = "password", mandatory = false, help = "The user's password") String password,
		@CliOption(key = "cloudControllerUrl", mandatory = false, help = "The cloud controller to target. Defaults to '" + UaaAwareAppCloudClient.VCLOUD_URL + "'") CloudControllerUrl cloudControllerUrl) {
		
		String url = null;
		String email = null;
		if (cloudControllerUrl != null) {
			url = cloudControllerUrl.getUrl();
		}
		if (loginEmail != null) {
			email = loginEmail.getEmail();
		}
		cloudFoundryOperations.login(email, password, url);
	}

	@CliCommand(value = "cloud foundry list services", help = "List of services available")
	public void services() {
		cloudFoundryOperations.services();
	}

	@CliCommand(value = "cloud foundry create service", help = "Create a service instance")
	public void createService(
		@CliOption(key = "serviceType", mandatory = true, help = "The service type") ServiceConfiguration service,
		@CliOption(key = "serviceName", mandatory = true, help = "The name of your service instance") String name,
		@CliOption(key = "appName", mandatory = false, help = "The name of the app to bind the service to") CloudApp bind) {
		
		String appName = null;
		if (bind != null) {
			appName = bind.getName();
		}
		cloudFoundryOperations.createService(service.getVendor(), name, appName);
	}

	@CliCommand(value = "cloud foundry delete service", help = "Delete a service")
	public void deleteService(
		@CliOption(key = "serviceName", mandatory = false, help = "The name of the service instance to delete") CloudService service) {
		
		cloudFoundryOperations.deleteService(service.getName());
	}

	@CliCommand(value = "cloud foundry bind service", help = "Bind a service to a deployed application")
	public void bindService(
		@CliOption(key = "serviceName", mandatory = true, help = "The name of the service instance you want to bind") CloudService service,
		@CliOption(key = "appName", mandatory = true, help = "The name of the application you want to bind the service to") CloudApp appName) {
		
		cloudFoundryOperations.bindService(service.getName(), appName.getName());
	}

	@CliCommand(value = "cloud foundry unbind service", help = "Unbind a service from a deployed application")
	public void unbindService(
		@CliOption(key = "serviceName", mandatory = true, help = "The name of the service instance you want to bind") CloudService service,
		@CliOption(key = "appName", mandatory = true, help = "The name of the application you want to bind the service to") CloudApp appName) {
		
		cloudFoundryOperations.unbindService(service.getName(), appName.getName());
	}

	@CliCommand(value = "cloud foundry list apps", help = "List deployed applications")
	public void apps() {
		cloudFoundryOperations.apps();
	}

	@CliCommand(value = "cloud foundry deploy", help = "Deploy an application")
	public void push(
		@CliOption(key = "appName", mandatory = true, help = "The desired name of the application") CloudApp appName,
		@CliOption(key = "instances", mandatory = false, help = "The number of instances to be created") Integer instances,
		@CliOption(key = "memory", mandatory = false, help = "The amount of memory required by the application") CloudAppMemoryOption memSize,
		@CliOption(key = "path", mandatory = true, help = "The path to the application to deploy") CloudDeployableFile path,
		@CliOption(key = "urls", mandatory = false, help = "The URL(s) to be mapped to the application") List<String> urls) {
		
		Integer memory = null;
		if (memSize != null) {
			memory = memSize.getMemoryOption();
		}
		cloudFoundryOperations.push(appName.getName(), instances, memory, path.getPath(), urls);
	}

	@CliCommand(value = "cloud foundry start app", help = "Start an application")
	public void start(
		@CliOption(key = "appName", mandatory = true, help = "The name of the application to start") CloudApp appName) {
		
		cloudFoundryOperations.start(appName.getName());
	}

	@CliCommand(value = "cloud foundry stop app", help = "Stop an application")
	public void stop(
		@CliOption(key = "appName", mandatory = true, help = "The name of the application to stop") CloudApp appName) {
		
		cloudFoundryOperations.stop(appName.getName());
	}

	@CliCommand(value = "cloud foundry restart app", help = "Restart an application")
	public void restart(
		@CliOption(key = "appName", mandatory = true, help = "The name of the application to restart") CloudApp appName) {
		
		cloudFoundryOperations.restart(appName.getName());
	}

	@CliCommand(value = "cloud foundry delete app", help = "Delete an application")
	public void delete(
		@CliOption(key = "appName", mandatory = true, help = "The name of the application to delete") CloudApp appName) {
		
		cloudFoundryOperations.delete(appName.getName());
	}

	// TODO: This feature was not working in the initial release   JT
	// @CliCommand(value = "cloud foundry rename app", help = "Delete an application")
	public void renameApp(
		@CliOption(key = "appName", mandatory = true, help = "The name of the application to rename") CloudApp appName,
		@CliOption(key = "newAppName", mandatory = true, help = "The new name of the application") String newAppName) {
		
		cloudFoundryOperations.renameApp(appName.getName(), newAppName);
	}

	@CliCommand(value = "cloud foundry list instances", help = "List and/or change the number of application instances")
	public void instances(
		@CliOption(key = "appName", mandatory = true, help = "The name of the application to list and/or change the number instances for") CloudApp appName,
		@CliOption(key = "num", mandatory = false, help = "The number of instances to scale the application to") String number) {
		
		cloudFoundryOperations.instances(appName.getName(), number);
	}

	@CliCommand(value = "cloud foundry view app memory", help = "View or update the memory reservation for an application")
	public void viewMemory(
		@CliOption(key = "appName", mandatory = true, help = "The name of the application to view or update the reservation for") CloudApp appName) {
		
		cloudFoundryOperations.mem(appName.getName(), null);
	}

	@CliCommand(value = "cloud foundry update app memory", help = "View or update the memory reservation for an application")
	public void updateMemory(
		@CliOption(key = "appName", mandatory = true, help = "The name of the application to view or update the reservation for") CloudApp appName,
		@CliOption(key = "memSize", mandatory = false, help = "The amount of memory to change the application's memory reservation to") CloudAppMemoryOption memSize) {
		
		Integer memory = null;
		if (memSize != null) {
			memory = memSize.getMemoryOption();
		}
		cloudFoundryOperations.mem(appName.getName(), memory);
	}

	@CliCommand(value = "cloud foundry view crashes", help = "View recent application crashes")
	public void crashes(
		@CliOption(key = "appName", mandatory = true, help = "The name of the application to view recent crashes for") CloudApp appName) {
		
		cloudFoundryOperations.crashes(appName.getName());
	}

	@CliCommand(value = "cloud foundry view crash logs", help = "Display all the logs for crashed instance")
	public void crashLogs(
		@CliOption(key = "appName", mandatory = true, help = "The name of the application to view the crash logs for") CloudApp appName,
		@CliOption(key = "instance", mandatory = false, help = "The name specific instance of the application of interest") String instance) {
		
		cloudFoundryOperations.crashLogs(appName.getName(), instance);
	}

	@CliCommand(value = "cloud foundry view logs", help = "Display all the logs for a specific application")
	public void logs(
		@CliOption(key = "appName", mandatory = true, help = "The name of the application to view the logs for") CloudApp appName,
		@CliOption(key = "instance", mandatory = false, help = "The name specific instance of the application of interest") String instance) {
		
		cloudFoundryOperations.logs(appName.getName(), instance);
	}

	@CliCommand(value = "cloud foundry files", help = "Directory listings or file download")
	public void files(
		@CliOption(key = "appName", mandatory = true, help = "The name of the application to target") CloudApp appName,
		@CliOption(key = "path", mandatory = true, help = "The path of the file or folder to navigate to or view") CloudFile path,
		@CliOption(key = "instance", mandatory = false, help = "The specific instance of the application to be targeted") String instance) {
		
		cloudFoundryOperations.files(appName.getName(), path.getFileName(), instance);
	}

	@CliCommand(value = "cloud foundry view app stats", help = "Report resource usage for the application")
	public void viewApptats(
		@CliOption(key = "appName", mandatory = true, help = "The name of the application to target") CloudApp appName) {
		
		cloudFoundryOperations.stats(appName.getName());
	}

	@CliCommand(value = "cloud foundry map url", help = "Register an application with a URL")
	public void mapUrl(
		@CliOption(key = "appName", mandatory = true, help = "The name of the application to bind the URL to") CloudApp appName,
		@CliOption(key = "url", mandatory = true, help = "The URL to bind the application to") String url) {
		
		cloudFoundryOperations.map(appName.getName(), url);
	}

	@CliCommand(value = "cloud foundry unmap url", help = "Register an application with a URL")
	public void unMapUrl(
		@CliOption(key = "appName", mandatory = true, help = "The name of the application to unbind the URL from") CloudApp appName,
		@CliOption(key = "url", mandatory = true, help = "The URL to unbind the application from") CloudUri url) {
		
		cloudFoundryOperations.unMap(appName.getName(), url.getUri());
	}

	@CliAvailabilityIndicator(
		{"cloud foundry list apps", "cloud foundry bind service", "cloud foundry view crash logs", "cloud foundry view crashes", "cloud foundry create service", "cloud foundry delete app",
		"cloud foundry delete service", "cloud foundry files", "cloud foundry info", "cloud foundry list instances", "cloud foundry view logs", "cloud foundry map url", "cloud foundry view app memory",
		"cloud foundry restart app", "cloud foundry list services", "cloud foundry setup", "cloud foundry start app", "cloud foundry view app stats", "cloud foundry stop app", "cloud foundry unbind service", "cloud foundry unmap url",
		"cloud foundry deploy", "cloud foundry rename app", "cloud foundry update app memory"})
	public boolean isCommandAvailable() {
		return cloudFoundryOperations.isCloudFoundryCommandAvailable();
	}

	/*	
 	@CliAvailabilityIndicator("cloud foundry setup")
	public boolean isSetupCommandAvailable() {
		return cloudFoundryOperations.isSetupCommandAvailable();
	}*/

	// TODO: Add setup command once cloud namespace is more understood
	// @CliCommand(value = "cloud foundry setup", help = "Setup Cloud Foundry")
	public void config() {
		cloudFoundryOperations.setup();
	}
}
