package org.springframework.roo.project;

import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;
import org.osgi.service.component.ComponentContext;

/**
 * Provides Spring application context-related operations.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class ApplicationContextOperationsImpl implements
        ApplicationContextOperations {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(ApplicationContextOperationsImpl.class);

	// ------------ OSGi component attributes ----------------
   	private BundleContext context;
   	
   	protected void activate(final ComponentContext cContext) {
    	context = cContext.getBundleContext();
    }
	
    private FileManager fileManager;
    private MetadataService metadataService;
    private PathResolver pathResolver;

    public void createMiddleTierApplicationContext(
            final JavaPackage topLevelPackage, final String moduleName) {
        final ProjectMetadata projectMetadata = (ProjectMetadata) getMetadataService()
                .get(ProjectMetadata.getProjectIdentifier(moduleName));
        Validate.notNull(projectMetadata,
                "Project metadata required for module '%s'", moduleName);
        final Document document = XmlUtils.readXml(FileUtils.getInputStream(
                getClass(), "applicationContext-template.xml"));
        final Element root = document.getDocumentElement();
        DomUtils.findFirstElementByName("context:component-scan", root)
                .setAttribute("base-package",
                        topLevelPackage.getFullyQualifiedPackageName());
        getFileManager().createOrUpdateTextFileIfRequired(getPathResolver()
                .getIdentifier(
                        Path.SPRING_CONFIG_ROOT.getModulePathId(moduleName),
                        "applicationContext.xml"), XmlUtils
                .nodeToString(document), false);
        getFileManager().scan();
    }
    
    public FileManager getFileManager(){
    	if(fileManager == null){
    		// Get all Services implement FileManager interface
    		try {
    			ServiceReference<?>[] references = context.getAllServiceReferences(FileManager.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (FileManager) context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load FileManager on ApplicationContextOperationsImpl.");
    			return null;
    		}
    	}else{
    		return fileManager;
    	}
    }
    
    public MetadataService getMetadataService(){
    	if(metadataService == null){
    		// Get all Services implement MetadataService interface
    		try {
    			ServiceReference<?>[] references = context.getAllServiceReferences(MetadataService.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (MetadataService) context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load MetadataService on ApplicationContextOperationsImpl.");
    			return null;
    		}
    	}else{
    		return metadataService;
    	}
    }
    
    public PathResolver getPathResolver(){
    	if(pathResolver == null){
    		// Get all Services implement PathResolver interface
    		try {
    			ServiceReference<?>[] references = context.getAllServiceReferences(PathResolver.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (PathResolver) context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load PathResolver on ApplicationContextOperationsImpl.");
    			return null;
    		}
    	}else{
    		return pathResolver;
    	}
    }
}
