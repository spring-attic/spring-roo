package org.springframework.roo.project.packaging;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.springframework.roo.support.util.StringUtils;

/**
 * Convenient superclass for writing tests of concrete {@link PackagingType} implementations.
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public abstract class PackagingTypeTestCase<T extends AbstractPackagingType> {
	
	// Fixture
	private T packagingType;

	@Before
	public void setUp() throws Exception {
		this.packagingType = getPackagingType();
	}
	
	@Test
	public void testNameIsNotBlank() {
		assertTrue(StringUtils.hasText(packagingType.getName()));
	}

	protected abstract T getPackagingType();

	@Test
	public void testTemplateExists() {
		// Set up
		final String pomTemplate = packagingType.getPomTemplate();
		
		// Invoke
		final URL pomTemplateUrl = packagingType.getClass().getResource(pomTemplate);
		
		// Check
		assertNotNull("Can't find POM template '" + pomTemplate + "'", pomTemplateUrl);
	}
}
