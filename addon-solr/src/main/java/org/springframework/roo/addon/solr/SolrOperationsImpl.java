package org.springframework.roo.addon.solr;

import static org.springframework.roo.model.RooJavaType.ROO_SOLR_SEARCHABLE;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.Properties;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.IOUtils;
import org.springframework.roo.support.util.XmlElementBuilder;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides Search configuration operations.
 *
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class SolrOperationsImpl implements SolrOperations {

	// Constants
	private static final Dependency SOLRJ = new Dependency("org.apache.solr", "solr-solrj", "1.4.1");

	// Fields
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;
	@Reference private TypeLocationService typeLocationService;
	@Reference private TypeManagementService typeManagementService;

	public boolean isSolrInstallationPossible() {
		return projectOperations.isFocusedProjectAvailable() && !solrPropsInstalled() && projectOperations.isFeatureInstalled(FeatureNames.JPA);
	}

	public boolean isSearchAvailable() {
		return solrPropsInstalled();
	}

	private boolean solrPropsInstalled() {
		return fileManager.exists(projectOperations.getPathResolver().getFocusedIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/spring/solr.properties"));
	}

	public void setupConfig(final String solrServerUrl) {
		projectOperations.addDependency(projectOperations.getFocusedModuleName(), SOLRJ);

		updateSolrProperties(solrServerUrl);

		String contextPath = projectOperations.getPathResolver().getFocusedIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext.xml");
		Document appCtx = XmlUtils.readXml(fileManager.getInputStream(contextPath));
		Element root = appCtx.getDocumentElement();

		if (DomUtils.findFirstElementByName("task:annotation-driven", root) == null) {
			if (root.getAttribute("xmlns:task").length() == 0) {
				root.setAttribute("xmlns:task", "http://www.springframework.org/schema/task");
				root.setAttribute("xsi:schemaLocation", root.getAttribute("xsi:schemaLocation") + "  http://www.springframework.org/schema/task http://www.springframework.org/schema/task/spring-task-3.0.xsd");
			}
			root.appendChild(new XmlElementBuilder("task:annotation-driven", appCtx).addAttribute("executor", "asyncExecutor").addAttribute("mode", "aspectj").build());
			root.appendChild(new XmlElementBuilder("task:executor", appCtx).addAttribute("id", "asyncExecutor").addAttribute("pool-size", "${executor.poolSize}").build());
		}

		Element solrServer = XmlUtils.findFirstElement("/beans/bean[@id='solrServer']", root);
		if (solrServer != null) {
			return;
		}

		root.appendChild(new XmlElementBuilder("bean", appCtx).addAttribute("id", "solrServer").addAttribute("class", "org.apache.solr.client.solrj.impl.CommonsHttpSolrServer").addChild(new XmlElementBuilder("constructor-arg", appCtx).addAttribute("value", "${solr.serverUrl}").build()).build());
		DomUtils.removeTextNodes(root);

		fileManager.createOrUpdateTextFileIfRequired(contextPath, XmlUtils.nodeToString(appCtx), false);
	}

	private void updateSolrProperties(final String solrServerUrl) {
		String solrPath = projectOperations.getPathResolver().getFocusedIdentifier(Path.SPRING_CONFIG_ROOT, "solr.properties");
		boolean solrExists = fileManager.exists(solrPath);

		Properties props = new Properties();
		InputStream inputStream = null;
		try {
			if (fileManager.exists(solrPath)) {
				inputStream = fileManager.getInputStream(solrPath);
				props.load(inputStream);
			}
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}

		props.put("solr.serverUrl", solrServerUrl);
		props.put("executor.poolSize", "10");

		OutputStream outputStream = null;
		try {
			MutableFile mutableFile = solrExists ? fileManager.updateFile(solrPath) : fileManager.createFile(solrPath);
			outputStream = mutableFile.getOutputStream();
			props.store(outputStream, "Updated at " + new Date());
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} finally {
			IOUtils.closeQuietly(outputStream);
		}
	}

	public void addAll() {
		Set<ClassOrInterfaceTypeDetails> cids = typeLocationService.findClassesOrInterfaceDetailsWithTag(CustomDataKeys.PERSISTENT_TYPE);
		for (ClassOrInterfaceTypeDetails cid : cids) {
			if (!Modifier.isAbstract(cid.getModifier())) {
				addSolrSearchableAnnotation(cid);
			}
		}
	}

	public void addSearch(final JavaType javaType) {
		Assert.notNull(javaType, "Java type required");

		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = typeLocationService.getTypeDetails(javaType);
		if (classOrInterfaceTypeDetails == null) {
			throw new IllegalArgumentException("Cannot locate source for '" + javaType.getFullyQualifiedTypeName() + "'");
		}

		if (Modifier.isAbstract(classOrInterfaceTypeDetails.getModifier())) {
			throw new IllegalStateException("The class specified is an abstract type. Can only add solr search for concrete types.");
		}
		addSolrSearchableAnnotation(classOrInterfaceTypeDetails);
	}

	private void addSolrSearchableAnnotation(final ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails) {
		if (classOrInterfaceTypeDetails.getTypeAnnotation(ROO_SOLR_SEARCHABLE) == null) {
			AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(ROO_SOLR_SEARCHABLE);
			ClassOrInterfaceTypeDetailsBuilder classOrInterfaceTypeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(classOrInterfaceTypeDetails);
			classOrInterfaceTypeDetailsBuilder.addAnnotation(annotationBuilder);
			typeManagementService.createOrUpdateTypeOnDisk(classOrInterfaceTypeDetailsBuilder.build());
		}
	}
}