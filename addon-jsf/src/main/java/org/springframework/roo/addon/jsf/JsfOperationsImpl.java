package org.springframework.roo.addon.jsf;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Repository;
import org.springframework.roo.shell.Shell;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.WebXmlUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link JsfOperations}.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component
@Service
public class JsfOperationsImpl implements JsfOperations {
	private static final String PRIMEFACES_XPATH = "/configuration/jsf-libraries/jsf-library[@id = 'PRIMEFACES']";
	@Reference private FileManager fileManager;
	@Reference private MetadataDependencyRegistry dependencyRegistry;
	@Reference private MetadataService metadataService;
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;
	@Reference private Shell shell;

	public boolean isSetupAvailable() {
		return projectOperations.isProjectAvailable();
	}

	public boolean isScaffoldAvailable() {
		return isSetupAvailable() && hasWebXml();
	}

	public void setup(JsfImplementation jsfImplementation) {
		Assert.notNull(jsfImplementation, "JSF implementation required");
		updateConfiguration(jsfImplementation);
		copyWebXml();
	}

	public void generateAll(JavaPackage destinationPackage) {
		Assert.notNull(destinationPackage, "Destination package required");
		
		// Create JSF managed bean for each entity
		generateManagedBeans(destinationPackage);
	}

	private boolean hasWebXml() {
		return fileManager.exists(getWebXmlFile());
	}
	
	private String getWebXmlFile() {
		return projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
	}

