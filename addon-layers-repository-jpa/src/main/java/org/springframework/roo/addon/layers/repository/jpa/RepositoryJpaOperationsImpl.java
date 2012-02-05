package org.springframework.roo.addon.layers.repository.jpa;

import static org.springframework.roo.model.RooJavaType.ROO_REPOSITORY_JPA;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
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
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.XmlUtils;
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

    @Reference private FileManager fileManager;
    @Reference private PathResolver pathResolver;
    @Reference private ProjectOperations projectOperations;
    @Reference private TypeManagementService typeManagementService;

    private void configureProject() {
        final Element configuration = XmlUtils.getConfiguration(getClass());

        final List<Dependency> dependencies = new ArrayList<Dependency>();
        final List<Element> springDependencies = XmlUtils.findElements(
                "/configuration/spring-data-jpa/dependencies/dependency",
                configuration);
        for (final Element dependencyElement : springDependencies) {
            dependencies.add(new Dependency(dependencyElement));
        }

        projectOperations.addDependencies(
                projectOperations.getFocusedModuleName(), dependencies);

        final String appCtxId = pathResolver.getFocusedIdentifier(
                Path.SPRING_CONFIG_ROOT, "applicationContext-jpa.xml");
        if (fileManager.exists(appCtxId)) {
            return;
        }
        else {
            InputStream templateInputStream = null;
            OutputStream outputStream = null;
            try {
                templateInputStream = getClass().getResourceAsStream(
                        "applicationContext-jpa.xml");
                Validate.notNull(templateInputStream,
                        "Could not acquire 'applicationContext-jpa.xml' template");

                String input = IOUtils.toString(templateInputStream);
                input = input.replace("TO_BE_CHANGED_BY_ADDON",
                        projectOperations.getFocusedTopLevelPackage()
                                .getFullyQualifiedPackageName());
                final MutableFile mutableFile = fileManager
                        .createFile(appCtxId);
                outputStream = mutableFile.getOutputStream();
                IOUtils.write(input, outputStream);
            }
            catch (final IOException e) {
                throw new IllegalStateException("Unable to create '" + appCtxId
                        + "'", e);
            }
            finally {
                IOUtils.closeQuietly(templateInputStream);
                IOUtils.closeQuietly(outputStream);
            }
        }
    }

    public String getName() {
        return FeatureNames.JPA;
    }

    public boolean isInstalledInModule(final String moduleName) {
        final LogicalPath resourcesPath = LogicalPath.getInstance(
                Path.SRC_MAIN_RESOURCES, moduleName);
        return projectOperations.isFocusedProjectAvailable()
                && fileManager.exists(projectOperations.getPathResolver()
                        .getIdentifier(resourcesPath,
                                "META-INF/persistence.xml"));
    }

    public boolean isRepositoryInstallationPossible() {
        return isInstalledInModule(projectOperations.getFocusedModuleName())
                && !projectOperations
                        .isFeatureInstalledInFocusedModule(FeatureNames.MONGO);
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
                ROO_REPOSITORY_JPA);
        interfaceAnnotationMetadata.addAttribute(new ClassAttributeValue(
                new JavaSymbolName("domainType"), domainType));
        final String interfaceMdId = PhysicalTypeIdentifier.createIdentifier(
                interfaceType, pathResolver.getPath(interfaceIdentifier));
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                interfaceMdId, Modifier.PUBLIC, interfaceType,
                PhysicalTypeCategory.INTERFACE);
        cidBuilder.addAnnotation(interfaceAnnotationMetadata.build());
        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());

        // Take care of project configuration
        configureProject();
    }
}
