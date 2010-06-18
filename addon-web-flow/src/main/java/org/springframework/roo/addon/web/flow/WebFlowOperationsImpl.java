package org.springframework.roo.addon.web.flow;

import java.io.IOException;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.addon.web.mvc.jsp.JspOperations;
import org.springframework.roo.addon.web.mvc.jsp.menu.MenuOperations;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperations;
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
import org.springframework.roo.project.Repository;
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
@Component
@Service
public class WebFlowOperationsImpl implements WebFlowOperations {
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private MetadataService metadataService;
	@Reference private ProjectOperations projectOperations;
	@Reference private MenuOperations menuOperations;
	@Reference private WebMvcOperations webMvcOperations;
	@Reference private JspOperations jspOperations;
	@Reference private TilesOperations tilesOperations;

	public boolean isInstallWebFlowAvailable() {
		return getPathResolver() != null && !fileManager.exists(getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/spring/webflow-config.xml"));
	}

	public boolean isManageWebFlowAvailable() {
		return fileManager.exists(getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/spring/webflow-config.xml"));
	}

	/**
	 * 
	 * @param flowName
	 */
	public void installWebFlow(String flowName) {
		// If this param is not defined we make 'sample-flow' default directory
		flowName = ((flowName != null && flowName.length() != 0) ? flowName : "sample-flow");

		// Clean up leading '/' if required
		if (flowName.startsWith("/")) {
			flowName = flowName.substring(1);
		}

		try {
			if (!fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/spring/webflow-config.xml"))) {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "webflow-config.xml"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/spring/webflow-config.xml")).getOutputStream());
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		// Adjust MVC config to accommodate Spring Web Flow
		String mvcContextPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/spring/webmvc-config.xml");
		MutableFile mvcContextMutableFile = null;

		Document mvcAppCtx;
		try {
			if (!fileManager.exists(mvcContextPath)) {
				webMvcOperations.installAllWebMvcArtifacts();
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

		String flowDirectoryId = flowName.replaceAll("[^a-zA-Z/_]", "");
		String flowDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/" + flowDirectoryId);

		// Install flow directory
		if (!fileManager.exists(flowDirectory)) {
			fileManager.createDirectory(flowDirectory);
		}

		String flowConfig = flowDirectory.concat("/config-flow.xml");
		if (!fileManager.exists(flowConfig)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "flow-template.xml"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/" + flowDirectoryId + "/config-flow.xml")).getOutputStream());
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

		try {
			FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "view-state-1.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/" + flowDirectoryId + "/view-state-1.jspx")).getOutputStream());
			FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "view-state-2.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/" + flowDirectoryId + "/view-state-2.jspx")).getOutputStream());
			FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "end-state.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/" + flowDirectoryId + "/end-state.jspx")).getOutputStream());
		} catch (IOException e) {
			new IllegalStateException("Encountered an error during copying of resources for Web Flow addon.", e);
		}
		
		JavaSymbolName name = new JavaSymbolName(flowName.replaceAll("[^a-zA-Z_]", ""));

		// Add 'create new' menu item
		menuOperations.addMenuItem(name, name, name, "webflow_menu_enter", "/" + flowDirectoryId, null);

		tilesOperations.addViewDefinition(flowDirectoryId, "view-state-1", TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + flowDirectoryId + "/view-state-1.jspx");
		tilesOperations.addViewDefinition(flowDirectoryId, "view-state-2", TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + flowDirectoryId + "/view-state-2.jspx");
		tilesOperations.addViewDefinition(flowDirectoryId, "end-state", TilesOperations.DEFAULT_TEMPLATE, "/WEB-INF/views/" + flowDirectoryId + "/end-state.jspx");

		updateConfiguration();
	}

	private void updateConfiguration() {
		Element configuration = XmlUtils.getConfiguration(getClass());

		List<Element> springDependencies = XmlUtils.findElements("/configuration/springWebFlow/dependencies/dependency", configuration);
		for (Element dependency : springDependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependency));
		}
		
		List<Element> repositories = XmlUtils.findElements("/configuration/springWebFlow/repositories/repository", configuration);
		for (Element repository : repositories) {
			projectOperations.addRepository(new Repository(repository));
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