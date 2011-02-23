package org.springframework.roo.addon.web.mvc.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.ProjectType;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.WebXmlUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides operations to create various view layer resources.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 */
@Component 
@Service
public class WebMvcOperationsImpl implements WebMvcOperations {
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;

	public void installMinmalWebArtefacts() {
		// Note that the sequence matters here as some of these artifacts are loaded further down the line
		createWebApplicationContext();
		copyWebXml();
	}

	public void installAllWebMvcArtifacts() {
		installMinmalWebArtefacts();
		manageWebXml();
		updateConfiguration();
	}

	private void copyWebXml() {
		Assert.isTrue(projectOperations.isProjectAvailable(), "Project metadata required");
		PathResolver pathResolver = projectOperations.getPathResolver();
		
		// Verify the servlet application context already exists
		String servletCtxFilename = "WEB-INF/spring/webmvc-config.xml";
		Assert.isTrue(fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, servletCtxFilename)), "'" + servletCtxFilename + "' does not exist");

		if (fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml"))) {
			// File exists, so nothing to do
			return;
		}

		InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "web-template.xml");
		Document webXml;
		try {
			webXml = XmlUtils.getDocumentBuilder().parse(templateInputStream);
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

		String projectName = projectOperations.getProjectMetadata().getProjectName();
		WebXmlUtils.setDisplayName(projectName, webXml, null);
		WebXmlUtils.setDescription("Roo generated " + projectName + " application", webXml, null);

		writeToDiskIfNecessary(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml"), webXml);
		try {
			templateInputStream.close();
		} catch (IOException ignore) {
		}
		fileManager.scan();
	}

	private void manageWebXml() {
		Assert.isTrue(projectOperations.isProjectAvailable(), "Project metadata required");
		PathResolver pathResolver = projectOperations.getPathResolver();

		// Verify that the web.xml already exists
		String webXmlFile = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");
		Assert.isTrue(fileManager.exists(webXmlFile), "'" + webXmlFile + "' does not exist");

		Document webXml;
		try {
			webXml = XmlUtils.getDocumentBuilder().parse(fileManager.getInputStream(webXmlFile));
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

		WebXmlUtils.addContextParam(new WebXmlUtils.WebXmlParam("defaultHtmlEscape", "true"), webXml, "Enable escaping of form submission contents");
		WebXmlUtils.addContextParam(new WebXmlUtils.WebXmlParam("contextConfigLocation", "classpath*:META-INF/spring/applicationContext*.xml"), webXml, null);
		WebXmlUtils.addFilter(WebMvcOperations.CHARACTER_ENCODING_FILTER_NAME, "org.springframework.web.filter.CharacterEncodingFilter", "/*", webXml, null, new WebXmlUtils.WebXmlParam("encoding", "UTF-8"), new WebXmlUtils.WebXmlParam("forceEncoding", "true"));
		WebXmlUtils.addFilter(WebMvcOperations.HTTP_METHOD_FILTER_NAME, "org.springframework.web.filter.HiddenHttpMethodFilter", "/*", webXml, null);
		if (fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"))) {
			WebXmlUtils.addFilter(WebMvcOperations.OPEN_ENTITYMANAGER_IN_VIEW_FILTER_NAME, "org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter", "/*", webXml, null);
		}
		WebXmlUtils.addListener("org.springframework.web.context.ContextLoaderListener", webXml, "Creates the Spring Container shared by all Servlets and Filters");
		WebXmlUtils.addServlet(projectOperations.getProjectMetadata().getProjectName(), "org.springframework.web.servlet.DispatcherServlet", "/", new Integer(1), webXml, "Handles Spring requests", new WebXmlUtils.WebXmlParam("contextConfigLocation", "/WEB-INF/spring/webmvc-config.xml"));
		WebXmlUtils.setSessionTimeout(new Integer(10), webXml, null);
		// WebXmlUtils.addWelcomeFile("/", webXml, null);
		WebXmlUtils.addExceptionType("java.lang.Exception", "/uncaughtException", webXml, null);
		WebXmlUtils.addErrorCode(new Integer(404), "/resourceNotFound", webXml, null);

		writeToDiskIfNecessary(webXmlFile, webXml);
	}

	private void createWebApplicationContext() {
		Assert.isTrue(projectOperations.isProjectAvailable(), "Project metadata required");
		PathResolver pathResolver = projectOperations.getPathResolver();

		// Verify the middle tier application context already exists
		Assert.isTrue(fileManager.exists(pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext.xml")), "Application context does not exist");

		if (fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml"))) {
			// This file already exists, nothing to do
			return;
		}

		InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "webmvc-config.xml");
		Document webMvcConfig;
		try {
			webMvcConfig = XmlUtils.getDocumentBuilder().parse(templateInputStream);
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

		Element rootElement = (Element) webMvcConfig.getFirstChild();
		XmlUtils.findFirstElementByName("context:component-scan", rootElement).setAttribute("base-package", projectOperations.getProjectMetadata().getTopLevelPackage().getFullyQualifiedPackageName());
		writeToDiskIfNecessary(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/spring/webmvc-config.xml"), webMvcConfig);
		try {
			templateInputStream.close();
		} catch (IOException ignore) {
		}
		fileManager.scan();
	}

	private void updateConfiguration() {
		Element configuration = XmlUtils.getConfiguration(getClass());

		List<Dependency> dependencies = new ArrayList<Dependency>();
		List<Element> springDependencies = XmlUtils.findElements("/configuration/springWebMvc/dependencies/dependency", configuration);
		for (Element dependency : springDependencies) {
			dependencies.add(new Dependency(dependency));
		}
		projectOperations.addDependencies(dependencies);
		
		projectOperations.updateProjectType(ProjectType.WAR);
	}

	/** return indicates if disk was changed (ie updated or created) */
	private boolean writeToDiskIfNecessary(String fileName, Document proposed) {
		// Build a string representation of the JSP
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		XmlUtils.writeXml(XmlUtils.createIndentingTransformer(), byteArrayOutputStream, proposed);
		String xmlContent = byteArrayOutputStream.toString();
		try {
			byteArrayOutputStream.close();
		} catch (IOException ignore) {}

		// If mutableFile becomes non-null, it means we need to use it to write out the contents of jspContent to the file
		MutableFile mutableFile = null;
		if (fileManager.exists(fileName)) {
			// First verify if the file has even changed
			File f = new File(fileName);
			String existing = null;
			try {
				existing = FileCopyUtils.copyToString(new FileReader(f));
			} catch (IOException ignoreAndJustOverwriteIt) {
			}

			if (!xmlContent.equals(existing)) {
				mutableFile = fileManager.updateFile(fileName);
			}
		} else {
			mutableFile = fileManager.createFile(fileName);
			Assert.notNull(mutableFile, "Could not create XML file '" + fileName + "'");
		}

		if (mutableFile != null) {
			try {
				// We need to write the file out (it's a new file, or the existing file has different contents)
				FileCopyUtils.copy(xmlContent, new OutputStreamWriter(mutableFile.getOutputStream()));
				
				// Return and indicate we wrote out the file
				return true;
			} catch (IOException ioe) {
				throw new IllegalStateException("Could not output '" + mutableFile.getCanonicalPath() + "'", ioe);
			}
		}

		// A file existed, but it contained the same content, so we return false
		return false;
	}
}
