package org.springframework.roo.classpath.converters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.roo.project.LogicalPath.MODULE_PATH_SEPARATOR;
import static org.springframework.roo.support.util.AnsiEscapeCode.FG_CYAN;
import static org.springframework.roo.support.util.AnsiEscapeCode.decorate;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.OptionContexts;
import org.springframework.roo.support.util.AnsiEscapeCode;

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
    public void testGetAllPossibleValuesInProjectWhenModulePrefixIsUsed() {
        // Set up
        @SuppressWarnings("unchecked")
        final List<Completion> mockCompletions = mock(List.class);
        when(mockProjectOperations.isFocusedProjectAvailable())
                .thenReturn(true);
        final String otherModuleName = "core";
        final Pom mockOtherModule = mock(Pom.class);
        when(mockOtherModule.getModuleName()).thenReturn(otherModuleName);
        when(mockProjectOperations.getPomFromModuleName(otherModuleName))
                .thenReturn(mockOtherModule);
        final String topLevelPackage = "com.example";
        when(
                mockTypeLocationService
                        .getTopLevelPackageForModule(mockOtherModule))
                .thenReturn(topLevelPackage);
        final String focusedModuleName = "web";
        when(mockProjectOperations.getModuleNames()).thenReturn(
                Arrays.asList(focusedModuleName, otherModuleName));
        final String modulePath = "/path/to/it";
        when(mockOtherModule.getPath()).thenReturn(modulePath);
        final JavaType type1 = new JavaType("com.example.web.ShouldBeFound");
        final JavaType type2 = new JavaType("com.example.foo.ShouldNotBeFound");
        when(mockTypeLocationService.getTypesForModule(mockOtherModule))
                .thenReturn(Arrays.asList(type1, type2));

        // Invoke
        converter.getAllPossibleValues(mockCompletions, JavaType.class,
                otherModuleName + MODULE_PATH_SEPARATOR + "~.web",
                OptionContexts.PROJECT, null);

        // Check
        verify(mockCompletions).add(
                new Completion(focusedModuleName + MODULE_PATH_SEPARATOR,
                        AnsiEscapeCode
                                .decorate(focusedModuleName
                                        + MODULE_PATH_SEPARATOR,
                                        AnsiEscapeCode.FG_CYAN), "Modules", 0));
        // prefix + topLevelPackage, formattedPrefix + topLevelPackage, heading
        final String formattedPrefix = decorate(otherModuleName
                + MODULE_PATH_SEPARATOR, FG_CYAN);
        final String prefix = otherModuleName + MODULE_PATH_SEPARATOR;
        verify(mockCompletions).add(
                new Completion(prefix + topLevelPackage, formattedPrefix
                        + topLevelPackage, "", 1));
        verify(mockCompletions).add(
                new Completion(prefix + "~.web.ShouldBeFound", formattedPrefix
                        + "~.web.ShouldBeFound", "", 1));
        verifyNoMoreInteractions(mockCompletions);
    }

    @Test
    public void testGetAllPossibleValuesInProjectWhenNoModuleHasFocus() {
        // Set up
        @SuppressWarnings("unchecked")
        final List<Completion> mockCompletions = mock(List.class);

        // Invoke
        converter.getAllPossibleValues(mockCompletions, JavaType.class, "",
                OptionContexts.PROJECT, null);

        // Check
        verifyNoMoreInteractions(mockCompletions);
    }

    @Test
    public void testGetAllPossibleValuesInProjectWhenNoModulePrefixIsUsed() {
        // Set up
        @SuppressWarnings("unchecked")
        final List<Completion> mockCompletions = mock(List.class);
        when(mockProjectOperations.isFocusedProjectAvailable())
                .thenReturn(true);
        final Pom mockFocusedModule = mock(Pom.class);
        when(mockProjectOperations.getFocusedModule()).thenReturn(
                mockFocusedModule);
        final String topLevelPackage = "com.example";
        when(
                mockTypeLocationService
                        .getTopLevelPackageForModule(mockFocusedModule))
                .thenReturn(topLevelPackage);
        final String focusedModuleName = "web";
        when(mockFocusedModule.getModuleName()).thenReturn(focusedModuleName);
        final String modulePath = "/path/to/it";
        when(mockFocusedModule.getPath()).thenReturn(modulePath);
        final String otherModuleName = "core";
        when(mockProjectOperations.getModuleNames()).thenReturn(
                Arrays.asList(focusedModuleName, otherModuleName));
        final JavaType type1 = new JavaType("com.example.Foo");
        final JavaType type2 = new JavaType("com.example.sub.Bar");
        when(mockTypeLocationService.getTypesForModule(mockFocusedModule))
                .thenReturn(Arrays.asList(type1, type2));

        // Invoke
        converter.getAllPossibleValues(mockCompletions, JavaType.class, "",
                OptionContexts.PROJECT, null);

        // Check
        verify(mockCompletions).add(
                new Completion(otherModuleName + MODULE_PATH_SEPARATOR,
                        AnsiEscapeCode
                                .decorate(otherModuleName
                                        + MODULE_PATH_SEPARATOR,
                                        AnsiEscapeCode.FG_CYAN), "Modules", 0));
        verify(mockCompletions).add(
                new Completion(topLevelPackage, topLevelPackage,
                        focusedModuleName, 1));
        verify(mockCompletions).add(
                new Completion("~.Foo", "~.Foo", focusedModuleName, 1));
        verify(mockCompletions).add(
                new Completion("~.sub.Bar", "~.sub.Bar", focusedModuleName, 1));
        verifyNoMoreInteractions(mockCompletions);
    }

    @Test
    public void testSupportsJavaType() {
        assertTrue(converter.supports(JavaType.class, null));
    }
}
