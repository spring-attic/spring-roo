package org.springframework.roo.project.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.roo.project.Path.SRC_MAIN_JAVA;
import static org.springframework.roo.project.Path.SRC_TEST_JAVA;
import static org.springframework.roo.project.maven.Pom.DEFAULT_PACKAGING;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.packaging.PackagingProvider;
import org.springframework.roo.project.packaging.PackagingProviderRegistry;
import org.springframework.uaa.client.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Unit test of {@link PomFactoryImpl}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class PomFactoryImplTest {

    private static final String MODULE_NAME = "my-module";

    // Fixture
    private PomFactoryImpl factory;
    @Mock private PackagingProviderRegistry mockPackagingProviderRegistry;

    private void assertGav(final Pom pom, final String expectedGroupId,
            final String expectedArtifactId, final String expectedVersion) {
        assertEquals(expectedGroupId, pom.getGroupId());
        assertEquals(expectedArtifactId, pom.getArtifactId());
        assertEquals(expectedVersion, pom.getVersion());
    }

    private void assertModule(final Module module, final String expectedName,
            final String pomFileName) throws Exception {
        assertEquals(expectedName, module.getName());
        final File parentPomDirectory = getPomFile(pomFileName).getParentFile();
        final File moduleDirectory = new File(parentPomDirectory, expectedName);
        final File modulePom = new File(moduleDirectory, "pom.xml");
        assertEquals(modulePom.getCanonicalPath(), module.getPomPath());
    }

    /**
     * Returns the root element and canonical path of the given POM file
     * 
     * @param pomFileName the name of a POM in this test's package
     * @return a non-<code>null</code> pair
     * @throws Exception
     */
    private ImmutablePair<Element, String> getPom(final String pomFileName)
            throws Exception {
        final URL pomUrl = getPomUrl(pomFileName);
        final File pomFile = new File(pomUrl.toURI());
        final Document pomDocument = XmlUtils.parse(pomUrl.openStream());
        return new ImmutablePair<Element, String>(
                pomDocument.getDocumentElement(), pomFile.getCanonicalPath());
    }

    private File getPomFile(final String pomFileName) throws Exception {
        final URL pomUrl = getPomUrl(pomFileName);
        return new File(pomUrl.toURI());
    }

    /**
     * Returns the URL of the given POM file
     * 
     * @param pomFileName the name of a POM in this test's package
     * @return a non-<code>null</code> URL
     * @throws Exception
     */
    private URL getPomUrl(final String pomFileName) throws Exception {
        final URL pomUrl = getClass().getResource(pomFileName);
        assertNotNull("Can't find test POM '" + pomFileName
                + "' on classpath of " + getClass().getName(), pomUrl);
        return pomUrl;
    }

    private Pom invokeFactory(final String pomFile) throws Exception {
        final ImmutablePair<Element, String> pomDetails = getPom(pomFile);
        return factory.getInstance(pomDetails.getKey(), pomDetails.getValue(),
                MODULE_NAME);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        factory = new PomFactoryImpl();
        factory.packagingProviderRegistry = mockPackagingProviderRegistry;
    }

    private void setUpMockPackagingProvider(final String providerId) {
        final PackagingProvider mockPackagingProvider = mock(PackagingProvider.class);
        when(mockPackagingProviderRegistry.getPackagingProvider(providerId))
                .thenReturn(mockPackagingProvider);
    }

    @Test
    public void testGetInstanceWithDependency() throws Exception {
        // Set up
        setUpMockPackagingProvider(DEFAULT_PACKAGING);

        // Invoke
        final Pom pom = invokeFactory("pom-with-dependencies.xml");

        // Check
        assertGav(pom, "com.example", "dependent-app", "2.1");
        final Collection<Dependency> dependencies = pom.getDependencies();
        assertEquals(1, dependencies.size());
        final Dependency dependency = dependencies.iterator().next();
        assertEquals("org.apache", dependency.getGroupId());
        assertEquals("commons-lang", dependency.getArtifactId());
        assertEquals("2.5", dependency.getVersion());
    }

    @Test
    public void testGetInstanceWithInheritedGroupId() throws Exception {
        // Set up
        setUpMockPackagingProvider(DEFAULT_PACKAGING);

        // Invoke
        final Pom pom = invokeFactory("inherited-groupId-pom.xml");

        // Check
        assertGav(pom, "com.example", "child-app", "2.0");
        assertEquals("prod-sources", pom.getSourceDirectory());
        assertEquals("test-sources", pom.getTestSourceDirectory());
    }

    @Test
    public void testGetInstanceWithPomPackaging() throws Exception {
        // Set up
        setUpMockPackagingProvider("pom");
        final String pomFileName = "parent-pom.xml";

        // Invoke
        final Pom pom = invokeFactory(pomFileName);

        // Check
        assertGav(pom, "com.example", "parent-app", "3.0");
        assertEquals("pom", pom.getPackaging());
        assertEquals(SRC_MAIN_JAVA.getDefaultLocation(),
                pom.getSourceDirectory());
        assertEquals(SRC_TEST_JAVA.getDefaultLocation(),
                pom.getTestSourceDirectory());
        final Collection<Module> modules = pom.getModules();
        assertEquals(2, modules.size());
        final Iterator<Module> moduleIterator = modules.iterator();
        assertModule(moduleIterator.next(), "module-one", pomFileName);
        assertModule(moduleIterator.next(), "module-two", pomFileName);
    }

    @Test
    public void testGetMinimalInstance() throws Exception {
        // Set up
        setUpMockPackagingProvider(DEFAULT_PACKAGING);

        // Invoke
        final Pom pom = invokeFactory("minimal-pom.xml");

        // Check
        assertGav(pom, "com.example", "minimal-app", "2.0");
        assertEquals(SRC_MAIN_JAVA.getDefaultLocation(),
                pom.getSourceDirectory());
        assertEquals(SRC_TEST_JAVA.getDefaultLocation(),
                pom.getTestSourceDirectory());
    }
}
