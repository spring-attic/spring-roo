package org.springframework.roo.addon.web.mvc.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;

import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Provides operations to create various view layer resources.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public class WebMvcOperations {
	
	private FileManager fileManager;
	private PathResolver pathResolver;
	private MetadataService metadataService;
	private ProjectOperations projectOperations;
	
	public WebMvcOperations(FileManager fileManager, PathResolver pathResolver, MetadataService metadataService, ProjectOperations projectOperations) {
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(pathResolver, "Path resolver required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(projectOperations, "Project operations required");
		
		this.fileManager = fileManager;
		this.pathResolver = pathResolver;
		this.metadataService = metadataService;
		this.projectOperations = projectOperations;
	}
	
	public void installMvcArtefacts() {
		//note that the sequence matters here as some of these artifacts are loaded further down the line
		createWebApplicationContext();
		copyUrlRewrite();
		createWebXml();
		createIndexJsp();		
		updateJpaWebXml();
		updateDependencies();
	}
	
	private void copyUrlRewrite(){
		String urlrewriteFilename = "WEB-INF/urlrewrite.xml";
	
		if (fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, urlrewriteFilename))) {
			//file exists, so nothing to do
			return;
		}		
		
		try {
			FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "urlrewrite-template.xml"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/urlrewrite.xml")).getOutputStream());
		} catch (IOException e) {
			new IllegalStateException("Encountered an error during copying of resources for maven addon.", e);
		}
	}

	private void createWebXml() {		
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata required");
		// Verify the servlet application context already exists
		String servletCtxFilename = "WEB-INF/" + projectMetadata.getProjectName() + "-servlet.xml";
		Assert.isTrue(fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, servletCtxFilename)), "'" + servletCtxFilename + "' does not exist");
		
		if (fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml"))) {
			//file exists, so nothing to do
			return;
		}
		
		InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "web-template.xml");
		Document webXml;
		try {
			webXml = XmlUtils.getDocumentBuilder().parse(templateInputStream);
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

		Element rootElement = (Element) webXml.getFirstChild();
		XmlUtils.findRequiredElement("//display-name", rootElement).setTextContent(projectMetadata.getProjectName());
		XmlUtils.findRequiredElement("//description", rootElement).setTextContent("Roo generated " + projectMetadata.getProjectName() + " application");
		
		XmlUtils.findRequiredElement("/web-app/context-param[param-value='TO_BE_CHANGED_BY_LISTENER']/param-value", rootElement).setTextContent(projectMetadata.getProjectName() + ".root");
		
		List<Element> servletNames = XmlUtils.findElements("//*[servlet-name='TO_BE_CHANGED_BY_LISTENER']", rootElement);
		for(Element element: servletNames){
			XmlUtils.findRequiredElement("servlet-name", element).setTextContent(projectMetadata.getProjectName());
		}
		
		Element urlPattern = XmlUtils.findRequiredElement("/web-app/servlet-mapping[url-pattern='TO_BE_CHANGED_BY_LISTENER']/url-pattern", rootElement);
		urlPattern.setTextContent("/app/*");
		
		MutableFile mutableFile = fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml"));
		XmlUtils.writeXml(mutableFile.getOutputStream(), webXml);

		fileManager.scanAll();
	}
	
	private void createIndexJsp() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata required");
		// Verify the web.xml already exists
		Assert.isTrue(fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml")), "web.xml does not exist");
		
		if (fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/jsp/index.jsp"))) {
			//file exists, so nothing to do
			return;
		}
		
		DocumentBuilder builder = XmlUtils.getDocumentBuilder();		
		Document jspDocument = builder.newDocument();	

		//this node is just for temporary purpose - it will not be in the final result
		Node documentRoot = jspDocument.createElement("tempNode");
		
		jspDocument.appendChild(documentRoot);
		
		documentRoot.appendChild(jspDocument.createComment("WARNING: This file is maintained by ROO! IT WILL BE OVERWRITTEN!"));
		
		Element includeIncludes = jspDocument.createElement("jsp:directive.include");
		includeIncludes.setAttribute("file", "/WEB-INF/jsp/includes.jsp");
		documentRoot.appendChild(includeIncludes);
		
		Element includeHeader = jspDocument.createElement("jsp:directive.include");
		includeHeader.setAttribute("file", "/WEB-INF/jsp/header.jsp");
		documentRoot.appendChild(includeHeader);
		
		Element h3 = jspDocument.createElement("h3");
		h3.setTextContent("Welcome to " + StringUtils.capitalize(projectMetadata.getProjectName()) + "!");
		documentRoot.appendChild(h3);
		
		Element includeFooter = jspDocument.createElement("jsp:directive.include");
		includeFooter.setAttribute("file", "/WEB-INF/jsp/footer.jsp");
		documentRoot.appendChild(includeFooter);
		
		MutableFile mutableFile = fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/jsp/index.jsp"));
		XmlUtils.writeMalformedXml(mutableFile.getOutputStream(), jspDocument.getFirstChild().getChildNodes());
	}	
	
	private void updateJpaWebXml() {				
		String persistence = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml");
				
		if (!fileManager.exists(persistence)) {
			// no persistence layer installed yet, nothing to do
			return;
		}
		
		String webXmlPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
		MutableFile webXmlMutableFile = null;		
		Document webXml;
		
		try {
			webXmlMutableFile = fileManager.updateFile(webXmlPath);
			webXml = XmlUtils.getDocumentBuilder().parse(webXmlMutableFile.getInputStream());			
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} 	
		Element root = webXml.getDocumentElement();
		
		if (null != XmlUtils.findFirstElement("/web-app/filter[filter-class='org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter']", root)) {
			//filter already installed, nothing to do
			return;
		}
		
		Element httpMethodFilter = XmlUtils.findRequiredElement("/web-app/filter-mapping[filter-name='httpMethodFilter']", root);

		Element filter = webXml.createElement("filter");
		Element filterName = webXml.createElement("filter-name");
		filterName.setTextContent("Spring OpenEntityManagerInViewFilter");
		filter.appendChild(filterName);
		Element filterClass = webXml.createElement("filter-class");
		filterClass.setTextContent("org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter");
		filter.appendChild(filterClass);
		root.insertBefore(filter, httpMethodFilter);
		
		Element filterMapping = webXml.createElement("filter-mapping");
		Element filterName2 = webXml.createElement("filter-name");
		filterName2.setTextContent("Spring OpenEntityManagerInViewFilter");
		filterMapping.appendChild(filterName2);
		Element urlMapping = webXml.createElement("url-pattern");
		urlMapping.setTextContent("/*");
		filterMapping.appendChild(urlMapping);
		root.insertBefore(filterMapping, httpMethodFilter);
		
		XmlUtils.writeXml(webXmlMutableFile.getOutputStream(), webXml);
	}
	
	private void createWebApplicationContext() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.isTrue(projectMetadata != null, "Project metadata required");
		
		// Verify the middle tier application context already exists
		PathResolver pathResolver = projectMetadata.getPathResolver();
		Assert.isTrue(fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "applicationContext.xml")), "Application context does not exist");
		
		String servletCtxFilename = "WEB-INF/" + projectMetadata.getProjectName() + "-servlet.xml";
		if (fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, servletCtxFilename))) {
			//this file already exists, nothing to do
			return;
		}
		
		InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "roo-servlet-template.xml");
		Document pom;
		try {
			pom = XmlUtils.getDocumentBuilder().parse(templateInputStream);
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

		Element rootElement = (Element) pom.getFirstChild();
		XmlUtils.findFirstElementByName("context:component-scan", rootElement).setAttribute("base-package", projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName());
		
		MutableFile mutableFile = fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, servletCtxFilename));
		XmlUtils.writeXml(mutableFile.getOutputStream(), pom);

		fileManager.scanAll();
	}
	
	private void updateDependencies() {	
		InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "dependencies.xml");
		Assert.notNull(templateInputStream, "Could not acquire dependencies.xml file");
		Document dependencyDoc;
		try {
			dependencyDoc = XmlUtils.getDocumentBuilder().parse(templateInputStream);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element dependenciesElement = (Element) dependencyDoc.getFirstChild();
		
		List<Element> springDependencies = XmlUtils.findElements("/dependencies/springWebMvc/dependency", dependenciesElement);
		for(Element dependency : springDependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependency));
		}
	}
}