	private void copyWebXml() {
		Assert.isTrue(projectOperations.isProjectAvailable(), "Project metadata required");
		if (hasWebXml()) {
			return;
		}
		
		InputStream inputStream = TemplateUtils.getTemplate(getClass(), "web-template.xml");
		Document webXml;
		try {
			webXml = XmlUtils.getDocumentBuilder().parse(inputStream);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		
		String projectName = projectOperations.getProjectMetadata().getProjectName();
		WebXmlUtils.setDisplayName(projectName, webXml, null);
		WebXmlUtils.setDescription("Roo generated " + projectName + " application", webXml, null);
		
		fileManager.createOrUpdateXmlFileIfRequired(getWebXmlFile(), webXml, true);

		fileManager.scan();
	}

	private void updateConfiguration(JsfImplementation jsfImplementation) {
		Element configuration = XmlUtils.getConfiguration(getClass());

		// Remove unnecessary artifacts not specific to current JSF implementation
		cleanup(configuration, jsfImplementation);

		// Update pom.xml with JSF/Primefaces dependencies and repositories
		updateDependencies(configuration, jsfImplementation);
		updateRepositories(configuration, jsfImplementation);
	}

	private void cleanup(Element configuration, JsfImplementation jsfImplementation) {
		String pomPath = projectOperations.getPathResolver().getIdentifier(Path.ROOT, "/pom.xml");
		MutableFile mutableFile = fileManager.updateFile(pomPath);

		Document pom;
		try {
			pom = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception e) {
			throw new IllegalStateException("Could not open POM '" + pomPath + "'", e);
		}

		Element root = (Element) pom.getFirstChild();
		
		List<JsfImplementation> jsfImplementations = new ArrayList<JsfImplementation>();
		for (JsfImplementation implementation : JsfImplementation.values()) {
			if (implementation != jsfImplementation) {
				jsfImplementations.add(implementation);
			}
		}
		if (removeArtifacts(getImplementationXPath(jsfImplementations), root, configuration)) {
			mutableFile.setDescriptionOfChange("Removed redundant artifacts");
			XmlUtils.writeXml(mutableFile.getOutputStream(), pom);
		}	
	}
	
	private boolean removeArtifacts(String xPathExpression, Element root, Element configuration) {
		boolean hasChanged = false;

		// Remove unwanted dependencies
		Element dependenciesElement = XmlUtils.findFirstElement("/project/dependencies", root);
		for (Element candidate : XmlUtils.findElements("/project/dependencies/dependency", root)) {
			for (Element dependencyElement : XmlUtils.findElements(xPathExpression + "/dependencies/dependency", configuration)) {
				if (new Dependency(dependencyElement).equals(new Dependency(candidate))) {
					// Found it
					dependenciesElement.removeChild(candidate);
					XmlUtils.removeTextNodes(dependenciesElement);
					hasChanged = true;
				}
			}
		}
		
		return hasChanged;
	}

	private void updateDependencies(Element configuration, JsfImplementation jsfImplementation) {
		List<Dependency> dependencies = new ArrayList<Dependency>();
		List<Element> jsfImplementationDependencies = XmlUtils.findElements(getImplementationXPath(jsfImplementation) +"/dependencies/dependency", configuration);
		for (Element dependencyElement : jsfImplementationDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}
		
		List<Element> jsfLibraryDependencies = XmlUtils.findElements(PRIMEFACES_XPATH + "/dependencies/dependency", configuration);
		for (Element dependencyElement : jsfLibraryDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}
		
		projectOperations.addDependencies(dependencies);
	}

	private void updateRepositories(Element configuration, JsfImplementation jsfImplementation) {
		List<Repository> repositories = new ArrayList<Repository>();

		List<Element> jsfRepositories = XmlUtils.findElements(getImplementationXPath(jsfImplementation) +"/repositories/repository", configuration);
		for (Element repositoryElement : jsfRepositories) {
			repositories.add(new Repository(repositoryElement));
		}

		List<Element> jsfLibraryRepositories = XmlUtils.findElements(PRIMEFACES_XPATH + "/repositories/repository", configuration);
		for (Element repositoryElement : jsfLibraryRepositories) {
			repositories.add(new Repository(repositoryElement));
		}

		projectOperations.addRepositories(repositories);
	}

	private void generateManagedBeans(JavaPackage destinationPackage) {
		Set<ClassOrInterfaceTypeDetails> cids = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(new JavaType(RooEntity.class.getName()));
		for (ClassOrInterfaceTypeDetails cid : cids) {
			if (Modifier.isAbstract(cid.getModifier())) {
				continue;
			}
			
			JavaType entity = cid.getName();
			Path path = PhysicalTypeIdentifier.getPath(cid.getDeclaredByMetadataId());
			EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(EntityMetadata.createIdentifier(entity, path));
			if (entityMetadata == null || (!entityMetadata.isValid())) {
				continue;
			}
			
			// Check to see if this entity metadata has a JSF metadata listening to it
			String downstreamJsfMetadataId = JsfManagedBeanMetadata.createIdentifier(entity, path);
			if (dependencyRegistry.getDownstream(entityMetadata.getId()).contains(downstreamJsfMetadataId)) {
				// There is already a JSF managed bean for this entity
				continue;
			}
			
			// To get here, there is no listening managed bean, so add one
			JavaType managedBean = new JavaType(destinationPackage.getFullyQualifiedPackageName() + "." + entity.getSimpleTypeName() + "Bean");
			createManagedBean(managedBean, entity);
		}
	}

	public void createManagedBean(JavaType managedBean, JavaType entity) {
		if (fileManager.exists(typeLocationService.getPhysicalLocationCanonicalPath(managedBean, Path.SRC_MAIN_JAVA))) {
			// Type exists already - nothing to do
			return; 
		}
		
		// Create type annotation for new managed bean
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new ClassAttributeValue(new JavaSymbolName("entity"), entity));
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(managedBean, Path.SRC_MAIN_JAVA);
		ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(declaredByMetadataId, Modifier.PUBLIC, managedBean, PhysicalTypeCategory.CLASS);
		typeDetailsBuilder.addAnnotation(new AnnotationMetadataBuilder(new JavaType(RooJsfManagedBean.class.getName()), attributes));

		typeManagementService.generateClassFile(typeDetailsBuilder.build());

		shell.flash(Level.FINE, "Created " + managedBean.getFullyQualifiedTypeName(), JsfOperationsImpl.class.getName());
		shell.flash(Level.FINE, "", JsfOperationsImpl.class.getName());
	}
	
	private String getImplementationXPath(List<JsfImplementation> jsfImplementations) {
		StringBuilder builder = new StringBuilder("/configuration/jsf-implementations/jsf-implementation[");
		for (int i = 0, n = jsfImplementations.size(); i < n; i++) {
			builder.append("@id = '");
			builder.append(jsfImplementations.get(i).name());
			builder.append("'");
			if (i < n - 1) {
				builder.append(" or ");
			}
		}
		builder.append("]");
		return builder.toString();
	}
	
	private String getImplementationXPath(JsfImplementation jsfImplementation) {
		return "/configuration/jsf-implementations/jsf-implementation[@id = '" + jsfImplementation.name() + "']";
	}
}