package org.springframework.roo.project.layers;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.model.JavaType;

/**
 * Unit test of {@link LayerServiceImpl}
 *
 * @author Andrew Swan
 * @since 1.2
 */
public class LayerServiceImplTest {
	
	// Constants
	private static final int LAYER_POSITION = LayerType.HIGHEST.getPosition();
	private static final String METADATA_ID = "myMID";
	
	// Fixture
	@Mock private LayerProvider<Repair> mockProvider1;
	@Mock private JavaType mockEntityType; 
	private LayerServiceImpl layerService;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		when(mockEntityType.getSimpleTypeName()).thenReturn("Person");
		this.layerService = new LayerServiceImpl();
	}

	@Test
	public void testWithNoProviders() {
		// Invoke
		final MemberTypeAdditions additions = layerService.getAdditions(METADATA_ID, mockEntityType, LAYER_POSITION, Vandalise.FOLD);
		
		// Check
		assertNull(additions);
	}
	
	@Test
	public void testWithNoLowerLevelProviders() {
		// Set up a provider at the same layer (should be ignored)
		when(mockProvider1.getLayerPosition()).thenReturn(LAYER_POSITION);
		layerService.bindLayerProvider(mockProvider1);
		
		// Invoke
		final MemberTypeAdditions additions = layerService.getAdditions(METADATA_ID, mockEntityType, LAYER_POSITION, Vandalise.FOLD);
		
		// Check
		assertNull(additions);
	}
	
	@Test
	public void testWithNoProvidersOfGivenEnum() {
		// Set up the wrong type of provider at a lower layer
		when(mockProvider1.getLayerPosition()).thenReturn(LAYER_POSITION - 1);
		layerService.bindLayerProvider(mockProvider1);
		
		// Invoke
		final MemberTypeAdditions additions = layerService.getAdditions(METADATA_ID, mockEntityType, LAYER_POSITION, Vandalise.FOLD);
		
		// Check
		assertNull(additions);
	}
	
	@Test
	public void testWithLowerLevelProvidersOfGivenEnumThatDoesNotImplementMethod() {
		// Set up a provider of the right enum type
		layerService.bindLayerProvider(new StubLayerProvider(LAYER_POSITION - 1, 0, null));
		
		// Invoke
		final MemberTypeAdditions additions = layerService.getAdditions(METADATA_ID, mockEntityType, LAYER_POSITION, Vandalise.FOLD);
		
		// Check
		assertNull(additions);
	}
	
	@Test
	public void testWithLowerLevelProvidersOfGivenEnumThatDoesImplementMethod() {
		// Set up a provider of the right enum type
		final MemberTypeAdditions mockAdditions = mock(MemberTypeAdditions.class);
		layerService.bindLayerProvider(new StubLayerProvider(LAYER_POSITION - 1, 0, mockAdditions));
		
		// Invoke
		final MemberTypeAdditions additions = layerService.getAdditions(METADATA_ID, mockEntityType, LAYER_POSITION, Vandalise.FOLD);
		
		// Check
		assertSame(mockAdditions, additions);
	}
	
	@Test
	public void testWhenFirstEligibleProviderDoesNotImplementMethod() {
		// Set up a provider of the right enum type
		final MemberTypeAdditions mockAdditions = mock(MemberTypeAdditions.class);
		layerService.bindLayerProvider(new StubLayerProvider(LAYER_POSITION - 1, 0, null));
		layerService.bindLayerProvider(new StubLayerProvider(LAYER_POSITION - 2, 0, mockAdditions));
		
		// Invoke
		final MemberTypeAdditions additions = layerService.getAdditions(METADATA_ID, mockEntityType, LAYER_POSITION, Vandalise.FOLD);
		
		// Check
		assertSame(mockAdditions, additions);
	}
	
	@Test
	public void testHigherPriorityProviderWinsAtSameLevel() {
		// Set up a provider of the right enum type
		final MemberTypeAdditions mockAdditions1 = mock(MemberTypeAdditions.class);
		final MemberTypeAdditions mockAdditions2 = mock(MemberTypeAdditions.class);
		final int priority = 1;
		layerService.bindLayerProvider(new StubLayerProvider(LAYER_POSITION - 1, priority, mockAdditions1));
		layerService.bindLayerProvider(new StubLayerProvider(LAYER_POSITION - 1, priority + 1, mockAdditions2));
		
		// Invoke
		final MemberTypeAdditions additions = layerService.getAdditions(METADATA_ID, mockEntityType, LAYER_POSITION, Vandalise.FOLD);
		
		// Check
		assertSame(mockAdditions2, additions);
	}
	
	private enum Vandalise { FOLD, BEND, MUTILATE }
	
	private enum Repair { MEND, REPLACE }
	
	/**
	 * A dummy {@link LayerProvider}. Can't use Mockito because there's no way
	 * to mock the generic type. Extending {@link LayerAdapter} to test the
	 * case where the class doesn't directly implement {@link LayerProvider}.
	 *
	 * @author Andrew Swan
	 * @since 1.2
	 */
	private static class StubLayerProvider extends LayerAdapter<Vandalise> {
		
		// Fields
		private final int layerPosition;
		private final int priority;
		private final MemberTypeAdditions returnValue;
		
		/**
		 * Constructor
		 *
		 * @param layerPosition
		 * @param priority
		 * @param returnValue
		 */
		private StubLayerProvider(final int layerPosition, final int priority, final MemberTypeAdditions returnValue) {
			this.layerPosition = layerPosition;
			this.priority = priority;
			this.returnValue = returnValue;
		}

		public boolean supports(final Class<?> methodType) {
			return Vandalise.class.equals(methodType);
		}
		
		public MemberTypeAdditions getAdditions(final String metadataId, final JavaType targetEntity, final Vandalise method) {
			return returnValue;
		}

		public int getLayerPosition() {
			return layerPosition;
		}

		public int getPriority() {
			return priority;
		}
	}
}
