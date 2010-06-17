package org.springframework.roo.addon.dbre;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.db.DbModel;
import org.springframework.roo.addon.dbre.db.IdentifiableTable;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.StringUtils;

/**
 * Provides database reverse engineering configuration operations.
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
	@Reference private DbModel dbModel;

	public boolean isDbreAvailable() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null && fileManager.exists(pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "database.properties"));
	}

	public void displayDbMetadata(String table, String file) {
		String dbMetadata = dbModel.getDbMetadata(new IdentifiableTable(null, null, table));
		if (StringUtils.hasText(dbMetadata)) {
			if (StringUtils.hasText(file)) {
				try {
					FileCopyUtils.copy(dbMetadata, new FileWriter(new File(file)));
					logger.info("Metadata written to file " + file);
				} catch (IOException e) {
					logger.warning("Unable to write metadata: " + e.getMessage());
				}
			} else {
				logger.info(dbMetadata);
			}
		} else {
			logger.warning("Database metadata unavailable" + (StringUtils.hasLength(table) ? " for table " + table : ""));
		}
	}

	public void updateDbreXml(JavaPackage javaPackage) {
		dbModel.setJavaPackage(javaPackage);
		dbModel.serialize();
	}
}