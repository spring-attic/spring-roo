package org.springframework.roo.project;

import java.util.List;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.packaging.PackagingProvider;
import org.springframework.roo.project.providers.ProjectManagerProviderId;

/**
 * Methods for various project-related operations.
 * 
 * @author Ben Alex
 * @author Juan Carlos García
 * @since 1.0
 */
public interface ProjectService extends ProjectManager{
	
    /**
     * Indicates whether a new project can be created checking all available ProjectManager provider
     * 
     * @return see above
     */
    boolean isCreateProjectAvailable();
    
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
     * @param provider ProjectManager provider to use on the new project
     */
    void createProject(JavaPackage topLevelPackage, String projectName,
            Integer majorJavaVersion,
            PackagingProvider packagingType, ProjectManagerProviderId provider);
	
    /**
     * @return the ProjectManager providers id by name
     */
    ProjectManagerProviderId getProviderIdByName(String value);
    
    /**
     * @return the ProjectManager providers id available for current project
     */
    List<ProjectManagerProviderId> getProvidersId();
    
    /**
     * Indicates whether the supplied feature is installed in any module of a
     * project.
     * 
     * @param featureName the name of the feature (see {@link FeatureNames} for
     *            available features)
     * @return true if the feature is installed in any module, otherwise false
     */
    boolean isFeatureInstalled(String featureName);

    /**
     * Indicates whether the supplied feature is installed in the module with
     * the supplied name.
     * 
     * @param featureName the name of the feature (see {@link FeatureNames} for
     *            available features)
     * @param moduleName the name of the module to be checked
     * @return true if the feature is installed the module, otherwise false
     */
    boolean isFeatureInstalledInModule(String featureName, String moduleName);

    /**
     * Indicates whether any of the supplied features are installed in the
     * focused module.
     * 
     * @param featureNames the names of the features (see {@link FeatureNames}
     *            for available features)
     * @return true if any of the supplied features are installed in the focused
     *         module, otherwise false
     */
    boolean isFeatureInstalledInFocusedModule(String... featureNames);
}