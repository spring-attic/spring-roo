package org.springframework.roo.addon.cloud.foundry;

import java.util.List;

/**
 * Operations offered by Cloud Foundry add-on.
 * 
 * @author James Tyrrell
 * @since 1.1.3
 */
public interface CloudFoundryOperations {

    void apps();

    void bindService(String service, String appName);

    void clearStoredLoginDetails();

    void crashes(String appName);

    void crashLogs(String appName, String instance);

    void createService(String service, String name, String bind);

    void delete(String appName);

    void deleteService(String service);

    void files(String appName, String path, String instance);

    void info();

    void instances(String appName, String number);

    /**
     * Check if Cloud Foundry commands are available in Shell. Depends on the
     * user being logged into Cloud Foundry.
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

    void login(String email, String password, String cloudControllerUrl);

    void logs(String appName, String instance);

    void map(String appName, String url);

    void mem(String appName, Integer memSize);

    void push(String appName, Integer instances, Integer memory, String path,
            List<String> urls);

    void register(String email, String password);

    void renameApp(String appName, String newAppName);

    void restart(String appName);

    void services();

    /**
     * Initial setup of Cloud Foundry in target project.
     */
    void setup();

    void start(String appName);

    void stats(String appName);

    void stop(String appName);

    void unbindService(String service, String appName);

    void unMap(String appName, String url);

    void update(String appName);
}
