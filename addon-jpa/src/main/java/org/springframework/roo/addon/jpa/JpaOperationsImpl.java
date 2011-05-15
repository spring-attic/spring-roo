package org.springframework.roo.addon.jpa;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Filter;
import org.springframework.roo.project.Path;
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
import org.springframework.roo.support.util.XmlElementBuilder;
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
	private static Logger logger = HandlerUtils.getLogger(JpaOperationsImpl.class);
	private static final String DATABASE_URL = "database.url";
	private static final String DATABASE_DRIVER = "database.driverClassName";
	private static final String DATABASE_USERNAME = "database.username";
	private static final String DATABASE_PASSWORD = "database.password";
	private static final String PERSISTENCE_UNIT = "persistence-unit";
	private static final String GAE_PERSISTENCE_UNIT_NAME = "transactions-optional";
	private static final String PERSISTENCE_UNIT_NAME = "persistenceUnit";
	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private ProjectOperations projectOperations;
	@Reference private PropFileOperations propFileOperations;
	
	public boolean isJpaInstallationPossible() {
		return projectOperations.isProjectAvailable() && !fileManager.exists(getPersistencePath());
	}

	public boolean isJpaInstalled() {
		return projectOperations.isProjectAvailable() && fileManager.exists(getPersistencePath());
	}

	public boolean hasDatabaseProperties() {
		return fileManager.exists(getDatabasePropertiesPath());
	}

	public SortedSet<String> getDatabaseProperties() {
		if (fileManager.exists(getDatabasePropertiesPath())) {
			return propFileOperations.getPropertyKeys(Path.SPRING_CONFIG_ROOT, "database.properties", true);
		}
		return getPropertiesFromDataNucleusConfiguration();
	}

	private String getPersistencePath() {
		return projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml");
	}

	private String getDatabasePropertiesPath() {
		return projectOperations.getPathResolver().getIdentifier(Path.SPRING_CONFIG_ROOT, "database.properties");
	}

	public void configureJpa(OrmProvider ormProvider, JdbcDatabase jdbcDatabase, String jndi, String applicationId, String hostName, String databaseName, String userName, String password, String persistenceUnit) {
		// long start = System.currentTimeMillis();
		Assert.notNull(ormProvider, "ORM provider required");
		Assert.notNull(jdbcDatabase, "JDBC database required");

		// Parse the configuration.xml file
		Element configuration = XmlUtils.getConfiguration(getClass());

		// Remove unnecessary artifacts not specific to current database and JPA provider
		cleanup(configuration, ormProvider, jdbcDatabase);

		updateApplicationContext(ormProvider, jdbcDatabase, jndi, persistenceUnit);
		updatePersistenceXml(ormProvider, jdbcDatabase, hostName, databaseName, userName, password, persistenceUnit);
		manageGaeXml(ormProvider, jdbcDatabase, applicationId);
		updateVMforceConfigProperties(ormProvider, jdbcDatabase, userName, password);

		if (!StringUtils.hasText(jndi)) {
			updateDatabaseProperties(ormProvider, jdbcDatabase, hostName, databaseName, userName, password);
		}

		updateLog4j(ormProvider);
		updatePomProperties(configuration, ormProvider, jdbcDatabase);
		updateDependencies(configuration, ormProvider, jdbcDatabase);
		updateRepositories(configuration, ormProvider, jdbcDatabase);
		updatePluginRepositories(configuration, ormProvider, jdbcDatabase);
		updateFilters(configuration, ormProvider, jdbcDatabase);
		updateResources(configuration, ormProvider, jdbcDatabase);
		updateBuildPlugins(configuration, ormProvider, jdbcDatabase);
		// System.out.println("Elapsed time: " + (System.currentTimeMillis() - start));
	}

	private void updateApplicationContext(OrmProvider ormProvider, JdbcDatabase jdbcDatabase, String jndi, String persistenceUnit) {
		String contextPath = projectOperations.getPathResolver().getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext.xml");
		Document appCtx = XmlUtils.readXml(fileManager.getInputStream(contextPath));
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
			dataSource.appendChild(createPropertyElement("testOnBorrow", "true", appCtx));
			dataSource.appendChild(createPropertyElement("testOnReturn", "true", appCtx));
			dataSource.appendChild(createPropertyElement("testWhileIdle", "true", appCtx));
			dataSource.appendChild(createPropertyElement("timeBetweenEvictionRunsMillis", "1800000", appCtx));
			dataSource.appendChild(createPropertyElement("numTestsPerEvictionRun", "3", appCtx));
			dataSource.appendChild(createPropertyElement("minEvictableIdleTimeMillis", "1800000", appCtx));
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
			if (validationQueryElement != null) {
				dataSource.removeChild(validationQueryElement);
			}
			String validationQuery = "";
			switch (jdbcDatabase) {
			case ORACLE:
				validationQuery = "SELECT 1 FROM DUAL";
				break;
			case POSTGRES:
				validationQuery = "SELECT version();";
				break;
			case MYSQL:
				validationQuery = "SELECT 1";
				break;
			}
			if (StringUtils.hasText(validationQuery)) {
				dataSource.appendChild(createPropertyElement("validationQuery", validationQuery, appCtx));
			}
		}

		Element transactionManager = XmlUtils.findFirstElement("/beans/bean[@id = 'transactionManager']", root);
		if (transactionManager == null) {
			transactionManager = appCtx.createElement("bean");
			transactionManager.setAttribute("id", "transactionManager");
			transactionManager.setAttribute("class", "org.springframework.orm.jpa.JpaTransactionManager");
			if (StringUtils.hasText(persistenceUnit)) {
				Element qualifier = appCtx.createElement("qualifier");
				qualifier.setAttribute("value", persistenceUnit);
				transactionManager.appendChild(qualifier);
			}
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

		switch (jdbcDatabase) {
		case GOOGLE_APP_ENGINE:
			entityManagerFactory.setAttribute("class", "org.springframework.orm.jpa.LocalEntityManagerFactoryBean");
			entityManagerFactory.appendChild(createPropertyElement("persistenceUnitName", (StringUtils.hasText(persistenceUnit) ? persistenceUnit : GAE_PERSISTENCE_UNIT_NAME), appCtx));
			break;
		default:
			entityManagerFactory.setAttribute("class", "org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean");
			entityManagerFactory.appendChild(createPropertyElement("persistenceUnitName", (StringUtils.hasText(persistenceUnit) ? persistenceUnit : PERSISTENCE_UNIT_NAME), appCtx));
			if (!ormProvider.name().startsWith("DATANUCLEUS")) {
				entityManagerFactory.appendChild(createRefElement("dataSource", "dataSource", appCtx));
			}
			break;
		}

		root.appendChild(entityManagerFactory);

		XmlUtils.removeTextNodes(root);

		fileManager.createOrUpdateTextFileIfRequired(contextPath, XmlUtils.nodeToString(appCtx), false);
	}

	private void updatePersistenceXml(OrmProvider ormProvider, JdbcDatabase jdbcDatabase, String hostName, String databaseName, String userName, String password, String persistenceUnit) {
		String persistencePath = getPersistencePath();
		Document persistence;
		try {
			if (fileManager.exists(persistencePath)) {
				persistence = XmlUtils.readXml(fileManager.getInputStream(persistencePath));
			} else {
				InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "persistence-template.xml");
				Assert.notNull(templateInputStream, "Could not acquire persistence.xml template");
				persistence = XmlUtils.readXml(templateInputStream);
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
			persistenceUnitElement = XmlUtils.findFirstElement(PERSISTENCE_UNIT + "[@name = '" + persistenceUnit + "']", persistenceElement);
		} else {
			persistenceUnitElement = XmlUtils.findFirstElement(PERSISTENCE_UNIT + "[@name = '" + (jdbcDatabase == JdbcDatabase.GOOGLE_APP_ENGINE ? GAE_PERSISTENCE_UNIT_NAME : PERSISTENCE_UNIT_NAME) + "']", persistenceElement);
		}

		if (persistenceUnitElement != null) {
			while (persistenceUnitElement.getFirstChild() != null) {
				persistenceUnitElement.removeChild(persistenceUnitElement.getFirstChild());
			}
		} else {
			persistenceUnitElement = persistence.createElement(PERSISTENCE_UNIT);
			persistenceElement.appendChild(persistenceUnitElement);
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
		switch (jdbcDatabase) {
		case GOOGLE_APP_ENGINE:
			persistenceUnitElement.setAttribute("name", (StringUtils.hasText(persistenceUnit) ? persistenceUnit : GAE_PERSISTENCE_UNIT_NAME));
			persistenceUnitElement.removeAttribute("transaction-type");
			provider.setTextContent(ormProvider.getAlternateAdapter());
			break;
		case VMFORCE:
			persistenceUnitElement.setAttribute("name", (StringUtils.hasText(persistenceUnit) ? persistenceUnit : PERSISTENCE_UNIT_NAME));
			persistenceUnitElement.removeAttribute("transaction-type");
			provider.setTextContent(ormProvider.getAlternateAdapter());
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
			properties.appendChild(createPropertyElement("hibernate.dialect", dialects.getProperty(ormProvider.name() + "." + jdbcDatabase.name()), persistence));
			properties.appendChild(persistence.createComment(" value=\"create\" to build a new database on each run; value=\"update\" to modify an existing database; value=\"create-drop\" means the same as \"create\" but also drops tables when Hibernate closes; value=\"validate\" makes no changes to the database ")); // ROO-627
			String hbm2dll = "create";
			if (jdbcDatabase == JdbcDatabase.DB2_400) {
				hbm2dll = "validate";
			}
			properties.appendChild(createPropertyElement("hibernate.hbm2ddl.auto", hbm2dll, persistence));
			properties.appendChild(createPropertyElement("hibernate.ejb.naming_strategy", "org.hibernate.cfg.ImprovedNamingStrategy", persistence));
			properties.appendChild(createPropertyElement("hibernate.connection.charSet", "UTF-8", persistence));
			properties.appendChild(persistence.createComment(" Uncomment the following two properties for JBoss only "));
			properties.appendChild(persistence.createComment(" property name=\"hibernate.validator.apply_to_ddl\" value=\"false\" /"));
			properties.appendChild(persistence.createComment(" property name=\"hibernate.validator.autoregister_listeners\" value=\"false\" /"));
			break;
		case OPENJPA:
			properties.appendChild(createPropertyElement("openjpa.jdbc.DBDictionary", dialects.getProperty(ormProvider.name() + "." + jdbcDatabase.name()), persistence));
			properties.appendChild(persistence.createComment(" value=\"buildSchema\" to runtime forward map the DDL SQL; value=\"validate\" makes no changes to the database ")); // ROO-627
			properties.appendChild(createPropertyElement("openjpa.jdbc.SynchronizeMappings", "buildSchema", persistence));
			properties.appendChild(createPropertyElement("openjpa.RuntimeUnenhancedClasses", "supported", persistence));
			break;
		case ECLIPSELINK:
			properties.appendChild(createPropertyElement("eclipselink.target-database", dialects.getProperty(ormProvider.name() + "." + jdbcDatabase.name()), persistence));
			properties.appendChild(persistence.createComment(" value=\"drop-and-create-tables\" to build a new database on each run; value=\"create-tables\" creates new tables if needed; value=\"none\" makes no changes to the database ")); // ROO-627
			properties.appendChild(createPropertyElement("eclipselink.ddl-generation", "drop-and-create-tables", persistence));
			properties.appendChild(createPropertyElement("eclipselink.ddl-generation.output-mode", "database", persistence));
			properties.appendChild(createPropertyElement("eclipselink.weaving", "static", persistence));
			break;
		case DATANUCLEUS:
		case DATANUCLEUS_2:
			String connectionString = getConnectionString(jdbcDatabase, hostName, databaseName);
			switch (jdbcDatabase) {
			case GOOGLE_APP_ENGINE:
				properties.appendChild(createPropertyElement("datanucleus.NontransactionalRead", "true", persistence));
				properties.appendChild(createPropertyElement("datanucleus.NontransactionalWrite", "true", persistence));
				properties.appendChild(createPropertyElement("datanucleus.autoCreateSchema", "false", persistence));
				break;
			case VMFORCE:
				userName = "${sfdc.userName}";
				password = "${sfdc.password}";
				properties.appendChild(createPropertyElement("datanucleus.Optimistic", "false", persistence));
				properties.appendChild(createPropertyElement("datanucleus.datastoreTransactionDelayOperations", "true", persistence));
				properties.appendChild(createPropertyElement("sfdcConnectionName", "DefaultSFDCConnection", persistence));
				properties.appendChild(createPropertyElement("datanucleus.autoCreateSchema", "true", persistence));
				break;
			default:
				properties.appendChild(createPropertyElement("datanucleus.ConnectionDriverName", jdbcDatabase.getDriverClassName(), persistence));
				properties.appendChild(createPropertyElement("datanucleus.autoCreateSchema", "false", persistence));
				ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
				connectionString = connectionString.replace("TO_BE_CHANGED_BY_ADDON", projectMetadata.getProjectName());
				if (jdbcDatabase.getKey().equals("HYPERSONIC") || jdbcDatabase == JdbcDatabase.H2_IN_MEMORY || jdbcDatabase == JdbcDatabase.SYBASE) {
					userName = StringUtils.hasText(userName) ? userName : "sa";
				}
				properties.appendChild(createPropertyElement("datanucleus.storeManagerType", "rdbms", persistence));
			}

			properties.appendChild(createPropertyElement("datanucleus.ConnectionURL", connectionString, persistence));
			properties.appendChild(createPropertyElement("datanucleus.ConnectionUserName", userName, persistence));
			properties.appendChild(createPropertyElement("datanucleus.ConnectionPassword", password, persistence));
			properties.appendChild(createPropertyElement("datanucleus.autoCreateTables", "true", persistence));
			properties.appendChild(createPropertyElement("datanucleus.autoCreateColumns", "false", persistence));
			properties.appendChild(createPropertyElement("datanucleus.autoCreateConstraints", "false", persistence));
			properties.appendChild(createPropertyElement("datanucleus.validateTables", "false", persistence));
			properties.appendChild(createPropertyElement("datanucleus.validateConstraints", "false", persistence));
			properties.appendChild(createPropertyElement("datanucleus.jpa.addClassTransformer", "false", persistence));
			break;
		}

		persistenceUnitElement.appendChild(properties);

		fileManager.createOrUpdateTextFileIfRequired(persistencePath, XmlUtils.nodeToString(persistence), false);
		
		if (jdbcDatabase != JdbcDatabase.GOOGLE_APP_ENGINE && (ormProvider == OrmProvider.DATANUCLEUS || ormProvider == OrmProvider.DATANUCLEUS_2)) {
			logger.warning("Please update your database details in src/main/resources/META-INF/persistence.xml.");
		}
	}

	private String getConnectionString(JdbcDatabase jdbcDatabase, String hostName, String databaseName) {
		String connectionString = jdbcDatabase.getConnectionString();
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		connectionString = connectionString.replace("TO_BE_CHANGED_BY_ADDON", projectMetadata.getProjectName());
		if (StringUtils.hasText(databaseName)) {
			// Oracle uses a different connection URL - see ROO-1203
			String dbDelimiter = jdbcDatabase == JdbcDatabase.ORACLE ? ":" : "/";
			connectionString += databaseName.startsWith(dbDelimiter) ? databaseName : dbDelimiter + databaseName;
		}
		if (!StringUtils.hasText(hostName)) {
			hostName = "localhost";
		}
		return connectionString.replace("HOST_NAME", hostName);
	}

	private void manageGaeXml(OrmProvider ormProvider, JdbcDatabase jdbcDatabase, String applicationId) {
		String appenginePath = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/appengine-web.xml");
		boolean appenginePathExists = fileManager.exists(appenginePath);

		String loggingPropertiesPath = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/logging.properties");
		boolean loggingPropertiesPathExists = fileManager.exists(loggingPropertiesPath);

		if (jdbcDatabase != JdbcDatabase.GOOGLE_APP_ENGINE) {
			if (appenginePathExists) {
				fileManager.delete(appenginePath);
			}
			if (loggingPropertiesPathExists) {
				fileManager.delete(loggingPropertiesPath);
			}
			return;
		}

		Document appengine;
		try {
			if (appenginePathExists) {
				appengine = XmlUtils.readXml(fileManager.getInputStream(appenginePath));
			} else {
				InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "appengine-web-template.xml");
				Assert.notNull(templateInputStream, "Could not acquire appengine-web.xml template");
				appengine = XmlUtils.readXml(templateInputStream);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		Element root = appengine.getDocumentElement();
		Element applicationElement = XmlUtils.findFirstElement("/appengine-web-app/application", root);
		String textContent = StringUtils.hasText(applicationId) ? applicationId : getProjectName();
		if (!textContent.equals(applicationElement.getTextContent())) {
			applicationElement.setTextContent(StringUtils.hasText(applicationId) ? applicationId : getProjectName());
			fileManager.createOrUpdateTextFileIfRequired(appenginePath, XmlUtils.nodeToString(appengine), false);
			logger.warning("Please update your database details in src/main/resources/META-INF/persistence.xml.");
		}

		if (!loggingPropertiesPathExists) {
			try {
				InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "logging.properties");
				FileCopyUtils.copy(templateInputStream, fileManager.createFile(loggingPropertiesPath).getOutputStream());
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private void updateDatabaseProperties(OrmProvider ormProvider, JdbcDatabase jdbcDatabase, String hostName, String databaseName, String userName, String password) {
		String databasePath = getDatabasePropertiesPath();
		boolean databaseExists = fileManager.exists(databasePath);

		if (ormProvider == OrmProvider.DATANUCLEUS || ormProvider == OrmProvider.DATANUCLEUS_2) {
			if (databaseExists) {
				fileManager.delete(databasePath);
			}
			return;
		}

		Properties props = new Properties();
		try {
			if (databaseExists) {
				props.load(fileManager.getInputStream(databasePath));
			} else {
				InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "database-template.properties");
				Assert.notNull(templateInputStream, "Could not acquire database properties template");
				props.load(templateInputStream);
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		String connectionString = getConnectionString(jdbcDatabase, hostName, databaseName);
		if (jdbcDatabase.getKey().equals("HYPERSONIC") || jdbcDatabase == JdbcDatabase.H2_IN_MEMORY || jdbcDatabase == JdbcDatabase.SYBASE) {
			userName = StringUtils.hasText(userName) ? userName : "sa";
		}

		boolean hasChanged = !props.get(DATABASE_DRIVER).equals(jdbcDatabase.getDriverClassName());
		hasChanged |= !props.get(DATABASE_URL).equals(connectionString);
		hasChanged |= !props.get(DATABASE_USERNAME).equals(StringUtils.trimToEmpty(userName));
		hasChanged |= !props.get(DATABASE_PASSWORD).equals(StringUtils.trimToEmpty(password));
		if (!hasChanged) {
			// No changes from existing database configuration so exit now
			return;
		}

		// Write changes to database.properties file
		props.put(DATABASE_URL, connectionString);
		props.put(DATABASE_DRIVER, jdbcDatabase.getDriverClassName());
		props.put(DATABASE_USERNAME, StringUtils.trimToEmpty(userName));
		props.put(DATABASE_PASSWORD, StringUtils.trimToEmpty(password));

		OutputStream outputStream = null;
		try {
			MutableFile mutableFile = databaseExists ? fileManager.updateFile(databasePath) : fileManager.createFile(databasePath);
			outputStream = mutableFile.getOutputStream();
			props.store(outputStream, "Updated at " + new Date());
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException ignored) {}
			}
		}

		// Log message to console
		switch (jdbcDatabase) {
		case ORACLE:
		case DB2_EXPRESS_C:
		case DB2_400:
			logger.warning("The " + jdbcDatabase.name() + " JDBC driver is not available in public maven repositories. Please adjust the pom.xml dependency to suit your needs");
			break;
		case POSTGRES:
		case DERBY:
		case MSSQL:
		case SYBASE:
		case MYSQL:
			logger.warning("Please update your database details in src/main/resources/META-INF/spring/database.properties.");
			break;
		}
	}

	private void updateVMforceConfigProperties(OrmProvider ormProvider, JdbcDatabase jdbcDatabase, String userName, String password) {
		String configPath = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "config.properties");
		boolean configExists = fileManager.exists(configPath);

		if (jdbcDatabase != JdbcDatabase.VMFORCE) {
			if (configExists) {
				fileManager.delete(configPath);
			}
			return;
		}

		Properties props = new Properties();
		try {
			if (configExists) {
				props.load(fileManager.getInputStream(configPath));
			} else {
				InputStream templateInputStream = TemplateUtils.getTemplate(getClass(), "config-template.properties");
				Assert.notNull(templateInputStream, "Could not acquire config properties template");
				props.load(templateInputStream);
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		boolean hasChanged = !props.get("sfdc.userName").equals(StringUtils.trimToEmpty(userName));
		hasChanged |= !props.get("sfdc.password").equals(StringUtils.trimToEmpty(password));
		if (!hasChanged) {
			return;
		}

		props.put("sfdc.userName", StringUtils.trimToEmpty(userName));
		props.put("sfdc.password", StringUtils.trimToEmpty(password));

		OutputStream outputStream = null;
		try {
			MutableFile mutableFile = configExists ? fileManager.updateFile(configPath) : fileManager.createFile(configPath);
			outputStream = mutableFile.getOutputStream();
			props.store(outputStream, "Updated at " + new Date());
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException ignored) {
				}
			}
		}

		logger.warning("Please update your database details in src/main/resources/config.properties.");
	}

	private void updateLog4j(OrmProvider ormProvider) {
		try {
			String log4jPath = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "log4j.properties");
			if (fileManager.exists(log4jPath)) {
				MutableFile log4jMutableFile = fileManager.updateFile(log4jPath);
				Properties props = new Properties();
				props.load(log4jMutableFile.getInputStream());
				final String dnKey = "log4j.category.DataNucleus";
				if (ormProvider == OrmProvider.DATANUCLEUS && !props.containsKey(dnKey)) {
					OutputStream outputStream = log4jMutableFile.getOutputStream();
					props.put(dnKey, "WARN");
					props.store(outputStream, "Updated at " + new Date());
					outputStream.close();
				} else if (ormProvider != OrmProvider.DATANUCLEUS && props.containsKey(dnKey)) {
					OutputStream outputStream = log4jMutableFile.getOutputStream();
					props.remove(dnKey);
					props.store(outputStream, "Updated at " + new Date());
					outputStream.close();
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private void updatePomProperties(Element configuration, OrmProvider ormProvider, JdbcDatabase jdbcDatabase) {
		List<Element> databaseProperties = XmlUtils.findElements(getDbXPath(jdbcDatabase) + "/properties/*", configuration);
		for (Element property : databaseProperties) {
			projectOperations.addProperty(new Property(property));
		}

		List<Element> providerProperties = XmlUtils.findElements(getProviderXPath(ormProvider) + "/properties/*", configuration);
		for (Element property : providerProperties) {
			projectOperations.addProperty(new Property(property));
		}
	}

	private void updateDependencies(Element configuration, OrmProvider ormProvider, JdbcDatabase jdbcDatabase) {
		List<Dependency> dependencies = new ArrayList<Dependency>();

		List<Element> databaseDependencies = XmlUtils.findElements(getDbXPath(jdbcDatabase) + "/dependencies/dependency", configuration);
		for (Element dependencyElement : databaseDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}

		List<Element> ormDependencies = XmlUtils.findElements(getProviderXPath(ormProvider) + "/dependencies/dependency", configuration);
		for (Element dependencyElement : ormDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}

		// Hard coded to JPA & Hibernate Validator for now
		List<Element> jpaDependencies = XmlUtils.findElements("/configuration/persistence/provider[@id = 'JPA']/dependencies/dependency", configuration);
		for (Element dependencyElement : jpaDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}

		List<Element> springDependencies = XmlUtils.findElements("/configuration/spring/dependencies/dependency", configuration);
		for (Element dependencyElement : springDependencies) {
			dependencies.add(new Dependency(dependencyElement));
		}

		// Add all new dependencies to pom.xml
		projectOperations.addDependencies(dependencies);
	}

	private String getProjectName() {
		return ((ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier())).getProjectName();
	}

	private void updateRepositories(Element configuration, OrmProvider ormProvider, JdbcDatabase jdbcDatabase) {
		List<Repository> repositories = new ArrayList<Repository>();

		List<Element> databaseRepositories = XmlUtils.findElements(getDbXPath(jdbcDatabase) + "/repositories/repository", configuration);
		for (Element repositoryElement : databaseRepositories) {
			repositories.add(new Repository(repositoryElement));
		}

		List<Element> ormRepositories = XmlUtils.findElements(getProviderXPath(ormProvider) + "/repositories/repository", configuration);
		for (Element repositoryElement : ormRepositories) {
			repositories.add(new Repository(repositoryElement));
		}

		List<Element> jpaRepositories = XmlUtils.findElements("/configuration/persistence/provider[@id='JPA']/repositories/repository", configuration);
		for (Element repositoryElement : jpaRepositories) {
			repositories.add(new Repository(repositoryElement));
		}

		// Add all new repositories to pom.xml
		projectOperations.addRepositories(repositories);
	}

	private void updatePluginRepositories(Element configuration, OrmProvider ormProvider, JdbcDatabase jdbcDatabase) {
		List<Repository> pluginRepositories = new ArrayList<Repository>();

		List<Element> databasePluginRepositories = XmlUtils.findElements(getDbXPath(jdbcDatabase) + "/pluginRepositories/pluginRepository", configuration);
		for (Element pluginRepositoryElement : databasePluginRepositories) {
			pluginRepositories.add(new Repository(pluginRepositoryElement));
		}

		List<Element> ormPluginRepositories = XmlUtils.findElements(getProviderXPath(ormProvider) + "/pluginRepositories/pluginRepository", configuration);
		for (Element pluginRepositoryElement : ormPluginRepositories) {
			pluginRepositories.add(new Repository(pluginRepositoryElement));
		}

		// Add all new plugin repositories to pom.xml
		projectOperations.addPluginRepositories(pluginRepositories);
	}

	private void updateFilters(Element configuration, OrmProvider ormProvider, JdbcDatabase jdbcDatabase) {
		List<Filter> filters = new ArrayList<Filter>();

		List<Element> databaseFilters = XmlUtils.findElements(getDbXPath(jdbcDatabase) + "/filters/filter", configuration);
		for (Element filterElement : databaseFilters) {
			filters.add(new Filter(filterElement));
		}

		List<Element> ormFilters = XmlUtils.findElements(getProviderXPath(ormProvider) + "/filters/filter", configuration);
		for (Element filterElement : ormFilters) {
			filters.add(new Filter(filterElement));
		}

		for (Filter filter : filters) {
			projectOperations.addFilter(filter);
		}
	}

	private void updateResources(Element configuration, OrmProvider ormProvider, JdbcDatabase jdbcDatabase) {
		List<Resource> resources = new ArrayList<Resource>();

		List<Element> databaseResources = XmlUtils.findElements(getDbXPath(jdbcDatabase) + "/resources/resource", configuration);
		for (Element resourceElement : databaseResources) {
			resources.add(new Resource(resourceElement));
		}

		List<Element> ormResources = XmlUtils.findElements(getProviderXPath(ormProvider) + "/resources/resource", configuration);
		for (Element resourceElement : ormResources) {
			resources.add(new Resource(resourceElement));
		}

		for (Resource resource : resources) {
			projectOperations.addResource(resource);
		}
	}

	private void updateBuildPlugins(Element configuration, OrmProvider ormProvider, JdbcDatabase jdbcDatabase) {
		List<Plugin> buildPlugins = new ArrayList<Plugin>();

		List<Element> databasePlugins = XmlUtils.findElements(getDbXPath(jdbcDatabase) + "/plugins/plugin", configuration);
		for (Element pluginElement : databasePlugins) {
			buildPlugins.add(new Plugin(pluginElement));
		}

		List<Element> ormPlugins = XmlUtils.findElements(getProviderXPath(ormProvider) + "/plugins/plugin", configuration);
		for (Element pluginElement : ormPlugins) {
			buildPlugins.add(new Plugin(pluginElement));
		}

		projectOperations.addBuildPlugins(buildPlugins);

		if (jdbcDatabase == JdbcDatabase.GOOGLE_APP_ENGINE) {
			updateEclipsePlugin(true);
			updateDataNucleusPlugin(true);
		}
	}

	private void updateEclipsePlugin(boolean addToPlugin) {
		String pom = projectOperations.getPathResolver().getIdentifier(Path.ROOT, "pom.xml");
		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();
		String descriptionOfChange = "";

		// Manage GAE buildCommand
		Element additionalBuildcommandsElement = XmlUtils.findFirstElement("/project/build/plugins/plugin[artifactId = 'maven-eclipse-plugin']/configuration/additionalBuildcommands", root);
		Assert.notNull(additionalBuildcommandsElement, "additionalBuildcommands element of the maven-eclipse-plugin required");
		String gaeBuildCommandName = "com.google.appengine.eclipse.core.enhancerbuilder";
		Element gaeBuildCommandElement = XmlUtils.findFirstElement("buildCommand[name = '" + gaeBuildCommandName + "']", additionalBuildcommandsElement);
		if (addToPlugin && gaeBuildCommandElement == null) {
			Element nameElement = document.createElement("name");
			nameElement.setTextContent(gaeBuildCommandName);
			gaeBuildCommandElement = document.createElement("buildCommand");
			gaeBuildCommandElement.appendChild(nameElement);
			additionalBuildcommandsElement.appendChild(gaeBuildCommandElement);
			descriptionOfChange = "added GAE buildCommand to maven-eclipse-plugin";
		} else if (!addToPlugin && gaeBuildCommandElement != null) {
			additionalBuildcommandsElement.removeChild(gaeBuildCommandElement);
			descriptionOfChange = "removed GAE buildCommand from maven-eclipse-plugin";
		}

		if (StringUtils.hasText(descriptionOfChange)) {
			descriptionOfChange += "; ";
		}
		
		// Manage GAE projectnature
		Element additionalProjectnaturesElement = XmlUtils.findFirstElement("/project/build/plugins/plugin[artifactId = 'maven-eclipse-plugin']/configuration/additionalProjectnatures", root);
		Assert.notNull(additionalProjectnaturesElement, "additionalProjectnatures element of the maven-eclipse-plugin required");
		String gaeProjectnatureName = "com.google.appengine.eclipse.core.gaeNature";
		Element gaeProjectnatureElement = XmlUtils.findFirstElement("projectnature[text() = '" + gaeProjectnatureName + "']", additionalProjectnaturesElement);
		if (addToPlugin && gaeProjectnatureElement == null) {
			gaeProjectnatureElement = new XmlElementBuilder("projectnature", document).setText(gaeProjectnatureName).build();
			additionalProjectnaturesElement.appendChild(gaeProjectnatureElement);
			descriptionOfChange += "added GAE projectnature to maven-eclipse-plugin";
		} else if (!addToPlugin && gaeProjectnatureElement != null) {
			additionalProjectnaturesElement.removeChild(gaeProjectnatureElement);
			descriptionOfChange += "removed GAE projectnature from maven-eclipse-plugin";
		}

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	private void updateDataNucleusPlugin(boolean addToPlugin) {
		String pom = projectOperations.getPathResolver().getIdentifier(Path.ROOT, "pom.xml");
		Document document = XmlUtils.readXml(fileManager.getInputStream(pom));
		Element root = document.getDocumentElement();
	
		// Manage mappingExcludes
		Element configurationElement = XmlUtils.findFirstElement("/project/build/plugins/plugin[artifactId = 'maven-datanucleus-plugin']/configuration", root);
		if (configurationElement == null) {
			return;
		}

		String descriptionOfChange = "";
		Element mappingExcludesElement = XmlUtils.findFirstElement("mappingExcludes", configurationElement);
		if (addToPlugin && mappingExcludesElement == null) {
			mappingExcludesElement = new XmlElementBuilder("mappingExcludes", document).setText("**/GaeAuthFilter.class").build();
			configurationElement.appendChild(mappingExcludesElement);
			descriptionOfChange = "added GAEAuthFilter mappingExcludes to maven-datanuclueus-plugin";
		} else if (!addToPlugin && mappingExcludesElement != null) {
			configurationElement.removeChild(mappingExcludesElement);
			descriptionOfChange = "removed GAEAuthFilter mappingExcludes from maven-datanuclueus-plugin";
		}

		fileManager.createOrUpdateTextFileIfRequired(pom, XmlUtils.nodeToString(document), descriptionOfChange, false);
	}

	private void cleanup(Element configuration, OrmProvider ormProvider, JdbcDatabase jdbcDatabase) {
		// Get unwanted databases
		List<JdbcDatabase> databases = new ArrayList<JdbcDatabase>();
		for (JdbcDatabase database : JdbcDatabase.values()) {
			if (!database.getKey().equals(jdbcDatabase.getKey()) && !database.getDriverClassName().equals(jdbcDatabase.getDriverClassName())) {
				databases.add(database);
			}
		}
		String databaseXPath = getDbXPath(databases);

		// Get unwanted ORM providers
		List<OrmProvider> ormProviders = new ArrayList<OrmProvider>();
		for (OrmProvider provider : OrmProvider.values()) {
			if (provider != ormProvider) {
				ormProviders.add(provider);
			}
		}
		String providersXPath = getProviderXPath(ormProviders);

		// Remove redundant dependencies
		List<Dependency> redundantDependencies = new ArrayList<Dependency>();
		redundantDependencies.addAll(getDependencies(databaseXPath, configuration));
		redundantDependencies.addAll(getDependencies(providersXPath, configuration));
		projectOperations.removeDependencies(redundantDependencies);

		// Remove redundant filters
		List<Filter> redundantFilters = new ArrayList<Filter>();
		redundantFilters.addAll(getFilters(databaseXPath, configuration));
		redundantFilters.addAll(getFilters(providersXPath, configuration));
		for (Filter filter : redundantFilters) {
			projectOperations.removeFilter(filter);
		}

		// Remove redundant build plugins
		List<Plugin> redundantBuildPlugins = new ArrayList<Plugin>();
		redundantBuildPlugins.addAll(getPlugins(databaseXPath, configuration));
		redundantBuildPlugins.addAll(getPlugins(providersXPath, configuration));
		projectOperations.removeBuildPlugins(redundantBuildPlugins);

		// Remove redundant resources
		List<Resource> redundantResources = new ArrayList<Resource>();
		redundantResources.addAll(getResources(databaseXPath, configuration));
		redundantResources.addAll(getResources(providersXPath, configuration));
		for (Resource resource : redundantResources) {
			projectOperations.removeResource(resource);
		}

		if (jdbcDatabase != JdbcDatabase.GOOGLE_APP_ENGINE) {
			updateEclipsePlugin(false);
			updateDataNucleusPlugin(false);
		}
	}

	private List<Dependency> getDependencies(String xPathExpression, Element configuration) {
		List<Dependency> dependencies = new ArrayList<Dependency>();
		for (Element dependencyElement : XmlUtils.findElements(xPathExpression + "/dependencies/dependency", configuration)) {
			Dependency dependency = new Dependency(dependencyElement);
			if (projectOperations.getProjectMetadata().isGwtEnabled() && dependency.getGroupId().equals("com.google.appengine") && dependency.getArtifactId().equals("appengine-api-1.0-sdk")) {
				continue;
			}
			dependencies.add(dependency);
		}
		return dependencies;
	}

	private List<Filter> getFilters(String xPathExpression, Element configuration) {
		List<Filter> filters = new ArrayList<Filter>();
		for (Element filterElement : XmlUtils.findElements(xPathExpression + "/filters/filter", configuration)) {
			filters.add(new Filter(filterElement));
		}
		return filters;
	}

	private List<Plugin> getPlugins(String xPathExpression, Element configuration) {
		List<Plugin> buildPlugins = new ArrayList<Plugin>();
		for (Element pluginElement : XmlUtils.findElements(xPathExpression + "/plugins/plugin", configuration)) {
			buildPlugins.add(new Plugin(pluginElement));
		}
		return buildPlugins;
	}

	private List<Resource> getResources(String xPathExpression, Element configuration) {
		List<Resource> resources = new ArrayList<Resource>();
		for (Element resourceElement : XmlUtils.findElements(xPathExpression + "/resources/resource", configuration)) {
			resources.add(new Resource(resourceElement));
		}
		return resources;
	}

	private String getDbXPath(JdbcDatabase jdbcDatabase) {
		return "/configuration/databases/database[@id = '" + jdbcDatabase.getKey() + "']";
	}

	private String getDbXPath(List<JdbcDatabase> databases) {
		StringBuilder builder = new StringBuilder("/configuration/databases/database[");
		for (int i = 0, n = databases.size(); i < n; i++) {
			builder.append("@id = '");
			builder.append(databases.get(i).getKey());
			builder.append("'");
			if (i < n - 1) {
				builder.append(" or ");
			}
		}
		builder.append("]");
		return builder.toString();
	}

	private String getProviderXPath(OrmProvider provider) {
		return "/configuration/ormProviders/provider[@id = '" + provider.name() + "']";
	}

	private String getProviderXPath(List<OrmProvider> ormProviders) {
		StringBuilder builder = new StringBuilder("/configuration/ormProviders/provider[");
		for (int i = 0, n = ormProviders.size(); i < n; i++) {
			builder.append("@id = '");
			builder.append(ormProviders.get(i).name());
			builder.append("'");
			if (i < n - 1) {
				builder.append(" or ");
			}
		}
		builder.append("]");
		return builder.toString();
	}

	private Element createPropertyElement(String name, String value, Document document) {
		Element property = document.createElement("property");
		property.setAttribute("name", name);
		property.setAttribute("value", value);
		return property;
	}

	private Element createRefElement(String name, String value, Document document) {
		Element property = document.createElement("property");
		property.setAttribute("name", name);
		property.setAttribute("ref", value);
		return property;
	}

	private SortedSet<String> getPropertiesFromDataNucleusConfiguration() {
		String persistenceXmlPath = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml");
		if (!fileManager.exists(persistenceXmlPath)) {
			throw new IllegalStateException("Failed to find " + persistenceXmlPath);
		}

		Document document = XmlUtils.readXml(fileManager.getInputStream(persistenceXmlPath));
		Element root = document.getDocumentElement();

		List<Element> propertyElements = XmlUtils.findElements("/persistence/persistence-unit/properties/property", root);
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

			if (properties.size() == 4) {
				// All required properties have been found so ignore rest of elements
				break;
			}
		}
		return properties;
	}
}
