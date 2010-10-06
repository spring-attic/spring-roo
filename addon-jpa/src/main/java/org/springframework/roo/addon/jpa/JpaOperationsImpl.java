package org.springframework.roo.addon.jpa;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Filter;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.Plugin;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Property;
import org.springframework.roo.project.Repository;
import org.springframework.roo.project.Resource;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.StringUtils;
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
	private static final String PERSISTENCE_UNIT_NAME = "persistenceUnit";
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private MetadataService metadataService;
	@Reference private ProjectOperations projectOperations;
	@Reference private PropFileOperations propFileOperations;

	public boolean isJpaInstallationPossible() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null && !fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"));
	}

	public boolean isJpaInstalled() {
		return metadataService.get(ProjectMetadata.getProjectIdentifier()) != null && fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"));
	}

	public boolean hasDatabaseProperties() {
		return fileManager.exists(getDatabasePropertiesPath());
	}

	public SortedSet<String> getDatabaseProperties() {
		if (fileManager.exists(getDatabasePropertiesPath())) {
			return propFileOperations.getPropertyKeys(Path.SPRING_CONFIG_ROOT, "database.properties", true);
		} else {
			return getPropertiesFromDataNucleusConfiguration();
		}
	}

	private String getDatabasePropertiesPath() {
		return pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "database.properties");
	}

	public void configureJpa(OrmProvider ormProvider, JdbcDatabase database, String jndi, String applicationId, String databaseName, String userName, String password, String persistenceUnit) {
		Assert.notNull(ormProvider, "ORM provider required");
		Assert.notNull(database, "JDBC database required");

		// Parse the configuration.xml file
		Element configuration = XmlUtils.getConfiguration(getClass());

		// Remove unnecessary artifacts not specific to current database and JPA provider
		cleanup(configuration, ormProvider, database);

		updateApplicationContext(ormProvider, database, jndi, persistenceUnit);
		updatePersistenceXml(ormProvider, database, databaseName, userName, password, persistenceUnit);
		updateGaeXml(ormProvider, database, applicationId);
		updateVMforceConfigProperties(ormProvider, database, userName, password);
		if (!StringUtils.hasText(jndi)) {
			updateDatabaseProperties(ormProvider, database, databaseName, userName, password);
		}

		updateLog4j(ormProvider);
		updatePomProperties(configuration, ormProvider, database);
		updateDependencies(configuration, ormProvider, database);
		updateRepositories(configuration, ormProvider, database);
		updatePluginRepositories(configuration, ormProvider, database);
		updateFilters(configuration, ormProvider, database);
		updateResources(configuration, ormProvider, database);
		updateBuildPlugins(configuration, ormProvider, database);
	}

	private void updateApplicationContext(OrmProvider ormProvider, JdbcDatabase database, String jndi, String persistenceUnit) {
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
		Element dataSource = XmlUtils.findFirstElement("/beans/bean[@id = 'dataSource']", root);
		Element dataSourceJndi = XmlUtils.findFirstElement("/beans/jndi-lookup[@id = 'dataSource']", root);

		if (ormProvider == OrmProvider.DATANUCLEUS || ormProvider == OrmProvider.DATANUCLEUS_2) {
			if (dataSource != null) {
				root.removeChild(dataSource);
			}
			if (dataSourceJndi != null) {
				root.removeChild(dataSourceJndi);
			}
		} else if (!StringUtils.hasText(jndi) && dataSource == null) {
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
		} else if (StringUtils.hasText(jndi)) {
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

		if (dataSource != null) {
			Element validationQueryElement = XmlUtils.findFirstElement("property[@name = 'validationQuery']", dataSource);
			Element testOnBorrowElement = XmlUtils.findFirstElement("property[@name = 'testOnBorrow']", dataSource);
			if (database != JdbcDatabase.MYSQL && validationQueryElement != null && testOnBorrowElement != null) {
				dataSource.removeChild(validationQueryElement);
				dataSource.removeChild(testOnBorrowElement);
			} else if (database == JdbcDatabase.MYSQL && validationQueryElement == null && testOnBorrowElement == null) {
				dataSource.appendChild(createPropertyElement("validationQuery", "SELECT 1 FROM DUAL", appCtx));
				dataSource.appendChild(createPropertyElement("testOnBorrow", "true", appCtx));
			}
		}

		Element transactionManager = XmlUtils.findFirstElement("/beans/bean[@id = 'transactionManager']", root);
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

		Element entityManagerFactory = XmlUtils.findFirstElement("/beans/bean[@id = 'entityManagerFactory']", root);
		if (entityManagerFactory != null) {
			root.removeChild(entityManagerFactory);
		}

		entityManagerFactory = appCtx.createElement("bean");
		entityManagerFactory.setAttribute("id", "entityManagerFactory");

		switch (database) {
			case GOOGLE_APP_ENGINE:
				entityManagerFactory.setAttribute("class", "org.springframework.orm.jpa.LocalEntityManagerFactoryBean");
				entityManagerFactory.appendChild(createPropertyElement("persistenceUnitName", GAE_PERSISTENCE_UNIT_NAME, appCtx));
				break;
			default:
				entityManagerFactory.setAttribute("class", "org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean");
				switch (ormProvider) {
					case DATANUCLEUS:
					case DATANUCLEUS_2:
						entityManagerFactory.appendChild(createPropertyElement("persistenceUnitName", (StringUtils.hasText(persistenceUnit) ? persistenceUnit : PERSISTENCE_UNIT_NAME), appCtx));
						break;
					default:
						entityManagerFactory.appendChild(createRefElement("dataSource", "dataSource", appCtx));
						break;
				}
				break;
		}

		root.appendChild(entityManagerFactory);
		XmlUtils.removeTextNodes(root);

		XmlUtils.writeXml(contextMutableFile.getOutputStream(), appCtx);
	}

	private void updatePersistenceXml(OrmProvider ormProvider, JdbcDatabase database, String databaseName, String userName, String password, String persistenceUnit) {
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

		Element root = persistence.getDocumentElement();
		Element persistenceElement = XmlUtils.findFirstElement("/persistence", root);
		Assert.notNull(persistenceElement, "No persistence element found");
		
		Element persistenceUnitElement;
		if (StringUtils.hasText(persistenceUnit)) {
			persistenceUnitElement = XmlUtils.findFirstElement("persistence-unit[@name = '" + persistenceUnit + "']", persistenceElement);
			if (persistenceUnitElement == null) {
				persistenceUnitElement = persistence.createElement("persistence-unit");
				persistenceElement.appendChild(persistenceUnitElement);
			}
		} else {
			persistenceUnitElement = XmlUtils.findFirstElement("persistence-unit[@name = '" + (database == JdbcDatabase.GOOGLE_APP_ENGINE ? GAE_PERSISTENCE_UNIT_NAME : PERSISTENCE_UNIT_NAME) + "']", persistenceElement);
			if (persistenceUnitElement == null) {
				persistenceUnitElement = XmlUtils.findFirstElement("persistence-unit", persistenceElement);				
			}
		}
		Assert.notNull(persistenceUnitElement, "No persistence-unit elements found");
		
		while (persistenceUnitElement.getFirstChild() != null) {
			persistenceUnitElement.removeChild(persistenceUnitElement.getFirstChild());
		}

		// Set attributes for DataNuclueus 1.1.x/GAE-specific requirements
		switch (ormProvider) {
			case DATANUCLEUS:
				persistenceElement.setAttribute("version", "1.0");
				persistenceElement.setAttribute("xsi:schemaLocation", "http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_1_0.xsd");
				break;
			default:
				persistenceElement.setAttribute("version", "2.0");
				persistenceElement.setAttribute("xsi:schemaLocation", "http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd");
				break;
		}
		
		// Add provider element
		Element provider = persistence.createElement("provider");
		switch (database) {
			case GOOGLE_APP_ENGINE:
				persistenceUnitElement.setAttribute("name", GAE_PERSISTENCE_UNIT_NAME);
				persistenceUnitElement.removeAttribute("transaction-type");
				provider.setTextContent(ormProvider.getAlternateAdapter());
				break;
			case VMFORCE:
				persistenceUnitElement.setAttribute("name", (StringUtils.hasText(persistenceUnit) ? persistenceUnit : PERSISTENCE_UNIT_NAME));
				persistenceUnitElement.removeAttribute("transaction-type");
				provider.setTextContent(ormProvider.getAdapter());
				break;
			default:
				persistenceUnitElement.setAttribute("name", (StringUtils.hasText(persistenceUnit) ? persistenceUnit : PERSISTENCE_UNIT_NAME));
				persistenceUnitElement.setAttribute("transaction-type", "RESOURCE_LOCAL");
				provider.setTextContent(ormProvider.getAdapter());
				break;
		}
		persistenceUnitElement.appendChild(provider);

		// Add properties
		Element properties = persistence.createElement("properties");
		switch (ormProvider) {
			case HIBERNATE:
				properties.appendChild(createPropertyElement("hibernate.dialect", dialects.getProperty(ormProvider.name() + "." + database.name()), persistence));
				properties.appendChild(persistence.createComment("value='create' to build a new database on each run; value='update' to modify an existing database; value='create-drop' means the same as 'create' but also drops tables when Hibernate closes; value='validate' makes no changes to the database")); // ROO-627
				String hbm2dll = "create";
				if (database == JdbcDatabase.DB2400) {
					hbm2dll = "validate";
				}
				properties.appendChild(createPropertyElement("hibernate.hbm2ddl.auto", hbm2dll, persistence));
				properties.appendChild(createPropertyElement("hibernate.ejb.naming_strategy", "org.hibernate.cfg.ImprovedNamingStrategy", persistence));
				break;
			case OPENJPA:
				properties.appendChild(createPropertyElement("openjpa.jdbc.DBDictionary", dialects.getProperty(ormProvider.name() + "." + database.name()), persistence));
				properties.appendChild(persistence.createComment("value='buildSchema' to runtime forward map the DDL SQL; value='validate' makes no changes to the database")); // ROO-627
				properties.appendChild(createPropertyElement("openjpa.jdbc.SynchronizeMappings", "buildSchema", persistence));
				properties.appendChild(createPropertyElement("openjpa.RuntimeUnenhancedClasses", "supported", persistence));
				break;
			case ECLIPSELINK:
				properties.appendChild(createPropertyElement("eclipselink.target-database", dialects.getProperty(ormProvider.name() + "." + database.name()), persistence));
				properties.appendChild(persistence.createComment("value='drop-and-create-tables' to build a new database on each run; value='create-tables' creates new tables if needed; value='none' makes no changes to the database")); // ROO-627
				properties.appendChild(createPropertyElement("eclipselink.ddl-generation", "drop-and-create-tables", persistence));
				properties.appendChild(createPropertyElement("eclipselink.ddl-generation.output-mode", "database", persistence));
				properties.appendChild(createPropertyElement("eclipselink.weaving", "static", persistence));
				break;
			case DATANUCLEUS:
			case DATANUCLEUS_2:
				String connectionString = database.getConnectionString();
				switch (database) {
					case GOOGLE_APP_ENGINE:
						properties.appendChild(createPropertyElement("datanucleus.NontransactionalRead", "true", persistence));
						properties.appendChild(createPropertyElement("datanucleus.NontransactionalWrite", "true", persistence));
						break;
					case VMFORCE:
						userName = "${sfdc.userName}";
						password = "${sfdc.password}";
						properties.appendChild(createPropertyElement("datanucleus.Optimistic", "false", persistence));
						properties.appendChild(createPropertyElement("datanucleus.datastoreTransactionDelayOperations", "true", persistence));
						properties.appendChild(createPropertyElement("sfdcConnectionName", "DefaultSFDCConnection", persistence));
						break;
					default:
						properties.appendChild(createPropertyElement("datanucleus.ConnectionDriverName", database.getDriverClassName(), persistence));
						ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
						connectionString = connectionString.replace("TO_BE_CHANGED_BY_ADDON", projectMetadata.getProjectName());
						switch (database) {
							case HYPERSONIC_IN_MEMORY:
							case HYPERSONIC_PERSISTENT:
							case H2_IN_MEMORY:
								userName = StringUtils.hasText(userName) ? userName : "sa";
								break;
							case DERBY:
								break;
							default:
								logger.warning("Please enter your database details in src/main/resources/META-INF/persistence.xml.");
								break;
						}
						properties.appendChild(createPropertyElement("datanucleus.storeManagerType", "rdbms", persistence));
				}

				properties.appendChild(createPropertyElement("datanucleus.ConnectionURL", connectionString, persistence));
				properties.appendChild(createPropertyElement("datanucleus.ConnectionUserName", userName, persistence));
				properties.appendChild(createPropertyElement("datanucleus.ConnectionPassword", password, persistence));
				properties.appendChild(createPropertyElement("datanucleus.autoCreateSchema", "false", persistence));
				properties.appendChild(createPropertyElement("datanucleus.autoCreateTables", "true", persistence));
				properties.appendChild(createPropertyElement("datanucleus.autoCreateColumns", "false", persistence));
				properties.appendChild(createPropertyElement("datanucleus.autoCreateConstraints", "false", persistence));
				properties.appendChild(createPropertyElement("datanucleus.validateTables", "false", persistence));
				properties.appendChild(createPropertyElement("datanucleus.validateConstraints", "false", persistence));
				properties.appendChild(createPropertyElement("datanucleus.jpa.addClassTransformer", "false", persistence));
				break;
		}

		persistenceUnitElement.appendChild(properties);
		XmlUtils.writeXml(persistenceMutableFile.getOutputStream(), persistence);
	}

	private void updateGaeXml(OrmProvider ormProvider, JdbcDatabase database, String applicationId) {
		String appenginePath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/appengine-web.xml");
		boolean appenginePathExists = fileManager.exists(appenginePath);
		String loggingPropertiesPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/logging.properties");
		boolean loggingPropertiesPathExists = fileManager.exists(loggingPropertiesPath);

		if (database != JdbcDatabase.GOOGLE_APP_ENGINE) {
			if (appenginePathExists) {
				fileManager.delete(appenginePath);
			}
			if (loggingPropertiesPathExists) {
				fileManager.delete(loggingPropertiesPath);
			}
			return;
		} else {
			MutableFile appengineMutableFile = null;
			Document appengine;
			try {
				if (appenginePathExists) {
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

			Element rootElement = appengine.getDocumentElement();
			Element applicationElement = XmlUtils.findFirstElement("/appengine-web-app/application", rootElement);
			applicationElement.setTextContent(StringUtils.hasText(applicationId) ? applicationId : getProjectName());

			XmlUtils.writeXml(appengineMutableFile.getOutputStream(), appengine);

			if (!loggingPropertiesPathExists) {
				try {
					InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "logging.properties");
					FileCopyUtils.copy(templateInputStream, fileManager.createFile(loggingPropertiesPath).getOutputStream());
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		}
	}

	private void updateDatabaseProperties(OrmProvider ormProvider, JdbcDatabase database, String databaseName, String userName, String password) {
		String databasePath = getDatabasePropertiesPath();
		boolean databaseExists = fileManager.exists(databasePath);

		if (ormProvider == OrmProvider.DATANUCLEUS || ormProvider == OrmProvider.DATANUCLEUS_2) {
			if (databaseExists) {
				fileManager.delete(databasePath);
			}
			return;
		}

		MutableFile databaseMutableFile = null;
		Properties props = new Properties();

		try {
			if (databaseExists) {
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

		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		String connectionString = database.getConnectionString();
		connectionString = connectionString.replace("TO_BE_CHANGED_BY_ADDON", projectMetadata.getProjectName());
		if (StringUtils.hasText(databaseName)) {
			// Oracle uses a different connection URL - see ROO-1203
			String dbDelimiter = database == JdbcDatabase.ORACLE ? ":" : "/";
			connectionString += databaseName.startsWith(dbDelimiter) ? databaseName : dbDelimiter + databaseName;
		}
		props.put("database.url", connectionString);

		String dbPropsMsg = "Please enter your database details in src/main/resources/META-INF/spring/database.properties.";
		switch (database) {
		case HYPERSONIC_IN_MEMORY:
		case HYPERSONIC_PERSISTENT:
		case H2_IN_MEMORY:
			userName = StringUtils.hasText(userName) ? userName : "sa";
			break;
		case DERBY:
			break;
		case SYBASE:
			userName = StringUtils.hasText(userName) ? userName : "sa";
			logger.warning(dbPropsMsg);
			break;
		default:
			logger.warning(dbPropsMsg);
			break;
		}

		props.put("database.username", StringUtils.trimToEmpty(userName));
		props.put("database.password", StringUtils.trimToEmpty(password));

		try {
			props.store(databaseMutableFile.getOutputStream(), "Updated at " + new Date());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}

	private void updateVMforceConfigProperties(OrmProvider ormProvider, JdbcDatabase database, String userName, String password) {
		String configPath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "config.properties");
		boolean configExists = fileManager.exists(configPath);

		if (database != JdbcDatabase.VMFORCE) {
			if (configExists) {
				fileManager.delete(configPath);
			}
			return;
		}
		
		MutableFile configMutableFile = null;
		Properties props = new Properties();

		try {
			if (configExists) {
				configMutableFile = fileManager.updateFile(configPath);
				props.load(configMutableFile.getInputStream());
			} else {
				configMutableFile = fileManager.createFile(configPath);
				InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "config-template.properties");
				Assert.notNull(templateInputStream, "Could not acquire config properties template");
				props.load(templateInputStream);
			}
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}

		props.put("sfdc.userName", StringUtils.trimToEmpty(userName));
		props.put("sfdc.password", StringUtils.trimToEmpty(password));

		try {
			props.store(configMutableFile.getOutputStream(), "Updated at " + new Date());
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}

	private void updateLog4j(OrmProvider ormProvider) {
		try {
			String log4jPath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "log4j.properties");
			if (fileManager.exists(log4jPath)) {
				MutableFile log4jMutableFile = fileManager.updateFile(log4jPath);
				Properties props = new Properties();
				props.load(log4jMutableFile.getInputStream());
				final String dnKey = "log4j.category.DataNucleus";
				if (ormProvider == OrmProvider.DATANUCLEUS && !props.containsKey(dnKey)) {
					props.put(dnKey, "WARN");
					props.store(log4jMutableFile.getOutputStream(), "Updated at " + new Date());
				} else if (ormProvider != OrmProvider.DATANUCLEUS && props.containsKey(dnKey)) {
					props.remove(dnKey);
					props.store(log4jMutableFile.getOutputStream(), "Updated at " + new Date());
				}
			}
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}
	}

	private void updatePomProperties(Element configuration, OrmProvider ormProvider, JdbcDatabase database) {
		List<Element> databaseProperties = XmlUtils.findElements(getDbXPath(database) + "/properties/*", configuration);
		for (Element property : databaseProperties) {
			projectOperations.addProperty(new Property(property));
		}
	
		List<Element> providerProperties = XmlUtils.findElements(getProviderXPath(ormProvider) + "/properties/*", configuration);
		for (Element property : providerProperties) {
			projectOperations.addProperty(new Property(property));
		}
	}

	private void updateDependencies(Element configuration, OrmProvider ormProvider, JdbcDatabase database) {
		List<Element> databaseDependencies = XmlUtils.findElements(getDbXPath(database) + "/dependencies/dependency", configuration);
		for (Element dependencyElement : databaseDependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependencyElement));
		}

		List<Element> ormDependencies = XmlUtils.findElements(getProviderXPath(ormProvider) + "/dependencies/dependency", configuration);
		for (Element dependencyElement : ormDependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependencyElement));
		}

		// Hard coded to JPA & Hibernate Validator for now
		List<Element> jpaDependencies = XmlUtils.findElements("/configuration/persistence/provider[@id = 'JPA']/dependencies/dependency", configuration);
		for (Element dependencyElement : jpaDependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependencyElement));
		}

		List<Element> springDependencies = XmlUtils.findElements("/configuration/spring/dependencies/dependency", configuration);
		for (Element dependencyElement : springDependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependencyElement));
		}

		if (database == JdbcDatabase.ORACLE || database == JdbcDatabase.DB2) {
			logger.warning("The " + database.name() + " JDBC driver is not available in public maven repositories. Please adjust the pom.xml dependency to suit your needs");
		}
	}

	private String getProjectName() {
		return ((ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier())).getProjectName();
	}

	private void updateRepositories(Element configuration, OrmProvider ormProvider, JdbcDatabase database) {
		List<Element> databaseRepositories = XmlUtils.findElements(getDbXPath(database) + "/repositories/repository", configuration);
		for (Element repositoryElement : databaseRepositories) {
			projectOperations.addRepository(new Repository(repositoryElement));
		}
	
		List<Element> ormRepositories = XmlUtils.findElements(getProviderXPath(ormProvider) + "/repositories/repository", configuration);
		for (Element repositoryElement : ormRepositories) {
			projectOperations.addRepository(new Repository(repositoryElement));
		}
	
		List<Element> jpaRepositories = XmlUtils.findElements("/configuration/persistence/provider[@id='JPA']/repositories/repository", configuration);
		for (Element repositoryElement : jpaRepositories) {
			projectOperations.addRepository(new Repository(repositoryElement));
		}
	}

	private void updatePluginRepositories(Element configuration, OrmProvider ormProvider, JdbcDatabase database) {
		List<Element> databasePluginRepositories = XmlUtils.findElements(getDbXPath(database) + "/pluginRepositories/pluginRepository", configuration);
		for (Element pluginRepositoryElement : databasePluginRepositories) {
			projectOperations.addPluginRepository(new Repository(pluginRepositoryElement));
		}
		
		List<Element> ormPluginRepositories = XmlUtils.findElements(getProviderXPath(ormProvider) + "/pluginRepositories/pluginRepository", configuration);
		for (Element pluginRepositoryElement : ormPluginRepositories) {
			projectOperations.addPluginRepository(new Repository(pluginRepositoryElement));
		}
	}

	private void updateFilters(Element configuration, OrmProvider ormProvider, JdbcDatabase database) {
		List<Element> databaseFilters = XmlUtils.findElements(getDbXPath(database) + "/filters/filter", configuration);
		for (Element filterElement : databaseFilters) {
			projectOperations.addFilter(new Filter(filterElement));
		}
	
		List<Element> ormFilters = XmlUtils.findElements(getProviderXPath(ormProvider) + "/filters/filter", configuration);
		for (Element filterElement : ormFilters) {
			projectOperations.addFilter(new Filter(filterElement));
		}
	}

	private void updateResources(Element configuration, OrmProvider ormProvider, JdbcDatabase database) {
		List<Element> databaseResources = XmlUtils.findElements(getDbXPath(database) + "/resources/resource", configuration);
		for (Element resourceElement : databaseResources) {
			projectOperations.addResource(new Resource(resourceElement));
		}
	
		List<Element> ormResources = XmlUtils.findElements(getProviderXPath(ormProvider) + "/resources/resource", configuration);
		for (Element resourceElement : ormResources) {
			projectOperations.addResource(new Resource(resourceElement));
		}
	}

	private void updateBuildPlugins(Element configuration, OrmProvider ormProvider, JdbcDatabase database) {
		List<Element> databasePlugins = XmlUtils.findElements(getDbXPath(database) + "/plugins/plugin", configuration);
		for (Element pluginElement : databasePlugins) {
			projectOperations.addBuildPlugin(new Plugin(pluginElement));
		}
	
		List<Element> ormPlugins = XmlUtils.findElements(getProviderXPath(ormProvider) + "/plugins/plugin", configuration);
		for (Element pluginElement : ormPlugins) {
			projectOperations.addBuildPlugin(new Plugin(pluginElement));
		}
		
		if (database == JdbcDatabase.GOOGLE_APP_ENGINE) {
			updateEclipsePlugin(true);
		}
	}

	private void updateEclipsePlugin(boolean addBuildCommand) {
		String pomPath = pathResolver.getIdentifier(Path.ROOT, "pom.xml");
		MutableFile pomMutableFile = null;

		Document pom;
		try {
			if (fileManager.exists(pomPath)) {
				pomMutableFile = fileManager.updateFile(pomPath);
				pom = XmlUtils.getDocumentBuilder().parse(pomMutableFile.getInputStream());
			} else {
				throw new IllegalStateException("Could not acquire pom.xml in " + pomPath);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element root = pom.getDocumentElement();
		String gaeBuildCommandName = "com.google.appengine.eclipse.core.enhancerbuilder";
		Element additionalBuildcommandsElement = XmlUtils.findFirstElement("/project/build/plugins/plugin[artifactId = 'maven-eclipse-plugin']/configuration/additionalBuildcommands", root);
		Assert.notNull(additionalBuildcommandsElement, "additionalBuildcommands element of the maven-eclipse-plugin reqired");
		Element buildCommandElement = XmlUtils.findFirstElement("buildCommand[name = '" + gaeBuildCommandName + "']", additionalBuildcommandsElement);
		
		if (addBuildCommand && buildCommandElement == null) {
			Element nameElement = pom.createElement("name");
			nameElement.setTextContent(gaeBuildCommandName);
			buildCommandElement = pom.createElement("buildCommand");
			buildCommandElement.appendChild(nameElement);
			additionalBuildcommandsElement.appendChild(buildCommandElement);
			XmlUtils.writeXml(pomMutableFile.getOutputStream(), pom);
		} 
		
		if (!addBuildCommand && buildCommandElement != null) {
			additionalBuildcommandsElement.removeChild(buildCommandElement);
			XmlUtils.writeXml(pomMutableFile.getOutputStream(), pom);
		}
	}

	private void cleanup(Element configuration, OrmProvider ormProvider, JdbcDatabase database) {
		for (JdbcDatabase jdbcDatabase : JdbcDatabase.values()) {
			if (!jdbcDatabase.getKey().equals(database.getKey()) && !jdbcDatabase.getDriverClassName().equals(database.getDriverClassName())) {
				List<Element> dependencies = XmlUtils.findElements(getDbXPath(jdbcDatabase) + "/dependencies/dependency", configuration);
				for (Element dependencyElement : dependencies) {
					projectOperations.removeDependency(new Dependency(dependencyElement));
				}
				
				List<Element> filters = XmlUtils.findElements(getDbXPath(jdbcDatabase) + "/filters/filter", configuration);
				for (Element filterElement : filters) {
					projectOperations.removeFilter(new Filter(filterElement));
				}
				
				List<Element> plugins = XmlUtils.findElements(getDbXPath(jdbcDatabase) + "/plugins/plugin", configuration);
				for (Element pluginElement : plugins) {
					projectOperations.removeBuildPlugin(new Plugin(pluginElement));
				}
			}
		}
		for (OrmProvider provider : OrmProvider.values()) {
			if (provider != ormProvider) {
				// List<Element> pomProperties = XmlUtils.findElements("/configuration/ormProviders/provider[@id = '" + provider.name() + "']/properties/*", configuration);
				// for (Element propertyElement : pomProperties) {
				// projectOperations.removeProperty(new Property(propertyElement));
				// }
				List<Element> dependencies = XmlUtils.findElements(getProviderXPath(provider) + "/dependencies/dependency", configuration);
				for (Element dependencyElement : dependencies) {
					projectOperations.removeDependency(new Dependency(dependencyElement));
				}
				
				List<Element> filters = XmlUtils.findElements(getProviderXPath(provider) + "/filters/filter", configuration);
				for (Element filterElement : filters) {
					projectOperations.removeFilter(new Filter(filterElement));
				}
				
				List<Element> plugins = XmlUtils.findElements(getProviderXPath(provider) + "/plugins/plugin", configuration);
				for (Element pluginElement : plugins) {
					projectOperations.removeBuildPlugin(new Plugin(pluginElement));
				}
			}
		}
		
		if (database != JdbcDatabase.GOOGLE_APP_ENGINE) {
			updateEclipsePlugin(false);
		}
	}

	private String getDbXPath(JdbcDatabase database) {
		return "/configuration/databases/database[@id = '" + database.getKey() + "']";
	}

	private String getProviderXPath(OrmProvider provider) {
		return "/configuration/ormProviders/provider[@id = '" + provider.name() + "']";
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

	private SortedSet<String> getPropertiesFromDataNucleusConfiguration() {
		String persistenceXmlPath = pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml");
		if (!fileManager.exists(persistenceXmlPath)) {
			throw new IllegalStateException("Failed to find " + persistenceXmlPath);
		}
		FileDetails fileDetails = fileManager.readFile(persistenceXmlPath);
		Document document = null;
		try {
			InputStream is = new FileInputStream(fileDetails.getFile());
			DocumentBuilder builder = XmlUtils.getDocumentBuilder();
			builder.setErrorHandler(null);
			document = builder.parse(is);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		List<Element> propertyElements = XmlUtils.findElements("/persistence/persistence-unit/properties/property", document.getDocumentElement());
		Assert.notEmpty(propertyElements, "Failed to find property elements in " + persistenceXmlPath);
		SortedSet<String> properties = new TreeSet<String>();

		for (Element propertyElement : propertyElements) {
			String key = propertyElement.getAttribute("name");
			String value = propertyElement.getAttribute("value");
			if ("datanucleus.ConnectionDriverName".equals(key)) {
				properties.add("datanucleus.ConnectionDriverName = " + value);
			}
			if ("datanucleus.ConnectionURL".equals(key)) {
				properties.add("datanucleus.ConnectionURL = " + value);
			}
			if ("datanucleus.ConnectionUserName".equals(key)) {
				properties.add("datanucleus.ConnectionUserName = " + value);
			}
			if ("datanucleus.ConnectionPassword".equals(key)) {
				properties.add("datanucleus.ConnectionPassword = " + value);
			}
		}
		return properties;
	}
}
