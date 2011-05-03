package org.springframework.roo.project;

import java.io.InputStream;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides Spring application context-related operations.
 *  
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.0
 */
@Component
@Service
public class ApplicationContextOperationsImpl implements ApplicationContextOperations {
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	
	public void createMiddleTierApplicationContext() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Assert.notNull(projectMetadata, "Project metadata required");
		
		Document document;
		try {
			InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "applicationContext-template.xml");
			document = XmlUtils.readXml(templateInputStream);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		
		Element root = document.getDocumentElement();
		XmlUtils.findFirstElementByName("context:component-scan", root).setAttribute("base-package", projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName());

		fileManager.createOrUpdateTextFileIfRequired(projectMetadata.getPathResolver().getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext.xml"), XmlUtils.nodeToString(document), false);

		fileManager.scan();
	}
}
