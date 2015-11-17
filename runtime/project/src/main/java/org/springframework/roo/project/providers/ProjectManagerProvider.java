package org.springframework.roo.project.providers;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.packaging.PackagingProvider;

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
     * Creates new project using a ProjectManager provider
     * 
     * @param topLevelPackage the top-level Java package (required)
     * @param projectName the name of the project (can be blank to generate it
     *            from the top-level package)
     * @param majorJavaVersion the major Java version to which this project is
     *            targetted (can be <code>null</code> to autodetect)
     * @param packagingType the packaging of the project (can be
     *            <code>null</code> to use the default)
     */
    void createProject(JavaPackage topLevelPackage, String projectName,
            Integer majorJavaVersion,
            PackagingProvider packagingType);

}