package org.springframework.roo.obr.addon.search;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
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
	private ConfigurationAdmin configurationAdmin;
	private Configuration config;
	private Dictionary installedRepos;

	protected void activate(final ComponentContext cContext) throws Exception {
		context = cContext.getBundleContext();
		repositories = new ArrayList<Repository>();
		
		config = getConfigurationAdmin().getConfiguration(
			    "installedRepositories");
		installedRepos = config.getProperties();
		if (installedRepos == null) {
			installedRepos = new Hashtable();
		}

		populateRepositories();
	}

	/**
	 * Method to populate current Repositories using OSGi Service
	 * @throws Exception 
	 */
	private void populateRepositories() throws Exception {
		
		// Cleaning Repositories
		repositories.clear();

		// Validating that RepositoryAdmin exists
		Validate.notNull(getRepositoryAdmin(), "RepositoryAdmin not found");
		
		// Checking if exists installed Repos and adding to repositories
		Enumeration persistedRepos = installedRepos.keys();
		while(persistedRepos.hasMoreElements()){
			String repositoryURL = (String) persistedRepos.nextElement();
			// Checking if is a valid URL
			if(repositoryURL.startsWith("http") || repositoryURL.startsWith("file")){
				// Installing persisted repositories 
				getRepositoryAdmin().addRepository(repositoryURL);
			}
		}
		
		for (Repository repo : getRepositoryAdmin().listRepositories()) {
			repositories.add(repo);
		}
	}
	
	@Override
	public void addRepository(String url) throws Exception {
		LOGGER.log(Level.INFO, "Adding repository " + url + "...");
		getRepositoryAdmin().addRepository(url);
		// Including repos into installed Repos list
		installedRepos.put(url, "");
		// Updating configuration service with installed repos
		config.update(installedRepos);
		LOGGER.log(Level.INFO, "Repository '" + url + "' added!");
	}

	@Override
	public void removeRepo(String url) throws Exception {
		LOGGER.log(Level.INFO, "Removing repository " + url + "...");
		getRepositoryAdmin().removeRepository(url);
		// Removing repos from installed Repos list
		installedRepos.remove(url);
		// Updating configuration service with installed repos
		config.update(installedRepos);
		LOGGER.log(Level.INFO, "Repository '" + url + "' removed!");

	}

	@Override
	public void listRepos() throws Exception {
		LOGGER.log(Level.INFO, "Getting current installed repositories...");
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
	
	@Override
	public void introspectRepos() throws Exception {
		LOGGER.log(Level.INFO, "Getting available bundles on installed repositories...");
		// Populating repositories
		populateRepositories();
		LOGGER.log(Level.INFO, "");
		// Getting all names of installed bundles 
		List<String> installedBundles = getNamesOfInstalledBundles();
		// Getting all addons installed on repositories
		int totalAddons = 0;
		LOGGER.log(Level.INFO, "Status               Bundle Description and version");
		LOGGER.log(Level.INFO, "-------------------------------------------------------------------------------");
		for (Repository repo : repositories) {
			Resource[] allResources = repo.getResources();
			// Getting all resources for repo
			for(Resource resource : allResources){
				String status = "";
				// Checking if resource is installed or not
				if(installedBundles.indexOf(resource.getSymbolicName()) != -1){
					status = "Installed    ";
				}else{
					status = "Not Installed";
				}
				// Printing all resources
				LOGGER.log(Level.INFO, status + "        "  + resource.getPresentationName() + " (" + resource.getVersion() + ")");
				totalAddons++;
			}
		}
		LOGGER.log(Level.INFO, "");
		LOGGER.log(Level.INFO, String.format(
				"%s available bundles on installed repositories were found",
				totalAddons));
	}

	/**
	 * Method to get all names of installed bundles on 
	 * Spring Roo Shell
	 * 
	 * @return list with all names of installed bundles
	 */
	private List<String> getNamesOfInstalledBundles() {
		List<String> names = new ArrayList<String>();
		// Getting all installed bundles
		Bundle[] bundles = context.getBundles();
		for(Bundle bundle : bundles){
			names.add(bundle.getSymbolicName());
		}
		return names;
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
				LOGGER.warning("Cannot load RepositoryAdmin on ObrRepositoryOperationsImpl.");
				return null;
			}
		} else {
			return repositoryAdmin;
		}
	}
	
	/**
	 * Method to get ConfigurationAdmin Service implementation
	 * 
	 * @return
	 */
	public ConfigurationAdmin getConfigurationAdmin() {
		if (configurationAdmin == null) {
			// Get all Services implement ConfigurationAdmin interface
			try {
				ServiceReference<?>[] references = context
						.getAllServiceReferences(
								ConfigurationAdmin.class.getName(), null);

				for (ServiceReference<?> ref : references) {
					configurationAdmin = (ConfigurationAdmin) context.getService(ref);
					return configurationAdmin;
				}

				return null;

			} catch (InvalidSyntaxException e) {
				LOGGER.warning("Cannot load ConfigurationAdmin on ObrRepositoryOperationsImpl.");
				return null;
			}
		} else {
			return configurationAdmin;
		}
	}
}
