package org.springframework.roo.project;

import java.util.Collection;
import java.util.Set;

import org.springframework.roo.project.maven.Pom;

public interface PomManagementService {

	/**
	 * Returns the {@link Pom} for the given canonical path
	 * 
	 * @param pomPath the canonical path of the pom.xml file
	 * @return <code>null</code> if there is no such Pom
	 */
	Pom getPomFromPath(String pomPath);

	/**
	 * Returns the {@link Pom} for the module with the given name.
	 * 
	 * @param moduleName the name of the module to look up (can be blank)
	 * @return <code>null</code> if there's no such module
	 */
	Pom getPomFromModuleName(String moduleName);

	/**
	 * Returns the {@link Pom} associated with the project's root pom.xml file
	 * 
	 * @return <code>null</code> if there's no such POM
	 */
	Pom getRootPom();

	/**
	 * Returns the {@link Pom} of the currently focussed module, or if no module
	 * has the focus, the root {@link Pom}.
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
	 * @param focusedModule the {@link Pom} to focus
	 */
	void setFocusedModule(Pom focusedModule);

	/**
	 *
	 * @param focusedModule the canonical path of the module's pom.xml file to focus
	 */
	void setFocusedModule(String focusedModule);

	/**
	 * @param fileIdentifier the canonical path to lookup
	 * @return the module name that represents the module the passed in file belongs to
	 */
	Pom getModuleForFileIdentifier(String fileIdentifier);


	/**
	 * @return a set of all available module names
	 */
	Set<String> getModuleNames();

	/**
	 * Returns the known {@link Pom}s
	 * 
	 * @return a non-<code>null</code> copy of this collection
	 */
	Collection<Pom> getPoms();
}
