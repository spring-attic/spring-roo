package org.springframework.roo.addon.cloud.foundry.model;

public class CloudControllerUrl {

    private final String url;

    /**
     * Constructor
     * 
     * @param url
     */
    public CloudControllerUrl(final String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
