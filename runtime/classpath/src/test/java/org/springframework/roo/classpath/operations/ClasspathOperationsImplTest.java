package org.springframework.roo.classpath.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.model.JavaType;

/**
 * Unit test of {@link ClasspathOperationsImpl}
 * 
 * @author Andrew Swan
 * @since 1.2.1
 */
public class ClasspathOperationsImplTest {

    // Fixture
    private ClasspathOperationsImpl classpathOperations;
    @Mock private TypeLocationService mockTypeLocationService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        classpathOperations = new ClasspathOperationsImpl();
        classpathOperations.typeLocationService = mockTypeLocationService;
    }

    @Test
    public void testFocusOnTypeThatCannotBeLocated() {
        // Set up
        final JavaType mockType = mock(JavaType.class);
        final String typeName = "com.example.domain.Lost";
        when(mockType.getFullyQualifiedTypeName()).thenReturn(typeName);
        when(mockTypeLocationService.getPhysicalTypeIdentifier(mockType))
                .thenReturn(null);

        // Invoke and check
        try {
            classpathOperations.focus(mockType);
            fail("Expected a " + NullPointerException.class);
        }
        catch (final NullPointerException expected) {
            assertEquals("Cannot locate the type " + typeName,
                    expected.getMessage());
        }
    }
}
