package org.springframework.roo.addon.dbre;

import java.io.File;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.model.Schema;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.logging.HandlerUtils;

/**
 * Provides database reverse engineering operations.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DbreOperationsImpl implements DbreOperations {
	private static final Logger logger = HandlerUtils.getLogger(DbreOperationsImpl.class);
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private MetadataService metadataService;
	@Reference private DatabaseModelService databaseModelService;

	public boolean isDbreAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null && (fileManager.exists(pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "database.properties")) || fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml")));
	}

	public void displayDatabaseMetadata(Schema schema, File file) {
		if (file != null) {
			databaseModelService.serializeDatabaseMetadata(schema, null, file);
			logger.info("Database metadata written to file " + file.getAbsolutePath());
		} else {
			logger.info(databaseModelService.getDatabaseMetadata(schema, null));
		}
	}

	public void serializeDatabaseMetadata(Schema schema, JavaPackage javaPackage) {
		databaseModelService.serializeDatabaseMetadata(schema, javaPackage, null);
	}
}