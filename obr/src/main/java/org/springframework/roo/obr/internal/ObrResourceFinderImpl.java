package org.springframework.roo.obr.internal;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Resource;
import org.springframework.roo.obr.ObrResourceFinder;
import org.springframework.roo.url.stream.UrlInputStreamService;

/**
 * Default implementation of {@link ObrResourceFinder}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
@Component(immediate = true) // we want the background download to start ASAP
@Service
public class ObrResourceFinderImpl implements ObrResourceFinder {
	@Reference private RepositoryAdmin repositoryAdmin;
	@Reference private UrlInputStreamService urlInputStreamService;
	private boolean obrRepositoriesDownloaded = false;

	protected void activate(ComponentContext context) {
		// Do a quick background query so we have the results cached
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					// Do not proceed if the user appears disconnect from the network (ROO-1169)
					urlInputStreamService.openConnection(new URL("http://www.springsource.org/roo"));
					
					// To get this far the network appears connected, so let's proceed
					repositoryAdmin.listRepositories();
					obrRepositoriesDownloaded = true;
				} catch (Throwable ignore) {}
			}
		}, "OBR Resource Finder Eager Download");
		t.start();
	}

	public List<Resource> getKnownResources() {
		if (!obrRepositoriesDownloaded) {
			return null;
		}

		List<Resource> result = new ArrayList<Resource>();
		for (Repository repo : repositoryAdmin.listRepositories()) {
			for (Resource resource : repo.getResources()) {
				result.add(resource);
			}
		}

		return result;
	}

	public Integer getRepositoryCount() {
		if (!obrRepositoriesDownloaded) {
			return null;
		}

		return repositoryAdmin.listRepositories().length;
	}
}
