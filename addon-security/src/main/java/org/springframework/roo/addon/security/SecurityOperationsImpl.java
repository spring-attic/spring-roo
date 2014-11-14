package org.springframework.roo.addon.security;

import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.classpath.PhysicalTypeCategory.CLASS;
import static org.springframework.roo.model.RooJavaType.ROO_PERMISSION_EVALUATOR;
import static org.springframework.roo.model.SpringJavaType.PERMISSION_EVALUATOR;
import static org.springframework.roo.project.Path.SRC_MAIN_JAVA;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperations;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.WebXmlUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.osgi.service.component.ComponentContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Provides security installation services.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
@Component
@Service
public class SecurityOperationsImpl implements SecurityOperations {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(SecurityOperationsImpl.class);
	
	// ------------ OSGi component attributes ----------------
   	private BundleContext context;

    private static final Dependency SPRING_SECURITY = new Dependency(
            "org.springframework.security", "spring-security-core",
            "3.1.0.RELEASE");

    private FileManager fileManager;
    private PathResolver pathResolver;
    private ProjectOperations projectOperations;
    private TilesOperations tilesOperations;
    private TypeManagementService typeManagementService;
    private MetadataService metadataService;
    
    protected void activate(final ComponentContext context) {
    	this.context = context.getBundleContext();
    }

