package org.springframework.roo.addon.maven;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;

import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
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
 * Provides web.xml project operations.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopment
public class WebXmlOperations {
	
	private FileManager fileManager;
	private MetadataService metadataService;
	
	public WebXmlOperations(FileManager fileManager, MetadataService metadataService) {
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(metadataService, "Metadata service required");
		this.fileManager = fileManager;
		this.metadataService = metadataService;
	}
	
	public void copyUrlRewrite(){
		
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.isTrue(projectMetadata != null, "Project metadata required");
		
		String urlrewriteFilename = "WEB-INF/urlrewrite.xml";
		PathResolver pathResolver = projectMetadata.getPathResolver();
		Assert.isTrue(!fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, urlrewriteFilename)), "'" + urlrewriteFilename + "' does already exist");
		
		try {
			FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "urlrewrite-template.xml"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/urlrewrite.xml")).getOutputStream());
		} catch (IOException e) {
			new IllegalStateException("Encountered an error during copying of resources for maven addon.", e);
		}
	}

	public void createWebXml() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.isTrue(projectMetadata != null, "Project metadata required");
		
		// Verify the servlet application context already exists
		String servletCtxFilename = "WEB-INF/" + projectMetadata.getProjectName() + "-servlet.xml";
		PathResolver pathResolver = projectMetadata.getPathResolver();
		Assert.isTrue(fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, servletCtxFilename)), "'" + servletCtxFilename + "' does not exist");
		
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
	
	public void createIndexJsp() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.isTrue(projectMetadata != null, "Project metadata required");
		
		// Verify the web.xml already exists
		PathResolver pathResolver = projectMetadata.getPathResolver();
		Assert.isTrue(fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml")), "web.xml does not exist");
		
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

}
