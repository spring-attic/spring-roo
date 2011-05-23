package org.springframework.roo.felix;

import java.util.List;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link BundleSymbolicName}.
 *
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component
@Service
public class BundleSymbolicNameConverter implements Converter<BundleSymbolicName> {

	// handler service field is solely to ensure it starts before BundleSymbolicNameConverter
	@Reference protected HttpPgpUrlStreamHandlerService handlerService;
	@Reference private RepositoryAdmin repositoryAdmin;
	private ComponentContext context;
	
	protected void activate(ComponentContext context) {
		this.context = context;
	}
	
	protected void deactivate(ComponentContext context) {
		this.context = null;
	}

	public BundleSymbolicName convertFromText(String value, Class<?> requiredType, String optionContext) {
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