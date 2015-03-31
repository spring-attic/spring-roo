package org.springframework.roo.obr.addon.search;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * 
 * ObrRepositoryOperations implementation that manage OBR Repositories on Spring
 * Roo Shell
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
@Component
@Service
public class ObrRepositoryOperationsImpl implements ObrRepositoryOperations {

	private BundleContext context;
	private static final Logger LOGGER = HandlerUtils
			.getLogger(ObrRepositoryOperationsImpl.class);

	private RepositoryAdmin repositoryAdmin;
	private List<Repository> repositories;

	protected void activate(final ComponentContext cContext) {
		context = cContext.getBundleContext();
		repositories = new ArrayList<Repository>();
	}

	/**
	 * Method to populate current Repositories using OSGi Serive
	 */
	private void populateRepositories() {

		// Cleaning Repositories
		repositories.clear();

		// Validating that RepositoryAdmin exists
		Validate.notNull(getRepositoryAdmin(), "RepositoryAdmin not found");

		for (Repository repo : getRepositoryAdmin().listRepositories()) {
			repositories.add(repo);
		}
	}

	@Override
	public void addRepository(String url) throws Exception {
		LOGGER.log(Level.INFO, "Adding repository " + url + "...");
		getRepositoryAdmin().addRepository(url);
		LOGGER.log(Level.INFO, "Repository '" + url + "' added!");

	}

	@Override
	public void removeRepo(String url) {
		LOGGER.log(Level.INFO, "Removing repository " + url + "...");
		getRepositoryAdmin().removeRepository(url);
		LOGGER.log(Level.INFO, "Repository '" + url + "' removed!");

	}

	@Override
	public void listRepos() {
		LOGGER.log(Level.INFO, "Getting current Repositories...");
		// Populating repositories
		populateRepositories();
		LOGGER.log(Level.INFO, "");
		// Printing Installed repositories
		for (Repository repo : repositories) {
			LOGGER.log(Level.INFO, "   " + repo.getURI());
		}
		LOGGER.log(Level.INFO, "");
		LOGGER.log(Level.INFO, String.format(
				"%s installed repositories on Spring Roo were found",
				getRepositoryAdmin().listRepositories().length));
	}

	/**
	 * Method to get RepositoryAdmin Service implementation
	 * 
	 * @return
	 */
	public RepositoryAdmin getRepositoryAdmin() {
		if (repositoryAdmin == null) {
			// Get all Services implement RepositoryAdmin interface
			try {
				ServiceReference<?>[] references = context
						.getAllServiceReferences(
								RepositoryAdmin.class.getName(), null);

				for (ServiceReference<?> ref : references) {
					repositoryAdmin = (RepositoryAdmin) context.getService(ref);
					return repositoryAdmin;
				}

				return null;

			} catch (InvalidSyntaxException e) {
				LOGGER.warning("Cannot load RepositoryAdmin on AddonSearchImpl.");
				return null;
			}
		} else {
			return repositoryAdmin;
		}
	}
}