    @Override
    public void installSecurity() {
        // Parse the configuration.xml file
        final Element configuration = XmlUtils.getConfiguration(getClass());

        // Add POM properties
        updatePomProperties(configuration,
                getProjectOperations().getFocusedModuleName());

        // Add dependencies to POM
        updateDependencies(configuration,
                getProjectOperations().getFocusedModuleName());

        // Copy the template across
        final String destination = getPathResolver().getFocusedIdentifier(
                Path.SPRING_CONFIG_ROOT, "applicationContext-security.xml");
        if (!getFileManager().exists(destination)) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = FileUtils.getInputStream(getClass(),
                        "applicationContext-security-template.xml");
                outputStream = getFileManager().createFile(destination)
                        .getOutputStream();
                IOUtils.copy(inputStream, outputStream);
            }
            catch (final IOException ioe) {
                throw new IllegalStateException(ioe);
            }
            finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }

        // Copy the template across
        final String loginPage = getPathResolver().getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/views/login.jspx");
        if (!getFileManager().exists(loginPage)) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = FileUtils
                        .getInputStream(getClass(), "login.jspx");
                outputStream = getFileManager().createFile(loginPage)
                        .getOutputStream();
                IOUtils.copy(inputStream, outputStream);
            }
            catch (final IOException ioe) {
                throw new IllegalStateException(ioe);
            }
            finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }

        if (getFileManager().exists(getPathResolver().getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/views/views.xml"))) {
            getTilesOperations().addViewDefinition("",
                    getPathResolver().getFocusedPath(Path.SRC_MAIN_WEBAPP), "login",
                    getTilesOperations().PUBLIC_TEMPLATE,
                    "/WEB-INF/views/login.jspx");
        }

        final String webXmlPath = getPathResolver().getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
        final Document webXmlDocument = XmlUtils.readXml(getFileManager()
                .getInputStream(webXmlPath));

		WebXmlUtils.addFilterAtPosition(WebXmlUtils.FilterPosition.LAST, null,
				null, SecurityOperations.SECURITY_FILTER_NAME,
				"org.springframework.web.filter.DelegatingFilterProxy", "/*",
				webXmlDocument, null);
		getFileManager().createOrUpdateTextFileIfRequired(webXmlPath,
				XmlUtils.nodeToString(webXmlDocument), false);

        // Include static view controller handler to webmvc-config.xml
        final String webConfigPath = getPathResolver().getFocusedIdentifier(
                Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml");
        final Document webConfigDocument = XmlUtils.readXml(getFileManager()
                .getInputStream(webConfigPath));
        final Element webConfig = webConfigDocument.getDocumentElement();
        final Element viewController = DomUtils.findFirstElementByName(
                "mvc:view-controller", webConfig);
        Validate.notNull(viewController,
                "Could not find mvc:view-controller in %s", webConfig);
        viewController.getParentNode()
                .insertBefore(
                        new XmlElementBuilder("mvc:view-controller",
                                webConfigDocument).addAttribute("path",
                                "/login").build(), viewController);
        getFileManager().createOrUpdateTextFileIfRequired(webConfigPath,
                XmlUtils.nodeToString(webConfigDocument), false);
    }

    private void createPermissionEvaluator(
            final JavaPackage permissionEvaluatorPackage) {
        installPermissionEvaluatorTemplate(permissionEvaluatorPackage);
        final LogicalPath focusedSrcMainJava = LogicalPath.getInstance(
                SRC_MAIN_JAVA, getProjectOperations().getFocusedModuleName());
        JavaType permissionEvaluatorClass = new JavaType(
                permissionEvaluatorPackage.getFullyQualifiedPackageName()
                        + ".ApplicationPermissionEvaluator");
        final String identifier = getPathResolver().getFocusedCanonicalPath(
                Path.SRC_MAIN_JAVA, permissionEvaluatorClass);
        if (getFileManager().exists(identifier)) {
            return; // Type already exists - nothing to do
        }

        final AnnotationMetadataBuilder classAnnotationMetadata = new AnnotationMetadataBuilder(
                ROO_PERMISSION_EVALUATOR);
        final String classMid = PhysicalTypeIdentifier.createIdentifier(
                permissionEvaluatorClass, getPathResolver().getPath(identifier));
        final ClassOrInterfaceTypeDetailsBuilder classBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                classMid, PUBLIC, permissionEvaluatorClass, CLASS);
        classBuilder.addAnnotation(classAnnotationMetadata.build());
        classBuilder.addImplementsType(PERMISSION_EVALUATOR);
        getTypeManagementService().createOrUpdateTypeOnDisk(classBuilder.build());

        getMetadataService().get(PermissionEvaluatorMetadata.createIdentifier(
                permissionEvaluatorClass, focusedSrcMainJava));
    }

    private void installPermissionEvaluatorTemplate(
            JavaPackage permissionEvaluatorPackage) {
        // Copy the template across
        final String destination = getPathResolver().getFocusedIdentifier(
                Path.SPRING_CONFIG_ROOT,
                "applicationContext-security-permissionEvaluator.xml");
        if (!getFileManager().exists(destination)) {
            try {
                InputStream inputStream = FileUtils
                        .getInputStream(getClass(),
                                "applicationContext-security-permissionEvaluator-template.xml");
                String content = IOUtils.toString(inputStream);
                content = content.replace("__PERMISSION_EVALUATOR_PACKAGE__",
                        permissionEvaluatorPackage
                                .getFullyQualifiedPackageName());

                getFileManager().createOrUpdateTextFileIfRequired(destination,
                        content, true);
            }
            catch (final IOException ioe) {
                throw new IllegalStateException(ioe);
            }
        }
    }

    @Override
    public void installPermissionEvaluator(
            final JavaPackage permissionEvaluatorPackage) {
        Validate.isTrue(
                getProjectOperations().isFeatureInstalled(FeatureNames.SECURITY),
                "Security must first be setup before securing a method");
        Validate.notNull(permissionEvaluatorPackage, "Package required");
        createPermissionEvaluator(permissionEvaluatorPackage);
    }

    @Override
    public boolean isSecurityInstallationPossible() {
        // Permit installation if they have a web project (as per ROO-342) and
        // no version of Spring Security is already installed.
        return getProjectOperations().isFocusedProjectAvailable()
                && getFileManager().exists(getPathResolver().getFocusedIdentifier(
                        Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml"))
                && !getProjectOperations().getFocusedModule()
                        .hasDependencyExcludingVersion(SPRING_SECURITY)
                && !getProjectOperations()
                        .isFeatureInstalledInFocusedModule(FeatureNames.JSF);
    }

    @Override
    public boolean isServicePermissionEvaluatorInstallationPossible() {
        return getProjectOperations().isFocusedProjectAvailable()
                && getProjectOperations().isFeatureInstalled(FeatureNames.SECURITY);
    }

    private void updateDependencies(final Element configuration,
            final String moduleName) {
        final List<Dependency> dependencies = new ArrayList<Dependency>();
        final List<Element> securityDependencies = XmlUtils.findElements(
                "/configuration/spring-security/dependencies/dependency",
                configuration);
        for (final Element dependencyElement : securityDependencies) {
            dependencies.add(new Dependency(dependencyElement));
        }
        getProjectOperations().addDependencies(moduleName, dependencies);
    }

    private void updatePomProperties(final Element configuration,
            final String moduleName) {
        final List<Element> databaseProperties = XmlUtils.findElements(
                "/configuration/spring-security/properties/*", configuration);
        for (final Element property : databaseProperties) {
            getProjectOperations().addProperty(moduleName, new Property(property));
        }
    }

    @Override
    public String getName() {
        return FeatureNames.SECURITY;
    }

    @Override
    public boolean isInstalledInModule(String moduleName) {
        final Pom pom = getProjectOperations().getPomFromModuleName(moduleName);
        if (pom == null) {
            return false;
        }
        for (final Dependency dependency : pom.getDependencies()) {
            if ("spring-security-core".equals(dependency.getArtifactId())) {
                return true;
            }
        }
        return false;
    }
    
    public FileManager getFileManager(){
    	if(fileManager == null){
    		// Get all Services implement FileManager interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(FileManager.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (FileManager) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load FileManager on SecurityOperationsImpl.");
    			return null;
    		}
    	}else{
    		return fileManager;
    	}
    }
    
    public PathResolver getPathResolver(){
    	if(pathResolver == null){
    		// Get all Services implement PathResolver interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(PathResolver.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (PathResolver) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load PathResolver on SecurityOperationsImpl.");
    			return null;
    		}
    	}else{
    		return pathResolver;
    	}
    }
    
    public ProjectOperations getProjectOperations(){
    	if(projectOperations == null){
    		// Get all Services implement ProjectOperations interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(ProjectOperations.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (ProjectOperations) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load ProjectOperations on SecurityOperationsImpl.");
    			return null;
    		}
    	}else{
    		return projectOperations;
    	}
    }
    
    public TilesOperations getTilesOperations(){
    	if(tilesOperations == null){
    		// Get all Services implement TilesOperations interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(TilesOperations.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (TilesOperations) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load TilesOperations on SecurityOperationsImpl.");
    			return null;
    		}
    	}else{
    		return tilesOperations;
    	}
    }
    
    public TypeManagementService getTypeManagementService(){
    	if(typeManagementService == null){
    		// Get all Services implement TypeManagementService interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(TypeManagementService.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (TypeManagementService) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load TypeManagementService on SecurityOperationsImpl.");
    			return null;
    		}
    	}else{
    		return typeManagementService;
    	}
    }
    
    public MetadataService getMetadataService(){
    	if(metadataService == null){
    		// Get all Services implement MetadataService interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(MetadataService.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (MetadataService) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load MetadataService on SecurityOperationsImpl.");
    			return null;
    		}
    	}else{
    		return metadataService;
    	}
    }
}
