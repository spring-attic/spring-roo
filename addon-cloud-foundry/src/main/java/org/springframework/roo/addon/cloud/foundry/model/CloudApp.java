package org.springframework.roo.addon.cloud.foundry.model;

public class CloudApp {
    private final String appName;

    public CloudApp(final String appName) {
        this.appName = appName;
    }

    public String getName() {
        return appName;
    }
}
