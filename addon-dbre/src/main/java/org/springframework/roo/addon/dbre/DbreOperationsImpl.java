package org.springframework.roo.addon.dbre;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.dbre.model.DatabaseXmlUtils;
import org.springframework.roo.addon.dbre.model.Schema;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;

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
	@Reference private DbreModelService dbreModelService;
	@Reference private DbreDatabaseListener dbreDatabaseListener;

	public boolean isDbreAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null && (fileManager.exists(pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "database.properties")) || fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml")));
	}

	public void displayDatabaseMetadata(Schema schema, File file) {
		Database database = dbreModelService.refreshDatabaseSafely(schema);
		if (database == null) {
			throw new IllegalStateException("Cannot obtain database information for schema '" + schema + "'");
		}
		if (file != null) {
			try {
				OutputStream outputStream = new FileOutputStream(file);
				DatabaseXmlUtils.writeDatabaseStructureToOutputStream(database, outputStream);
				logger.info("Database metadata written to file " + file.getAbsolutePath());
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		} else {
			logger.info(database.toString());
		}
	}
	
	public void reverseEngineerDatabase(Schema schema, JavaPackage destinationPackage) {
		if (destinationPackage == null) {
			// No destination package, so verify that DBRE has run before and thus we know where to put the entities
			Assert.notNull(dbreDatabaseListener.getDestinationPackage(), "Must specify a destination package given no prior database introspection entities are available");
		} else {
			// User provided a destination package, so set it for use before we complete the introspection
			dbreDatabaseListener.setDestinationPackage(destinationPackage);
		}
		if (schema == null) {
			// No schema, so try to look it up
			schema = dbreModelService.getLastSchema();
			Assert.notNull(schema, "Schema must be specified given no prior database introspection information is available");
		}
		// Force it to refresh the database from the actual JDBC connection
		dbreModelService.refreshDatabase(schema);
	}
}