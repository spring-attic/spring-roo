package org.springframework.roo.addon.serializable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.JdkJavaType;

/**
 * Unit test of {@link SerializableMetadata}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class SerializableMetadataTest {

    private static final String METADATA_ID = "MID:org.springframework.roo.addon.serializable.SerializableMetadata#SRC_MAIN_JAVA?com.example.Person";

    @Mock private JavaType mockAspectName;
    // Fixture
    @Mock private ClassOrInterfaceTypeDetails mockClassDetails;
    @Mock private PhysicalTypeMetadata mockGovernor;
    @Mock private JavaPackage mockPackage;
    @Mock private JavaType mockTargetType;

    /**
     * Asserts that the ITD has the expected contents when the governor does or
     * does not contain the required members
     * 
     * @param alreadySerializable
     * @param alreadyHasVersionField
     */
    private void assertItdContents(final boolean alreadySerializable,
            final boolean alreadyHasVersionField) {
        // Set up
        when(mockClassDetails.implementsType(JdkJavaType.SERIALIZABLE))
                .thenReturn(alreadySerializable);
        when(
                mockClassDetails
                        .declaresField(SerializableMetadata.SERIAL_VERSION_FIELD))
                .thenReturn(alreadyHasVersionField);
        final SerializableMetadata metadata = new SerializableMetadata(
                METADATA_ID, mockAspectName, mockGovernor);

        // Invoke
        final ItdTypeDetails itd = metadata.getItdTypeDetails();

        // Check
        assertEquals(alreadySerializable ? 0 : 1, itd.getImplementsTypes()
                .size());
        assertEquals(alreadyHasVersionField ? 0 : 1, itd.getDeclaredFields()
                .size());
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockAspectName.getPackage()).thenReturn(mockPackage);
        when(mockClassDetails.getName()).thenReturn(mockTargetType);
        when(mockGovernor.getMemberHoldingTypeDetails()).thenReturn(
                mockClassDetails);
    }

    @Test
    public void testWhenGovernorAlreadyHasSerialVersionField() {
        assertItdContents(false, true);
    }

    @Test
    public void testWhenGovernorAlreadyImplementsSerializable() {
        assertItdContents(true, false);
    }

    @Test
    public void testWhenGovernorIsAlreadyFullySerializable() {
        assertItdContents(true, true);
    }

    @Test
    public void testWhenGovernorIsNotAtAllSerializable() {
        assertItdContents(false, false);
    }
}
