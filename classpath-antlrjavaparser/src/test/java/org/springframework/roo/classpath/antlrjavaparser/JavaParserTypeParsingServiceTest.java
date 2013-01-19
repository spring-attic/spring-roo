package org.springframework.roo.classpath.antlrjavaparser;

import com.github.antlrjavaparser.JavaParser;
import com.github.antlrjavaparser.api.CompilationUnit;
import com.github.antlrjavaparser.api.body.TypeDeclaration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.antlrjavaparser.details.JavaParserClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Unit test of {@link JavaParserTypeParsingService}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ JavaParserClassOrInterfaceTypeDetailsBuilder.class,
        JavaParser.class, JavaParserUtils.class })
public class JavaParserTypeParsingServiceTest {

    private static final String DECLARED_BY_MID = "MID:foo#bar";
    private static final String EMPTY_FILE = "package com.example;";

    private static final String SOURCE_FILE = "package com.example;" + ""
            + "public class MyClass {}" + "" + "class TargetClass {}" + ""
            + "class OtherClass {}";
    @Mock private MetadataService mockMetadataService;
    @Mock private TypeLocationService mockTypeLocationService;

    // Fixture
    private JavaParserTypeParsingService typeParsingService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        typeParsingService = new JavaParserTypeParsingService();
        typeParsingService.metadataService = mockMetadataService;
        typeParsingService.typeLocationService = mockTypeLocationService;
    }

    @Test
    public void testGetTypeFromStringWhenFileContainsNoSuchType() {
        // Set up
        final JavaType mockTargetType = mock(JavaType.class);
        when(mockTargetType.getSimpleTypeName()).thenReturn("NoSuchType");

        // Invoke
        final ClassOrInterfaceTypeDetails locatedType = typeParsingService
                .getTypeFromString(SOURCE_FILE, DECLARED_BY_MID, mockTargetType);

        // Check
        assertNull(locatedType);
    }

    @Test
    public void testGetTypeFromStringWhenFileContainsNoTypes() {
        // Set up
        final JavaType mockTargetType = mock(JavaType.class);

        // Invoke
        final ClassOrInterfaceTypeDetails locatedType = typeParsingService
                .getTypeFromString(EMPTY_FILE, DECLARED_BY_MID, mockTargetType);

        // Check
        assertNull(locatedType);
    }

    @Test
    public void testGetTypeFromStringWhenFileContainsThatType()
            throws Exception {
        // Set up
        final JavaType mockTargetType = mock(JavaType.class);
        final TypeDeclaration mockTypeDeclaration = mock(TypeDeclaration.class);
        final ClassOrInterfaceTypeDetails mockClassOrInterfaceTypeDetails = mock(ClassOrInterfaceTypeDetails.class);
        final JavaParserClassOrInterfaceTypeDetailsBuilder mockBuilder = mock(JavaParserClassOrInterfaceTypeDetailsBuilder.class);
        when(mockBuilder.build()).thenReturn(mockClassOrInterfaceTypeDetails);

        mockStatic(JavaParserUtils.class);
        when(
                JavaParserUtils.locateTypeDeclaration(
                        any(CompilationUnit.class), eq(mockTargetType)))
                .thenReturn(mockTypeDeclaration);

        mockStatic(JavaParserClassOrInterfaceTypeDetailsBuilder.class);
        when(
                JavaParserClassOrInterfaceTypeDetailsBuilder.getInstance(
                        any(CompilationUnit.class),
                        (CompilationUnitServices) eq(null),
                        eq(mockTypeDeclaration), eq(DECLARED_BY_MID),
                        eq(mockTargetType), eq(mockMetadataService),
                        eq(mockTypeLocationService))).thenReturn(mockBuilder);

        // Invoke
        final ClassOrInterfaceTypeDetails locatedType = typeParsingService
                .getTypeFromString(SOURCE_FILE, DECLARED_BY_MID, mockTargetType);

        // Check
        assertSame(mockClassOrInterfaceTypeDetails, locatedType);
    }
}
