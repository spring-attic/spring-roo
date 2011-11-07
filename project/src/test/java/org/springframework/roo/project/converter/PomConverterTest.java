package org.springframework.roo.project.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.roo.project.converter.PomConverter.ROOT_MODULE_SYMBOL;

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

	private static final String ROOT_MODULE = "";
	private static final String CHILD_MODULE = "child";
	// Fixture
	private PomConverter converter;
	@Mock private ProjectOperations mockProjectOperations;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		this.converter = new PomConverter();
		this.converter.projectOperations = mockProjectOperations;
	}
	
	@Test
	public void testSupportsPoms() {
		assertTrue(this.converter.supports(Pom.class, null));
	}
	
	@Test
	public void testDoesNotSupportOtherTypes() {
		assertFalse(this.converter.supports(Object.class, null));
	}
	
	@Test
	public void testGetCompletionsWhenNoModulesExist() {
		assertCompletions(ROOT_MODULE, Collections.<String>emptyList());
	}

	@Test
	public void testGetCompletionsWhenOnlyRootModuleExists() {
		assertCompletions(ROOT_MODULE, Arrays.asList(ROOT_MODULE));
	}

	@Test
	public void testGetCompletionsWhenChildModuleExistsAndRootIsFocused() {
		assertCompletions(ROOT_MODULE, Arrays.asList(ROOT_MODULE, CHILD_MODULE), CHILD_MODULE);
	}
	
	@Test
	public void testGetCompletionsWhenChildModuleExistsAndChildIsFocused() {
		assertCompletions(CHILD_MODULE, Arrays.asList(ROOT_MODULE, CHILD_MODULE), ROOT_MODULE_SYMBOL);
	}
	
	private void assertCompletions(final String focusedModuleName, final Collection<String> moduleNames, final String... expectedCompletions) {
		// Set up
		when(mockProjectOperations.getFocusedModuleName()).thenReturn(focusedModuleName);
		when(mockProjectOperations.getModuleNames()).thenReturn(moduleNames);
		final List<Completion> completions = new ArrayList<Completion>();
		
		// Invoke
		final boolean allValuesComplete = this.converter.getAllPossibleValues(completions, null, null, null, null);
		
		// Check
		assertTrue(allValuesComplete);
		assertEquals(expectedCompletions.length, completions.size());
		for (int i = 0; i < expectedCompletions.length; i++) {
			assertEquals(expectedCompletions[i], completions.get(i).getValue());
		}
	}
	
	@Test
	public void testConvertRootModuleSymbol() {
		final Pom mockRootPom = mock(Pom.class);
		when(mockProjectOperations.getPomFromModuleName("")).thenReturn(mockRootPom);
		assertEquals(mockRootPom, this.converter.convertFromText(ROOT_MODULE_SYMBOL, null, null));
	}
	
	@Test
	public void testConvertOtherModuleName() {
		final Pom mockPom = mock(Pom.class);
		final String moduleName = "foo";
		when(mockProjectOperations.getPomFromModuleName(moduleName)).thenReturn(mockPom);
		assertEquals(mockPom, this.converter.convertFromText(moduleName, null, null));
	}
}
