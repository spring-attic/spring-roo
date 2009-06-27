package org.springframework.roo.addon.mvc.jsp;

import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
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
		}
		String imageFile = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/banner-graphic.png");
		if (!fileManager.exists(imageFile)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/banner-graphic.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/banner-graphic.png")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "images/springsource-logo.png"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "images/springsource-logo.png")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}
		}

		String cssDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "styles");
		if (!fileManager.exists(cssDirectory)) {
			fileManager.createDirectory(cssDirectory);
		}
		String cssFile = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "styles/roo.css");
		if (!fileManager.exists(cssFile)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "styles/roo.css"), fileManager.createFile(cssFile).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for view layer.", e);
			}
		}

		String jspDirectory = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/jsp");
		if (!fileManager.exists(jspDirectory)) {
			fileManager.createDirectory(jspDirectory);
		}
		String headerFile = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/jsp/header.jsp");
		if (!fileManager.exists(headerFile)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "header.jsp"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/jsp/header.jsp")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "footer.jsp"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/jsp/footer.jsp")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "includes.jsp"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/jsp/includes.jsp")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "dataAccessFailure.jsp"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/jsp/dataAccessFailure.jsp")).getOutputStream());
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "uncaughtException.jsp"), fileManager.createFile(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/jsp/uncaughtException.jsp")).getOutputStream());
			} catch (Exception e) {
				new IllegalStateException("Encountered an error during copying of resources for MVC JSP addon.", e);
			}
		}
	}

}
