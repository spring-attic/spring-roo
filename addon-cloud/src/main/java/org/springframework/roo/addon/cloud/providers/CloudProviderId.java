package org.springframework.roo.addon.cloud.providers;

/**
 * Cloud Provider Class
 * 
 * @author Juan Carlos García del Canto
 * @since 1.2.6
 */
public class CloudProviderId {

    private String name;
    private String description;
    private String className;

    public CloudProviderId(CloudProvider provider) {
        this.name = provider.getName();
        this.description = provider.getDescription();
        this.className = provider.getClass().getCanonicalName();
    }

    public String getId() {
        return this.name;
    }

    public String getDescription() {
        return description;
    }

    public boolean is(CloudProvider provider) {
        return name.equals(provider.getName())
                && className.equals(provider.getClass().getCanonicalName());
    }

}
