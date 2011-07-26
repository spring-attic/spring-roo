package org.springframework.roo.addon.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.CLEAR_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FLUSH_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.REMOVE_METHOD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Unit test of the {@link EntityLayerMethod} enum
 *
 * @author Andrew Swan
 * @since 1.2
 */
public class EntityLayerMethodTest {

	// Constants
	private static final String PLURAL = "People";

	private static final List<JavaType> NO_TYPES = Collections.<JavaType>emptyList();
	
	// Maps the entity methods supported by @RooEntity to their parameter types
	private static final Map<MethodMetadataCustomDataKey, List<JavaType>> METHODS = new HashMap<MethodMetadataCustomDataKey, List<JavaType>>();
	
	static {
		METHODS.put(CLEAR_METHOD, NO_TYPES);
		METHODS.put(COUNT_ALL_METHOD, NO_TYPES);
		METHODS.put(FIND_ALL_METHOD, NO_TYPES);
		METHODS.put(FIND_ENTRIES_METHOD, Arrays.asList(JavaType.INT_PRIMITIVE, JavaType.INT_PRIMITIVE));
		// TODO re-enable once we work out how to get the entity's ID type in the enum
		// METHODS.put(FIND_METHOD, Arrays.asList(JavaType.LONG_PRIMITIVE));
		METHODS.put(FLUSH_METHOD, NO_TYPES);
		METHODS.put(MERGE_METHOD, NO_TYPES);
		METHODS.put(PERSIST_METHOD, NO_TYPES);
		METHODS.put(REMOVE_METHOD, NO_TYPES);
	}
	
	// Fixture
	@Mock private JavaType mockTargetEntity;
	@Mock private EntityAnnotationValues mockAnnotationValues;
	@Mock private JavaSymbolName mockParameterName;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		when(mockParameterName.getSymbolName()).thenReturn("person");
		when(mockTargetEntity.getFullyQualifiedTypeName()).thenReturn("com.example.Person");
		when(mockTargetEntity.getSimpleTypeName()).thenReturn("Person");
	}
	
	/**
	 * Returns a list of mock {@link JavaSymbolName}s with the given names
	 * 
	 * @param parameters
	 * @return a non-<code>null</code> list
	 */
	private List<JavaSymbolName> getMockParameterNames(final String... parameters) {
		final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
		for (final String parameterName : parameters) {
			final JavaSymbolName mockSymbol = mock(JavaSymbolName.class);
			when(mockSymbol.getSymbolName()).thenReturn(parameterName);
			parameterNames.add(mockSymbol);
		}
		return parameterNames;
	}
	
	@Test
	public void testCallCountAllMethod() {
		// Set up
		when(mockAnnotationValues.getCountMethod()).thenReturn("total");
		final List<JavaSymbolName> parameterNames = getMockParameterNames();
		
		// Invoke and check
		assertEquals("com.example.Person.totalPeople()", EntityLayerMethod.COUNT_ALL.getCall(mockAnnotationValues, mockTargetEntity, PLURAL, parameterNames));
	}
	
	@Test
	public void testCallClearMethod() {
		// Set up
		when(mockAnnotationValues.getClearMethod()).thenReturn("erase");
		final List<JavaSymbolName> parameterNames = getMockParameterNames();
		
		// Invoke and check
		assertEquals("com.example.Person.erase()", EntityLayerMethod.CLEAR.getCall(mockAnnotationValues, mockTargetEntity, PLURAL, parameterNames));
	}
	
	@Test
	public void testCallFindAllMethod() {
		// Set up
		when(mockAnnotationValues.getFindAllMethod()).thenReturn("seekAll");
		final List<JavaSymbolName> parameterNames = getMockParameterNames();
		
		// Invoke and check
		assertEquals("com.example.Person.seekAllPeople()", EntityLayerMethod.FIND_ALL.getCall(mockAnnotationValues, mockTargetEntity, PLURAL, parameterNames));
	}
	
	@Test
	public void testCallFindEntriesMethod() {
		// Set up
		when(mockAnnotationValues.getFindEntriesMethod()).thenReturn("lookFor");
		final List<JavaSymbolName> parameterNames = getMockParameterNames("x", "y");
		
		// Invoke and check
		assertEquals("com.example.Person.lookForPersonEntries(x, y)", EntityLayerMethod.FIND_ENTRIES.getCall(mockAnnotationValues, mockTargetEntity, PLURAL, parameterNames));
	}
	
	@Test
	public void testCallFlushMethod() {
		// Set up
		when(mockAnnotationValues.getFlushMethod()).thenReturn("bloosh");
		final List<JavaSymbolName> parameterNames = getMockParameterNames();
		
		// Invoke and check
		assertEquals("com.example.Person.bloosh()", EntityLayerMethod.FLUSH.getCall(mockAnnotationValues, mockTargetEntity, PLURAL, parameterNames));
	}
	
	@Test
	public void testCallMergeMethod() {
		// Set up
		when(mockAnnotationValues.getMergeMethod()).thenReturn("blend");
		final List<JavaSymbolName> parameterNames = getMockParameterNames("person");
		
		// Invoke and check
		assertEquals("person.blend()", EntityLayerMethod.MERGE.getCall(mockAnnotationValues, mockTargetEntity, PLURAL, parameterNames));
	}
	
	@Test
	public void testCallPersistMethod() {
		// Set up
		when(mockAnnotationValues.getPersistMethod()).thenReturn("store");
		final List<JavaSymbolName> parameterNames = getMockParameterNames("person");
		
		// Invoke and check
		assertEquals("person.store()", EntityLayerMethod.PERSIST.getCall(mockAnnotationValues, mockTargetEntity, PLURAL, parameterNames));
	}
	
	@Test
	public void testCallRemoveMethod() {
		// Set up
		when(mockAnnotationValues.getRemoveMethod()).thenReturn("trash");
		final List<JavaSymbolName> parameterNames = getMockParameterNames("person");
		
		// Invoke and check
		assertEquals("person.trash()", EntityLayerMethod.REMOVE.getCall(mockAnnotationValues, mockTargetEntity, PLURAL, parameterNames));
	}
	
	@Test
	public void testValueOfBogusMethodId() {
		assertNull(EntityLayerMethod.valueOf("foo", NO_TYPES, mockTargetEntity));
	}
	
	@Test
	public void testParameterTypesAreNotNull() {
		for (final EntityLayerMethod method : EntityLayerMethod.values()) {
			assertNotNull(method + " method has null parameter types", method.getParameterTypes(mockTargetEntity));
		}
	}
	
	@Test
	public void testAllEntityMethodsHaveAnEnumValue() {
		for (final Entry<MethodMetadataCustomDataKey, List<JavaType>> entry : METHODS.entrySet()) {
			final String methodId = entry.getKey().name();
			assertNotNull("No enum constant for method " + methodId, EntityLayerMethod.valueOf(methodId, entry.getValue(), mockTargetEntity));
		}
	}
}
