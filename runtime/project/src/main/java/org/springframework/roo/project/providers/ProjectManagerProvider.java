package org.springframework.roo.project.providers;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

/**
 * Interface for Project Manager providers
 * <p/>
 * Providers must implements this interface and must be annotated with
 * {@link Component} and {@link Service}.
 * <p/>
 * Provider registry will be done automatically by the OSGi injection mechanism
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public interface ProjectManagerProvider {

    /**
     * @return if this provider can be configured on current project
     */
    boolean isAvailable();

    /**
     * @return if this provider is the provider set on current project. <b>Just
     *         ONLY ONE provider can return true at time</b>
     */
    boolean isActive();

    /**
     * @return Name for this Provider
     */
    String getName();

    /**
     * @return Description of this provider
     */
    String getDescription();
    
    /**
     * Performs the requiered operation to create new project using this provider
     */
    void createProject();

}