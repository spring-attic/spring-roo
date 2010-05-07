package org.springframework.roo.addon.solr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.SortedSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
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

	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private PhysicalTypeMetadataProvider physicalTypeMetadataProvider;
	@Reference private MetadataService metadataService;
	@Reference private ProjectOperations projectOperations;

	private static final Dependency SOLRJ = new Dependency("org.apache.solr", "solr-solrj", "1.4.0");

	public boolean isInstallSearchAvailable() {
		return fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"));
	}

	public void setupConfig(String solrServerUrl) {
		projectOperations.dependencyUpdate(SOLRJ);

		updateSolrProperties(solrServerUrl);
		copyAsyncAspect();

		String contextPath = pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext.xml");
		MutableFile contextMutableFile = null;

		Document appCtx;
		try {
			if (fileManager.exists(contextPath)) {
				contextMutableFile = fileManager.updateFile(contextPath);
				appCtx = XmlUtils.getDocumentBuilder().parse(contextMutableFile.getInputStream());
			} else {
				throw new IllegalStateException("Could not find applicationContext.xml");
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element root = (Element) appCtx.getFirstChild();

		if (XmlUtils.findFirstElementByName("task:annotation-driven", root) == null) {
			if (root.getAttribute("xmlns:task").length() == 0) {
				root.setAttribute("xmlns:task", "http://www.springframework.org/schema/task");
				root.setAttribute("xsi:schemaLocation", root.getAttribute("xsi:schemaLocation") + "  http://www.springframework.org/schema/task http://www.springframework.org/schema/task/spring-task-3.0.xsd");
			}
			root.appendChild(new XmlElementBuilder("task:annotation-driven", appCtx).addAttribute("executor", "asyncExecutor").build());
			root.appendChild(new XmlElementBuilder("task:executor", appCtx).addAttribute("id", "asyncExecutor").addAttribute("pool-size", "${executor.poolSize}").build());
		}

		Element solrServer = XmlUtils.findFirstElement("/beans/bean[@id='solrServer']", root);

		if (solrServer != null) {
			return;
		}

		root.appendChild(new XmlElementBuilder("bean", appCtx).addAttribute("id", "solrServer").addAttribute("class", "org.apache.solr.client.solrj.impl.CommonsHttpSolrServer").addChild(new XmlElementBuilder("constructor-arg", appCtx).addAttribute("value", "${solr.serverUrl}").build()).build());

		XmlUtils.writeXml(contextMutableFile.getOutputStream(), appCtx);
	}

	private void copyAsyncAspect() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Could not obtain project metadata");
		String aspectLocation = pathResolver.getIdentifier(Path.SRC_MAIN_JAVA, StringUtils.replace(projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName(), ".", "/") + "/SolrSearchAsyncTaskExecutor.aj");
		if (!fileManager.exists(aspectLocation)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "SolrSearchAsyncTaskExecutor.aj-template"), fileManager.createFile(aspectLocation).getOutputStream());
				String contents = FileCopyUtils.copyToString(new FileReader(aspectLocation));
				contents = StringUtils.replace(contents, "<TO_BE_REPLACED_BY_ADDON>", projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName());
			    Writer output = new BufferedWriter(new FileWriter(aspectLocation));
			    try {
			      output.write(contents.toString());
			    }
			    finally {
			      output.close();
			    }
			} catch (IOException e) {
				new IllegalStateException("Could not copy SolrSearchAsyncTaskExecutor.aj into project", e);
			}
		}
	}

	private void updateSolrProperties(String solrServerUrl) {
		String solrPath = pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "solr.properties");
		MutableFile solrMutableFile = null;

		Properties props = new Properties();

		try {
			if (fileManager.exists(solrPath)) {
				solrMutableFile = fileManager.updateFile(solrPath);
				props.load(solrMutableFile.getInputStream());
			} else {
				solrMutableFile = fileManager.createFile(solrPath);
			}
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}

		props.put("solr.serverUrl", solrServerUrl);
		props.put("executor.poolSize", "10");

		try {
			props.store(solrMutableFile.getOutputStream(), "Updated at " + new Date());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}
	
	public void addAll() {
		FileDetails srcRoot = new FileDetails(new File(pathResolver.getRoot(Path.SRC_MAIN_JAVA)), null);
		String antPath = pathResolver.getRoot(Path.SRC_MAIN_JAVA) + File.separatorChar + "**" + File.separatorChar + "*.java";
		SortedSet<FileDetails> entries = fileManager.findMatchingAntPath(antPath);

		for (FileDetails file : entries) {
			String fullPath = srcRoot.getRelativeSegment(file.getCanonicalPath());
			fullPath = fullPath.substring(1, fullPath.lastIndexOf(".java")).replace(File.separatorChar, '.'); // ditch the first / and .java
			JavaType javaType = new JavaType(fullPath);
			String id = physicalTypeMetadataProvider.findIdentifier(javaType);
			if (id != null) {
				PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(id);
				if (ptm == null || ptm.getPhysicalTypeDetails() == null || !(ptm.getPhysicalTypeDetails() instanceof ClassOrInterfaceTypeDetails)) {
					continue;
				}
				
				ClassOrInterfaceTypeDetails cid = (ClassOrInterfaceTypeDetails) ptm.getPhysicalTypeDetails();
				if (Modifier.isAbstract(cid.getModifier())) {
					continue;
				}
				PhysicalTypeDetails ptd = ptm.getPhysicalTypeDetails();
				if (ptd == null || !(ptd instanceof MutableClassOrInterfaceTypeDetails)) {
					continue;
				}
				
				MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) ptd;
				for (AnnotationMetadata annotation : mutableTypeDetails.getTypeAnnotations()) {
					if (annotation.getAnnotationType().equals(new JavaType("javax.persistence.Entity"))) {
						addSolrSearchableAnnotation(mutableTypeDetails, javaType, id);
					} 
//					else if (annotation.getAnnotationType().equals(new JavaType("org.springframework.stereotype.Controller"))) {
//						addSearchToController(mutableTypeDetails);
//					}
				}
			}
		}
		return;
	}

	public void addSearch(JavaType javaType) {
		Assert.notNull(javaType, "Java type required");

		String id = physicalTypeMetadataProvider.findIdentifier(javaType);
		if (id == null) {
			throw new IllegalArgumentException("Cannot locate source for '" + javaType.getFullyQualifiedTypeName() + "'");
		}

		// Obtain the physical type and itd mutable details
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(id);
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		PhysicalTypeDetails ptd = ptm.getPhysicalTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) ptd;

		if (Modifier.isAbstract(mutableTypeDetails.getModifier())) {
			throw new IllegalStateException("The class specified is an abstract type. Can only add solr search for concrete types.");
		}
		for (AnnotationMetadata annotation : mutableTypeDetails.getTypeAnnotations()) {
			if (annotation.getAnnotationType().equals(new JavaType("javax.persistence.Entity"))) {
				addSolrSearchableAnnotation(mutableTypeDetails, javaType, id);
			} 
//			else if (annotation.getAnnotationType().equals(new JavaType("org.springframework.stereotype.Controller"))) {
//				addSearchToController(mutableTypeDetails);
//			}
		}
	}

//	private void addSearchToController(MutableClassOrInterfaceTypeDetails mutableTypeDetails) {
//		mutableTypeDetails.addTypeAnnotation(new DefaultAnnotationMetadata(new JavaType(RooSolrWebSearchable.class.getName()), new ArrayList<AnnotationAttributeValue<?>>()));
//	}

	private void addSolrSearchableAnnotation(MutableClassOrInterfaceTypeDetails mutableTypeDetails, JavaType javaType, String id) {
		// first add the @RooSolrSearchable annotation to type
		JavaType rooSolrSearchable = new JavaType(RooSolrSearchable.class.getName());
		if (!mutableTypeDetails.getTypeAnnotations().contains(rooSolrSearchable)) {
			mutableTypeDetails.addTypeAnnotation(new DefaultAnnotationMetadata(rooSolrSearchable, new ArrayList<AnnotationAttributeValue<?>>()));
		}
	}
}