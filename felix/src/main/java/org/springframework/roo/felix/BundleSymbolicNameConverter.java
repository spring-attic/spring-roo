package org.springframework.roo.felix;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Resource;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link BundleSymbolicName}.
 *
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component(immediate=true) // immediate true so the download starts before the first user command
@Service
public class BundleSymbolicNameConverter implements Converter {

	@Reference private RepositoryAdmin repositoryAdmin;
	private ComponentContext context;
	
	protected void activate(ComponentContext context) {
		this.context = context;
		// Do a quick background query so we have the results cached and ready to roll
		Thread t = new Thread(new Runnable() {
			public void run() {
				repositoryAdmin.listRepositories();
			}
		}, "OBR Eager Download");
		t.start();
	}
	
	protected void deactivate(ComponentContext context) {
		this.context = null;
	}

	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		return new BundleSymbolicName(value.trim());
	}
	
	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String originalUserInput, String optionContext, MethodTarget target) {
		boolean local = false;
		boolean obr = false;
		
		if ("".equals(optionContext)) {
			local = true;
		}
		
		if (optionContext.contains("local")) {
			local = true;
		}
		
		if (optionContext.contains("obr")) {
			obr = true;
		}
		
		if (local) {
			Bundle[] bundles = this.context.getBundleContext().getBundles();
			if (bundles != null) {
				for (Bundle bundle : bundles) {
					String bsn = bundle.getSymbolicName();
					if (bsn != null && bsn.startsWith(originalUserInput)) {
						completions.add(bsn);
					}
				}
			}
		}
		
		if (obr) {
			Repository[] repositories = repositoryAdmin.listRepositories();
			if (repositories != null) {
				for (Repository repository : repositories) {
					Resource[] resources = repository.getResources();
					if (resources != null) {
						for (Resource resource : resources) {
							if (resource.getSymbolicName().startsWith(originalUserInput)) {
								completions.add(resource.getSymbolicName());
							}
						}
					}
				}
			}
		}

		return false;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return BundleSymbolicName.class.isAssignableFrom(requiredType);
	}

}