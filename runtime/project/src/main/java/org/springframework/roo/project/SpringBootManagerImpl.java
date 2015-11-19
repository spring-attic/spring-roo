package org.springframework.roo.project;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.FileUtils;

/**
 * Implementation of SpringBootManager service interface
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
@Component
@Service
public class SpringBootManagerImpl implements SpringBootManager{
    
    protected final static Logger LOGGER = HandlerUtils.getLogger(SpringBootManagerImpl.class);
    
    private PathResolver pathResolver;
    private FileManager fileManager;
    
    // ------------ OSGi component attributes ----------------
    private BundleContext context;
    
    protected void activate(final ComponentContext context) {
        this.context = context.getBundleContext();
    }

    @Override
    public void createSpringBootApplicationClass(JavaPackage topLevelPackage,
            String projectName) {
        // Set projectName if null
        if (projectName == null) {
            projectName = topLevelPackage.getLastElement();
        }
        // Uppercase projectName
        projectName = projectName.substring(0, 1).toUpperCase()
                .concat(projectName.substring(1, projectName.length()));
        String bootClass = projectName.concat("Application");

        final JavaType javaType = new JavaType(topLevelPackage
                .getFullyQualifiedPackageName().concat(".").concat(bootClass));
        final String physicalPath = getPathResolver()
                .getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, javaType);
        if (getFileManager().exists(physicalPath)) {
            throw new RuntimeException(
                    "ERROR: You are trying to create two Java classes annotated with @SpringBootApplication");
        }

        InputStream inputStream = null;
        try {
            inputStream = FileUtils.getInputStream(getClass(),
                    "SpringBootApplication-template._java");
            String input = IOUtils.toString(inputStream);
            // Replacing package
            input = input.replace("__PACKAGE__",
                    topLevelPackage.getFullyQualifiedPackageName());
            input = input.replace("__PROJECT_NAME__", projectName);
            getFileManager().createOrUpdateTextFileIfRequired(physicalPath, input,
                    false);
        }
        catch (final IOException e) {
            throw new IllegalStateException(
                    "Unable to create '" + physicalPath + "'", e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
        
    }

    @Override
    public void createSpringBootApplicationPropertiesFile() {
        LogicalPath resourcesPath = Path.SRC_MAIN_RESOURCES
                .getModulePathId("");
        
        if(!getFileManager().exists(getPathResolver().getIdentifier(resourcesPath,
                "application.properties"))){
            getFileManager().createFile(getPathResolver().getIdentifier(resourcesPath,
                    "application.properties"));
        }
        
    }
    
    @Override
    public void createApplicationTestsClass(JavaPackage topLevelPackage,
            String projectName) {
        // Set projectName if null
        if (projectName == null) {
            projectName = topLevelPackage.getLastElement();
        }
        // Uppercase projectName
        projectName = projectName.substring(0, 1).toUpperCase()
                .concat(projectName.substring(1, projectName.length()));
        String testClass = projectName.concat("ApplicationTests");

        final JavaType javaType = new JavaType(topLevelPackage
                .getFullyQualifiedPackageName().concat(".").concat(testClass));
        final String physicalPath = getPathResolver()
                .getFocusedCanonicalPath(Path.SRC_TEST_JAVA, javaType);
        if (getFileManager().exists(physicalPath)) {
            throw new RuntimeException(
                    "ERROR: You are trying to create two Java classes annotated with @SpringApplicationConfiguration that will be used to execute JUnit tests");
        }

        InputStream inputStream = null;
        try {
            inputStream = FileUtils.getInputStream(getClass(),
                    "SpringApplicationTests-template._java");
            String input = IOUtils.toString(inputStream);
            // Replacing package
            input = input.replace("__PACKAGE__",
                    topLevelPackage.getFullyQualifiedPackageName());
            input = input.replace("__PROJECT_NAME__", projectName);
            getFileManager().createOrUpdateTextFileIfRequired(physicalPath, input,
                    false);
        }
        catch (final IOException e) {
            throw new IllegalStateException(
                    "Unable to create '" + physicalPath + "'", e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
        
    }
    
    public PathResolver getPathResolver(){
        if(pathResolver == null){
            // Get all Services implement PathResolver interface
            try {
                ServiceReference<?>[] references = this.context.getAllServiceReferences(PathResolver.class.getName(), null);
                
                for(ServiceReference<?> ref : references){
                    pathResolver = (PathResolver) this.context.getService(ref);
                    return pathResolver;
                }
                
                return null;
                
            } catch (InvalidSyntaxException e) {
                LOGGER.warning("Cannot load PathResolver on SpringBootManagerImpl.");
                return null;
            }
        }else{
            return pathResolver;
        }
    }
    
    public FileManager getFileManager(){
        if(fileManager == null){
            // Get all Services implement FileManager interface
            try {
                ServiceReference<?>[] references = this.context.getAllServiceReferences(FileManager.class.getName(), null);
                
                for(ServiceReference<?> ref : references){
                    fileManager = (FileManager) this.context.getService(ref);
                    return fileManager;
                }
                
                return null;
                
            } catch (InvalidSyntaxException e) {
                LOGGER.warning("Cannot load FileManager on SpringBootManagerImpl.");
                return null;
            }
        }else{
            return fileManager;
        }
    }
}
