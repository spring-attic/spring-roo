package org.springframework.roo.classpath.converters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.roo.classpath.converters.JavaPackageConverter.TOP_LEVEL_PACKAGE_SYMBOL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Completion;

/**
 * Unit test of {@link JavaPackageConverter}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class JavaPackageConverterTest {

    private static final String TOP_LEVEL_PACKAGE = "com.example";

    // Fixture
    private JavaPackageConverter converter;
    private @Mock LastUsed mockLastUsed;
    private @Mock ProjectOperations mockProjectOperations;
    private @Mock TypeLocationService mockTypeLocationService;

    /**
     * Asserts that converting the given text in the given option context
     * results in the expected package name
     * 
     * @param text
     * @param optionContext
     * @param expectedPackage
     */
    private void assertConvertFromValidText(final String text,
            final String optionContext, final String expectedPackage) {
        // Set up
        when(mockProjectOperations.isFocusedProjectAvailable())
                .thenReturn(true);
        final Pom mockPom = mock(Pom.class);
        when(mockProjectOperations.getFocusedModule()).thenReturn(mockPom);
        when(mockTypeLocationService.getTopLevelPackageForModule(mockPom))
                .thenReturn(TOP_LEVEL_PACKAGE);
        assertEquals(
                expectedPackage,
                converter.convertFromText(text, JavaPackage.class,
                        optionContext).getFullyQualifiedPackageName());
    }

    /**
     * Asserts that when the converter is asked for possible completions, the
     * expected completions are provided.
     * 
     * @param projectAvailable
     * @param expectedAllComplete
     * @param expectedCompletions
     */
    private void assertGetAllPossibleValues(final boolean projectAvailable,
            final Completion... expectedCompletions) {
        // Set up
        when(mockProjectOperations.isFocusedProjectAvailable()).thenReturn(
                projectAvailable);
        final List<Completion> completions = new ArrayList<Completion>();

        // Invoke
        final boolean allComplete = converter.getAllPossibleValues(completions,
                JavaPackage.class, null, null, null);

        // Check
        assertEquals(false, allComplete);
        assertEquals(Arrays.asList(expectedCompletions), completions);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        converter = new JavaPackageConverter();
        converter.lastUsed = mockLastUsed;
        converter.projectOperations = mockProjectOperations;
        converter.typeLocationService = mockTypeLocationService;
    }

    private Pom setUpMockPom(final String path, final JavaType... types) {
        final Pom mockPom = mock(Pom.class);
        when(mockPom.getPath()).thenReturn(path);
        when(mockTypeLocationService.getTypesForModule(mockPom)).thenReturn(
                Arrays.asList(types));
        return mockPom;
    }

    @Test
    public void testConvertFromBlankText() {
        assertNull(converter
                .convertFromText(" \n\r\t", JavaPackage.class, null));
    }

    @Test
    public void testConvertFromCompoundPackageNameInUpdateContext() {
        assertConvertFromValidText("COM.example", "update", TOP_LEVEL_PACKAGE);
        verify(mockLastUsed).setPackage(new JavaPackage(TOP_LEVEL_PACKAGE));
    }

    @Test
    public void testConvertFromEmptyText() {
        assertNull(converter.convertFromText("", JavaPackage.class, null));
    }

    @Test
    public void testConvertFromNullText() {
        assertNull(converter.convertFromText(null, JavaPackage.class, null));
    }

    @Test
    public void testConvertFromSimplePackageNameInNoContext() {
        assertEquals("foo",
                converter.convertFromText("FOO", JavaPackage.class, null)
                        .getFullyQualifiedPackageName());
        verifyNoMoreInteractions(mockLastUsed);
    }

    @Test
    public void testConvertFromSimplePackageNameWithTrailingDotInNonUpdateContext() {
        assertConvertFromValidText("FOO.", "create", "foo");
        verifyNoMoreInteractions(mockLastUsed);
    }

    @Test
    public void testConvertFromTextThatIsTopLevelPackageSymbol() {
        assertConvertFromValidText(TOP_LEVEL_PACKAGE_SYMBOL, null,
                TOP_LEVEL_PACKAGE);
        verifyNoMoreInteractions(mockLastUsed);
    }

    @Test
    public void testConvertFromTextThatStartsWithTopLevelPackageSymbolPlusDot() {
        assertConvertFromValidText("~.Domain", null, TOP_LEVEL_PACKAGE
                + ".domain");
        verifyNoMoreInteractions(mockLastUsed);
    }

    @Test
    public void testConvertFromTextThatStartsWithTopLevelPackageSymbolPlusNoDot() {
        assertConvertFromValidText(TOP_LEVEL_PACKAGE_SYMBOL + "Domain", null,
                TOP_LEVEL_PACKAGE + ".domain");
        verifyNoMoreInteractions(mockLastUsed);
    }

    @Test
    public void testConvertFromTextWithTrailingDot() {
        assertConvertFromValidText("~.Domain.", null, TOP_LEVEL_PACKAGE
                + ".domain");
        verifyNoMoreInteractions(mockLastUsed);
    }

    @Test
    public void testGetAllPossibleValuesWhenProjectIsAvailable() {
        // Set up
        final Pom mockPom1 = setUpMockPom("/path/to/pom/1", new JavaType(
                "com.example.domain.Choice"), new JavaType(
                "com.example.domain.Vote"));
        final Pom mockPom2 = setUpMockPom("/path/to/pom/2", new JavaType(
                "com.example.web.ChoiceController"), new JavaType(
                "com.example.web.VoteController"));
        when(mockProjectOperations.getPoms()).thenReturn(
                Arrays.asList(mockPom1, mockPom2));

        // Invoke and check
        assertGetAllPossibleValues(true, new Completion("com.example.domain"),
                new Completion("com.example.web"));
    }

    @Test
    public void testGetAllPossibleValuesWhenProjectNotAvailable() {
        assertGetAllPossibleValues(false);
    }

    @Test
    public void testSupportsJavaPackage() {
        assertTrue(converter.supports(JavaPackage.class, null));
    }
}
