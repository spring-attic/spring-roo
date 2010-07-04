package org.springframework.roo.addon.dbre;

import java.io.File;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.model.DatabaseModelService;
import org.springframework.roo.addon.dbre.model.Schema;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;

/**
 * Provides database reverse engineering operations.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DbreOperationsImpl implements DbreOperations {
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private MetadataService metadataService;
	@Reference private DatabaseModelService databaseModelService;

	public boolean isDbreAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null && fileManager.exists(pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "database.properties"));
	}

	public void displayDatabaseMetadata(String catalog, Schema schema, File file) {
		JavaPackage javaPackage = getTopLevelpackage();
		if (file != null) {
			databaseModelService.serializeDatabaseMetadata(catalog, schema, javaPackage, file);
		} else {
			databaseModelService.displayDatabaseMetadata(catalog, schema, javaPackage);
		}
	}

	public void serializeDatabaseMetadata(String catalog, Schema schema, JavaPackage javaPackage) {
		if (javaPackage == null) {
			javaPackage = getTopLevelpackage();
		}
		databaseModelService.serializeDatabaseMetadata(catalog, schema, javaPackage, null);
	}
	
	private JavaPackage getTopLevelpackage() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		return projectMetadata.getTopLevelPackage();
	}
}