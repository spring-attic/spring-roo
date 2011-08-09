package org.springframework.roo.addon.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.CLEAR_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FLUSH_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.REMOVE_METHOD;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;

/**
 * Unit test of {@link EntityLayerProvider}
 *
 * @author Andrew Swan
 * @since 1.2
 */
public class EntityLayerProviderTest {
	
	// Constants
	private static final String CALLER_MID = "MID:caller#com.example.MyService";
	
	// Maps the supported entity methods to their test parameter names
	private static final Map<MethodMetadataCustomDataKey, List<String>> METHODS = new HashMap<MethodMetadataCustomDataKey, List<String>>();
	
	static {
		METHODS.put(CLEAR_METHOD, Collections.<String>emptyList());
		METHODS.put(COUNT_ALL_METHOD, Collections.<String>emptyList());
		METHODS.put(FIND_ALL_METHOD, Collections.<String>emptyList());
		METHODS.put(FIND_ENTRIES_METHOD, Arrays.asList("x", "y"));
		METHODS.put(FIND_METHOD, Arrays.asList("id"));
		METHODS.put(FLUSH_METHOD, Collections.<String>emptyList());
		METHODS.put(MERGE_METHOD, Collections.<String>emptyList());
		METHODS.put(PERSIST_METHOD, Collections.<String>emptyList());
		METHODS.put(REMOVE_METHOD, Collections.<String>emptyList());
	}

	// Fixture
	private EntityLayerProvider layerProvider;
	private String pluralId;

	@Mock private EntityMetadataProvider mockEntityMetadataProvider;
	@Mock private JavaType mockTargetEntity;
	@Mock private JavaType mockIdType;
	@Mock private JpaCrudAnnotationValues mockAnnotationValues;
	@Mock private MetadataService mockMetadataService;
	@Mock private PluralMetadata mockPluralMetadata;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		when(mockTargetEntity.getFullyQualifiedTypeName()).thenReturn("com.example.Pizza");
		when(mockIdType.getFullyQualifiedTypeName()).thenReturn(Long.class.getName());
		this.pluralId = PluralMetadata.createIdentifier(mockTargetEntity);
		
		this.layerProvider = new EntityLayerProvider();
		this.layerProvider.setEntityMetadataProvider(mockEntityMetadataProvider);
		this.layerProvider.setMetadataService(mockMetadataService);
	}

	private void setUpMockAnnotationValues() {
		when(mockEntityMetadataProvider.getAnnotationValues(mockTargetEntity)).thenReturn(mockAnnotationValues);
	}

	private void setUpPlural(final String plural) {
		when(mockMetadataService.get(pluralId)).thenReturn(mockPluralMetadata);
		when(mockPluralMetadata.getPlural()).thenReturn(plural);
	}
	
	@Test
	public void testGetAdditionsWhenEntityAnnotationValuesNotAvailable() {
		// Set up
		when(mockEntityMetadataProvider.getAnnotationValues(mockTargetEntity)).thenReturn(null);
		
		// Invoke
		final MemberTypeAdditions additions = layerProvider.getMemberTypeAdditions(CALLER_MID, FIND_ALL_METHOD.name(), mockTargetEntity, mockIdType);
		
		// Check
		assertNull(additions);
	}
	
	@Test
	public void testGetAdditionsWhenGovernorPluralMetadataIsNull() {
		setUpMockAnnotationValues();
		when(mockMetadataService.get(pluralId)).thenReturn(null);
		
		// Invoke
		final MemberTypeAdditions additions = layerProvider.getMemberTypeAdditions(CALLER_MID, FIND_ALL_METHOD.name(), mockTargetEntity, mockIdType);
		
		// Check
		assertNull(additions);
	}
	
	@Test
	public void testGetAdditionsWhenGovernorPluralIsEmpty() {
		// Set up
		setUpMockAnnotationValues();
		setUpPlural("");
		
		// Invoke
		final MemberTypeAdditions additions = layerProvider.getMemberTypeAdditions(CALLER_MID, FIND_ALL_METHOD.name(), mockTargetEntity, mockIdType);
		
		// Check
		assertNull(additions);
	}
	
	@Test
	public void testGetAdditionsForBogusMethod() {
		// Set up
		setUpMockAnnotationValues();
		setUpPlural("anything");
		
		// Invoke
		final MemberTypeAdditions additions = layerProvider.getMemberTypeAdditions(CALLER_MID, "bogus", mockTargetEntity, mockIdType);
		
		// Check
		assertNull(additions);
	}
	
	@Test
	public void testGetAdditionsForMethodAnnotatedWithEmptyName() {
		// Set up
		setUpMockAnnotationValues();
		when(mockAnnotationValues.getFindAllMethod()).thenReturn("");
		setUpPlural("anything");
		
		// Invoke
		final MemberTypeAdditions additions = layerProvider.getMemberTypeAdditions(CALLER_MID, FIND_ALL_METHOD.name(), mockTargetEntity, mockIdType);
		
		// Check
		assertNull(additions);
	}
	
	@Test
	public void testGetAdditionsForMethodAnnotatedWithNonEmptyName() {
		// Set up
		setUpMockAnnotationValues();
		when(mockAnnotationValues.getFindAllMethod()).thenReturn("getAll");
		setUpPlural("Pizzas");
		
		// Invoke
		final MemberTypeAdditions additions = layerProvider.getMemberTypeAdditions(CALLER_MID, FIND_ALL_METHOD.name(), mockTargetEntity, mockIdType);
		
		// Check
		assertEquals("getAllPizzas", additions.getMethodName());
	}
}
