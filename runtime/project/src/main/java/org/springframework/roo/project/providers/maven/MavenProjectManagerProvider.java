package org.springframework.roo.project.providers.maven;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.providers.ProjectManagerProvider;

/**
 * ProjectManager provider based on Maven.
 * 
 * This provider is only available on projects which uses Maven dependency
 * management.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */

@Component
@Service
public class MavenProjectManagerProvider implements ProjectManagerProvider {
	
	private static String PROVIDER_NAME = "MAVEN";
    private static String PROVIDER_DESCRIPTION = "ProjectManager provider based on Maven dependency manager";

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public boolean isActive() {
		return true;
	}

	@Override
	public String getName() {
		return PROVIDER_NAME;
	}

	@Override
	public String getDescription() {
		return PROVIDER_DESCRIPTION;
	}

	@Override
	public void createProject() {
		// TODO
	}

}
