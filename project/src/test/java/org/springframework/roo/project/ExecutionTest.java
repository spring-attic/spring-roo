package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Unit test of the {@link Execution} class
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class ExecutionTest extends XmlTestCase {

    private static final String EXECUTION_CONFIGURATION_XML = "    <configuration>\n"
            + "        <sources>\n"
            + "            <source>src/main/groovy</source>\n"
            + "        </sources>\n" + "    </configuration>\n";
    private static final String GOAL_1 = "lock";
    private static final String GOAL_2 = "load";
    private static final String[] GOALS = { GOAL_1, GOAL_2 };
    private static final String ID = "some-id";
    private static final String PHASE = "test";
    private static final String EXECUTION_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<execution>\n"
            + "    <id>"
            + ID
            + "</id>\n"
            + "    <phase>"
            + PHASE
            + "</phase>\n"
            + "    <goals>\n"
            + "        <goal>"
            + GOAL_1
            + "</goal>\n"
            + "        <goal>"
            + GOAL_2
            + "</goal>\n"
            + "    </goals>\n" + EXECUTION_CONFIGURATION_XML + "</execution>";

    // Fixture
    @Mock private Configuration mockConfiguration;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testExecutionWithConfigurationDoesNotEqualOneWithout() {
        // Set up
        final Execution execution1 = new Execution(ID, PHASE, GOALS);
        final Execution execution2 = new Execution(ID, PHASE,
                mockConfiguration, GOALS);

        // Invoke
        assertFalse(execution1.equals(execution2));
        assertFalse(execution2.equals(execution1));
    }

    @Test
    public void testGetElementForMinimalExecution() throws Exception {
        // Set up
        final Document document = DOCUMENT_BUILDER.newDocument();
        final Configuration mockConfiguration = mock(Configuration.class);
        when(mockConfiguration.getConfiguration()).thenReturn(
                XmlUtils.stringToElement(EXECUTION_CONFIGURATION_XML));
        final Execution execution = new Execution(ID, PHASE, mockConfiguration,
                GOALS);

        // Invoke
        final Element element = execution.getElement(document);

        // Check
        assertXmlEquals(EXECUTION_XML, element);
    }

    @Test
    public void testIdenticalExecutionsAreEqual() {
        assertEquals(new Execution(ID, PHASE, mockConfiguration, GOALS),
                new Execution(ID, PHASE, mockConfiguration, GOALS));
    }

    @Test
    public void testIdenticalExecutionsWithNoConfigurationAreEqual() {
        assertEquals(new Execution(ID, PHASE, GOALS), new Execution(ID, PHASE,
                GOALS));
    }
}
