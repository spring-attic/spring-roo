package org.springframework.roo.project;

import java.util.Map;
import java.util.Set;

import org.springframework.roo.project.maven.Pom;

public interface PomManagementService {

	/**
	 * @param pomPath the canonical path of the pom.xml file
	 * @return the {@link Pom} associated with the passed in path
	 */
	Pom getPomFromPath(String pomPath);

	/**
	 * @param moduleName the name of the module to lookup
	 * @return the {@link Pom} associated with the passed in module name
	 */
	Pom getPomFromModuleName(String moduleName);

	/**
	 * @return a map whose key is the pom.xml path and value is the associated {@link Pom}
	 */
	Map<String, Pom> getPomMap();

	/**
	 * @return the {@link Pom} associated with the root pom.xml file
	 */
	Pom getRootPom();

	/**
	 * @return the currently focused {@link Pom}
	 */
	Pom getFocusedModule();

	/**
	 * @return the name of the currently focused module
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
}
