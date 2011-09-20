package org.springframework.roo.addon.jpa;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.roo.addon.jpa.JdbcDatabase.H2_IN_MEMORY;
import static org.springframework.roo.addon.jpa.JpaOperationsImpl.JPA_DIALECTS_FILE;
import static org.springframework.roo.addon.jpa.JpaOperationsImpl.PERSISTENCE_XML;
import static org.springframework.roo.addon.jpa.OrmProvider.HIBERNATE;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.StringUtils;

/**
 * Unit test of {@link JpaOperationsImpl}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class JpaOperationsImplTest {

	// Constants
	private static final String APPLICATION_CONTEXT_PATH = "/path/to/the/app/context";
	private static final String DB_DIALECT = "dbDialect";
	private static final String DB_HOST_NAME = "myDbHost";
	private static final String DB_JNDI_NAME = "myDataSource";
	private static final String DB_NAME = "myDbName";
	private static final String DB_PASSWORD = "myDbPassword";
	private static final String DB_USER_NAME = "myDbUserName";
	private static final String PERSISTENCE_PATH = "/path/to/persistence";
	private static final String PERSISTENCE_UNIT = "myPersistenceUnit";
	private static final String POM_PATH = "/path/to/the/pom";
	private static final String TRANSACTION_MANAGER = "myTransactionManager";
	
	// Fixture
	private JpaOperationsImpl jpaOperations;
	private Properties dialects;
	@Mock private FileManager mockFileManager;
	@Mock private PathResolver mockPathResolver;
	@Mock private ProjectOperations mockProjectOperations;
	@Mock private PropFileOperations mockPropFileOperations;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		
		// Mocks
		when(mockProjectOperations.getPathResolver()).thenReturn(mockPathResolver);
		when(mockPathResolver.getIdentifier(Path.ROOT, JpaOperationsImpl.POM_XML)).thenReturn(POM_PATH);
		when(mockPathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, JpaOperationsImpl.APPLICATION_CONTEXT_XML)).thenReturn(APPLICATION_CONTEXT_PATH);
		
		// Object under test
		this.jpaOperations = new JpaOperationsImpl();
		this.jpaOperations.fileManager = mockFileManager;
		this.jpaOperations.projectOperations = mockProjectOperations;
		this.jpaOperations.propFileOperations = mockPropFileOperations;
		
		// Things that are too hard or ugly to mock
		this.dialects = new Properties();
	}
	
	private static final String POM =
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
		"<project>" +
		"    <build>" +
		"        <plugins>" +
		"            <plugin>" +
		"                <groupId>org.apache.maven.plugins</groupId>" +
		"                <artifactId>maven-eclipse-plugin</artifactId>" +
		"                <version>2.7</version>" +
		"                <configuration>" +
		"                    <additionalBuildcommands></additionalBuildcommands>" +
		"                    <additionalProjectnatures></additionalProjectnatures>" +
		"                </configuration>" +
		"            </plugin>" +
		"        </plugins>" +
		"    </build>" +
		"</project>";
	
	private static final String APP_CONTEXT =
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
		"<beans>" +
		"</beans>";

	/**
	 * Creates a new {@link InputStream} each time we need to read from the 
	 * Spring application context
	 * 
	 * @param pom the application context XML as a String (required)
	 * @return a fresh stream
	 */
	private InputStream getAppContextInputStream(final String appContext) {
		return new ByteArrayInputStream(appContext.getBytes());
	}

	/**
	 * Creates a new {@link InputStream} each time we need to read from the POM
	 * 
	 * @param pom the POM XML as a String (required)
	 * @return a fresh stream
	 */
	private ByteArrayInputStream getPomInputStream(final String pom) {
		return new ByteArrayInputStream(pom.getBytes());
	}
	
	private static final String EXPECTED_APPLICATION_CONTEXT =
		"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
		"<beans>\n" +
		"    <jee:jndi-lookup id=\"dataSource\" jndi-name=\"myDataSource\"/>\n" +
		"    <bean class=\"org.springframework.orm.jpa.JpaTransactionManager\" id=\"myTransactionManager\">\n" +
		"        <property name=\"entityManagerFactory\" ref=\"entityManagerFactory\"/>\n" +
		"    </bean>\n" +
		"    <tx:annotation-driven mode=\"aspectj\" transaction-manager=\"myTransactionManager\"/>\n" +
		"    <bean class=\"org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean\" id=\"entityManagerFactory\">\n" +
		"        <property name=\"persistenceUnitName\" value=\"myPersistenceUnit\"/>\n" +
		"        <property name=\"dataSource\" ref=\"dataSource\"/>\n" +
		"    </bean>\n" +
		"</beans>\n";
	
	private static final String EXPECTED_PERSISTENCE_XML_FOR_H2_IN_MEMORY_AND_HIBERNATE =
		"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
		"<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"2.0\" xsi:schemaLocation=\"http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd\">\n" +
		"<persistence-unit name=\"myPersistenceUnit\" transaction-type=\"RESOURCE_LOCAL\">\n" +
		"        <provider>org.hibernate.ejb.HibernatePersistence</provider>\n" +
		"        <properties>\n" +
		"            <property name=\"hibernate.dialect\" value=\"dbDialect\"/>\n" +
		"            <!-- value=\"create\" to build a new database on each run; value=\"update\" to modify an existing database; value=\"create-drop\" means the same as \"create\" but also drops tables when Hibernate closes; value=\"validate\" makes no changes to the database -->\n" +
		"            <property name=\"hibernate.hbm2ddl.auto\" value=\"create\"/>\n" +
		"            <property name=\"hibernate.ejb.naming_strategy\" value=\"org.hibernate.cfg.ImprovedNamingStrategy\"/>\n" +
		"            <property name=\"hibernate.connection.charSet\" value=\"UTF-8\"/>\n" +
		"            <!-- Uncomment the following two properties for JBoss only -->\n" +
		"            <!-- property name=\"hibernate.validator.apply_to_ddl\" value=\"false\" /-->\n" +
		"            <!-- property name=\"hibernate.validator.autoregister_listeners\" value=\"false\" /-->\n" +
		"        </properties>\n" +
		"    </persistence-unit>\n" +
		"</persistence>\n";
	
	@Test
	public void testConfigureJpaForH2InMemoryAndHibernateForNewProject() {
		// Set up
		when(mockFileManager.getInputStream(POM_PATH)).thenReturn(getPomInputStream(POM), getPomInputStream(POM));
		when(mockFileManager.getInputStream(APPLICATION_CONTEXT_PATH)).thenReturn(getAppContextInputStream(APP_CONTEXT));
		when(mockPathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, PERSISTENCE_XML)).thenReturn(PERSISTENCE_PATH);
		when(mockFileManager.exists(PERSISTENCE_PATH)).thenReturn(false);	// i.e. no existing persistence.xml
		when(mockPropFileOperations.loadProperties(JPA_DIALECTS_FILE, JpaOperationsImpl.class)).thenReturn(dialects);
		final ProjectMetadata mockProjectMetadata = mock(ProjectMetadata.class);
		when(mockProjectOperations.getProjectMetadata()).thenReturn(mockProjectMetadata);
		
		final OrmProvider ormProvider = HIBERNATE;
		final JdbcDatabase jdbcDatabase = H2_IN_MEMORY;
		dialects.put(ormProvider.name() + "." + jdbcDatabase.name(), DB_DIALECT);
		
		// Invoke
		this.jpaOperations.configureJpa(ormProvider, jdbcDatabase, DB_JNDI_NAME, null, DB_HOST_NAME, DB_NAME, DB_USER_NAME, DB_PASSWORD, TRANSACTION_MANAGER, PERSISTENCE_UNIT);
		
		// Check
		verifyFileUpdate(EXPECTED_APPLICATION_CONTEXT, APPLICATION_CONTEXT_PATH);
		verifyFileUpdate(EXPECTED_PERSISTENCE_XML_FOR_H2_IN_MEMORY_AND_HIBERNATE, PERSISTENCE_PATH);
	}
	
	/**
	 * Verifies that the mock {@link FileManager} was asked to write the given
	 * contents to the given file
	 *  
	 * @param expectedContents the contents we expect to be written
	 * @param filename the file we expect to be written to
	 */
	private void verifyFileUpdate(final String expectedContents, final String filename) {
		final ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
		verify(mockFileManager).createOrUpdateTextFileIfRequired(eq(filename), textCaptor.capture(), eq(false));
		// Replace the dummy line terminator with the platform-specific one that
		// will be applied by XmlUtils.nodeToString.
		final String normalisedContents = expectedContents.replace("\n", StringUtils.LINE_SEPARATOR);
		assertEquals(normalisedContents, textCaptor.getValue());
	}
}
