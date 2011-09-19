package org.springframework.roo.addon.jpa;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.roo.addon.jpa.JdbcDatabase.H2_IN_MEMORY;
import static org.springframework.roo.addon.jpa.OrmProvider.HIBERNATE;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;

/**
 * Unit test of {@link JpaOperationsImpl}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class JpaOperationsImplTest {

	// Constants
	private static final String APPLICATION_CONTEXT_PATH = "/path/to/the/app/context";
	private static final String DB_HOST_NAME = "myDbHost";
	private static final String DB_JNDI_NAME = "myDataSource";
	private static final String DB_NAME = "myDbName";
	private static final String DB_PASSWORD = "myDbPassword";
	private static final String DB_USER_NAME = "myDbUserName";
	private static final String PERSISTENCE_UNIT = "myPersistenceUnit";
	private static final String POM_PATH = "/path/to/the/pom";
	private static final String TRANSACTION_MANAGER = "myTransactionManager";
	
	// Fixture
	private JpaOperationsImpl jpaOperations;
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
	
//	@Test
	public void testConfigureJpaForH2InMemoryAndHibernate() {
		// Set up
		when(mockFileManager.getInputStream(POM_PATH)).thenReturn(getPomInputStream(POM), getPomInputStream(POM));
		when(mockFileManager.getInputStream(APPLICATION_CONTEXT_PATH)).thenReturn(getAppContextInputStream(APP_CONTEXT));
		
		// Invoke
		this.jpaOperations.configureJpa(HIBERNATE, H2_IN_MEMORY, DB_JNDI_NAME, null, DB_HOST_NAME, DB_NAME, DB_USER_NAME, DB_PASSWORD, TRANSACTION_MANAGER, PERSISTENCE_UNIT);
		
		// Check
		fail("verify");
		verifyNoMoreInteractions(mockFileManager, mockPathResolver, mockProjectOperations);
	}

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
}
