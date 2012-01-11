package org.springframework.roo.addon.jpa.activerecord;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Unit test of the {@link EntityLayerMethod} enum
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class EntityLayerMethodTest {

    private static final List<JavaType> NO_TYPES = Collections
            .<JavaType> emptyList();

    private static final String PLURAL = "People";

    @Mock private JpaCrudAnnotationValues mockAnnotationValues;
    @Mock private JavaType mockIdType;
    @Mock private JavaSymbolName mockParameterName;
    // Fixture
    @Mock private JavaType mockTargetEntity;

    private void assertMethodCall(final String expectedMethodCall,
            final EntityLayerMethod method, final String... parameterNames) {
        final List<MethodParameter> parameters = new ArrayList<MethodParameter>();
        for (final String parameterName : parameterNames) {
            final JavaSymbolName mockSymbol = mock(JavaSymbolName.class);
            when(mockSymbol.getSymbolName()).thenReturn(parameterName);
            // We can use any parameter type here, as it's ignored in production
            parameters.add(new MethodParameter(JavaType.OBJECT, mockSymbol));
        }

        // Invoke and check
        assertEquals(expectedMethodCall, method.getCall(mockAnnotationValues,
                mockTargetEntity, PLURAL, parameters));
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockParameterName.getSymbolName()).thenReturn("person");
        when(mockTargetEntity.getFullyQualifiedTypeName()).thenReturn(
                "com.example.Person");
        when(mockTargetEntity.getSimpleTypeName()).thenReturn("Person");
        when(mockIdType.getFullyQualifiedTypeName()).thenReturn(
                Long.class.getName());
    }

    @Test
    public void testCallClearMethod() {
        // Set up
        when(mockAnnotationValues.getClearMethod()).thenReturn("erase");

        // Invoke and check
        assertMethodCall("Person.erase()", EntityLayerMethod.CLEAR);
    }

    @Test
    public void testCallCountAllMethod() {
        // Set up
        when(mockAnnotationValues.getCountMethod()).thenReturn("total");

        // Invoke and check
        assertMethodCall("Person.totalPeople()", EntityLayerMethod.COUNT_ALL);
    }

    @Test
    public void testCallFindAllMethod() {
        // Set up
        when(mockAnnotationValues.getFindAllMethod()).thenReturn("seekAll");

        // Invoke and check
        assertMethodCall("Person.seekAllPeople()", EntityLayerMethod.FIND_ALL);
    }

    @Test
    public void testCallFindEntriesMethod() {
        // Set up
        when(mockAnnotationValues.getFindEntriesMethod()).thenReturn("lookFor");

        // Invoke and check
        assertMethodCall("Person.lookForPersonEntries(x, y)",
                EntityLayerMethod.FIND_ENTRIES, "x", "y");
    }

    @Test
    public void testCallFlushMethod() {
        // Set up
        when(mockAnnotationValues.getFlushMethod()).thenReturn("bloosh");

        // Invoke and check
        assertMethodCall("person.bloosh()", EntityLayerMethod.FLUSH, "person");
    }

    @Test
    public void testCallMergeMethod() {
        // Set up
        when(mockAnnotationValues.getMergeMethod()).thenReturn("blend");

        // Invoke and check
        assertMethodCall("person.blend()", EntityLayerMethod.MERGE, "person");
    }

    @Test
    public void testCallPersistMethod() {
        // Set up
        when(mockAnnotationValues.getPersistMethod()).thenReturn("store");

        // Invoke and check
        assertMethodCall("person.store()", EntityLayerMethod.PERSIST, "person");
    }

    @Test
    public void testCallRemoveMethod() {
        // Set up
        when(mockAnnotationValues.getRemoveMethod()).thenReturn("trash");

        // Invoke and check
        assertMethodCall("person.trash()", EntityLayerMethod.REMOVE, "person");
    }

    @Test
    public void testParameterTypes() {
        for (final EntityLayerMethod method : EntityLayerMethod.values()) {
            final List<JavaType> parameterTypes = method.getParameterTypes(
                    mockTargetEntity, mockIdType);
            if (method.isStatic()) {
                // All we can check is that it's not null
                assertNotNull(method + " method has null parameter types",
                        parameterTypes);
            }
            else {
                assertEquals(Arrays.asList(mockTargetEntity), parameterTypes);
            }
        }
    }

    @Test
    public void testValueOfBogusMethodId() {
        assertNull(EntityLayerMethod.valueOf("foo", NO_TYPES, mockTargetEntity,
                mockIdType));
    }
}
