package org.springframework.roo.addon.mvc.jsp;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.lifecycle.ScopeDevelopment;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;

/**
 * Provides operations to create various view layer resources.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@ScopeDevelopment
public class JspOperations {

	private FileManager fileManager;
	private MetadataService metadataService;

	public JspOperations(FileManager fileManager, MetadataService metadataService) {
		Assert.notNull(fileManager, "File manager required");
		Assert.notNull(metadataService, "Metadata service required");

		this.fileManager = fileManager;
		this.metadataService = metadataService;			
	}

	public void installCommonViewArtefacts() {			
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Unable to obtain project metadata");

		PathResolver pathResolver = projectMetadata.getPathResolver();
		Assert.notNull(projectMetadata, "Unable to obtain path resolver");	

		String imagesDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images");
		if (!fileManager.exists(imagesDirectory)) {
			fileManager.createDirectory(imagesDirectory);
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/banner-graphic.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/banner-graphic.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/springsource-logo.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/springsource-logo.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/resultset_first.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/resultset_first.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/resultset_next.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/resultset_next.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/resultset_previous.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/resultset_previous.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/resultset_last.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/resultset_last.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/us.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/us.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/de.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/de.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/sv.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/sv.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/list.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/list.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/add.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/add.png")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}
		}

		String cssDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "styles");
		if (!fileManager.exists(cssDirectory)) {
			fileManager.createDirectory(cssDirectory);
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "styles/roo-menu-left.css"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "styles/roo-menu-left.css")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "styles/roo-menu-right.css"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "styles/roo-menu-right.css")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "styles/left.properties"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/classes/left.properties")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "styles/right.properties"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/classes/right.properties")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for view layer.", e);
			}
		}
		
		String layoutsDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/layouts");
		if (!fileManager.exists(layoutsDirectory)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "layout/layouts.xml"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/layouts/layouts.xml")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "layout/default.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/layouts/default.jspx")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}
		}		

		String jspDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/views");
		if (!fileManager.exists(jspDirectory)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "dataAccessFailure.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/dataAccessFailure.jspx")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "resourceNotFound.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/resourceNotFound.jspx")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "uncaughtException.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/uncaughtException.jspx")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "index.jspx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/index.jspx")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "layout/views.xml"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/views/views.xml")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}
		}
		
		String tagsDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/tags");
		if (!fileManager.exists(tagsDirectory)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "tags/pagination.tagx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/pagination.tagx")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "tags/language.tagx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/language.tagx")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "tags/theme.tagx"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/tags/theme.tagx")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}
		}
		
		String i18nDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/i18n");
		if (!fileManager.exists(i18nDirectory)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "i18n/messages.properties"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages.properties")).getOutputStream());
				changeProperties(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages.properties"), "welcome.titlepane", "[roo_replace_app_name]", projectMetadata.getProjectName());
				changeProperties(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages.properties"), "welcome.h3", "[roo_replace_app_name]", projectMetadata.getProjectName());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "i18n/messages_de.properties"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_de.properties")).getOutputStream());
				changeProperties(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_de.properties"), "welcome.titlepane", "[roo_replace_app_name]", projectMetadata.getProjectName());
				changeProperties(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_de.properties"), "welcome.h3", "[roo_replace_app_name]", projectMetadata.getProjectName());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "i18n/messages_sv.properties"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_sv.properties")).getOutputStream());
				changeProperties(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_sv.properties"), "welcome.titlepane", "[roo_replace_app_name]", projectMetadata.getProjectName());
				changeProperties(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/i18n/messages_sv.properties"), "welcome.h3", "[roo_replace_app_name]", projectMetadata.getProjectName());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}
		}
	}	
	
	private void changeProperties(String fileIdentifier, String propKey, String replace, String with) {
		MutableFile mutableFile = fileManager.updateFile(fileIdentifier);
		Properties props = new Properties();
		try {
			props.load(mutableFile.getInputStream());
			String welcome = (String) props.get(propKey);
			if(null != welcome && welcome.length() > 0) {
				props.setProperty(propKey, welcome.replace(replace, with));
				props.store(mutableFile.getOutputStream(), "Updated at " + new Date());
			}
		} catch (IOException e) {
			new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
		}	
	}
}
