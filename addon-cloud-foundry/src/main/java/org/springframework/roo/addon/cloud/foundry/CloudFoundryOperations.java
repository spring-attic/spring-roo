package org.springframework.roo.addon.cloud.foundry;

import java.util.List;

/**
 * Operations offered by Cloud Foundry add-on.
 *
 * @author James Tyrrell
 * @since 1.1.3
 */
public interface CloudFoundryOperations {

	void info();

	void register(String email, String password);

	void login(String email, String password, String cloudControllerUrl);

	void services();

	void createService(String service, String name, String bind);

	void deleteService(String service);

	void bindService(String service, String appName);

	void unbindService(String service, String appName);

	void apps();

	void push(String appName, Integer instances, Integer memory, String path, List<String> urls);

	void start(String appName);

	void stop(String appName);

	void restart(String appName);

	void delete(String appName);

	void update(String appName);

	void instances(String appName, String number);

	void mem(String appName, Integer memSize);

	void crashes(String appName);

	void crashLogs(String appName, String instance);

	void logs(String appName, String instance);

	void files(String appName, String path, String instance);

	void stats(String appName);

	void map(String appName, String url);

	void unMap(String appName, String url);

	void renameApp(String appName, String newAppName);

	/**
	 * Initial setup of Cloud Foundry in target project.
	 */
	void setup();

	/**
	 * Check if Cloud Foundry commands are available in Shell. Depends on the user
	 * being logged into Cloud Foundry.
	 *
	 * @return availability
	 */
	boolean isCloudFoundryCommandAvailable();

	/**
	 * Check if Cloud Foundry setup command is available in Shell.
	 *
	 * @return availability
	 */
	boolean isSetupCommandAvailable();
}
