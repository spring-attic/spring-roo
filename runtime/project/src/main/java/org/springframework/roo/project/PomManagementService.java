package org.springframework.roo.project;

import java.util.Collection;

import org.springframework.roo.project.maven.Pom;

/**
 * Provides {@link Pom}-related methods to the "project" package. Code outside
 * this package should use {@link ProjectOperations}.
 * 
 * @author James Tyrrell
 * @since 1.2.0
 */
interface PomManagementService {

    /**
     * Returns the {@link ProjectDescriptor} of the currently focussed module,
     * or if no module has the focus, the root {@link ProjectDescriptor}.
     * 
     * @return <code>null</code> if none of the above Poms exist
     */
    Pom getFocusedModule();

    /**
     * Returns the name of the currently focussed module.
     * 
     * @return an empty string if no module has the focus.
     */
    String getFocusedModuleName();

    /**
     * @param fileIdentifier the canonical path to lookup
     * @return the module name that represents the module the passed in file
     *         belongs to
     */
    Pom getModuleForFileIdentifier(String fileIdentifier);

    /**
     * Returns the names of the modules of this project
     * 
     * @return a non-<code>null</code> collection
     */
    Collection<String> getModuleNames();

    /**
     * Returns the {@link ProjectDescriptor} for the module with the given name.
     * 
     * @param moduleName the name of the module to look up (can be blank)
     * @return <code>null</code> if there's no such module
     */
    Pom getPomFromModuleName(String moduleName);

    /**
     * Returns the {@link ProjectDescriptor} with the given canonical path
     * 
     * @param canonicalPath the canonical path of the descriptor file
     * @return <code>null</code> if there is no such file
     */
    Pom getPomFromPath(String canonicalPath);

    /**
     * Returns the known {@link ProjectDescriptor}s
     * 
     * @return a non-<code>null</code> copy of this collection
     */
    Collection<Pom> getPoms();

    /**
     * Returns the {@link ProjectDescriptor} associated with the project's root
     * descriptor file
     * 
     * @return <code>null</code> if there's no such POM
     */
    Pom getRootPom();

    /**
     * Focuses on the given module.
     * 
     * @param module the module to focus upon (required)
     */
    void setFocusedModule(Pom module);
}
