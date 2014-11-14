package org.springframework.roo.addon.jpa;

import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;

import org.osgi.service.component.ComponentContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link DatabaseDotComOperations}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component
@Service
public class DatabaseDotComOperationsImpl implements DatabaseDotComOperations {

protected final static Logger LOGGER = HandlerUtils.getLogger(GaeOperationsImpl.class);
	
	// ------------ OSGi component attributes ----------------
   	private BundleContext context;
	
    private ProjectOperations projectOperations;
    
    protected void activate(final ComponentContext context) {
    	this.context = context.getBundleContext();
    }

    public String getName() {
        return FeatureNames.DATABASE_DOT_COM;
    }

    public boolean isInstalledInModule(final String moduleName) {
    	
    	if(projectOperations == null){
    		projectOperations = getProjectOperations();
    	}
    	
    	Validate.notNull(projectOperations, "ProjectOperations is required");
    	
        final Pom pom = projectOperations.getPomFromModuleName(moduleName);
        if (pom == null) {
            return false;
        }
        for (final Plugin buildPlugin : pom.getBuildPlugins()) {
            if ("com.force.sdk".equals(buildPlugin.getArtifactId())) {
                return true;
            }
        }
        return false;
    }
    
    public ProjectOperations getProjectOperations(){
    	// Get all Services implement ProjectOperations interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (ProjectOperations) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load ProjectOperations on DatabaseDotComOperationsImpl.");
			return null;
		}
    }
}
