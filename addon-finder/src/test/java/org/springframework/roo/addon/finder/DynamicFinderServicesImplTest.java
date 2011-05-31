package org.springframework.roo.addon.finder;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test of {@link DynamicFinderServicesImpl}
 *
 * @author Andrew Swan
 * @since 1.1.5
 */
public class DynamicFinderServicesImplTest {
	
	// Constants
	private static final String ENTITY_NAME = "Person";
	
	// Fixture
	private DynamicFinderServicesImpl services;
	
	@Before
	public void setUp() {
		this.services = new DynamicFinderServicesImpl();
	}

	@Test
	public void testGetJpaQuery() {
		// Set up
		final List<Token> tokens = Arrays.asList();
		
		// Invoke
		final String jpaQuery = this.services.getJpaQuery(tokens, null, ENTITY_NAME);
		
		// Check
		assertEquals("SELECT o FROM Person AS o WHERE", jpaQuery);
	}
}
