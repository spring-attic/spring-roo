package org.springframework.roo.addon.layers.repository.mongo;

import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_MONGO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dod.DataOnDemandOperations;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.addon.test.IntegrationTestOperations;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Repository;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.osgi.service.component.ComponentContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;


/**
 * The {@link MongoOperations} implementation.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
public class MongoOperationsImpl implements MongoOperations {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(MongoOperationsImpl.class);
	
	// ------------ OSGi component attributes ----------------
   	private BundleContext context;

    private static final String MONGO_XML = "applicationContext-mongo.xml";

    private DataOnDemandOperations dataOnDemandOperations;
    private FileManager fileManager;
    private IntegrationTestOperations integrationTestOperations;
    private PathResolver pathResolver;
    private ProjectOperations projectOperations;
    private PropFileOperations propFileOperations;
    private TypeLocationService typeLocationService;
    private TypeManagementService typeManagementService;
    
    protected void activate(final ComponentContext context) {
    	this.context = context.getBundleContext();
    }

    public void createType(final JavaType classType, final JavaType idType,
            final boolean testAutomatically) {
        Validate.notNull(classType, "Class type required");
        Validate.notNull(idType, "Identifier type required");

        final String classIdentifier = getTypeLocationService()
                .getPhysicalTypeCanonicalPath(classType,
                        getPathResolver().getFocusedPath(Path.SRC_MAIN_JAVA));
        if (getFileManager().exists(classIdentifier)) {
            return; // Type exists already - nothing to do
        }

        final String classMdId = PhysicalTypeIdentifier.createIdentifier(
                classType, getPathResolver().getPath(classIdentifier));
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                classMdId, Modifier.PUBLIC, classType,
                PhysicalTypeCategory.CLASS);
        cidBuilder.addAnnotation(new AnnotationMetadataBuilder(
                RooJavaType.ROO_JAVA_BEAN));
        cidBuilder.addAnnotation(new AnnotationMetadataBuilder(
                RooJavaType.ROO_TO_STRING));

        final List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
        if (!idType.equals(JdkJavaType.BIG_INTEGER)) {
            attributes.add(new ClassAttributeValue(new JavaSymbolName(
                    "identifierType"), idType));
        }
        cidBuilder.addAnnotation(new AnnotationMetadataBuilder(
                RooJavaType.ROO_MONGO_ENTITY, attributes));
        getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());

        if (testAutomatically) {
            getIntegrationTestOperations().newIntegrationTest(classType, false);
            getDataOnDemandOperations().newDod(classType,
                    new JavaType(classType.getFullyQualifiedTypeName()
                            + "DataOnDemand"));
        }
    }

    public String getName() {
        return FeatureNames.MONGO;
    }

    public boolean isInstalledInModule(final String moduleName) {
        return getProjectOperations().isFocusedProjectAvailable()
                && getFileManager().exists(getPathResolver().getFocusedIdentifier(
                        Path.SPRING_CONFIG_ROOT, MONGO_XML));
    }

    public boolean isMongoInstallationPossible() {
        return getProjectOperations().isFocusedProjectAvailable()
                && !getProjectOperations()
                        .isFeatureInstalledInFocusedModule(FeatureNames.JPA);
    }

    public boolean isRepositoryInstallationPossible() {
        return isInstalledInModule(getProjectOperations().getFocusedModuleName())
                && !getProjectOperations()
                        .isFeatureInstalledInFocusedModule(FeatureNames.JPA);
    }

    private void manageAppCtx(final String username, final String password,
            final String name, final boolean cloudFoundry,
            final String moduleName) {
        final String appCtxId = getPathResolver().getFocusedIdentifier(
                Path.SPRING_CONFIG_ROOT, MONGO_XML);
        if (!getFileManager().exists(appCtxId)) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = FileUtils.getInputStream(getClass(), MONGO_XML);
                final MutableFile mutableFile = getFileManager()
                        .createFile(appCtxId);
                String input = IOUtils.toString(inputStream);
                input = input.replace("TO_BE_CHANGED_BY_ADDON",
                        getProjectOperations().getTopLevelPackage(moduleName)
                                .getFullyQualifiedPackageName());
                outputStream = mutableFile.getOutputStream();
                IOUtils.write(input, outputStream);
            }
            catch (final IOException e) {
                throw new IllegalStateException("Unable to create file "
                        + appCtxId);
            }
            finally {
                IOUtils.closeQuietly(inputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }

        final Document document = XmlUtils.readXml(getFileManager()
                .getInputStream(appCtxId));
        final Element root = document.getDocumentElement();
        Element mongoSetup = XmlUtils.findFirstElement("/beans/db-factory",
                root);
        Element mongoCloudSetup = XmlUtils.findFirstElement(
                "/beans/mongo-db-factory", root);
        if (!cloudFoundry) {
            if (mongoCloudSetup != null) {
                root.removeChild(mongoCloudSetup);
            }
            if (mongoSetup == null) {
                mongoSetup = document.createElement("mongo:db-factory");
                root.appendChild(mongoSetup);
            }
            if (StringUtils.isNotBlank(name)) {
                mongoSetup.setAttribute("dbname", "${mongo.database}");
            }
            if (StringUtils.isNotBlank(username)) {
                mongoSetup.setAttribute("username", "${mongo.username}");
            }
            if (StringUtils.isNotBlank(password)) {
                mongoSetup.setAttribute("password", "${mongo.password}");
            }
            mongoSetup.setAttribute("host", "${mongo.host}");
            mongoSetup.setAttribute("port", "${mongo.port}");
            mongoSetup.setAttribute("id", "mongoDbFactory");
        }
        else {
            if (mongoSetup != null) {
                root.removeChild(mongoSetup);
            }
            if (mongoCloudSetup == null) {
                mongoCloudSetup = XmlUtils.findFirstElement(
                        "/beans/mongo-db-factory", root);
            }
            if (mongoCloudSetup == null) {
                mongoCloudSetup = document
                        .createElement("cloud:mongo-db-factory");
                mongoCloudSetup.setAttribute("id", "mongoDbFactory");
                root.appendChild(mongoCloudSetup);
            }
        }
        getFileManager().createOrUpdateTextFileIfRequired(appCtxId,
                XmlUtils.nodeToString(document), false);
    }

    private void manageDependencies(final String moduleName) {
        final Element configuration = XmlUtils.getConfiguration(getClass());

        final List<Dependency> dependencies = new ArrayList<Dependency>();
        final List<Element> springDependencies = XmlUtils.findElements(
                "/configuration/spring-data-mongodb/dependencies/dependency",
                configuration);
        for (final Element dependencyElement : springDependencies) {
            dependencies.add(new Dependency(dependencyElement));
        }

        final List<Repository> repositories = new ArrayList<Repository>();
        final List<Element> repositoryElements = XmlUtils.findElements(
                "/configuration/spring-data-mongodb/repositories/repository",
                configuration);
        for (final Element repositoryElement : repositoryElements) {
            repositories.add(new Repository(repositoryElement));
        }

        getProjectOperations().addRepositories(moduleName, repositories);
        getProjectOperations().addDependencies(moduleName, dependencies);
    }

    public void setup(final String username, final String password,
            final String name, final String port, final String host,
            final boolean cloudFoundry) {
        final String moduleName = getProjectOperations().getFocusedModuleName();
        writeProperties(username, password, name, port, host, moduleName);
        manageDependencies(moduleName);
        manageAppCtx(username, password, name, cloudFoundry, moduleName);
    }

    public void setupRepository(final JavaType interfaceType,
            final JavaType domainType) {
        Validate.notNull(interfaceType, "Interface type required");
        Validate.notNull(domainType, "Domain type required");

        final String interfaceIdentifier = getPathResolver()
                .getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, interfaceType);

        if (getFileManager().exists(interfaceIdentifier)) {
            return; // Type exists already - nothing to do
        }

        // Build interface type
        final AnnotationMetadataBuilder interfaceAnnotationMetadata = new AnnotationMetadataBuilder(
                ROO_REPOSITORY_MONGO);
        interfaceAnnotationMetadata.addAttribute(new ClassAttributeValue(
                new JavaSymbolName("domainType"), domainType));
        final String interfaceMdId = PhysicalTypeIdentifier.createIdentifier(
                interfaceType, getPathResolver().getPath(interfaceIdentifier));
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                interfaceMdId, Modifier.PUBLIC, interfaceType,
                PhysicalTypeCategory.INTERFACE);
        cidBuilder.addAnnotation(interfaceAnnotationMetadata.build());
        final JavaType listType = new JavaType(List.class.getName(), 0,
                DataType.TYPE, null, Arrays.asList(domainType));
        cidBuilder.addMethod(new MethodMetadataBuilder(interfaceMdId, 0,
                new JavaSymbolName("findAll"), listType,
                new InvocableMemberBodyBuilder()));
        getTypeManagementService().createOrUpdateTypeOnDisk(cidBuilder.build());
    }

    private void writeProperties(String username, String password, String name,
            String port, String host, final String moduleName) {
        if (StringUtils.isBlank(username)) {
            username = "";
        }
        if (StringUtils.isBlank(password)) {
            password = "";
        }
        if (StringUtils.isBlank(name)) {
            name = getProjectOperations().getProjectName(moduleName);
        }
        if (StringUtils.isBlank(port)) {
            port = "27017";
        }
        if (StringUtils.isBlank(host)) {
            host = "127.0.0.1";
        }

        final Map<String, String> properties = new HashMap<String, String>();
        properties.put("mongo.username", username);
        properties.put("mongo.password", password);
        properties.put("mongo.database", name);
        properties.put("mongo.port", port);
        properties.put("mongo.host", host);
        getPropFileOperations().addProperties(Path.SPRING_CONFIG_ROOT
                .getModulePathId(getProjectOperations().getFocusedModuleName()),
                "database.properties", properties, true, false);
    }
    
    public DataOnDemandOperations getDataOnDemandOperations(){
    	if(dataOnDemandOperations == null){
    		// Get all Services implement DataOnDemandOperations interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(DataOnDemandOperations.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (DataOnDemandOperations) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load DataOnDemandOperations on MongoOperationsImpl.");
    			return null;
    		}
    	}else{
    		return dataOnDemandOperations;
    	}
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
    			LOGGER.warning("Cannot load FileManager on MongoOperationsImpl.");
    			return null;
    		}
    	}else{
    		return fileManager;
    	}
    }
    
    public IntegrationTestOperations getIntegrationTestOperations(){
    	if(integrationTestOperations == null){
    		// Get all Services implement IntegrationTestOperations interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(IntegrationTestOperations.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (IntegrationTestOperations) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load IntegrationTestOperations on MongoOperationsImpl.");
    			return null;
    		}
    	}else{
    		return integrationTestOperations;
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
    			LOGGER.warning("Cannot load PathResolver on MongoOperationsImpl.");
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
    			LOGGER.warning("Cannot load ProjectOperations on MongoOperationsImpl.");
    			return null;
    		}
    	}else{
    		return projectOperations;
    	}
    }
    
    public PropFileOperations getPropFileOperations(){
    	if(propFileOperations == null){
    		// Get all Services implement PropFileOperations interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(PropFileOperations.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (PropFileOperations) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load PropFileOperations on MongoOperationsImpl.");
    			return null;
    		}
    	}else{
    		return propFileOperations;
    	}
    }
    
    public TypeLocationService getTypeLocationService(){
    	if(typeLocationService == null){
    		// Get all Services implement TypeLocationService interface
    		try {
    			ServiceReference<?>[] references = this.context.getAllServiceReferences(TypeLocationService.class.getName(), null);
    			
    			for(ServiceReference<?> ref : references){
    				return (TypeLocationService) this.context.getService(ref);
    			}
    			
    			return null;
    			
    		} catch (InvalidSyntaxException e) {
    			LOGGER.warning("Cannot load TypeLocationService on MongoOperationsImpl.");
    			return null;
    		}
    	}else{
    		return typeLocationService;
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
    			LOGGER.warning("Cannot load TypeManagementService on MongoOperationsImpl.");
    			return null;
    		}
    	}else{
    		return typeManagementService;
    	}
    }
}
