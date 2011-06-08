package org.springframework.roo.addon.web.flow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.web.flow.XmlTemplate.DomElementCallback;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.addon.web.mvc.jsp.JspOperations;
import org.springframework.roo.addon.web.mvc.jsp.menu.MenuOperations;
import org.springframework.roo.addon.web.mvc.jsp.tiles.TilesOperations;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.ProjectType;
import org.springframework.roo.project.Repository;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides Web Flow configuration operations.
 * 
 * @author Stefan Schmidt
 * @author Rossen Stoyanchev
 * @since 1.0
 */
@Component
@Service
public class WebFlowOperationsImpl implements WebFlowOperations {
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;
	@Reference private MenuOperations menuOperations;
	@Reference private WebMvcOperations webMvcOperations;
	@Reference private JspOperations jspOperations;
	@Reference private TilesOperations tilesOperations;

	public boolean isInstallWebFlowAvailable() {
		return projectOperations.isProjectAvailable();
	}

	public boolean isManageWebFlowAvailable() {
		return isInstallWebFlowAvailable() && fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/spring/webflow-config.xml"));
	}

	/**
	 * See {@link WebFlowOperations#installWebFlow(String)}.
	 */
	public void installWebFlow(String flowName) {
		installWebFlowConfiguration();

		final String flowId = getFlowId(flowName);
		String webRelativeFlowPath = "/WEB-INF/views/" + flowId;
		String resolvedFlowPath = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, webRelativeFlowPath);
		String resolvedFlowDefinitionPath = resolvedFlowPath + "/flow.xml";

		if (fileManager.exists(resolvedFlowPath)) {
			throw new IllegalStateException("Flow directory already exists: " + resolvedFlowPath);
		}
		fileManager.createDirectory(resolvedFlowPath);

		copyTemplate("flow.xml", resolvedFlowPath);
		copyTemplate("view-state-1.jspx", resolvedFlowPath);
		copyTemplate("view-state-2.jspx", resolvedFlowPath);
		copyTemplate("end-state.jspx", resolvedFlowPath);

		new XmlTemplate(fileManager).update(resolvedFlowDefinitionPath, new DomElementCallback() {
			public boolean doWithElement(Document document, Element root) {
				List<Element> states = XmlUtils.findElements("/flow/view-state|end-state", root);
				for (Element state : states) {
					state.setAttribute("view", flowId + "/" + state.getAttribute("id"));
				}
				return true;
			}
		});

		JavaSymbolName flowMenuCategory = new JavaSymbolName("Flows");
		JavaSymbolName flowMenuName = new JavaSymbolName(flowId.replaceAll("/", "_"));
		menuOperations.addMenuItem(flowMenuCategory, flowMenuName, flowMenuName.getReadableSymbolName(), "webflow_menu_enter", "/" + flowId, null);

		tilesOperations.addViewDefinition(flowId, flowId + "/*", TilesOperations.DEFAULT_TEMPLATE, webRelativeFlowPath + "/{1}.jspx");

		updateConfiguration();
		webMvcOperations.registerWebFlowConversionServiceExposingInterceptor();
	}

	private void installWebFlowConfiguration() {
		String resolvedSpringConfigPath = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/spring");
		if (fileManager.exists(resolvedSpringConfigPath + "/webflow-config.xml")) {
			return;
		}

		copyTemplate("webflow-config.xml", resolvedSpringConfigPath);

		String webMvcConfigPath = resolvedSpringConfigPath + "/webmvc-config.xml";
		if (!fileManager.exists(webMvcConfigPath)) {
			webMvcOperations.installAllWebMvcArtifacts();
		}
		
		jspOperations.installCommonViewArtefacts();

		new XmlTemplate(fileManager).update(webMvcConfigPath, new DomElementCallback() {
			public boolean doWithElement(Document document, Element root) {
				if (null == XmlUtils.findFirstElement("/beans/import[@resource='webflow-config.xml']", root)) {
					Element importSWF = document.createElement("import");
					importSWF.setAttribute("resource", "webflow-config.xml");
					root.appendChild(importSWF);
					return true;
				}
				return false;
			}
		});
	}

	private void updateConfiguration() {
		Element configuration = XmlUtils.getConfiguration(getClass());

		List<Dependency> dependencies = new ArrayList<Dependency>();
		List<Element> webFlowDependencies = XmlUtils.findElements("/configuration/springWebFlow/dependencies/dependency", configuration);
		for (Element d : webFlowDependencies) {
			dependencies.add(new Dependency(d));
		}
		projectOperations.addDependencies(dependencies);
		
		List<Element> repositories = XmlUtils.findElements("/configuration/springWebFlow/repositories/repository", configuration);
		for (Element r : repositories) {
			projectOperations.addRepository(new Repository(r));
		}
		projectOperations.updateProjectType(ProjectType.WAR);
	}

	private String getFlowId(String flowName) {
		flowName = StringUtils.hasText(flowName) ? flowName : "sample-flow";
		if (flowName.startsWith("/")) {
			flowName = flowName.substring(1);
		}
		return flowName.replaceAll("[^a-zA-Z/_]", "");
	}

	private void copyTemplate(String templateFileName, String resolvedTargetDirectoryPath) {
		try {
			FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), templateFileName), fileManager.createFile(resolvedTargetDirectoryPath + "/" + templateFileName).getOutputStream());
		} catch (IOException e) {
			throw new IllegalStateException("Encountered an error during copying of resources for Web Flow addon.", e);
		}
	}
}