package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.roo.support.util.XmlUtils.stringToElement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Unit test of the {@link Plugin} class
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class PluginTest extends XmlTestCase {

    private static final String DEPENDENCY_ARTIFACT_ID = "huge-thing";
    private static final String DEPENDENCY_GROUP_ID = "com.acme";

    private static final String DEPENDENCY_VERSION = "1.1";
    private static final String DEPENDENCY_XML = "        <dependency>\n"
            + "            <groupId>" + DEPENDENCY_GROUP_ID + "</groupId>\n"
            + "            <artifactId>" + DEPENDENCY_ARTIFACT_ID
            + "</artifactId>\n" + "            <version>" + DEPENDENCY_VERSION
            + "</version>\n" + "        </dependency>\n";
    private static final String EXECUTION_CONFIGURATION_XML = "            <configuration>\n"
            + "                <sources>\n"
            + "                    <source>src/main/groovy</source>\n"
            + "                </sources>\n" + "            </configuration>\n";

    private static final String EXECUTION_GOAL = "add-tests";
    private static final String EXECUTION_ID = "build-it";
    private static final String EXECUTION_PHASE = "test";

    private static final String EXECUTION_XML = "        <execution>\n"
            + "            <id>" + EXECUTION_ID + "</id>\n"
            + "            <phase>" + EXECUTION_PHASE + "</phase>\n"
            + "            <goals>\n" + "                <goal>"
            + EXECUTION_GOAL + "</goal>\n" + "            </goals>\n"
            + EXECUTION_CONFIGURATION_XML + "        </execution>\n";

    private static final List<Dependency> NO_DEPENDENCIES = Collections
            .emptyList();

    private static final List<Execution> NO_EXECUTIONS = Collections
            .emptyList();

    private static final String PLUGIN_ARTIFACT_ID = "bar";

    private static final String PLUGIN_CONFIGURATION_XML = "    <configuration>\n"
            + "        <targets>\n"
            + "            <target>classes</target>\n"
            + "        </targets>\n" + "    </configuration>\n";

    private static final String PLUGIN_GROUP_ID = "com.foo";

    private static final String PLUGIN_VERSION = "1.2.3";

    private static final String PLUGIN_XML_AV = "<plugin>"
            + "<artifactId>foo-plugin</artifactId>" + "<version>1.6</version>"
            + "</plugin>";

    private static final String PLUGIN_XML_GAV = "<plugin>"
            + "<groupId>org.codehaus.mojo</groupId>"
            + "<artifactId>build-helper-maven-plugin</artifactId>"
            + "<version>1.5</version>" + "</plugin>";

    private static final String PLUGIN_XML_WITH_DEPENDENCY = "<plugin>"
            + "<groupId>com.example</groupId>"
            + "<artifactId>ball-of-mud</artifactId>" + "<version>1.4</version>"
            + "<dependencies>" + "<dependency>" + "<groupId>"
            + DEPENDENCY_GROUP_ID + "</groupId>" + "<artifactId>"
            + DEPENDENCY_ARTIFACT_ID + "</artifactId>" + "<version>"
            + DEPENDENCY_VERSION + "</version>" + "</dependency>"
            + "</dependencies>" + "</plugin>";

    private static final String PLUGIN_WITHOUT_VERSION_WITH_DEPENDENCY = "<plugin>"
            + "<groupId>com.example</groupId>"
            + "<artifactId>ball-of-mud</artifactId>"  
            + "<dependencies>" + "<dependency>" + "<groupId>"
            + DEPENDENCY_GROUP_ID + "</groupId>" + "<artifactId>"
            + DEPENDENCY_ARTIFACT_ID + "</artifactId>" + "<version>"
            + DEPENDENCY_VERSION + "</version>" + "</dependency>"
            + "</dependencies>" + "</plugin>";

    private static final String PLUGIN_XML_WITH_EXECUTION = "<plugin>"
            + "<groupId>tv.reality</groupId>"
            + "<artifactId>view-plugin</artifactId>" + "<version>2.5</version>"
            + "<executions>" + "<execution>" + "<id>" + EXECUTION_ID + "</id>"
            + "<phase>" + EXECUTION_PHASE + "</phase>" + "<goals>" + "<goal>"
            + EXECUTION_GOAL + "</goal>" + "</goals>"
            + EXECUTION_CONFIGURATION_XML + "</execution>" + "</executions>"
            + "</plugin>";

    private static final String MAXIMAL_PLUGIN_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<plugin>\n"
            + "    <groupId>"
            + PLUGIN_GROUP_ID
            + "</groupId>\n"
            + "    <artifactId>"
            + PLUGIN_ARTIFACT_ID
            + "</artifactId>\n"
            + "    <version>"
            + PLUGIN_VERSION
            + "</version>\n"
            + PLUGIN_CONFIGURATION_XML
            + "    <executions>\n"
            + EXECUTION_XML
            + "    </executions>\n"
            + "    <dependencies>\n"
            + DEPENDENCY_XML
            + "    </dependencies>\n" + "</plugin>";

    private static final String MININAL_PLUGIN_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<plugin>\n"
            + "    <groupId>"
            + PLUGIN_GROUP_ID
            + "</groupId>\n"
            + "    <artifactId>"
            + PLUGIN_ARTIFACT_ID
            + "</artifactId>\n"
            + "    <version>" + PLUGIN_VERSION + "</version>\n" + "</plugin>";

    /**
     * Asserts that the given plugin returns the expected XML for its POM
     * element
     * 
     * @param plugin the plugin for which to check the XML (required)
     * @param document the document in which to create the element (required)
     * @param expectedXml the expected XML element; can be blank
     */
    private void assertElement(final Plugin plugin, final Document document,
            final String expectedXml) {
        // Invoke
        final Element element = plugin.getElement(document);

        // Check
        assertXmlEquals(expectedXml, element);
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
    private void assertPluginEquals(final String groupId,
            final String artifactId, final String version,
            final Configuration configuration,
            final List<Dependency> dependencies,
            final List<Execution> executions, final Plugin actual) {
        assertEquals(artifactId, actual.getArtifactId());
        assertEquals(configuration, actual.getConfiguration());
        assertEquals(dependencies, actual.getDependencies());
        assertEquals(executions, actual.getExecutions());
        assertEquals(groupId, actual.getGroupId());
        assertEquals(groupId + ":" + artifactId + ":" + version,
                actual.getSimpleDescription());
        assertEquals(version, actual.getVersion());
    }

    /**
     * Asserts that constructing a {@link Plugin} from the given XML gives rise
     * to the given properties
     * 
     * @param xml the XML from which to construct the {@link Plugin}
     * @param expectedGroupId
     * @param expectedArtifactId
     * @param expectedVersion
     * @param expectedConfiguration
     * @param expectedDependencies
     * @param expectedExecutions
     * @throws Exception
     */
    private void assertPluginFromXml(final String xml,
            final String expectedGroupId, final String expectedArtifactId,
            final String expectedVersion,
            final Configuration expectedConfiguration,
            final List<Dependency> expectedDependencies,
            final List<Execution> expectedExecutions) {
        // Set up
        final Element pluginElement = stringToElement(xml);

        // Invoke
        final Plugin plugin = new Plugin(pluginElement);

        // Check
        assertPluginEquals(expectedGroupId, expectedArtifactId,
                expectedVersion, expectedConfiguration, expectedDependencies,
                expectedExecutions, plugin);
    }

    @Test
    public void testFullConstructor() {
        // Set up
        final Configuration mockConfiguration = mock(Configuration.class);
        final List<Dependency> mockDependencies = Arrays.asList(
                mock(Dependency.class), mock(Dependency.class));
        final List<Execution> mockExecutions = Arrays.asList(
                mock(Execution.class), mock(Execution.class));

        // Invoke
        final Plugin plugin = new Plugin(PLUGIN_GROUP_ID, PLUGIN_ARTIFACT_ID,
                PLUGIN_VERSION, mockConfiguration, mockDependencies,
                mockExecutions);

        // Check
        assertPluginEquals(PLUGIN_GROUP_ID, PLUGIN_ARTIFACT_ID, PLUGIN_VERSION,
                mockConfiguration, mockDependencies, mockExecutions, plugin);
    }

    @Test
    public void testGetElementForMaximalPlugin() throws Exception {
        // Set up
        final Configuration mockPluginConfiguration = mock(Configuration.class);
        when(mockPluginConfiguration.getConfiguration()).thenReturn(
                stringToElement(PLUGIN_CONFIGURATION_XML));

        final Document document = DOCUMENT_BUILDER.newDocument();

        final Dependency mockDependency = mock(Dependency.class);
        final Element dependencyElement = (Element) document.importNode(
                stringToElement(DEPENDENCY_XML), true);
        when(mockDependency.getElement(document)).thenReturn(dependencyElement);

        final Execution mockExecution = mock(Execution.class);
        final Element executionElement = (Element) document.importNode(
                stringToElement(EXECUTION_XML), true);
        when(mockExecution.getElement(document)).thenReturn(executionElement);

        final Plugin plugin = new Plugin(PLUGIN_GROUP_ID, PLUGIN_ARTIFACT_ID,
                PLUGIN_VERSION, mockPluginConfiguration,
                Arrays.asList(mockDependency), Arrays.asList(mockExecution));

        // Invoke and check
        assertElement(plugin, document, MAXIMAL_PLUGIN_XML);
    }

    @Test
    public void testGetElementForMinimalPlugin() {
        assertElement(new Plugin(PLUGIN_GROUP_ID, PLUGIN_ARTIFACT_ID,
                PLUGIN_VERSION), DOCUMENT_BUILDER.newDocument(),
                MININAL_PLUGIN_XML);
    }

    @Test
    public void testGroupArtifactVersionConstructor() {
        // Invoke
        final Plugin plugin = new Plugin(PLUGIN_GROUP_ID, PLUGIN_ARTIFACT_ID,
                PLUGIN_VERSION);

        // Check
        assertPluginEquals(PLUGIN_GROUP_ID, PLUGIN_ARTIFACT_ID, PLUGIN_VERSION,
                null, NO_DEPENDENCIES, NO_EXECUTIONS, plugin);
    }

    @Test
    public void testXmlElementConstructorWithArtifactAndVersion()
            throws Exception {
        // In this case we expect the default groupId to be used
        assertPluginFromXml(PLUGIN_XML_AV, Plugin.DEFAULT_GROUP_ID,
                "foo-plugin", "1.6", null, NO_DEPENDENCIES, NO_EXECUTIONS);
    }

    @Test
    public void testXmlElementConstructorWithGroupArtifactAndVersion()
            throws Exception {
        assertPluginFromXml(PLUGIN_XML_GAV, "org.codehaus.mojo",
                "build-helper-maven-plugin", "1.5", null, NO_DEPENDENCIES,
                NO_EXECUTIONS);
    }

    @Test
    public void testXmlElementConstructorWithOneDependency() throws Exception {
        final Dependency expectedDependency = new Dependency(
                DEPENDENCY_GROUP_ID, DEPENDENCY_ARTIFACT_ID, DEPENDENCY_VERSION);
        assertPluginFromXml(PLUGIN_XML_WITH_DEPENDENCY, "com.example",
                "ball-of-mud", "1.4", null, Arrays.asList(expectedDependency),
                NO_EXECUTIONS);
    }

//    @Test(expected = IllegalArgumentException.class)
//    public void testXmlElementConstructorWithoutVersionWithOneDependency() throws Exception {
//        final Dependency expectedDependency = new Dependency(
//                DEPENDENCY_GROUP_ID, DEPENDENCY_ARTIFACT_ID, DEPENDENCY_VERSION);
//        assertPluginFromXml(PLUGIN_WITHOUT_VERSION_WITH_DEPENDENCY, "com.example",
//                "ball-of-mud", "", null, Arrays.asList(expectedDependency),
//                NO_EXECUTIONS);
//    }

    @Test
    public void testXmlElementConstructorWithOneExecution() throws Exception {
        final Configuration executionConfiguration = new Configuration(
                stringToElement(EXECUTION_CONFIGURATION_XML));
        final Execution execution = new Execution(EXECUTION_ID,
                EXECUTION_PHASE, executionConfiguration, EXECUTION_GOAL);
        assertPluginFromXml(PLUGIN_XML_WITH_EXECUTION, "tv.reality",
                "view-plugin", "2.5", null, NO_DEPENDENCIES,
                Arrays.asList(execution));
    }
}
