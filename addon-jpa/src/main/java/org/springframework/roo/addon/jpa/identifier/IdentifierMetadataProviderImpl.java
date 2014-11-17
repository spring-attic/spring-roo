package org.springframework.roo.addon.jpa.identifier;

import static org.springframework.roo.model.RooJavaType.ROO_IDENTIFIER;

import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.jpa.AbstractIdentifierServiceAwareMetadataProvider;
import org.springframework.roo.addon.serializable.SerializableMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Implementation of {@link IdentifierMetadataProvider}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class IdentifierMetadataProviderImpl extends
        AbstractIdentifierServiceAwareMetadataProvider implements
        IdentifierMetadataProvider {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(IdentifierMetadataProviderImpl.class);
	
    private ConfigurableMetadataProvider configurableMetadataProvider;
    private ProjectOperations projectOperations;
    private SerializableMetadataProvider serializableMetadataProvider;

    protected void activate(final ComponentContext cContext) {
    	context = cContext.getBundleContext();
        getMetadataDependencyRegistry().registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        addMetadataTrigger(ROO_IDENTIFIER);
        getConfigurableMetadataProvider().addMetadataTrigger(ROO_IDENTIFIER);
        getSerializableMetadataProvider().addMetadataTrigger(ROO_IDENTIFIER);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return IdentifierMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        getMetadataDependencyRegistry().deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        removeMetadataTrigger(ROO_IDENTIFIER);
        getConfigurableMetadataProvider().removeMetadataTrigger(ROO_IDENTIFIER);
        getSerializableMetadataProvider().removeMetadataTrigger(ROO_IDENTIFIER);
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = IdentifierMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = IdentifierMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "Identifier";
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
    	
        final IdentifierAnnotationValues annotationValues = new IdentifierAnnotationValues(
                governorPhysicalTypeMetadata);
        if (!annotationValues.isAnnotationFound()) {
            return null;
        }

        // We know governor type details are non-null and can be safely cast
        final JavaType javaType = IdentifierMetadata
                .getJavaType(metadataIdentificationString);
        final List<Identifier> identifierServiceResult = getIdentifiersForType(javaType);

        final LogicalPath path = PhysicalTypeIdentifierNamingUtils
                .getPath(metadataIdentificationString);
        if (projectOperations.isProjectAvailable(path.getModule())) {
            // If the project itself changes, we want a chance to refresh this
            // item
            getMetadataDependencyRegistry().registerDependency(
                    ProjectMetadata.getProjectIdentifier(path.getModule()),
                    metadataIdentificationString);
        }

        return new IdentifierMetadata(metadataIdentificationString, aspectName,
                governorPhysicalTypeMetadata, annotationValues,
                identifierServiceResult);
    }

    public String getProvidesType() {
        return IdentifierMetadata.getMetadataIdentifierType();
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
    			LOGGER.warning("Cannot load ConfigurableMetadataProvider on IdentifierMetadataProviderImpl.");
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
			LOGGER.warning("Cannot load ProjectOperations on IdentifierMetadataProviderImpl.");
			return null;
		}
    }
    
    public SerializableMetadataProvider getSerializableMetadataProvider(){
    	if(serializableMetadataProvider == null){
    		// Get all Services implement SerializableMetadataProvider interface
    		try {
    			ServiceReference<?>[] references = context.getAllServiceReferences(SerializableMetadataProvider.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (SerializableMetadataProvider) context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load SerializableMetadataProvider on IdentifierMetadataProviderImpl.");
    			return null;
    		}
    	}else{
    		return serializableMetadataProvider;
    	}
    	
    }
    
}
