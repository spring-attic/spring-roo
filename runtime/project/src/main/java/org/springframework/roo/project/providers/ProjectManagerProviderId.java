package org.springframework.roo.project.providers;

/**
 * Immutable representation of a {@link ProjectManagerProvider}
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class ProjectManagerProviderId {

    private String name;
    private String description;
    private String className;

    public ProjectManagerProviderId(ProjectManagerProvider provider) {
        this.name = provider.getName();
        this.description = provider.getDescription();
        this.className = provider.getClass().getCanonicalName();
    }

    /**
     * @return provider identifier
     */
    public String getId() {
        return this.name;
    }

    /**
     * @return provider description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param provider
     * @return if provider is current provider
     */
    public boolean is(ProjectManagerProvider provider) {
        return name.equals(provider.getName())
                && className.equals(provider.getClass().getCanonicalName());
    }

}
