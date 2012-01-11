package org.springframework.roo.classpath.converters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.roo.project.LogicalPath.MODULE_PATH_SEPARATOR;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;

/**
 * Unit test of {@link JavaTypeConverter}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class JavaTypeConverterTest {

    // Fixture
    private JavaTypeConverter converter;
    @Mock FileManager mockFileManager;
    @Mock LastUsed mockLastUsed;
    @Mock ProjectOperations mockProjectOperations;
    @Mock TypeLocationService mockTypeLocationService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        converter = new JavaTypeConverter();
        converter.fileManager = mockFileManager;
        converter.lastUsed = mockLastUsed;
        converter.projectOperations = mockProjectOperations;
        converter.typeLocationService = mockTypeLocationService;
    }

    @Test
    public void testConvertAsteriskWhenLastUsedTypeIsKnown() {
        // Set up
        final JavaType mockLastUsedType = mock(JavaType.class);
        when(mockLastUsed.getJavaType()).thenReturn(mockLastUsedType);

        // Invoke and check
        assertEquals(mockLastUsedType, converter.convertFromText(
                JavaTypeConverter.LAST_USED_INDICATOR, null, null));
    }

    @Test(expected = IllegalStateException.class)
    public void testConvertAsteriskWhenLastUsedTypeIsUnknown() {
        converter.convertFromText(JavaTypeConverter.LAST_USED_INDICATOR, null,
                null);
    }

    @Test
    public void testConvertEmptyString() {
        assertNull(converter.convertFromText("", null, null));
    }

    @Test
    public void testConvertFullyQualifiedValueWithOneModulePrefix() {
        // Set up
        final String moduleName = "web";
        final Pom mockWebPom = mock(Pom.class);
        when(mockProjectOperations.getPomFromModuleName(moduleName))
                .thenReturn(mockWebPom);
        final String topLevelPackage = "com.example.app.mvc";
        when(mockTypeLocationService.getTopLevelPackageForModule(mockWebPom))
                .thenReturn(topLevelPackage);

        // Invoke
        final JavaType result = converter.convertFromText(moduleName
                + MODULE_PATH_SEPARATOR + topLevelPackage
                + ".pet.PetController", null, null);

        // Check
        assertEquals("com.example.app.mvc.pet.PetController",
                result.getFullyQualifiedTypeName());
    }

    @Test
    public void testConvertNullString() {
        assertNull(converter.convertFromText(null, null, null));
    }

    @Test
    public void testConvertTopLevelPackageWithOneModulePrefix() {
        // Set up
        final String moduleName = "web";
        final Pom mockWebPom = mock(Pom.class);
        when(mockProjectOperations.getPomFromModuleName(moduleName))
                .thenReturn(mockWebPom);
        final String topLevelPackage = "com.example.app.mvc";
        when(mockTypeLocationService.getTopLevelPackageForModule(mockWebPom))
                .thenReturn(topLevelPackage);

        // Invoke
        final JavaType result = converter.convertFromText(moduleName
                + MODULE_PATH_SEPARATOR + topLevelPackage, null, null);

        // Check
        assertNull(result);
    }

    @Test
    public void testConvertToPrimitiveByte() {
        assertEquals(JavaType.BYTE_PRIMITIVE,
                converter.convertFromText("byte", null, null));
    }

    @Test
    public void testConvertToPrimitiveDouble() {
        assertEquals(JavaType.DOUBLE_PRIMITIVE,
                converter.convertFromText("double", null, null));
    }

    @Test
    public void testConvertToPrimitiveFloat() {
        assertEquals(JavaType.FLOAT_PRIMITIVE,
                converter.convertFromText("float", null, null));
    }

    @Test
    public void testConvertToPrimitiveInt() {
        assertEquals(JavaType.INT_PRIMITIVE,
                converter.convertFromText("int", null, null));
    }

    @Test
    public void testConvertToPrimitiveLong() {
        assertEquals(JavaType.LONG_PRIMITIVE,
                converter.convertFromText("long", null, null));
    }

    @Test
    public void testConvertToPrimitiveShort() {
        assertEquals(JavaType.SHORT_PRIMITIVE,
                converter.convertFromText("short", null, null));
    }

    @Test
    public void testConvertWhitespace() {
        assertNull(converter.convertFromText(" \n\r\t", null, null));
    }

    @Test
    public void testSupportsJavaType() {
        assertTrue(converter.supports(JavaType.class, null));
    }
}
