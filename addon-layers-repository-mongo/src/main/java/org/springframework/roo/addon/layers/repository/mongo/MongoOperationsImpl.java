package org.springframework.roo.addon.layers.repository.mongo;

import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_MONGO;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Repository;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.springframework.uaa.client.util.Assert;
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

	// Fields
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;
	@Reference private PathResolver pathResolver;
	@Reference private PropFileOperations propFileOperations;
	@Reference private IntegrationTestOperations integrationTestOperations;
	@Reference private DataOnDemandOperations dataOnDemandOperations;

	public boolean isSetupCommandAvailable() {
		return projectOperations.isFocusedProjectAvailable();
	}

	public boolean isRepositoryCommandAvailable() {
		return projectOperations.isFocusedProjectAvailable() && fileManager.exists(pathResolver.getFocusedIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext-mongo.xml"));
	}

	public void setupRepository(final JavaType interfaceType, final JavaType classType, final JavaType domainType) {
		Assert.notNull(interfaceType, "Interface type required");
		Assert.notNull(classType, "Class type required");
		Assert.notNull(domainType, "Domain type required");

		String interfaceIdentifier = pathResolver.getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, interfaceType);
		String classIdentifier = pathResolver.getFocusedCanonicalPath(Path.SRC_MAIN_JAVA, classType);
		
		if (fileManager.exists(interfaceIdentifier) || fileManager.exists(classIdentifier)) {
			return; // Type exists already - nothing to do
		}

		// First build interface type
		AnnotationMetadataBuilder interfaceAnnotationMetadata = new AnnotationMetadataBuilder(ROO_REPOSITORY_MONGO);
		interfaceAnnotationMetadata.addAttribute(new ClassAttributeValue(new JavaSymbolName("domainType"), domainType));
		String interfaceMdId = PhysicalTypeIdentifier.createIdentifier(interfaceType, pathResolver.getPath(interfaceIdentifier));
		ClassOrInterfaceTypeDetailsBuilder interfaceTypeBuilder = new ClassOrInterfaceTypeDetailsBuilder(interfaceMdId, Modifier.PUBLIC, interfaceType, PhysicalTypeCategory.INTERFACE);
		interfaceTypeBuilder.addAnnotation(interfaceAnnotationMetadata.build());
		JavaType listType = new JavaType(List.class.getName(), 0, DataType.TYPE, null, Arrays.asList(domainType));
		interfaceTypeBuilder.addMethod(new MethodMetadataBuilder(interfaceMdId, 0, new JavaSymbolName("findAll"), listType, new InvocableMemberBodyBuilder()));
		typeManagementService.createOrUpdateTypeOnDisk(interfaceTypeBuilder.build());
	}

	public void createType(final JavaType classType, final JavaType idType, final boolean testAutomatically) {
		Assert.notNull(classType, "Class type required");
		Assert.notNull(idType, "Identifier type required");
		
		String classIdentifier = typeLocationService.getPhysicalTypeCanonicalPath(classType, pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA));
		if (fileManager.exists(classIdentifier)) {
			return; // Type exists already - nothing to do
		}
		
		String classMdId = PhysicalTypeIdentifier.createIdentifier(classType, pathResolver.getPath(classIdentifier));
		ClassOrInterfaceTypeDetailsBuilder classTypeBuilder = new ClassOrInterfaceTypeDetailsBuilder(classMdId, Modifier.PUBLIC, classType, PhysicalTypeCategory.CLASS);
		classTypeBuilder.addAnnotation(new AnnotationMetadataBuilder(RooJavaType.ROO_JAVA_BEAN));
		classTypeBuilder.addAnnotation(new AnnotationMetadataBuilder(RooJavaType.ROO_TO_STRING));

		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		if (!idType.equals(JdkJavaType.BIG_INTEGER)) {
			attributes.add(new ClassAttributeValue(new JavaSymbolName("identifierType"), idType));
		}
		classTypeBuilder.addAnnotation(new AnnotationMetadataBuilder(RooJavaType.ROO_MONGO_ENTITY, attributes));
		classTypeBuilder.addAnnotation(new AnnotationMetadataBuilder(RooJavaType.ROO_DISPLAY_STRING));
		typeManagementService.createOrUpdateTypeOnDisk(classTypeBuilder.build());

		if (testAutomatically) {
			integrationTestOperations.newIntegrationTest(classType, false);
			dataOnDemandOperations.newDod(classType, new JavaType(classType.getFullyQualifiedTypeName() + "DataOnDemand"), pathResolver.getFocusedPath(Path.SRC_TEST_JAVA));
		}
	}

	public void setup(final String username, final String password, final String name, final String port, final String host, final boolean cloudFoundry, final String moduleName) {
		writeProperties(username, password, name, port, host, moduleName);
		manageDependencies(moduleName);
		manageAppCtx(username, password, name, cloudFoundry, moduleName);
	}

	private void manageAppCtx(String username, String password, String name, boolean cloudFoundry, final String moduleName) {
		String appCtxId = pathResolver.getFocusedIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext-mongo.xml");
		if (!fileManager.exists(appCtxId)) {
			try {
				InputStream inputStream = TemplateUtils.getTemplate(getClass(), "applicationContext-mongo.xml");
				MutableFile mutableFile = fileManager.createFile(appCtxId);
				String input = FileCopyUtils.copyToString(new InputStreamReader(inputStream));
				input = input.replace("TO_BE_CHANGED_BY_ADDON", projectOperations.getTopLevelPackage(moduleName).getFullyQualifiedPackageName());
				FileCopyUtils.copy(input.getBytes(), mutableFile.getOutputStream());
				inputStream.close();
			} catch (IOException e) {
				throw new IllegalStateException("Unable to create file " + appCtxId);
			}
		}
		Document doc = XmlUtils.readXml(fileManager.getInputStream(appCtxId));
		Element root = doc.getDocumentElement();
		Element mongoSetup = XmlUtils.findFirstElement("/beans/db-factory", root);
		Element mongoCloudSetup = XmlUtils.findFirstElement("/beans/mongo-db-factory", root);
		if (!cloudFoundry) {
			if (mongoCloudSetup != null) {
				root.removeChild(mongoCloudSetup);
			}
			if (mongoSetup == null) {
				mongoSetup = doc.createElement("mongo:db-factory");
				root.appendChild(mongoSetup);
			}
			if (StringUtils.hasText(name)) {
				mongoSetup.setAttribute("dbname", "${mongo.database}");
			}
			if (StringUtils.hasText(username)) {
				mongoSetup.setAttribute("username", "${mongo.username}");
			}
			if (StringUtils.hasText(password)) {
				mongoSetup.setAttribute("password", "${mongo.password}");
			}
			mongoSetup.setAttribute("host", "${mongo.host}");
			mongoSetup.setAttribute("port", "${mongo.port}");
			mongoSetup.setAttribute("id", "mongoDbFactory");
		} else {
			if (mongoSetup != null) {
				root.removeChild(mongoSetup);
			}
			if (mongoCloudSetup == null) {
				mongoCloudSetup = XmlUtils.findFirstElement("/beans/mongo-db-factory", root);
			}
			if (mongoCloudSetup == null) {
				mongoCloudSetup = doc.createElement("cloud:mongo-db-factory");
				mongoCloudSetup.setAttribute("id", "mongoDbFactory");
				root.appendChild(mongoCloudSetup);
			}
		}
		fileManager.createOrUpdateTextFileIfRequired(appCtxId, XmlUtils.nodeToString(doc), false);
	}

	private void manageDependencies(final String moduleName) {
		Element configuration = XmlUtils.getConfiguration(getClass());

		List<Dependency> dependencies = new ArrayList<Dependency>();
		List<Element> springDependencies = XmlUtils.findElements("/configuration/repository/dependencies/dependency", configuration);
		for (Element dependencyElement : springDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}

		List<Repository> repositories = new ArrayList<Repository>();
		List<Element> repositoryElements = XmlUtils.findElements("/configuration/repository/repository", configuration);
		for (Element repositoryElement : repositoryElements) {
			repositories.add(new Repository(repositoryElement));
		}
		
		projectOperations.addRepositories(moduleName, repositories);
		projectOperations.addDependencies(moduleName, dependencies);
	}

	private void writeProperties(String username, String password, String name, String port, String host, String moduleName) {
		if (!StringUtils.hasText(username)) username = "";
		if (!StringUtils.hasText(password)) password = "";
		if (!StringUtils.hasText(name)) name = projectOperations.getProjectName(moduleName);
		if (!StringUtils.hasText(port)) port = "27017";
		if (!StringUtils.hasText(host)) host = "127.0.0.1";

		Map<String, String> properties = new HashMap<String, String>();
		properties.put("mongo.username", username);
		properties.put("mongo.password", password);
		properties.put("mongo.name", name);
		properties.put("mongo.port", port);
		properties.put("mongo.host", host);
		propFileOperations.addProperties(Path.SPRING_CONFIG_ROOT.contextualize(projectOperations.getFocusedModuleName()), "database.properties", properties, true, false);
	}
}
