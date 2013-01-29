package org.springframework.roo.project;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.FileUtils;
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
public class ApplicationContextOperationsImpl implements
        ApplicationContextOperations {

    @Reference private FileManager fileManager;
    @Reference private MetadataService metadataService;
    @Reference private PathResolver pathResolver;

    public void createMiddleTierApplicationContext(
            final JavaPackage topLevelPackage, final String moduleName) {
        final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService
                .get(ProjectMetadata.getProjectIdentifier(moduleName));
        Validate.notNull(projectMetadata,
                "Project metadata required for module '%s'", moduleName);
        final Document document = XmlUtils.readXml(FileUtils.getInputStream(
                getClass(), "applicationContext-template.xml"));
        final Element root = document.getDocumentElement();
        DomUtils.findFirstElementByName("context:component-scan", root)
                .setAttribute("base-package",
                        topLevelPackage.getFullyQualifiedPackageName());
        fileManager.createOrUpdateTextFileIfRequired(pathResolver
                .getIdentifier(
                        Path.SPRING_CONFIG_ROOT.getModulePathId(moduleName),
                        "applicationContext.xml"), XmlUtils
                .nodeToString(document), false);
        fileManager.scan();
    }
}
