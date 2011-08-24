package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.springframework.roo.support.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Unit test of the {@link Plugin} class
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class PluginTest {

	// Constants
	private static final DocumentBuilder DOCUMENT_BUILDER;
	private static final List<Dependency> NO_DEPENDENCIES = Collections.emptyList();
	private static final List<Execution> NO_EXECUTIONS = Collections.emptyList();
	private static final String ARTIFACT_ID = "bar";
	private static final String EXECUTION_GOAL = "add-tests";
	private static final String EXECUTION_ID = "build-it";
	private static final String EXECUTION_PHASE = "test";
	private static final String GROUP_ID = "com.foo";
	private static final String VERSION = "1.2.3";
	
	static {
		try {
			DOCUMENT_BUILDER = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static final String PLUGIN_XML_AV =
		"<plugin>" +
			"<artifactId>foo-plugin</artifactId>" +
			"<version>1.6</version>" +
		"</plugin>";
	
	private static final String PLUGIN_XML_GAV =
		"<plugin>" +
			"<groupId>org.codehaus.mojo</groupId>" +
			"<artifactId>build-helper-maven-plugin</artifactId>" +
			"<version>1.5</version>" +
		"</plugin>";
	
	private static final String PLUGIN_XML_WITH_DEPENDENCY =
		"<plugin>" +
			"<groupId>com.example</groupId>" +
			"<artifactId>ball-of-mud</artifactId>" +
			"<version>1.4</version>" +
			"<dependencies>" +
				"<dependency>" +
					"<groupId>com.acme</groupId>" +
					"<artifactId>huge-thing</artifactId>" +
					"<version>1.1</version>" +
				"</dependency>" +
			"</dependencies>" +
		"</plugin>";
	
	private static final String EXECUTION_CONFIGURATION_XML =
		"<configuration>" +
			"<sources>" +
				"<source>src/main/groovy</source>" +
			"</sources>" +
		"</configuration>";
	
	private static final String PLUGIN_XML_WITH_EXECUTION =
		"<plugin>" +
			"<groupId>tv.reality</groupId>" +
			"<artifactId>view-plugin</artifactId>" +
			"<version>2.5</version>" +
			"<executions>" +
				"<execution>" +
					"<id>" + EXECUTION_ID + "</id>" +
					"<phase>" + EXECUTION_PHASE + "</phase>" +
					"<goals>" +
						"<goal>" + EXECUTION_GOAL + "</goal>" +
					"</goals>" +
					EXECUTION_CONFIGURATION_XML +
				"</execution>" +
			"</executions>" +
		"</plugin>";
	
	@Test
	public void testXmlElementConstructorWithArtifactAndVersion() throws Exception {
		// In this case we expect the default groupId to be used
		assertPluginFromXml(PLUGIN_XML_AV, Plugin.DEFAULT_GROUP_ID, "foo-plugin", "1.6", null, NO_DEPENDENCIES, NO_EXECUTIONS);
	}
	
	@Test
	public void testXmlElementConstructorWithGroupArtifactAndVersion() throws Exception {
		assertPluginFromXml(PLUGIN_XML_GAV, "org.codehaus.mojo", "build-helper-maven-plugin", "1.5", null, NO_DEPENDENCIES, NO_EXECUTIONS);
	}
	
	@Test
	public void testXmlElementConstructorWithOneDependency() throws Exception {
		final Dependency dependency = new Dependency("com.acme", "huge-thing", "1.1");
		assertPluginFromXml(PLUGIN_XML_WITH_DEPENDENCY, "com.example", "ball-of-mud", "1.4", null, Arrays.asList(dependency), NO_EXECUTIONS);
	}
	
	@Test
	public void testXmlElementConstructorWithOneExecution() throws Exception {
		final Configuration executionConfiguration = new Configuration(getElement(EXECUTION_CONFIGURATION_XML));
		final Execution execution = new Execution(EXECUTION_ID, EXECUTION_PHASE, executionConfiguration, EXECUTION_GOAL);
		assertPluginFromXml(PLUGIN_XML_WITH_EXECUTION, "tv.reality", "view-plugin", "2.5", null, NO_DEPENDENCIES, Arrays.asList(execution));
	}
	
	/**
	 * Asserts that constructing a {@link Plugin} from the given XML gives rise to the given properties
	 * 
	 * @param xml the XML from which to construct the {@link Plugin}
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @param configuration
	 * @param dependencies
	 * @param executions
	 * @throws Exception
	 */
	private void assertPluginFromXml(final String xml, final String groupId, final String artifactId, final String version, final Configuration configuration, final List<Dependency> dependencies, final List<Execution> executions) throws Exception {
		// Set up
		final Element pluginElement = getElement(xml);
		
		// Invoke
		final Plugin plugin = new Plugin(pluginElement);
		
		// Check
		assertPluginEquals(groupId, artifactId, version, configuration, dependencies, executions, plugin);
	}

	@Test
	public void testGroupArtifactVersionConstructor() {
		// Invoke
		final Plugin plugin = new Plugin(GROUP_ID, ARTIFACT_ID, VERSION);
		
		// Check
		assertPluginEquals(GROUP_ID, ARTIFACT_ID, VERSION, null, NO_DEPENDENCIES, NO_EXECUTIONS, plugin);
	}

	@Test
	public void testFullConstructor() {
		// Set up
		final Configuration mockConfiguration = mock(Configuration.class);
		final List<Dependency> mockDependencies = Arrays.asList(mock(Dependency.class), mock(Dependency.class));
		final List<Execution> mockExecutions = Arrays.asList(mock(Execution.class), mock(Execution.class));
		
		// Invoke
		final Plugin plugin = new Plugin(GROUP_ID, ARTIFACT_ID, VERSION, mockConfiguration, mockDependencies, mockExecutions);
		
		// Check
		assertPluginEquals(GROUP_ID, ARTIFACT_ID, VERSION, mockConfiguration, mockDependencies, mockExecutions, plugin);
	}
	
	/**
	 * Asserts that the given {@link Plugin} has the given expected values
	 * 
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @param configuration
	 * @param dependencies
	 * @param executions
	 * @param actual
	 */
	private void assertPluginEquals(final String groupId, final String artifactId, final String version, final Configuration configuration, final List<Dependency> dependencies, final List<Execution> executions, final Plugin actual) {
		assertEquals(artifactId, actual.getArtifactId());
		assertEquals(configuration, actual.getConfiguration());
		assertEquals(dependencies, actual.getDependencies());
		assertEquals(executions, actual.getExecutions());
		assertEquals(groupId, actual.getGroupId());
		assertEquals(groupId + ":" + artifactId + ":" + version, actual.getSimpleDescription());
		assertEquals(version, actual.getVersion());
	}
	
	/**
	 * Returns the XML {@link Element} for the root of the given XML test
	 * 
	 * @param text can be blank
	 * @return <code>null</code> if the text is blank
	 * @throws Exception
	 */
	private Element getElement(final String text) throws Exception {
		if (StringUtils.hasText(text)) {
			return DOCUMENT_BUILDER.parse(new ByteArrayInputStream(text.getBytes())).getDocumentElement();
		}
		return null;
	}
}
