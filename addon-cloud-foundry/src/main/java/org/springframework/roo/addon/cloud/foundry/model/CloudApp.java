package org.springframework.roo.addon.cloud.foundry.model;

public class CloudApp {
	private String appName;

	public CloudApp(String appName) {
		this.appName = appName;
	}

	public String getName() {
		return appName;
	}
}
