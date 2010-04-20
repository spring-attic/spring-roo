package org.springframework.roo.addon.jpa;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.Repository;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Provides JPA configuration operations.
 * 
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
@Component
@Service
public class JpaOperationsImpl implements JpaOperations {
	private static final Logger logger = HandlerUtils.getLogger(JpaOperationsImpl.class);
	private static final String GAE_PERSISTENCE_UNIT_NAME = "transactions-optional";
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private MetadataService metadataService;
	@Reference private ProjectOperations projectOperations;

	public boolean isJpaInstallationPossible() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null && !fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"));
	}

	public boolean isJpaInstalled() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null && fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"));
	}

	public void configureJpa(OrmProvider ormProvider, JdbcDatabase database, String jndi) {
		Assert.notNull(ormProvider, "ORM provider required");
		Assert.notNull(database, "JDBC database required");

		updatePersistenceXml(ormProvider, database);
		if (jndi == null || jndi.length() == 0) {
			updateDatabaseProperties(ormProvider, database);
		}

		updateApplicationContext(ormProvider, database, jndi);
		updatePomProperties(ormProvider);
		updateDependencies(ormProvider, database);
		updateRepositories(ormProvider);
		updatePluginRepositories(ormProvider);
		updateBuildPlugins(ormProvider);
		updateGaeXml(ormProvider);
		
		// Remove unnecessary artifacts not specific to current JPA provider
		cleanup(ormProvider);
	}

	private void updatePomProperties(OrmProvider ormProvider) {
		List<Element> pomProperties = XmlUtils.findElements("/dependencies/ormProviders/provider[@id='" + ormProvider.name() + "']/properties/*", getDependencies());
		for (Element property : pomProperties) {
			projectOperations.addProperty(new Property(property));
		}
	}

	private void updateApplicationContext(OrmProvider ormProvider, JdbcDatabase database, String jndi) {
		String contextPath = pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext.xml");
		MutableFile contextMutableFile = null;

		Document appCtx;
		try {
			if (fileManager.exists(contextPath)) {
				contextMutableFile = fileManager.updateFile(contextPath);
				appCtx = XmlUtils.getDocumentBuilder().parse(contextMutableFile.getInputStream());
			} else {
				throw new IllegalStateException("Could not acquire applicationContext.xml in " + contextPath);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element root = appCtx.getDocumentElement();

		// Checking for existence of configurations, if found abort
		Element dataSource = XmlUtils.findFirstElement("/beans/bean[@id='dataSource']", root);
		Element dataSourceJndi = XmlUtils.findFirstElement("/beans/jndi-lookup[@id='dataSource']", root);

		if (ormProvider == OrmProvider.GOOGLE_APP_ENGINE) {
			if (dataSource != null) {
				root.removeChild(dataSource);
			}
			if (dataSourceJndi != null) {
				root.removeChild(dataSourceJndi);
			}
		} else if ((jndi == null || jndi.length() == 0) && dataSource == null) {
			dataSource = appCtx.createElement("bean");
			dataSource.setAttribute("class", "org.apache.commons.dbcp.BasicDataSource");
			dataSource.setAttribute("destroy-method", "close");
			dataSource.setAttribute("id", "dataSource");
			dataSource.appendChild(createPropertyElement("driverClassName", "${database.driverClassName}", appCtx));
			dataSource.appendChild(createPropertyElement("url", "${database.url}", appCtx));
			dataSource.appendChild(createPropertyElement("username", "${database.username}", appCtx));
			dataSource.appendChild(createPropertyElement("password", "${database.password}", appCtx));
			root.appendChild(dataSource);
			if (dataSourceJndi != null) {
				dataSourceJndi.getParentNode().removeChild(dataSourceJndi);
			}
		} else if (jndi != null && jndi.length() > 0) {
			if (dataSourceJndi == null) {
				dataSourceJndi = appCtx.createElement("jee:jndi-lookup");
				dataSourceJndi.setAttribute("id", "dataSource");
				root.appendChild(dataSourceJndi);
			}
			dataSourceJndi.setAttribute("jndi-name", jndi);
			if (dataSource != null) {
				dataSource.getParentNode().removeChild(dataSource);
			}
		}

		Element transactionManager = XmlUtils.findFirstElement("/beans/bean[@id='transactionManager']", root);
		if (transactionManager == null) {
			transactionManager = appCtx.createElement("bean");
			transactionManager.setAttribute("id", "transactionManager");
			transactionManager.setAttribute("class", "org.springframework.orm.jpa.JpaTransactionManager");
			transactionManager.appendChild(createRefElement("entityManagerFactory", "entityManagerFactory", appCtx));
			root.appendChild(transactionManager);
		}

		Element aspectJTxManager = XmlUtils.findFirstElement("/beans/annotation-driven", root);
		if (aspectJTxManager == null) {
			aspectJTxManager = appCtx.createElement("tx:annotation-driven");
			aspectJTxManager.setAttribute("mode", "aspectj");
			aspectJTxManager.setAttribute("transaction-manager", "transactionManager");
			root.appendChild(aspectJTxManager);
		}

		Element entityManagerFactory = XmlUtils.findFirstElement("/beans/bean[@id='entityManagerFactory']", root);
		if (entityManagerFactory != null) {
			root.removeChild(entityManagerFactory);
		}

		entityManagerFactory = appCtx.createElement("bean");
		entityManagerFactory.setAttribute("id", "entityManagerFactory");
		if (ormProvider == OrmProvider.GOOGLE_APP_ENGINE) {
			entityManagerFactory.setAttribute("class", "org.springframework.orm.jpa.LocalEntityManagerFactoryBean");
			entityManagerFactory.appendChild(createPropertyElement("persistenceUnitName", GAE_PERSISTENCE_UNIT_NAME, appCtx));
		} else {
			entityManagerFactory.setAttribute("class", "org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean");
			entityManagerFactory.appendChild(createRefElement("dataSource", "dataSource", appCtx));
		}
		root.appendChild(entityManagerFactory);

		XmlUtils.writeXml(contextMutableFile.getOutputStream(), appCtx);
	}

	private void updateDatabaseProperties(OrmProvider ormProvider, JdbcDatabase database) {
		String databasePath = pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "database.properties");

		MutableFile databaseMutableFile = null;
		Properties props = new Properties();

		try {
			if (fileManager.exists(databasePath)) {
				databaseMutableFile = fileManager.updateFile(databasePath);
				props.load(databaseMutableFile.getInputStream());
			} else {
				databaseMutableFile = fileManager.createFile(databasePath);
				InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "database-template.properties");
				Assert.notNull(templateInputStream, "Could not acquire database properties template");
				props.load(templateInputStream);
			}
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}

		props.put("database.driverClassName", database.getDriverClassName());

		String connectionString = database.getConnectionString();
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		connectionString = connectionString.replace("TO_BE_CHANGED_BY_ADDON", projectMetadata.getProjectName());
		props.put("database.url", connectionString);

		if (database.equals(JdbcDatabase.HYPERSONIC_IN_MEMORY) || database.equals(JdbcDatabase.HYPERSONIC_PERSISTENT) || database.equals(JdbcDatabase.H2_IN_MEMORY)) {
			props.put("database.username", "sa");
		} else {
			props.put("database.username", "");
			logger.warning("Please enter your database details in src/main/resources/META-INF/spring/database.properties.");
		}

		props.put("database.password", "");

		try {
			props.store(databaseMutableFile.getOutputStream(), "Updated at " + new Date());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}

	private void updateDependencies(OrmProvider ormProvider, JdbcDatabase database) {
		Element dependencies = getDependencies();

		List<Element> databaseDependencies = XmlUtils.findElements("/dependencies/databases/database[@id='" + database.getKey() + "']/dependency", dependencies);
		for (Element dependencyElement : databaseDependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependencyElement));
		}

		List<Element> ormDependencies = XmlUtils.findElements("/dependencies/ormProviders/provider[@id='" + ormProvider.name() + "']/dependency", dependencies);
		for (Element dependencyElement : ormDependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependencyElement));
		}

		// Hard coded to JPA & Hibernate Validator for now
		List<Element> jpaDependencies = XmlUtils.findElements("/dependencies/persistence/provider[@id='JPA']/dependency", dependencies);
		for (Element dependencyElement : jpaDependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependencyElement));
		}

		List<Element> springDependencies = XmlUtils.findElements("/dependencies/spring/dependency", dependencies);
		for (Element dependencyElement : springDependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependencyElement));
		}

		if (database.getKey().equals(JdbcDatabase.ORACLE.getKey()) || database.getKey().equals(JdbcDatabase.DB2.getKey())) {
			logger.warning("The " + database.getKey() + " JDBC driver is not available in public maven repositories. Please adjust the pom.xml dependency to suit your needs");
		}
	}

	private void updateRepositories(OrmProvider ormProvider) {
		Element dependencies = getDependencies();

		List<Element> repositories = XmlUtils.findElements("/dependencies/ormProviders/provider[@id='" + ormProvider.name() + "']/repository", dependencies);
		for (Element repositoryElement : repositories) {
			projectOperations.addRepository(new Repository(repositoryElement));
		}

		List<Element> jpaRepositories = XmlUtils.findElements("/dependencies/persistence/provider[@id='JPA']/repository", dependencies);
		for (Element repositoryElement : jpaRepositories) {
			projectOperations.addRepository(new Repository(repositoryElement));
		}
	}

	private void updatePluginRepositories(OrmProvider ormProvider) {
		List<Element> pluginRepositories = XmlUtils.findElements("/dependencies/ormProviders/provider[@id='" + ormProvider.name() + "']/pluginRepository", getDependencies());
		for (Element pluginRepositoryElement : pluginRepositories) {
			projectOperations.addPluginRepository(new Repository(pluginRepositoryElement));
		}
	}

	private void updateBuildPlugins(OrmProvider ormProvider) {
		List<Element> plugins = XmlUtils.findElements("/dependencies/ormProviders/provider[@id='" + ormProvider.name() + "']/plugin", getDependencies());
		for (Element pluginElement : plugins) {
			projectOperations.addBuildPlugin(new Plugin(pluginElement));
		}
	}

	private Element getDependencies() {
		InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "dependencies.xml");
		Assert.notNull(templateInputStream, "Could not acquire dependencies.xml file");
		Document dependencyDoc;
		try {
			dependencyDoc = XmlUtils.getDocumentBuilder().parse(templateInputStream);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		return (Element) dependencyDoc.getFirstChild();
	}

	private void updatePersistenceXml(OrmProvider ormProvider, JdbcDatabase database) {
		String persistencePath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml");
		MutableFile persistenceMutableFile = null;

		Document persistence;
		try {
			if (fileManager.exists(persistencePath)) {
				persistenceMutableFile = fileManager.updateFile(persistencePath);
				persistence = XmlUtils.getDocumentBuilder().parse(persistenceMutableFile.getInputStream());
			} else {
				persistenceMutableFile = fileManager.createFile(persistencePath);
				InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "persistence-template.xml");
				Assert.notNull(templateInputStream, "Could not acquire peristence.xml template");
				persistence = XmlUtils.getDocumentBuilder().parse(templateInputStream);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Properties dialects = new Properties();
		try {
			InputStream dialectsInputStream = TemplateUtils.getTemplate(getClass(), "jpa-dialects.properties");
			Assert.notNull(dialectsInputStream, "Could not acquire jpa-dialects.properties");
			dialects.load(dialectsInputStream);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element rootElement = persistence.getDocumentElement();
		Element persistenceElement = XmlUtils.findFirstElement("/persistence", rootElement);
		Element persistenceUnit = XmlUtils.findFirstElement("persistence-unit", persistenceElement);

		while (persistenceUnit.getFirstChild() != null) {
			persistenceUnit.removeChild(persistenceUnit.getFirstChild());
		}

		// Set attributes for GAE-specific requirements
		if (ormProvider == OrmProvider.GOOGLE_APP_ENGINE) {
			persistenceElement.setAttribute("version", "1.0");
			persistenceElement.setAttribute("xsi:schemaLocation", "http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_1_0.xsd");
			persistenceUnit.setAttribute("name", GAE_PERSISTENCE_UNIT_NAME);
			persistenceUnit.removeAttribute("transaction-type");
		} else {
			persistenceElement.setAttribute("version", "2.0");
			persistenceElement.setAttribute("xsi:schemaLocation", "http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd");
			persistenceUnit.setAttribute("name", "persistenceUnit");
			persistenceUnit.setAttribute("transaction-type", "RESOURCE_LOCAL");
		}

		// Add provider element
		Element provider = persistence.createElement("provider");
		provider.setTextContent(ormProvider.getAdapter());
		persistenceUnit.appendChild(provider);

		// Add properties
		Element properties = persistence.createElement("properties");
		switch (ormProvider) {
			case HIBERNATE:
				properties.appendChild(createPropertyElement("hibernate.dialect", dialects.getProperty(ormProvider.name() + "." + database.getKey()), persistence));
				properties.appendChild(persistence.createComment("value='create' to build a new database on each run; value='update' to modify an existing database; value='create-drop' means the same as 'create' but also drops tables when Hibernate closes; value='validate' makes no changes to the database")); // ROO-627
				properties.appendChild(createPropertyElement("hibernate.hbm2ddl.auto", "create", persistence));
				properties.appendChild(createPropertyElement("hibernate.ejb.naming_strategy", "org.hibernate.cfg.ImprovedNamingStrategy", persistence));
				break;
			case OPENJPA:
				properties.appendChild(createPropertyElement("openjpa.jdbc.DBDictionary", dialects.getProperty(ormProvider.name() + "." + database.getKey()), persistence));
				properties.appendChild(persistence.createComment("value='buildSchema' to runtime forward map the DDL SQL; value='validate' makes no changes to the database")); // ROO-627
				properties.appendChild(createPropertyElement("openjpa.jdbc.SynchronizeMappings", "buildSchema", persistence));
				break;
			case ECLIPSELINK:
				properties.appendChild(createPropertyElement("eclipselink.target-database", dialects.getProperty(ormProvider.name() + "." + database.getKey()), persistence));
				properties.appendChild(persistence.createComment("value='drop-and-create-tables' to build a new database on each run; value='create-tables' creates new tables if needed; value='none' makes no changes to the database")); // ROO-627
				properties.appendChild(createPropertyElement("eclipselink.ddl-generation", "drop-and-create-tables", persistence));
				properties.appendChild(createPropertyElement("eclipselink.ddl-generation.output-mode", "database", persistence));
				properties.appendChild(createPropertyElement("eclipselink.weaving", "static", persistence));
				break;
			case GOOGLE_APP_ENGINE:
				properties.appendChild(createPropertyElement("datanucleus.NontransactionalRead", "true", persistence));
				properties.appendChild(createPropertyElement("datanucleus.NontransactionalWrite", "true", persistence));
				properties.appendChild(createPropertyElement("datanucleus.ConnectionURL", "appengine", persistence));
				break;
		}

		persistenceUnit.appendChild(properties);
		XmlUtils.writeXml(persistenceMutableFile.getOutputStream(), persistence);
	}

	private void updateGaeXml(OrmProvider ormProvider) {
		String appenginePath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/appengine-web.xml");

		MutableFile appengineMutableFile = null;
		Document appengine;
		try {
			if (fileManager.exists(appenginePath)) {
				appengineMutableFile = fileManager.updateFile(appenginePath);
				appengine = XmlUtils.getDocumentBuilder().parse(appengineMutableFile.getInputStream());
			} else {
				appengineMutableFile = fileManager.createFile(appenginePath);
				InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "appengine-web-template.xml");
				Assert.notNull(templateInputStream, "Could not acquire appengine-web.xml template");
				appengine = XmlUtils.getDocumentBuilder().parse(templateInputStream);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		Element rootElement = appengine.getDocumentElement();
		Element applicationElement = XmlUtils.findFirstElement("/appengine-web-app/application", rootElement);
		applicationElement.setTextContent(projectMetadata.getProjectName());

		XmlUtils.writeXml(appengineMutableFile.getOutputStream(), appengine);

		String loggingPropertiesPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/logging.properties");
		if (!fileManager.exists(loggingPropertiesPath)) {
			try {
				InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "logging.properties");
				FileCopyUtils.copy(templateInputStream, fileManager.createFile(loggingPropertiesPath).getOutputStream());
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private void cleanup(OrmProvider ormProvider) {
		Element dependencies = getDependencies();

		for (OrmProvider provider : OrmProvider.values()) {
			if (provider != ormProvider) {
				// List<Element> pomProperties = XmlUtils.findElements("/dependencies/ormProviders/provider[@id='" + provider.name() + "']/properties/*", dependencies);
				// for (Element propertyElement : pomProperties) {
				// projectOperations.removeProperty(new Property(propertyElement));
				// }

				List<Element> ormDependencies = XmlUtils.findElements("/dependencies/ormProviders/provider[@id='" + provider.name() + "']/dependency", dependencies);
				for (Element dependency : ormDependencies) {
					projectOperations.removeDependency(new Dependency(dependency));
				}

				List<Element> plugins = XmlUtils.findElements("/dependencies/ormProviders/provider[@id='" + provider.name() + "']/plugin", dependencies);
				for (Element pluginElement : plugins) {
					projectOperations.removeBuildPlugin(new Plugin(pluginElement));
				}
			}
		}
		
		if (ormProvider != OrmProvider.GOOGLE_APP_ENGINE) {
			String appenginePath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/appengine-web.xml");
			if (fileManager.exists(appenginePath)) {
				fileManager.delete(appenginePath);
			}

			String loggingPropertiesPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/logging.properties");
			if (fileManager.exists(loggingPropertiesPath)) {
				fileManager.delete(loggingPropertiesPath);
			}
		} else {
			String databasePath = pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "database.properties");
			if (fileManager.exists(databasePath)) {
				fileManager.delete(databasePath);
			}
		}
	}

	private Element createPropertyElement(String name, String value, Document doc) {
		Element property = doc.createElement("property");
		property.setAttribute("name", name);
		property.setAttribute("value", value);
		return property;
	}

	private Element createRefElement(String name, String value, Document doc) {
		Element property = doc.createElement("property");
		property.setAttribute("name", name);
		property.setAttribute("ref", value);
		return property;
	}
}
