package org.springframework.roo.project.settings;

import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link ProjectSettingsOperations}.
 *
 * @author Paula Navarro
 * @since 2.0
 */
@Component
@Service
public class ProjectSettingsOperationsImpl implements ProjectSettingsOperations {

	protected final static Logger LOGGER = HandlerUtils.getLogger(ProjectSettingsOperationsImpl.class);

	ProjectSettingsService projectSettingsService;

	// ------------ OSGi component attributes ----------------
	private BundleContext context;

	protected void activate(final ComponentContext context) {
		this.context = context.getBundleContext();
	}

	@Override
	public void addSetting(String name, String value, boolean force) {

		Validate.notNull(name, "Name required");
		Validate.notNull(value, "Value required");

		// Checks if project settings file exists
		if (!getProjectSettingsService().existsProjectSettingsFile()) {

			// Creates project settings file
			getProjectSettingsService().createProjectSettingsFile();
		}

		// Adds a setting
		getProjectSettingsService().addProperty(name, value, force);
	}

	@Override
	public void listSettings() {

		// Checks if project settings file exists
		if (getProjectSettingsService().existsProjectSettingsFile()) {

			SortedSet<String> properties = getProjectSettingsService().getPropertyKeys(true);

			// Print results
			for (String property : properties) {
				LOGGER.log(Level.INFO, property);
			}
		} else {
			LOGGER.log(Level.INFO,
					"WARNING: Project settings file not found. Use 'project settings add' command to configure your project.");
		}

	}

	public ProjectSettingsService getProjectSettingsService() {
		if (projectSettingsService == null) {
			// Get all Services implement ProjectSettingsServic interface
			try {
				ServiceReference<?>[] references = this.context
						.getAllServiceReferences(ProjectSettingsService.class.getName(), null);

				for (ServiceReference<?> ref : references) {
					return (ProjectSettingsService) this.context.getService(ref);
				}

				return null;

			} catch (InvalidSyntaxException e) {
				LOGGER.warning("Cannot load ProjectSettingsService on ProjectSettingsOperationsImpl.");
				return null;
			}
		} else {
			return projectSettingsService;
		}

	}

}