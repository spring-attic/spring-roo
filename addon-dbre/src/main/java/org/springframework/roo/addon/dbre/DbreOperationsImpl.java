package org.springframework.roo.addon.dbre;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Set;
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
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
		if (schema == null && dbreModelService.supportsSchema()) {
			throw new IllegalStateException("Schema required");
		}
		
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

	public void reverseEngineerDatabase(Schema schema, JavaPackage destinationPackage, Set<String> excludeTables) {
		if (destinationPackage == null) {
			// No destination package, so verify that DBRE has run before and thus we know where to put the entities
			Assert.notNull(dbreDatabaseListener.getDestinationPackage(), "Must specify a destination package given no prior database introspection entities are available");
		} else {
			// User provided a destination package, so set it for use before we complete the introspection
			dbreDatabaseListener.setDestinationPackage(destinationPackage);
		}
		
		if (excludeTables == null) {
			excludeTables = dbreModelService.getExcludeTables();
		}
		dbreModelService.setExcludeTables(excludeTables);
		
		if (schema == null && dbreModelService.supportsSchema()) {
			// No schema, so try to look it up
			schema = dbreModelService.getLastSchema();
			Assert.notNull(schema, "Schema must be specified given no prior database introspection information is available");
		}
		// Force it to refresh the database from the actual JDBC connection
		dbreModelService.refreshDatabase(schema);

		// Change the persistence.xml file to prevent tables being created and dropped.
		updatePersistenceXml();
	}

	private void updatePersistenceXml() {
		String persistencePath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml");
		MutableFile persistenceMutableFile = null;

		Document persistence;
		try {
			persistenceMutableFile = fileManager.updateFile(persistencePath);
			persistence = XmlUtils.getDocumentBuilder().parse(persistenceMutableFile.getInputStream());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element root = persistence.getDocumentElement();
		Element providerElement = XmlUtils.findFirstElement("/persistence/persistence-unit/provider", root);
		Assert.notNull(providerElement, "/persistence/persistence-unit/provider is null");
		String provider = providerElement.getTextContent();
		Element propertyElement = null;
		boolean changed = false;
		if (provider.contains("hibernate")) {
			changed = setPropertyValue(root, propertyElement, "hibernate.hbm2ddl.auto", "validate");
		} else if (provider.contains("openjpa")) {
			changed = setPropertyValue(root, propertyElement, "openjpa.jdbc.SynchronizeMappings", "validate");
		} else if (provider.contains("eclipse")) {
			changed = setPropertyValue(root, propertyElement, "eclipselink.ddl-generation", "none");
		} else if (provider.contains("datanucleus")) {
			changed = setPropertyValue(root, propertyElement, "datanucleus.autoCreateSchema", "false");
			changed |= setPropertyValue(root, propertyElement, "datanucleus.autoCreateTables", "false");
			changed |= setPropertyValue(root, propertyElement, "datanucleus.autoCreateColumns", "false");
			changed |= setPropertyValue(root, propertyElement, "datanucleus.autoCreateConstraints", "false");
			changed |= setPropertyValue(root, propertyElement, "datanucleus.validateTables", "false");
			changed |= setPropertyValue(root, propertyElement, "datanucleus.validateConstraints", "false");
		} else {
			throw new IllegalStateException("Persistence provider " + provider + " is not supported");
		}

		if (changed) {
			XmlUtils.writeXml(persistenceMutableFile.getOutputStream(), persistence);
		}
	}

	private boolean setPropertyValue(Element root, Element propertyElement, String name, String value) {
		boolean changed = false;
		propertyElement = XmlUtils.findFirstElement("/persistence/persistence-unit/properties/property[@name = '" + name + "']", root);
		if (propertyElement != null && !propertyElement.getAttribute("value").equals(value)) {
			propertyElement.setAttribute("value", value);
			changed = true;
		}
		return changed;
	}
}