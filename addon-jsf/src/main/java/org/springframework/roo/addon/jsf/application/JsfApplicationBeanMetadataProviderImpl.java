package org.springframework.roo.addon.jsf.application;

import static org.springframework.roo.model.RooJavaType.ROO_JSF_APPLICATION_BEAN;
import static org.springframework.roo.model.RooJavaType.ROO_JSF_MANAGED_BEAN;

import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.jsf.managedbean.JsfManagedBeanMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link JsfApplicationBeanMetadataProvider}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component
@Service
public class JsfApplicationBeanMetadataProviderImpl extends
        AbstractItdMetadataProvider implements
        JsfApplicationBeanMetadataProvider {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(JsfApplicationBeanMetadataProviderImpl.class);
	
    private ConfigurableMetadataProvider configurableMetadataProvider;
    private ProjectOperations projectOperations;

    // Stores the MID (as accepted by this JsfApplicationBeanMetadataProvider)
    // for the one (and only one) application-wide menu bean
    private String applicationBeanMid;

    protected void activate(final ComponentContext cContext) {
    	context = cContext.getBundleContext();
        getMetadataDependencyRegistry().registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        getMetadataDependencyRegistry().registerDependency(
                JsfManagedBeanMetadata.getMetadataIdentiferType(),
                getProvidesType());
        addMetadataTrigger(ROO_JSF_APPLICATION_BEAN);
        getConfigurableMetadataProvider()
                .addMetadataTrigger(ROO_JSF_APPLICATION_BEAN);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return JsfApplicationBeanMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        getMetadataDependencyRegistry().deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        getMetadataDependencyRegistry().deregisterDependency(
                JsfManagedBeanMetadata.getMetadataIdentiferType(),
                getProvidesType());
        removeMetadataTrigger(ROO_JSF_APPLICATION_BEAN);
        getConfigurableMetadataProvider()
                .removeMetadataTrigger(ROO_JSF_APPLICATION_BEAN);
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = JsfApplicationBeanMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = JsfApplicationBeanMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "ApplicationBean";
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
    	
    	if(projectOperations == null){
    		projectOperations = getProjectOperations();
    	}
    	Validate.notNull(projectOperations, "ProjectOperations is required");
    	
        applicationBeanMid = metadataIdentificationString;

        // To get here we know the governor is the MenuBean so let's go ahead
        // and create its ITD
        final Set<ClassOrInterfaceTypeDetails> managedBeans = getTypeLocationService()
                .findClassesOrInterfaceDetailsWithAnnotation(ROO_JSF_MANAGED_BEAN);
        for (final ClassOrInterfaceTypeDetails managedBean : managedBeans) {
            getMetadataDependencyRegistry().registerDependency(
                    managedBean.getDeclaredByMetadataId(),
                    metadataIdentificationString);
        }

        final ProjectMetadata projectMetadata = projectOperations
                .getFocusedProjectMetadata();
        Validate.notNull(projectMetadata, "Project metadata required");

        return new JsfApplicationBeanMetadata(metadataIdentificationString,
                aspectName, governorPhysicalTypeMetadata, managedBeans,
                projectMetadata.getPom().getDisplayName());
    }

    public String getProvidesType() {
        return JsfApplicationBeanMetadata.getMetadataIdentiferType();
    }

    @Override
    protected String resolveDownstreamDependencyIdentifier(
            final String upstreamDependency) {
        if (MetadataIdentificationUtils.getMetadataClass(upstreamDependency)
                .equals(MetadataIdentificationUtils
                        .getMetadataClass(JsfManagedBeanMetadata
                                .getMetadataIdentiferType()))) {
            // A JsfManagedBeanMetadata upstream MID has changed or become
            // available for the first time
            // It's OK to return null if we don't yet know the MID because its
            // JavaType has never been found
            return applicationBeanMid;
        }

        // It wasn't a JsfManagedBeanMetadata, so we can let the superclass
        // handle it
        // (it's expected it would be a PhysicalTypeIdentifier notification, as
        // that's the only other thing we registered to receive)
        return super.resolveDownstreamDependencyIdentifier(upstreamDependency);
    }
    
    public ConfigurableMetadataProvider getConfigurableMetadataProvider(){
    	if(configurableMetadataProvider == null){
    		// Get all Services implement ConfigurableMetadataProvider interface
    		try {
    			ServiceReference<?>[] references = context.getAllServiceReferences(ConfigurableMetadataProvider.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (ConfigurableMetadataProvider) context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load ConfigurableMetadataProvider on JsfApplicationBeanMetadataProviderImpl.");
    			return null;
    		}
    	}else{
    		return configurableMetadataProvider;
    	}
    	
    }
    
    public ProjectOperations getProjectOperations(){
    	// Get all Services implement ProjectOperations interface
		try {
			ServiceReference<?>[] references = context.getAllServiceReferences(ProjectOperations.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (ProjectOperations) context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load ProjectOperations on JsfApplicationBeanMetadataProviderImpl.");
			return null;
		}
    }
}