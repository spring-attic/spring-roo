package org.springframework.roo.project.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.roo.project.converter.PomConverter.INCLUDE_CURRENT_MODULE;
import static org.springframework.roo.project.converter.PomConverter.ROOT_MODULE_SYMBOL;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Completion;

/**
 * Unit test of {@link PomConverter}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class PomConverterTest {

    private static final String CHILD_MODULE = "child";
    private static final String ROOT_MODULE = "";

    // Fixture
    private PomConverter converter;
    @Mock private ProjectOperations mockProjectOperations;

    private void assertCompletions(final String optionContext,
            final String focusedModuleName,
            final Collection<String> moduleNames,
            final String... expectedCompletions) {
        // Set up
        when(mockProjectOperations.getFocusedModuleName()).thenReturn(
                focusedModuleName);
        when(mockProjectOperations.getModuleNames()).thenReturn(moduleNames);
        final List<Completion> completions = new ArrayList<Completion>();

        // Invoke
        final boolean allValuesComplete = converter.getAllPossibleValues(
                completions, null, null, optionContext, null);

        // Check
        assertTrue(allValuesComplete);
        assertEquals("Expected " + Arrays.toString(expectedCompletions)
                + " but was " + completions, expectedCompletions.length,
                completions.size());
        for (int i = 0; i < expectedCompletions.length; i++) {
            assertEquals(expectedCompletions[i], completions.get(i).getValue());
        }
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        converter = new PomConverter();
        converter.projectOperations = mockProjectOperations;
    }

    @Test
    public void testConvertOtherModuleName() {
        final Pom mockPom = mock(Pom.class);
        final String moduleName = "foo" + File.separator + "bar";
        when(mockProjectOperations.getPomFromModuleName(moduleName))
                .thenReturn(mockPom);
        assertEquals(mockPom, converter.convertFromText(moduleName, null, null));
    }

    @Test
    public void testConvertRootModuleSymbol() {
        final Pom mockRootPom = mock(Pom.class);
        when(mockProjectOperations.getPomFromModuleName("")).thenReturn(
                mockRootPom);
        assertEquals(mockRootPom,
                converter.convertFromText(ROOT_MODULE_SYMBOL, null, null));
    }

    @Test
    public void testDoesNotSupportOtherTypes() {
        assertFalse(converter.supports(Object.class, null));
    }

    @Test
    public void testGetCompletionsExcludingCurrentWhenChildModuleExistsAndChildIsFocused() {
        assertCompletions(null, CHILD_MODULE,
                Arrays.asList(ROOT_MODULE, CHILD_MODULE), ROOT_MODULE_SYMBOL);
    }

    @Test
    public void testGetCompletionsExcludingCurrentWhenChildModuleExistsAndRootIsFocused() {
        assertCompletions(null, ROOT_MODULE,
                Arrays.asList(ROOT_MODULE, CHILD_MODULE), CHILD_MODULE);
    }

    @Test
    public void testGetCompletionsExcludingCurrentWhenNoModulesExist() {
        assertCompletions(null, ROOT_MODULE, Collections.<String> emptyList());
    }

    @Test
    public void testGetCompletionsExcludingCurrentWhenOnlyRootModuleExists() {
        assertCompletions(null, ROOT_MODULE, Arrays.asList(ROOT_MODULE));
    }

    @Test
    public void testGetCompletionsIncludingCurrentWhenChildModuleExistsAndChildIsFocused() {
        assertCompletions(INCLUDE_CURRENT_MODULE, CHILD_MODULE,
                Arrays.asList(ROOT_MODULE, CHILD_MODULE), ROOT_MODULE_SYMBOL,
                CHILD_MODULE);
    }

    @Test
    public void testGetCompletionsIncludingCurrentWhenChildModuleExistsAndRootIsFocused() {
        assertCompletions(INCLUDE_CURRENT_MODULE, ROOT_MODULE,
                Arrays.asList(ROOT_MODULE, CHILD_MODULE), ROOT_MODULE_SYMBOL,
                CHILD_MODULE);
    }

    @Test
    public void testGetCompletionsIncludingCurrentWhenNoModulesExist() {
        assertCompletions(INCLUDE_CURRENT_MODULE, ROOT_MODULE,
                Collections.<String> emptyList());
    }

    @Test
    public void testGetCompletionsIncludingCurrentWhenOnlyRootModuleExists() {
        assertCompletions(INCLUDE_CURRENT_MODULE, ROOT_MODULE,
                Arrays.asList(ROOT_MODULE), ROOT_MODULE_SYMBOL);
    }

    @Test
    public void testSupportsPoms() {
        assertTrue(converter.supports(Pom.class, null));
    }
}
