package org.springframework.roo.addon.dbre;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.dbre.model.DbreModelService;
import org.springframework.roo.addon.dbre.model.Schema;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link DbreOperations}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DbreOperationsImpl implements DbreOperations {
	private static final Logger logger = HandlerUtils.getLogger(DbreOperationsImpl.class);
	@Reference private DbreModelService dbreModelService;
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;

	public boolean isDbreAvailable() {
		return projectOperations.isProjectAvailable() && (fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SPRING_CONFIG_ROOT, "database.properties")) || fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml")));
	}

	public void displayDatabaseMetadata(Schema schema, File file, boolean view) {
		Assert.notNull(schema, "Schema required");
		// Force it to refresh the database from the actual JDBC connection
		Database database = dbreModelService.refreshDatabase(schema, view, Collections.<String> emptySet() , Collections.<String> emptySet());
		database.setIncludeNonPortableAttributes(true);
		processDatabase(database, schema, file, true);
	}

	public void reverseEngineerDatabase(Schema schema, JavaPackage destinationPackage, boolean testAutomatically, boolean view, Set<String> includeTables, Set<String> excludeTables, boolean includeNonPortableAttributes) {
		// Force it to refresh the database from the actual JDBC connection
		Database database = dbreModelService.refreshDatabase(schema, view, includeTables, excludeTables);
		database.setDestinationPackage(destinationPackage);
		database.setTestAutomatically(testAutomatically);
		database.setIncludeNonPortableAttributes(includeNonPortableAttributes);
		processDatabase(database, schema, null, false);
		
		// Update the pom.xml to add an exclusion for the dbre.xml file in the maven-war-plugin 
		updatePom();
		
		// Change the persistence.xml file to prevent tables being created and dropped.
		updatePersistenceXml();
	}
	
	private void processDatabase(Database database, Schema schema, File file, boolean displayOnly) {
		if (database == null) {
			logger.warning("Cannot obtain database information for schema '" + schema.getName() + "'");
		} else if (!database.hasTables()) {
			logger.warning("Schema '" + schema.getName() + "' does not exist or does not have any tables. Note that the schema names of some databases are case-sensitive");
		} else {
			OutputStream outputStream = null;
			try {
				outputStream = file != null ? new FileOutputStream(file) : new ByteArrayOutputStream();
				dbreModelService.serializeDatabase(database, outputStream, displayOnly);
				if (displayOnly) {
					logger.info(file != null ? "Database metadata written to file " + file.getAbsolutePath() : outputStream.toString());
				}
			} catch (Exception e) {
				throw new IllegalStateException(e);
			} finally {
				if (outputStream != null) {
					try {
						outputStream.close();
					} catch (IOException ignored) {
					}
				}
			}
		}
	}
	
	private void updatePom() {
		String pomPath = projectOperations.getPathResolver().getIdentifier(Path.ROOT, "pom.xml");
		MutableFile mutableFile = null;

		Document pom;
		try {
			if (fileManager.exists(pomPath)) {
				mutableFile = fileManager.updateFile(pomPath);
				pom = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
			} else {
				throw new IllegalStateException("Could not acquire pom.xml in " + pomPath);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		
		Element root = pom.getDocumentElement();
		
		String warPluginXPath = "/project/build/plugins/plugin[artifactId = 'maven-war-plugin']";
		Element warPluginElement = XmlUtils.findFirstElement(warPluginXPath, root);
		if (warPluginElement == null ) {
			// Project may not be a web project, so just exit 
			return;
		}
		Element excludeElement = XmlUtils.findFirstElement(warPluginXPath + "/configuration/webResources/resource/excludes/exclude[text() = 'dbre.xml']", root);
		if (excludeElement != null) {
			// <exclude> element is already there, so just exit 
			return;
		}
		
		Element configurationElement = XmlUtils.findFirstElement("configuration", warPluginElement);
		if (configurationElement == null) {
			configurationElement = pom.createElement("configuration");
		}
		Element webResourcesElement = XmlUtils.findFirstElement("configuration/webResources", warPluginElement);
		if (webResourcesElement == null) {
			webResourcesElement = pom.createElement("webResources");
		}
		Element excludesElement = XmlUtils.findFirstElement("configuration/webResources/resource/excludes", warPluginElement);
		if (excludesElement == null) {
			excludesElement = pom.createElement("excludes");
		}

		excludeElement = pom.createElement("exclude");
		excludeElement.setTextContent("dbre.xml");
		excludesElement.appendChild(excludeElement);
		
		Element directoryElement = pom.createElement("directory");
		directoryElement.setTextContent("src/main/resources");
		
		Element resourceElement = pom.createElement("resource");
		resourceElement.appendChild(directoryElement);
		resourceElement.appendChild(excludesElement);
		webResourcesElement.appendChild(resourceElement);
		
		configurationElement.appendChild(webResourcesElement);
		
		warPluginElement.appendChild(configurationElement);
		
		XmlUtils.writeXml(mutableFile.getOutputStream(), pom);
	}

	private void updatePersistenceXml() {
		String persistencePath = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml");
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
			changed |= setPropertyValue(root, propertyElement, "hibernate.ejb.naming_strategy", "org.hibernate.cfg.DefaultNamingStrategy");
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