package org.springframework.roo.project.packaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.model.JavaType;

/**
 * Unit test of {@link PackagingTypeConverter}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class PackagingTypeConverterTest {

	// Constants
	private static final String JAR = "jar";
	private static final String JOB = "job";
	private static final String WAR = "war";
	
	// Fixture
	private PackagingTypeConverter converter;
	@Mock private PackagingType mockPackagingType1;
	@Mock private PackagingType mockPackagingType2;
	@Mock private PackagingType mockPackagingType3;
	
	@Before
	public void setUp() {
		// Mocks
		MockitoAnnotations.initMocks(this);
		when(mockPackagingType1.getName()).thenReturn(JAR);
		when(mockPackagingType2.getName()).thenReturn(WAR);
		when(mockPackagingType3.getName()).thenReturn(JOB);
		
		// Object under test
		this.converter = new PackagingTypeConverter();
		this.converter.bindPackagingType(mockPackagingType1);
		this.converter.bindPackagingType(mockPackagingType2);
		this.converter.bindPackagingType(mockPackagingType3);
	}
	
	@Test
	public void testDoesNotSupportJavaType() {
		assertFalse(converter.supports(JavaType.class, null));
	}
	
	@Test
	public void testSupportsPackagingType() {
		assertTrue(converter.supports(PackagingType.class, null));
	}
	
	/**
	 * Asserts that the given string can't be converted to a {@link PackagingType}
	 * 
	 * @param string the string to convert (can be blank)
	 */
	private void assertInvalidString(final String string) {
		try {
			converter.convertFromText(string, PackagingType.class, null);
		} catch (UnsupportedOperationException expected) {
			assertEquals("Unsupported packaging type '" + string + "'", expected.getMessage());
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
		assertInvalidString(WAR.substring(0, 1));
	}
	
	@Test
	public void testConvertValidString() {
		// Invoke and check
		assertEquals(mockPackagingType2, converter.convertFromText(WAR, PackagingType.class, null));
	}
	
	@Test
	public void testGetAllPossibleValuesForInvalidExistingData() {
		assertPossibleValues("x");
	}
	
	@Test
	public void testGetAllPossibleValuesForNullExistingData() {
		assertPossibleValues(null, JAR.toUpperCase(), JOB.toUpperCase(), WAR.toUpperCase());
	}
	
	@Test
	public void testGetAllPossibleValuesForEmptyExistingData() {
		assertPossibleValues("", JAR.toUpperCase(), JOB.toUpperCase(), WAR.toUpperCase());
	}
	
	@Test
	public void testGetAllPossibleValuesForLowerCaseExistingData() {
		assertPossibleValues("j", JAR.toUpperCase(), JOB.toUpperCase());
	}
	
	@Test
	public void testGetAllPossibleValuesForUpperCaseExistingData() {
		assertPossibleValues("J", JAR.toUpperCase(), JOB.toUpperCase());
	}
	
	/**
	 * Asserts that the completions for the given existing data are as expected
	 * 
	 * @param existingData can't be <code>null</code> (not part of the contract)
	 * @param expectedCompletions
	 */
	private void assertPossibleValues(final String existingData, final String... expectedCompletions) {
		// Set up
		final List<String> completions = new ArrayList<String>();
		
		// Invoke
		final boolean addSpace = converter.getAllPossibleValues(completions, PackagingType.class, existingData, null, null);
		
		// Check
		assertTrue(addSpace);
		assertEquals(Arrays.asList(expectedCompletions), completions);
	}
}
