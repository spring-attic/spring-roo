package org.springframework.roo.addon.layers.repository.jpa;

import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_JPA;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.springframework.uaa.client.util.Assert;
import org.w3c.dom.Element;

/**
 * The {@link RepositoryJpaOperations} implementation.
 *
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
public class RepositoryJpaOperationsImpl implements RepositoryJpaOperations {

	// Fields
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeManagementService typeManagementService;

	public boolean isRepositoryCommandAvailable() {
		return projectOperations.isFocusedProjectAvailable() && fileManager.exists(pathResolver.getFocusedIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"));
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
		AnnotationMetadataBuilder interfaceAnnotationMetadata = new AnnotationMetadataBuilder(ROO_REPOSITORY_JPA);
		interfaceAnnotationMetadata.addAttribute(new ClassAttributeValue(new JavaSymbolName("domainType"), domainType));
		String interfaceMdId = PhysicalTypeIdentifier.createIdentifier(interfaceType, pathResolver.getPath(interfaceIdentifier));
		ClassOrInterfaceTypeDetailsBuilder interfaceTypeBuilder = new ClassOrInterfaceTypeDetailsBuilder(interfaceMdId, Modifier.PUBLIC, interfaceType, PhysicalTypeCategory.INTERFACE);
		interfaceTypeBuilder.addAnnotation(interfaceAnnotationMetadata.build());
		typeManagementService.createOrUpdateTypeOnDisk(interfaceTypeBuilder.build());

		// Second build the implementing class
		// String classMdId = PhysicalTypeIdentifier.createIdentifier(classType, projectOperations.getPathResolver().getPath(classIdentifier));
		// ClassOrInterfaceTypeDetailsBuilder classTypeBuilder = new ClassOrInterfaceTypeDetailsBuilder(classMdId, Modifier.PUBLIC, classType, PhysicalTypeCategory.CLASS);
		// classTypeBuilder.addImplementsType(interfaceType);
		// typeManagementService.createOrUpdateTypeOnDisk(classTypeBuilder.build());

		// Third, take care of project configs
		configureProject();
	}

	private void configureProject() {
		Element configuration = XmlUtils.getConfiguration(getClass());

		List<Dependency> dependencies = new ArrayList<Dependency>();
		List<Element> springDependencies = XmlUtils.findElements("/configuration/repository/dependencies/dependency", configuration);
		for (Element dependencyElement : springDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}
		
		projectOperations.addDependencies(projectOperations.getFocusedModuleName(), dependencies);
		
		String appCtxId = pathResolver.getFocusedIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext-jpa.xml");
		
		if (fileManager.exists(appCtxId)) {
			return;
		} else {
			InputStream templateInputStream = FileUtils.getInputStream(getClass(), "applicationContext-jpa.xml");
			try {
				String input = FileCopyUtils.copyToString(new InputStreamReader(templateInputStream));
				input = input.replace("TO_BE_CHANGED_BY_ADDON", projectOperations.getFocusedTopLevelPackage().getFullyQualifiedPackageName());
				MutableFile mutableFile = fileManager.createFile(appCtxId);
				FileCopyUtils.copy(input.getBytes(), mutableFile.getOutputStream());
			} catch (IOException e) {
				throw new IllegalStateException("Unable to create '" + appCtxId + "'", e);
			}
		}
	}
}
