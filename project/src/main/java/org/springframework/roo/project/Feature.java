package org.springframework.roo.project;

/**
 * Implemented by classes to identify them as an installed project feature.
 * <p>
 * For example, add-ons such as JSF and MVC can each use the
 * {@link Feature#isInstalledInModule(String)} to verify that they can be
 * installed in the current module.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public interface Feature {

    String getName();

    boolean isInstalledInModule(String moduleName);
}
