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

/**
 * The {@link MongoOperations} implementation.
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
public class MongoOperationsImpl implements MongoOperations {

    private static final String MONGO_XML = "applicationContext-mongo.xml";

    @Reference private DataOnDemandOperations dataOnDemandOperations;
    @Reference private FileManager fileManager;
    @Reference private IntegrationTestOperations integrationTestOperations;
    @Reference private PathResolver pathResolver;
    @Reference private ProjectOperations projectOperations;
    @Reference private PropFileOperations propFileOperations;
    @Reference private TypeLocationService typeLocationService;
    @Reference private TypeManagementService typeManagementService;

    public void createType(final JavaType classType, final JavaType idType,
            final boolean testAutomatically) {
        Validate.notNull(classType, "Class type required");
        Validate.notNull(idType, "Identifier type required");

        final String classIdentifier = typeLocationService
                .getPhysicalTypeCanonicalPath(classType,
                        pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA));
        if (fileManager.exists(classIdentifier)) {
            return; // Type exists already - nothing to do
        }

        final String classMdId = PhysicalTypeIdentifier.createIdentifier(
                classType, pathResolver.getPath(classIdentifier));
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
        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());

        if (testAutomatically) {
            integrationTestOperations.newIntegrationTest(classType, false);
            dataOnDemandOperations.newDod(classType,
                    new JavaType(classType.getFullyQualifiedTypeName()
                            + "DataOnDemand"));
        }
    }

    public String getName() {
        return FeatureNames.MONGO;
    }

    public boolean isInstalledInModule(final String moduleName) {
        return projectOperations.isFocusedProjectAvailable()
                && fileManager.exists(pathResolver.getFocusedIdentifier(
                        Path.SPRING_CONFIG_ROOT, MONGO_XML));
    }

    public boolean isMongoInstallationPossible() {
        return projectOperations.isFocusedProjectAvailable()
                && !projectOperations
                        .isFeatureInstalledInFocusedModule(FeatureNames.JPA);
    }

    public boolean isRepositoryInstallationPossible() {
        return isInstalledInModule(projectOperations.getFocusedModuleName())
                && !projectOperations
                        .isFeatureInstalledInFocusedModule(FeatureNames.JPA);
    }

    private void manageAppCtx(final String username, final String password,
            final String name, final boolean cloudFoundry,
            final String moduleName) {
        final String appCtxId = pathResolver.getFocusedIdentifier(
                Path.SPRING_CONFIG_ROOT, MONGO_XML);
        if (!fileManager.exists(appCtxId)) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = FileUtils.getInputStream(getClass(), MONGO_XML);
                final MutableFile mutableFile = fileManager
                        .createFile(appCtxId);
                String input = IOUtils.toString(inputStream);
                input = input.replace("TO_BE_CHANGED_BY_ADDON",
                        projectOperations.getTopLevelPackage(moduleName)
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

        final Document document = XmlUtils.readXml(fileManager
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
        fileManager.createOrUpdateTextFileIfRequired(appCtxId,
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

        projectOperations.addRepositories(moduleName, repositories);
        projectOperations.addDependencies(moduleName, dependencies);
    }

    public void setup(final String username, final String password,
            final String name, final String port, final String host,
            final boolean cloudFoundry) {
        final String moduleName = projectOperations.getFocusedModuleName();
        writeProperties(username, password, name, port, host, moduleName);
        manageDependencies(moduleName);
        manageAppCtx(username, password, name, cloudFoundry, moduleName);
    }

    public void setupRepository(final JavaType interfaceType,
            final JavaType domainType) {
        Validate.notNull(interfaceType, "Interface type required");
        Validate.notNull(domainType, "Domain type required");

        final String interfaceIdentifier = pathResolver
                .getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, interfaceType);

        if (fileManager.exists(interfaceIdentifier)) {
            return; // Type exists already - nothing to do
        }

        // Build interface type
        final AnnotationMetadataBuilder interfaceAnnotationMetadata = new AnnotationMetadataBuilder(
                ROO_REPOSITORY_MONGO);
        interfaceAnnotationMetadata.addAttribute(new ClassAttributeValue(
                new JavaSymbolName("domainType"), domainType));
        final String interfaceMdId = PhysicalTypeIdentifier.createIdentifier(
                interfaceType, pathResolver.getPath(interfaceIdentifier));
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                interfaceMdId, Modifier.PUBLIC, interfaceType,
                PhysicalTypeCategory.INTERFACE);
        cidBuilder.addAnnotation(interfaceAnnotationMetadata.build());
        final JavaType listType = new JavaType(List.class.getName(), 0,
                DataType.TYPE, null, Arrays.asList(domainType));
        cidBuilder.addMethod(new MethodMetadataBuilder(interfaceMdId, 0,
                new JavaSymbolName("findAll"), listType,
                new InvocableMemberBodyBuilder()));
        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
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
            name = projectOperations.getProjectName(moduleName);
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
        propFileOperations.addProperties(Path.SPRING_CONFIG_ROOT
                .getModulePathId(projectOperations.getFocusedModuleName()),
                "database.properties", properties, true, false);
    }
}
