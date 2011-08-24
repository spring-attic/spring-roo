package org.springframework.roo.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit test of the {@link Execution} class
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class ExecutionTest {

	// Constants
	private static final String ID = "some-id";
	private static final String PHASE = "test";
	private static final String[] GOALS = {"lock", "load"};
	
	// Fixture
	@Mock private Configuration mockConfiguration;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testIdenticalExecutionsWithNoConfigurationAreEqual() {
		assertEquals(new Execution(ID, PHASE, GOALS), new Execution(ID, PHASE, GOALS));
	}

	@Test
	public void testIdenticalExecutionsAreEqual() {
		assertEquals(new Execution(ID, PHASE, mockConfiguration, GOALS), new Execution(ID, PHASE, mockConfiguration, GOALS));
	}
	
	@Test
	public void testExecutionWithConfigurationDoesNotEqualOneWithout() {
		// Set up
		final Execution execution1 = new Execution(ID, PHASE, GOALS);
		final Execution execution2 = new Execution(ID, PHASE, mockConfiguration, GOALS);
		
		// Invoke
		assertFalse(execution1.equals(execution2));
		assertFalse(execution2.equals(execution1));
	}
}
