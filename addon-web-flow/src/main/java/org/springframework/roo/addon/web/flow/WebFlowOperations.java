package org.springframework.roo.addon.web.flow;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.roo.addon.mvc.jsp.JspOperations;
import org.springframework.roo.addon.mvc.jsp.TilesOperations;
import org.springframework.roo.addon.web.menu.MenuOperations;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.ProjectType;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Provides Web Flow configuration operations.
 *
 * @author Stefan Schmidt
 * @since 1.0
 */
@ScopeDevelopment
public class WebFlowOperations {
	
	Logger logger = Logger.getLogger(WebFlowOperations.class.getName());
	
	private FileManager fileManager;
	private PathResolver pathResolver;
	private MetadataService metadataService;
	private ProjectOperations projectOperations;
	private MenuOperations menuOperations;
	private WebMvcOperations webMvcOperations;
	private JspOperations jspOperations;
	
	public WebFlowOperations(FileManager fileManager, PathResolver pathResolver, MetadataService metadataService, ProjectOperations projectOperations, MenuOperations menuOperations, WebMvcOperations webMvcOperations, JspOperations jspOperations) {
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(pathResolver, "Path resolver required");
		Assert.notNull(metadataService, "Metadata service required");
		Assert.notNull(projectOperations, "Project operations required");
		Assert.notNull(menuOperations, "Menu operations required");
		Assert.notNull(webMvcOperations, "Web MVC operations required");
		Assert.notNull(jspOperations, "Jsp operations required");
		this.fileManager = fileManager;
		this.pathResolver = pathResolver;
		this.metadataService = metadataService;
		this.projectOperations = projectOperations;
		this.menuOperations = menuOperations;
		this.webMvcOperations = webMvcOperations;
		this.jspOperations = jspOperations;
	}
	
	public boolean isInstallWebFlowAvailable() {		
		return getPathResolver() != null && !fileManager.exists(getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/config/webflow-config.xml"));
	}
	
	public boolean isManageWebFlowAvailable() {
		return fileManager.exists(getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/config/webflow-config.xml"));
	}
	
	/**
	 * 
	 * @param flowName
	 */
	public void installWebFlow(String flowName) {
		//if this param is not defined we make 'sample-flow' default directory
		flowName = ((flowName != null && flowName.length() != 0) ? flowName : "sample-flow");
		
		//clean up leading '/' if required
		if(flowName.startsWith("/")) {
			flowName = flowName.substring(1);
		}
		
		try {
			if (!fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/config/webflow-config.xml"))) {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "webflow-config.xml"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/config/webflow-config.xml")).getOutputStream());
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} 	
		
		//adjust MVC config to accommodate Spring Web Flow
		String mvcContextPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/config/webmvc-config.xml");
		MutableFile mvcContextMutableFile = null;
		
		Document mvcAppCtx;
		try {
			if (!fileManager.exists(mvcContextPath)) {
				webMvcOperations.installMvcArtefacts();		
				jspOperations.installCommonViewArtefacts();
			} 
			mvcContextMutableFile = fileManager.updateFile(mvcContextPath);
			mvcAppCtx = XmlUtils.getDocumentBuilder().parse(mvcContextMutableFile.getInputStream());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} 				
		
		Element root = mvcAppCtx.getDocumentElement();

		if (null == XmlUtils.findFirstElement("/beans/import[@resource='webflow-config.xml']", root)) {
			Element importSWF = mvcAppCtx.createElement("import");
			importSWF.setAttribute("resource", "webflow-config.xml");
			root.appendChild(importSWF);
			XmlUtils.writeXml(mvcContextMutableFile.getOutputStream(), mvcAppCtx);	
		}
		
		String flowDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/" + flowName);
		
		//install flow directory
		if (!fileManager.exists(flowDirectory)) {			
			fileManager.createDirectory(flowDirectory);
		}
		
		String flowConfig = flowDirectory.concat("/" + flowName + "-flow.xml");
		if (!fileManager.exists(flowConfig)) {			
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "flow-template.xml"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/" + flowName + "/" + flowName + "-flow.xml")).getOutputStream());
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		
		try {
			FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "view-state-1.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/" + flowName + "/view-state-1.jspx")).getOutputStream());
			FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "view-state-2.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/" + flowName + "/view-state-2.jspx")).getOutputStream());		
			FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "end-state.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/" + flowName + "/end-state.jspx")).getOutputStream());	
		} catch (IOException e) {
			new IllegalStateException("Encountered an error during copying of resources for Web Flow addon.", e);
		}

		//add 'create new' menu item
		menuOperations.addMenuItem(
				"web_flow_category", 
				new JavaSymbolName(flowName), 
				"web_flow_" + flowName.toLowerCase().replaceAll("-", "_") + "_menu_item", 
				new JavaSymbolName(flowName),
				"webflow.menu.enter",
				"/" + flowName);
		
		TilesOperations tilesOperations = new TilesOperations(flowName, fileManager, pathResolver, "config/webmvc-config.xml");
		tilesOperations.addViewDefinition("view-state-1", TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + flowName + "/view-state-1.jspx");
		tilesOperations.addViewDefinition("view-state-2", TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + flowName + "/view-state-2.jspx");
		tilesOperations.addViewDefinition("end-state", TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + flowName + "/end-state.jspx");
		tilesOperations.writeToDiskIfNecessary();

		updateDependencies();
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
		
		List<Element> springDependencies = XmlUtils.findElements("/dependencies/springWebFlow/dependency", dependenciesElement);
		for(Element dependency : springDependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependency));
		}
		
		projectOperations.updateProjectType(ProjectType.WAR);
	}	
	
	private PathResolver getPathResolver() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (projectMetadata == null) {
			return null;
		}
		return projectMetadata.getPathResolver();
	}
}