package org.springframework.roo.addon.dbre;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.dbre.model.DatabaseXmlUtils;
import org.springframework.roo.addon.dbre.model.DbreModelService;
import org.springframework.roo.addon.dbre.model.Schema;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.DomUtils;
import org.springframework.roo.support.util.StringUtils;
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
	
	// Constants
	private static final Logger logger = HandlerUtils.getLogger(DbreOperationsImpl.class);
	
	// Fields
	@Reference private DbreModelService dbreModelService;
	@Reference private FileManager fileManager;
	@Reference private ProjectOperations projectOperations;

	public boolean isDbreAvailable() {
		return projectOperations.isProjectAvailable() && (fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SPRING_CONFIG_ROOT, "database.properties")) || fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml")));
	}

	public void displayDatabaseMetadata(Set<Schema> schemas, File file, boolean view) {
		Assert.notNull(schemas, "Schemas required");

		// Force it to refresh the database from the actual JDBC connection
		Database database = dbreModelService.refreshDatabase(schemas, view, Collections.<String> emptySet() , Collections.<String> emptySet());
		database.setIncludeNonPortableAttributes(true);
		outputSchemaXml(database, schemas, file, true);
	}

	public void reverseEngineerDatabase(final Set<Schema> schemas, final JavaPackage destinationPackage, final boolean testAutomatically, final boolean view, final Set<String> includeTables, final Set<String> excludeTables, final boolean includeNonPortableAttributes, final boolean activeRecord) {
		// Force it to refresh the database from the actual JDBC connection
		final Database database = dbreModelService.refreshDatabase(schemas, view, includeTables, excludeTables);
		database.setActiveRecord(activeRecord);
		database.setDestinationPackage(destinationPackage);
		database.setIncludeNonPortableAttributes(includeNonPortableAttributes);
		database.setTestAutomatically(testAutomatically);
		outputSchemaXml(database, schemas, null, false);
		
		// Update the pom.xml to add an exclusion for the DBRE XML file in the maven-war-plugin 
		updatePom();
		
		// Change the persistence.xml file to prevent tables being created and dropped.
		updatePersistenceXml();
	}
	
	private void outputSchemaXml(Database database, Set<Schema> schemas, File file, boolean displayOnly) {
		if (database == null) {
			logger.warning("Cannot obtain database information for schema(s) '" + StringUtils.collectionToCommaDelimitedString(schemas) + "'");
		} else if (!database.hasTables()) {
			logger.warning("Schema(s) '" + StringUtils.collectionToCommaDelimitedString(schemas) + "' do not exist or does not have any tables. Note that the schema names of some databases are case-sensitive");
		} else {
			try {
				if (displayOnly) {
					Document document = DatabaseXmlUtils.getDatabaseDocument(database);
					OutputStream outputStream = file != null ? new FileOutputStream(file) : new ByteArrayOutputStream();
					XmlUtils.writeXml(outputStream, document);
					logger.info(file != null ? "Database metadata written to file " + file.getAbsolutePath() : outputStream.toString());
				} else {
					dbreModelService.writeDatabase(database);
				}
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}
	
	private void updatePom() {
		String pom = projectOperations.getPathResolver().getIdentifier(Path.ROOT, "pom.xml");
		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();
		
		String warPluginXPath = "/project/build/plugins/plugin[artifactId = 'maven-war-plugin']";
		Element warPluginElement = XmlUtils.findFirstElement(warPluginXPath, root);
		if (warPluginElement == null ) {
			// Project may not be a web project, so just exit 
			return;
		}
		Element excludeElement = XmlUtils.findFirstElement(warPluginXPath + "/configuration/webResources/resource/excludes/exclude[text() = '" + DbreModelService.DBRE_XML + "']", root);
		if (excludeElement != null) {
			// <exclude> element is already there, so just exit 
			return;
		}
		
		Element configurationElement = XmlUtils.findFirstElement("configuration", warPluginElement);
		if (configurationElement == null) {
			configurationElement = document.createElement("configuration");
		}
		Element webResourcesElement = XmlUtils.findFirstElement("configuration/webResources", warPluginElement);
		if (webResourcesElement == null) {
			webResourcesElement = document.createElement("webResources");
		}
		Element excludesElement = XmlUtils.findFirstElement("configuration/webResources/resource/excludes", warPluginElement);
		if (excludesElement == null) {
			excludesElement = document.createElement("excludes");
		}

		excludeElement = document.createElement("exclude");
		excludeElement.setTextContent(DbreModelService.DBRE_XML);
		excludesElement.appendChild(excludeElement);
		
		Element directoryElement = document.createElement("directory");
		directoryElement.setTextContent("src/main/resources");
		
		Element resourceElement = document.createElement("resource");
		resourceElement.appendChild(directoryElement);
		resourceElement.appendChild(excludesElement);
		webResourcesElement.appendChild(resourceElement);
		
		configurationElement.appendChild(webResourcesElement);
		
		warPluginElement.appendChild(configurationElement);
		DomUtils.removeTextNodes(warPluginElement);
		
		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), false);
	}

	private void updatePersistenceXml() {
		String persistencePath = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml");
		Document document = XmlUtils.readXml(fileManager.getInputStream(persistencePath));
		Element root = document.getDocumentElement();
		
		Element providerElement = XmlUtils.findFirstElement("/persistence/persistence-unit[@transaction-type = 'RESOURCE_LOCAL']/provider", root);
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
			fileManager.createOrUpdateTextFileIfRequired(persistencePath, XmlUtils.nodeToString(document), false);
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