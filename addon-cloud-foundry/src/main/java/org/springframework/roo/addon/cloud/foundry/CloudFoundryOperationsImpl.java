package org.springframework.roo.addon.cloud.foundry;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.osgi.AbstractFlashingObject;
import org.springframework.roo.support.util.StringUtils;

import com.vmware.appcloud.client.AppCloudClient;
import com.vmware.appcloud.client.ApplicationStats;
import com.vmware.appcloud.client.CloudApplication;
import com.vmware.appcloud.client.CloudInfo;
import com.vmware.appcloud.client.CloudService;
import com.vmware.appcloud.client.CrashInfo;
import com.vmware.appcloud.client.CrashesInfo;
import com.vmware.appcloud.client.InstanceStats;
import com.vmware.appcloud.client.InstancesInfo;
import com.vmware.appcloud.client.ServiceConfiguration;

/**
 * Operations for Cloud Foundry add-on.
 * <p/>
 * TODO:
 * - Move the table rendering stuff out to a separate class in org.sfw.shell so can be used elsewhere; feel free to try using it in AddOnOperationsImpl (talk to him)
 * <p/>
 * DONE:
 * - Syso must become logger calls (limited debugging for your own use is OK, but nothing that will ship in a release)
 * - Get rid of execute method, replace with more obvious try...catch.
 * - Make all failures via a throw new illegalstateexception(theText) (and with a second argument containing the root exception if possible)
 * - Make CloudFoundryoperationsImpl extend AbstractFlashingObject (see JdkUrlInputStreamService)
 * - Login should take a Cloud Controller URL, defaulting to the main URL
 * - Make an effort to encrypt the username and password in the preferences API
 * - Ensure the preferences API understands which cloud controller URL the particular username + password applies to
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
@Component
@Service
public class CloudFoundryOperationsImpl extends AbstractFlashingObject implements CloudFoundryOperations {
	private static final Logger logger = Logger.getLogger(CloudFoundryOperationsImpl.class.getName());
	@Reference private CloudFoundrySession session;
	private AppCloudClient client = null;

	public void info() {
		executeCommand(new CloudCommand("Cloud information failed to be retrieved.") {
			@Override
			public void execute() throws Exception {
				CloudInfo cloudInfo = client.getCloudInfo();
				if (cloudInfo == null || cloudInfo.getUsage() == null || cloudInfo.getLimits() == null) {
					logger.warning("Information could not be retrieved");
					return;
				}
				logger.info("\n");
				logger.info(cloudInfo.getDescription());
				logger.info("For support visit " + cloudInfo.getSupport());
				logger.info("\n");
				logger.info("Target:\t " + client.getCloudControllerUrl() + " (" + cloudInfo.getVersion() + ")");
				logger.info("\n");
				logger.info("User:\t " + cloudInfo.getUser());
				logger.info("Usage:\t Memory (" + cloudInfo.getUsage().getTotalMemory() + "MB of " + cloudInfo.getLimits().getMaxTotalMemory() + "MB total)");
				logger.info("\t Services (" + cloudInfo.getUsage().getServices() + " of " + cloudInfo.getLimits().getMaxServices() + " total)");
				logger.info("\t Apps (" + cloudInfo.getUsage().getApps() + " of " + cloudInfo.getLimits().getMaxApps() + " total)");
				logger.info("\n");
			}
		});
	}

	public void register(final String email, final String password) {
		String successMessage = "Registration was successful";
		String failureMessage = "Registration failed";
		executeCommand(new CloudCommand(failureMessage, successMessage) {
			@Override
			public void execute() throws Exception {
				client.register(email, password);
			}
		});
	}

	public void login(String email, String password, final String cloudControllerUrl) {
		final String finalEmail = email;
		final String finalPassword = password;
		executeCommand(new CloudCommand("Login failed") {
			@Override
			public void execute() throws Exception {
				session.login(finalEmail, finalPassword, cloudControllerUrl);
				client = session.getClient();
			}
		});
	}

	public void services() {
		executeCommand(new CloudCommand("Services could not be retrieved") {
			@Override
			public void execute() throws Exception {
				List<ServiceConfiguration> globalServices = client.getServiceConfigurations();
				List<CloudService> localServices = client.getServices();
				if (globalServices.isEmpty()) {
					logger.info("There are currently no services available.");
				} else {
					ShellTableRenderer table = new ShellTableRenderer("System Services", "Service", "Version", "Description");
					for (ServiceConfiguration service : globalServices) {
						table.addRow(service.getVendor(), service.getVersion(), service.getDescription());
					}
					logger.info(table.getOutput());
				}

				if (localServices.isEmpty()) {
					logger.info("There are currently no provisioned services.");
				} else {
					ShellTableRenderer table = new ShellTableRenderer("Provisioned Services", "Name", "Service");

					for (CloudService service : localServices) {
						table.addRow(service.getName(), service.getVendor());
					}
					logger.info(table.getOutput());
				}
			}
		});
	}

	public void createService(final String service, final String name, String bind) {
		String failureMessage = "The service '" + name + "' failed to be created";
		String successMessage = "The service '" + name + "' was successfully created";
		executeCommand(new CloudCommand(failureMessage, successMessage) {
			@Override
			public void execute() throws Exception {
				CloudService cloudService = new CloudService();
				cloudService.setName(name);
				cloudService.setTier("free");
				List<ServiceConfiguration> serviceConfigurations = client.getServiceConfigurations();
				for (ServiceConfiguration serviceConfiguration : serviceConfigurations) {
					if (serviceConfiguration.getVendor().equals(service)) {
						cloudService.setVendor(serviceConfiguration.getVendor());
						cloudService.setType(serviceConfiguration.getType());
						cloudService.setVersion(serviceConfiguration.getVersion());
					}
				}
				client.createService(cloudService);
			}
		});
	}

	public void deleteService(final String service) {
		String failureMessage = "The service '" + service + "' failed to be deleted";
		String successMessage = "The service '" + service + "' was deleted successfully";
		executeCommand(new CloudCommand(failureMessage, successMessage) {
			@Override
			public void execute() throws Exception {
				client.deleteService(service);
			}
		});
	}

	public void bindService(final String service, final String appName) {
		String failureMessage = "The binding of the service '" + service + "' to the application '" + appName + "' failed";
		String successMessage = "The service '" + service + "' was successfully bound to the application '" + appName + "'";
		executeCommand(new CloudCommand(failureMessage, successMessage) {
			@Override
			public void execute() throws Exception {
				client.bindService(appName, service);
			}
		});
	}

	public void unbindService(final String service, final String appName) {
		String failureMessage = "The unbinding of the service '" + service + "' from the application '" + appName + "' failed";
		String successMessage = "The service '" + service + "' was successfully unbound from the application '" + appName + "'";
		executeCommand(new CloudCommand(failureMessage, successMessage) {
			@Override
			public void execute() throws Exception {
				client.unbindService(appName, service);
			}
		});
	}

	public void apps() {
		executeCommand(new CloudCommand("The applications failed to be listed.") {
			@Override
			public void execute() throws Exception {
				List<CloudApplication> applications = client.getApplications();
				if (applications.isEmpty()) {
					logger.info("No applications available.");
					return;
				}
				ShellTableRenderer table = new ShellTableRenderer("Applications", "Name", "Status", "Instances", "Services", "URLs");
				for (CloudApplication application : applications) {
					StringBuilder uris = new StringBuilder();
					for (int i = 0; i < application.getUris().size(); i++) {
						uris.append(application.getUris().get(i));
						if (i < application.getUris().size() - 1) {
							uris.append(", ");
						}
					}
					StringBuilder services = new StringBuilder();
					for (String service : application.getServices()) {
						services.append(service);
					}
					table.addRow(application.getName(), application.getState().name(), String.valueOf(application.getInstances()), services.toString(), uris.toString());
				}
				logger.info(table.getOutput());
			}
		});
	}

	public void push(final String appName, final Integer instances, Integer memory, final String path, final List<String> urls) {
		if (path == null) {
			logger.severe("The file path cannot be null; cannot continue");
			return;
		}
		File fileToDeploy = new File(path);
		if (!fileToDeploy.exists()) {
			logger.severe("The file at path '" + path + "' doesn't exist; cannot continue");
			return;
		}
		if (memory == null) {
			memory = 256;
		}
		String failureMessage = "The application '" + appName + "' could not be pushed";
		String successMessage = "The application '" + appName + "' was successfully pushed";
		final Integer finalMem = memory;
		executeCommand(new CloudCommand(failureMessage, successMessage, "Uploading") {
			@Override
			public void execute() throws Exception {
				CloudApplication cloudApplication = getApplication(appName);
				List<String> finalUrls = urls;
				if (finalUrls == null) {
					finalUrls = new ArrayList<String>();
					finalUrls.add(appName + ".cloudfoundry.com");
				}
				if (cloudApplication == null) {
					client.createApplication(appName, CloudApplication.SPRING, finalMem, finalUrls, null, false);
				}
				client.uploadApplication(appName, path);
				Integer finalInstances = instances;
				if (finalInstances == null) {
					finalInstances = 1;
					if (cloudApplication != null) {
						finalInstances = cloudApplication.getInstances();
					}
				}
				client.updateApplicationInstances(appName, finalInstances);
			}
		});
	}

	public void start(final String appName) {
		String failureMessage = "The application '" + appName + "' could not be started";
		String successMessage = "The application '" + appName + "' was successfully started";
		executeCommand(new CloudCommand(failureMessage, successMessage, "Starting") {
			@Override
			public void execute() throws Exception {
				if (getApplication(appName).getState() == CloudApplication.AppState.STARTED) {
					logger.info("Application '" + appName + "' is already running.");
					displaySuccessMessage = false;
					return;
				}
				client.startApplication(appName);
			}
		});
	}

	public void stop(final String appName) {
		String failureMessage = "The application '" + appName + "' could not be stopped";
		String successMessage = "The application '" + appName + "' was successfully stopped";
		executeCommand(new CloudCommand(failureMessage, successMessage, appName) {
			@Override
			public void execute() throws Exception {
				if (getApplication(appName).getState() == CloudApplication.AppState.STOPPED) {
					logger.info("Application '" + appName + "' is not running.");
					displaySuccessMessage = false;
					return;
				}
				client.stopApplication(appName);
			}
		});
	}

	public void restart(final String appName) {
		String failureMessage = "The application '" + appName + "' could not be restarted";
		String successMessage = "The application '" + appName + "' was successfully restarted";
		executeCommand(new CloudCommand(failureMessage, successMessage) {
			@Override
			public void execute() throws Exception {
				client.restartApplication(appName);
			}
		});
	}

	public void delete(final String appName) {
		String failureMessage = "The application '" + appName + "' could not be deleted";
		String successMessage = "The application '" + appName + "' was successfully deleted";
		executeCommand(new CloudCommand(failureMessage, successMessage) {
			@Override
			public void execute() throws Exception {
				client.deleteApplication(appName);
			}
		});
	}

	public void update(String appName) {

	}

	public void instances(final String appName, final String number) {
		executeCommand(new CloudCommand("Retrieving instances for application '" + appName + "' failed") {
			@Override
			public void execute() throws Exception {
				Integer instances = getInteger(number);
				if (instances == null) {
					InstancesInfo instancesInfo = client.getApplicationInstances(appName);
					if (instancesInfo.getInstances().isEmpty()) {
						logger.info("No running instances for '" + appName + "'");
					}
				} else {
					client.updateApplicationInstances(appName, instances);
				}
			}
		});
	}

	public void mem(final String appName, final Integer memSize) {
		executeCommand(new CloudCommand("Updating the memory allocation for application '" + appName + "' failed") {
			@Override
			public void execute() throws Exception {
				if (memSize != null) {
					client.updateApplicationMemory(appName, memSize);
				}
				ShellTableRenderer shellTable = new ShellTableRenderer("Application Memory", "Name", "Memory");
				shellTable.addRow(appName, getApplication(appName).getMemory() + "MB");
				logger.info(shellTable.getOutput());
			}
		});
	}

	public void crashes(final String appName) {
		executeCommand(new CloudCommand("Crashes for application '" + appName + "' could not be retrieved") {
			@Override
			public void execute() throws Exception {
				CrashesInfo crashes = client.getCrashes(appName);
				if (crashes == null) {
					logger.severe(this.failureMessage);
					return;
				}
				if (crashes.getCrashes().isEmpty()) {
					logger.info("The application '" + appName + "' has never crashed");
					return;
				}
				ShellTableRenderer table = new ShellTableRenderer("Crashes", "Name", "Id", "Since");
				for (CrashInfo crash : crashes.getCrashes()) {
					table.addRow(appName, crash.getInstance(), SimpleDateFormat.getDateTimeInstance().format(crash.getSince()));
				}
				logger.info(table.getOutput());
			}
		});
	}

	public void crashLogs(String appName, String instance) {
		logs(appName, instance);
	}

	public void logs(final String appName, final String instance) {
		String failureMessage = "The logs for application '" + appName + "' failed to be retrieved";
		String successMessage = null;
		executeCommand(new CloudCommand(failureMessage, successMessage, "Loading") {
			@Override
			public void execute() throws Exception {
				Integer instanceIndex = getInteger(instance);
				if (instanceIndex == null) {
					instanceIndex = 1;
				}

				String stderrLog = client.getFile(appName, instanceIndex, "logs/stderr.log");
				String stdoutlog = client.getFile(appName, instanceIndex, "logs/stdout.log");

				logger.info("\n");
				logger.info("==== logs/stderr.log ====");
				logger.info("\n");
				logger.info(stderrLog);

				logger.info("\n");
				logger.info("==== logs/stdout.log ====");
				logger.info("\n");
				logger.info(stdoutlog);
				logger.info("\n");
			}
		});
	}

	public void files(final String appName, final String path, final String instance) {
		executeCommand(new CloudCommand("The files failed to be retrieved") {
			@Override
			public void execute() throws Exception {
				Integer instanceIndex = getInteger(instance);
				if (instanceIndex == null) {
					instanceIndex = 1;
				}
				String file = client.getFile(appName, instanceIndex, path);
				logger.info(file);
			}
		});
	}

	public void stats(final String appName) {
		executeCommand(new CloudCommand("The stats for application '" + appName + "' failed to be retrieved") {
			@Override
			public void execute() throws Exception {
				ApplicationStats stats = client.getApplicationStats(appName);
				if (stats.getRecords().isEmpty()) {
					logger.info("There is currently no stats for the application '" + appName + "'");
					return;
				}
				ShellTableRenderer table = new ShellTableRenderer("App. Stats", "Instance", "CPU (Cores)", "Memory (limit)", "Disk (limit)", "Uptime");
				for (InstanceStats instanceStats : stats.getRecords()) {
					String instance = instanceStats.getId();
					InstanceStats.Usage usage = instanceStats.getUsage();
					String cpu = "N/A";
					String memory = "N/A";
					String disk = "N/A";
					if (usage != null) {
						cpu = instanceStats.getUsage().getCpu() + " (" + instanceStats.getCores() + ")";
						memory = roundTwoDecimals(instanceStats.getUsage().getMem() / 1024) + "M (" + instanceStats.getMemQuota() / (1024 * 1024) + "M)";
						disk = roundTwoDecimals(instanceStats.getUsage().getDisk() / (1024 * 1024)) + "M (" + instanceStats.getDiskQuota() / (1024 * 1024) + "M)";
					}
					Double uptime = instanceStats.getUptime();
					if (uptime == null) {
						uptime = 0D;
					}
					String formattedUptime = formatDurationInSeconds(uptime);
					table.addRow(instance, cpu, memory, disk, formattedUptime);
				}
				logger.info(table.getOutput());
			}
		});
	}

	public void map(final String appName, final String url) {
		String failureMessage = "The url failed to be mapped to application '" + appName + "'";
		String successMessage = "The url was successfully mapped to application '" + appName + "'";
		executeCommand(new CloudCommand(failureMessage, successMessage) {
			@Override
			public void execute() throws Exception {
				CloudApplication application = getApplication(appName);
				if (application == null) {
					displaySuccessMessage = false;
					return;
				}
				List<String> uris = new ArrayList<String>(application.getUris());
				uris.add(url);
				client.updateApplicationUris(appName, uris);
			}
		});
	}

	public void unMap(final String appName, final String url) {
		String failureMessage = "The url failed to be unmapped from application '" + appName + "'";
		String successMessage = "The url was successfully unmapped from application '" + appName + "'";
		executeCommand(new CloudCommand(failureMessage, successMessage) {
			@Override
			public void execute() throws Exception {
				CloudApplication application = getApplication(appName);
				if (application == null) {
					displaySuccessMessage = false;
					return;
				}
				List<String> uris = new ArrayList<String>(application.getUris());
				uris.remove(url);
				client.updateApplicationUris(appName, uris);
			}
		});
	}

	public void renameApp(final String appName, final String newAppName) {
		String failureMessage = "The application '" + appName + "'failed to be renamed";
		String successMessage = "The application '" + appName + "' was successfully renamed as '" + newAppName + "'";
		executeCommand(new CloudCommand(failureMessage, successMessage) {
			@Override
			public void execute() throws Exception {
				for (CloudApplication cloudApplication : client.getApplications()) {
					if (cloudApplication.getName().equals(newAppName)) {
						logger.severe("An application of that name already exists, please choose another name");
						displaySuccessMessage = false;
						return;
					}
				}
				client.rename(appName, newAppName);
			}
		});
	}

	public void clearStoredLoginDetails() {
		session.clearStoredLoginDetails();
	}

	public void setup() {
		//TODO: This is where a cloud environment profile would be added to the application config
	}

	public boolean isCloudFoundryCommandAvailable() {
		return client != null;
	}

	public boolean isSetupCommandAvailable() {
		return true;
	}

	private double roundTwoDecimals(double d) {
		DecimalFormat twoDForm = new DecimalFormat("#.##");
		return Double.valueOf(twoDForm.format(d));
	}

	private abstract class CloudCommand {
		protected String failureMessage = "";
		protected String successMessage = "";
		protected String gerund;
		protected boolean displaySuccessMessage = true;

		protected CloudCommand(String failureMessage, String successMessage, String gerund) {
			this.failureMessage = failureMessage;
			this.successMessage = successMessage;
			this.gerund = gerund;
		}

		protected CloudCommand(String failureMessage, String successMessage) {
			this(failureMessage, successMessage, "Performing operation");
		}

		protected CloudCommand(String failureMessage) {
			this(failureMessage, null);
		}

		public abstract void execute() throws Exception;

		public String getFailureMessage() {
			return failureMessage;
		}

		public String getSuccessMessage() {
			return successMessage;
		}

		public String getGerund() {
			return gerund;
		}

		public boolean isDisplaySuccessMessage() {
			return displaySuccessMessage;
		}
	}

	private void executeCommand(final CloudCommand command) {
		Timer timer = new Timer();
		try {
			final char[] statusIndicators = new char[]{'|', '/', '-', '\\'};
			final int[] statusCount = new int[]{0};
			TimerTask timerTask = new TimerTask() {
				@Override
				public void run() {
					flash(Level.FINE, command.getGerund() + " " + statusIndicators[statusCount[0]], MY_SLOT);
					if (statusCount[0] < statusIndicators.length - 1) {
						statusCount[0] = statusCount[0] + 1;
					} else {
						statusCount[0] = 0;
					}
				}
			};
			timer.scheduleAtFixedRate(timerTask, 0, 100);
			command.execute();
			if (StringUtils.hasText(command.getSuccessMessage()) && command.isDisplaySuccessMessage()) {
				logger.info(command.getSuccessMessage());
			}
		} catch (Exception e) {
			throw new IllegalStateException(command.getFailureMessage() + " - " + e.getMessage(), e);
		} finally {
			timer.cancel();
			flash(Level.FINE, "Complete!", MY_SLOT);
			flash(Level.FINE, "", MY_SLOT);
		}
	}

	private String formatDurationInSeconds(Double seconds) {
		long secondsInMinute = 60;
		long secondsInHour = secondsInMinute ^ 2;
		long secondsInDay = secondsInHour * 24;
		StringBuilder sb = new StringBuilder();
		long days = (long) (seconds / secondsInDay);
		sb.append(days).append("d:");
		if (days > 1) {
			double remainder = seconds % secondsInDay;
			long hours = (long) ((remainder) / (secondsInHour));
			sb.append(hours).append("h:");

			remainder = remainder % (secondsInHour);
			long minutes = (long) (remainder / (60));
			sb.append(minutes).append("m:");

			remainder = remainder % (60);
			long secs = (long) (remainder);
			sb.append(secs).append("s");
		}
		return sb.toString();
	}

	private CloudApplication getApplication(String appName) {
		try {
			return client.getApplication(appName);
		} catch (Exception ignored) {
		}
		return null;
	}

	private Integer getInteger(String potentialInt) {
		if (potentialInt == null) {
			return null;
		}
		for (Character c : potentialInt.toCharArray()) {
			if (!Character.isDigit(c)) {
				return null;
			}
		}
		return Integer.valueOf(potentialInt);
	}
}
