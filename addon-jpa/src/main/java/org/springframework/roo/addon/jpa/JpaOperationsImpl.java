package org.springframework.roo.addon.jpa;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

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
		Assert.notNull(ormProvider, "ORM provider required");
		Assert.notNull(jdbcDatabase, "JDBC database required");

		// Parse the configuration.xml file
		Element configuration = XmlUtils.getConfiguration(getClass());

		// Remove unnecessary artifacts not specific to current database and JPA provider
		cleanup(configuration, ormProvider, jdbcDatabase);

		updateApplicationContext(ormProvider, jdbcDatabase, jndi, persistenceUnit);
		updatePersistenceXml(ormProvider, jdbcDatabase, hostName, databaseName, userName, password, persistenceUnit);
		updateGaeXml(ormProvider, jdbcDatabase, applicationId);
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
	}

	private void updateApplicationContext(OrmProvider ormProvider, JdbcDatabase jdbcDatabase, String jndi, String persistenceUnit) {
		String contextPath = projectOperations.getPathResolver().getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext.xml");
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
			if (jdbcDatabase != JdbcDatabase.MYSQL && validationQueryElement != null && testOnBorrowElement != null) {
				dataSource.removeChild(validationQueryElement);
				dataSource.removeChild(testOnBorrowElement);
			} else if (jdbcDatabase == JdbcDatabase.MYSQL && validationQueryElement == null && testOnBorrowElement == null) {
				dataSource.appendChild(createPropertyElement("validationQuery", "SELECT 1 FROM DUAL", appCtx));
				dataSource.appendChild(createPropertyElement("testOnBorrow", "true", appCtx));
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

		fileManager.createOrUpdateXmlFileIfRequired(contextPath, appCtx, true);
	}

	private void updatePersistenceXml(OrmProvider ormProvider, JdbcDatabase jdbcDatabase, String hostName, String databaseName, String userName, String password, String persistenceUnit) {
		String persistencePath = getPersistencePath();

		Document persistence;
		try {
			MutableFile persistenceMutableFile = null;
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
		
		fileManager.createOrUpdateXmlFileIfRequired(persistencePath, persistence, true);
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

	private void updateGaeXml(OrmProvider ormProvider, JdbcDatabase jdbcDatabase, String applicationId) {
		PathResolver pathResolver = projectOperations.getPathResolver();
		String appenginePath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/appengine-web.xml");
		boolean appenginePathExists = fileManager.exists(appenginePath);
		String loggingPropertiesPath = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/logging.properties");
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

		fileManager.createOrUpdateXmlFileIfRequired(appenginePath, appengine, true);

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
			outputStream = databaseMutableFile.getOutputStream();
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

		OutputStream outputStream = null;
		try {
			outputStream = configMutableFile.getOutputStream();
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
		} catch (IOException ioe) {
			throw new IllegalStateException(ioe);
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
		
		for (Plugin buildPlugin : buildPlugins) {
			projectOperations.addBuildPlugin(buildPlugin);
		}

		if (jdbcDatabase == JdbcDatabase.GOOGLE_APP_ENGINE) {
			updateEclipsePlugin(true);
		}
	}

	private void updateEclipsePlugin(boolean addBuildCommand) {
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
		
		updateEclipsePlugin(addBuildCommand, pom, mutableFile);
	}
	
	private void updateEclipsePlugin(boolean addBuildCommand, Document pom, MutableFile mutableFile) {
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
			mutableFile.setDescriptionOfChange("Updated maven-eclipse-plugin");
			XmlUtils.writeXml(mutableFile.getOutputStream(), pom);
		} 
		
		if (!addBuildCommand && buildCommandElement != null) {
			additionalBuildcommandsElement.removeChild(buildCommandElement);
			mutableFile.setDescriptionOfChange("Updated maven-eclipse-plugin");
			XmlUtils.writeXml(mutableFile.getOutputStream(), pom);
		}
	}

	private void cleanup(Element configuration, OrmProvider ormProvider, JdbcDatabase jdbcDatabase) {
		String pomPath = projectOperations.getPathResolver().getIdentifier(Path.ROOT, "/pom.xml");
		MutableFile mutableFile = fileManager.updateFile(pomPath);

		Document pom;
		try {
			pom = XmlUtils.getDocumentBuilder().parse(mutableFile.getInputStream());
		} catch (Exception ex) {
			throw new IllegalStateException("Could not open POM '" + pomPath + "'", ex);
		}

		Element root = (Element) pom.getFirstChild();
		
		// Removed unwanted database dependencies
		List<JdbcDatabase> databases = new ArrayList<JdbcDatabase>();
		for (JdbcDatabase database : JdbcDatabase.values()) {
			if (!database.getKey().equals(jdbcDatabase.getKey()) && !database.getDriverClassName().equals(jdbcDatabase.getDriverClassName())) {
				databases.add(database);
			}
		}
		boolean hasChanged = removeArtifacts(getDbXPath(databases), root, configuration);

		// Removed unwanted ORM providers
		List<OrmProvider> ormProviders = new ArrayList<OrmProvider>();
		for (OrmProvider provider : OrmProvider.values()) {
			if (provider != ormProvider) {
				ormProviders.add(provider);
			}
		}
		
		hasChanged |= removeArtifacts(getProviderXPath(ormProviders), root, configuration);

		if (hasChanged) {
			// Something has changed so write changes to pom.xml
			mutableFile.setDescriptionOfChange("Removed redundant artifacts");
			XmlUtils.writeXml(mutableFile.getOutputStream(), pom);
		}

		if (jdbcDatabase != JdbcDatabase.GOOGLE_APP_ENGINE) {
			updateEclipsePlugin(false, pom, mutableFile);
		}
	}
		
	private boolean removeArtifacts(String xPathExpression, Element root, Element configuration) {
		boolean hasChanged = false;
		
		// Remove unwanted dependencies
		Element dependenciesElement = XmlUtils.findFirstElement("/project/dependencies", root);
		for (Element candidate : XmlUtils.findElements("/project/dependencies/dependency", root)) {
			for (Element dependencyElement : XmlUtils.findElements(xPathExpression + "/dependencies/dependency", configuration)) {
				if (new Dependency(dependencyElement).equals(new Dependency(candidate))) {
					// Found it
					dependenciesElement.removeChild(candidate);
					XmlUtils.removeTextNodes(dependenciesElement);
					hasChanged = true;
				}
			}
		}

		// Remove unwanted filters
		Element filtersElement = XmlUtils.findFirstElement("/project/build/filters", root);
		if (filtersElement != null) {
			for (Element candidate : XmlUtils.findElements("/project/build/filters/filter", root)) {
				for (Element filterElement : XmlUtils.findElements(xPathExpression + "/filters/filter", configuration)) {
					if (new Filter(filterElement).equals(new Filter(candidate))) {
						// Found it
						filtersElement.removeChild(candidate);
						XmlUtils.removeTextNodes(filtersElement);
						hasChanged = true;
					}
				}
			}

			if (!filtersElement.hasChildNodes()) {
				filtersElement.getParentNode().removeChild(filtersElement);
			}
		}
		
		// Remove unwanted plugins
		Element pluginsElement = XmlUtils.findFirstElement("/project/build/plugins", root);
		for (Element candidate : XmlUtils.findElements("/project/build/plugins/plugin", root)) {
			for (Element pluginElement : XmlUtils.findElements(xPathExpression + "/plugins/plugin", configuration)) {
				if (new Plugin(pluginElement).equals(new Plugin(candidate))) {
					// Found it
					pluginsElement.removeChild(candidate);
					XmlUtils.removeTextNodes(pluginsElement);
					hasChanged = true;
				}
			}
		}
		
		return hasChanged;
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
		String persistenceXmlPath = projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml");
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
			
			if (properties.size() == 4) {
				// All required properties have been found so ignore rest of elements
				break;
			}
		}
		return properties;
	}
}
