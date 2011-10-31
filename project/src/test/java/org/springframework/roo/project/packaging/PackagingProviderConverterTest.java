package org.springframework.roo.project.packaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.Completion;

/**
 * Unit test of {@link PackagingProviderConverter}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class PackagingProviderConverterTest {

	// Constants
	private static final String CORE_JAR_ID = "jar";
	private static final String CUSTOM_JAR_ID = "jar_custom";
	private static final String CORE_WAR_ID = "war";

	// Fixture
	private PackagingProviderConverter converter;
	@Mock private PackagingProviderRegistry mockPackagingProviderRegistry;
	@Mock private CorePackagingProvider mockCoreJarPackaging;
	@Mock private PackagingProvider mockCustomJarPackaging;
	@Mock private CorePackagingProvider mockWarPackaging;

	@Before
	public void setUp() {
		// Mocks
		MockitoAnnotations.initMocks(this);
		setUpMockPackagingProvider(mockCoreJarPackaging, CORE_JAR_ID, true);
		setUpMockPackagingProvider(mockCustomJarPackaging, CUSTOM_JAR_ID, true);
		setUpMockPackagingProvider(mockWarPackaging, CORE_WAR_ID, false);

		// Object under test
		this.converter = new PackagingProviderConverter();
		this.converter.packagingProviderRegistry = mockPackagingProviderRegistry;
	}
	
	private void setUpMockPackagingProvider(final PackagingProvider mockPackagingProvider, final String id, final boolean isDefault) {
		when(mockPackagingProvider.getId()).thenReturn(id);
		when(mockPackagingProvider.isDefault()).thenReturn(isDefault);
	}
	
	@Test
	public void testDoesNotSupportWrongType() {
		assertFalse(converter.supports(JavaType.class, null));
	}

	@Test
	public void testSupportsCorrectType() {
		assertTrue(converter.supports(PackagingProvider.class, null));
	}

	/**
	 * Asserts that the given string can't be converted to a {@link PackagingProvider}
	 *
	 * @param string the string to convert (can be blank)
	 */
	private void assertInvalidString(final String string) {
		try {
			converter.convertFromText(string, PackagingProvider.class, null);
			fail("Expected a " + IllegalArgumentException.class);
		} catch (IllegalArgumentException expected) {
			assertEquals("Unsupported packaging id '" + string + "'", expected.getMessage());
		}
	}

	@Test
	public void testConvertNullString() {
		assertInvalidString(null);
	}

	@Test
	public void testConvertEmptyString() {
		assertInvalidString("");
	}

	@Test
	public void testConvertUnknownString() {
		assertInvalidString("ear");
	}

	@Test
	public void testConvertPartialString() {
		assertInvalidString(CORE_WAR_ID.substring(0, 1));
	}

	@Test
	public void testConvertValidString() {
		// Set up
		final String id = "some-id";
		when(mockPackagingProviderRegistry.getPackagingProvider(id)).thenReturn(mockCoreJarPackaging);
		
		// Invoke
		final PackagingProvider packagingProvider = converter.convertFromText(id, PackagingProvider.class, null);
		
		// Check
		assertEquals(mockCoreJarPackaging, packagingProvider);
	}

	@Test
	public void testGetAllPossibleValues() {
		// Set up
		final PackagingProvider[] providers = {mockCoreJarPackaging, mockCustomJarPackaging, mockWarPackaging};
		when(mockPackagingProviderRegistry.getAllPackagingProviders()).thenReturn(Arrays.asList(providers));
		final List<Completion> expectedCompletions = new ArrayList<Completion>();
		for (PackagingProvider provider : providers) {
			expectedCompletions.add(new Completion(provider.getId().toUpperCase()));
		}
		final List<Completion> completions = new ArrayList<Completion>();

		// Invoke
		final boolean addSpace = converter.getAllPossibleValues(completions, PackagingProvider.class, "ignored", null, null);

		// Check
		assertTrue(addSpace);
		assertEquals(expectedCompletions.size(), completions.size());
		assertTrue("Expected " + expectedCompletions + " but was " + completions, completions.containsAll(expectedCompletions));
	}
}
