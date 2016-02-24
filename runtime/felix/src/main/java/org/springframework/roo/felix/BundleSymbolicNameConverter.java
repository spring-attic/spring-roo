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
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link BundleSymbolicName}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class BundleSymbolicNameConverter implements
        Converter<BundleSymbolicName> {

    private ComponentContext context;
    // Handler service field is solely to ensure it starts before
    // BundleSymbolicNameConverter
    @Reference protected HttpPgpUrlStreamHandlerService handlerService;
    @Reference private RepositoryAdmin repositoryAdmin;

    protected void activate(final ComponentContext context) {
        this.context = context;
    }

    public BundleSymbolicName convertFromText(final String value,
            final Class<?> requiredType, final String optionContext) {
        return new BundleSymbolicName(value.trim());
    }

    protected void deactivate(final ComponentContext context) {
        this.context = null;
    }

    public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> requiredType, final String originalUserInput,
            final String optionContext, final MethodTarget target) {
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
            final Bundle[] bundles = context.getBundleContext().getBundles();
            if (bundles != null) {
                for (final Bundle bundle : bundles) {
                    final String bsn = bundle.getSymbolicName();
                    if (bsn != null && bsn.startsWith(originalUserInput)) {
                        completions.add(new Completion(bsn));
                    }
                }
            }
        }

        if (obr) {
            final Repository[] repositories = repositoryAdmin
                    .listRepositories();
            if (repositories != null) {
                for (final Repository repository : repositories) {
                    final Resource[] resources = repository.getResources();
                    if (resources != null) {
                        for (final Resource resource : resources) {
                            if (resource.getSymbolicName().startsWith(
                                    originalUserInput)) {
                                completions.add(new Completion(resource
                                        .getSymbolicName()));
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    public boolean supports(final Class<?> requiredType,
            final String optionContext) {
        return BundleSymbolicName.class.isAssignableFrom(requiredType);
    }
}