package org.springframework.roo.project;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.w3c.dom.Element;
import static org.junit.Assume.assumeTrue;

/**
 * Unit test of the {@link Repository} class
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class RepositoryTest extends XmlTestCase {

    private static final boolean ENABLE_SNAPSHOTS = true;
    private static final String EXPECTED_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<pluginRepo>\n"
            + "    <id>the-id</id>\n"
            + "    <url>the-url</url>\n"
            + "    <name>the_name</name>\n"
            + "    <snapshots>\n"
            + "        <enabled>true</enabled>\n"
            + "    </snapshots>\n" + "</pluginRepo>";
    private static final String ID = "the-id";
    private static final String NAME = "the_name";
    private static final String PATH = "pluginRepo";

    private static final String URL = "the-url";

    public void setUp() {
        assumeTrue(SystemUtils.IS_JAVA_1_6);
    }

    @Test
    public void testGetElement() {
        // Set up
        final Repository repository = new Repository(ID, NAME, URL,
                ENABLE_SNAPSHOTS);

        // Invoke
        final Element element = repository.getElement(
                DOCUMENT_BUILDER.newDocument(), PATH);

        // Check
        assertXmlEquals(EXPECTED_XML, element);
    }
}
