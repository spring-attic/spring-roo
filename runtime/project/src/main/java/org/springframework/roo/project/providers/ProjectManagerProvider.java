package org.springframework.roo.project.providers;

import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.Feature;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.ProjectManager;
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
public interface ProjectManagerProvider extends ProjectManager{

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
    
    /**
     * Indicates whether the supplied feature is installed in any module of a
     * project.
     * 
     * @param featureName the name of the feature (see {@link FeatureNames} for
     *            available features)
     * @param features 
     * 			map where featureName should be defined
     * @return true 
     * 			if the feature is installed in any module, otherwise false
     */
    boolean isFeatureInstalled(String featureName, Map<String, Feature> features);
    
    /**
     * Indicates whether the supplied feature is installed in the module with
     * the supplied name.
     * 
     * @param featureName the name of the feature (see {@link FeatureNames} for
     *            available features)
     * @param moduleName the name of the module to be checked
     * @param features 
     * 			map where featureName should be defined
     * @return true if the feature is installed the module, otherwise false
     */
    boolean isFeatureInstalledInModule(String featureName, String moduleName, Map<String, Feature> features);
    
    
    /**
     * Indicates whether any of the supplied features are installed in the
     * focused module.
     * 
     * @param features 
     * 			map where featureName should be defined
     * @param featureNames the names of the features (see {@link FeatureNames}
     *            for available features)
     * @return true if any of the supplied features are installed in the focused
     *         module, otherwise false
     */
    boolean isFeatureInstalledInFocusedModule(Map<String, Feature> features, String... featureNames);
    
}