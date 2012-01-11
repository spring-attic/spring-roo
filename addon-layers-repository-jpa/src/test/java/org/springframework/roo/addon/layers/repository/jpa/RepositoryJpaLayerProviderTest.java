package org.springframework.roo.addon.layers.repository.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FLUSH_METHOD;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.model.JavaType;

/**
 * Unit test of {@link RepositoryJpaLayerProvider}
 * 
 * @author Andrew Swan
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public class RepositoryJpaLayerProviderTest {

    private static final String CALLER_MID = "MID:anything#com.example.PetService";

    // Fixture
    private RepositoryJpaLayerProvider layerProvider;
    @Mock private JavaType mockIdType;
    @Mock private RepositoryJpaLocator mockRepositoryLocator;
    @Mock private JavaType mockTargetEntity;

    /**
     * Asserts that the {@link RepositoryJpaLayerProvider} generates the
     * expected call for the given method with the given parameters
     * 
     * @param expectedMethodCall
     * @param methodKey
     * @param callerParameters
     */
    private void assertMethodCall(final String expectedMethodCall,
            final MethodMetadataCustomDataKey methodKey,
            final MethodParameter... callerParameters) {
        // Set up
        setUpMockRepository();

        // Invoke
        final MemberTypeAdditions additions = layerProvider
                .getMemberTypeAdditions(CALLER_MID, methodKey.name(),
                        mockTargetEntity, mockIdType, callerParameters);

        // Check
        assertEquals(expectedMethodCall, additions.getMethodCall());
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        layerProvider = new RepositoryJpaLayerProvider();
        layerProvider.setRepositoryLocator(mockRepositoryLocator);
    }

    /**
     * Sets up the mock {@link RepositoryJpaLocator} and
     * {@link PersistenceMemberLocator} to return a mock repository for our test
     * entity.
     */
    private void setUpMockRepository() {
        final ClassOrInterfaceTypeDetails mockRepositoryDetails = mock(ClassOrInterfaceTypeDetails.class);
        final FieldMetadata mockFieldMetadata = mock(FieldMetadata.class);
        final JavaType mockRepositoryType = mock(JavaType.class);
        when(mockRepositoryType.getSimpleTypeName()).thenReturn("ClinicRepo");
        when(mockIdType.getFullyQualifiedTypeName()).thenReturn(
                Long.class.getName());
        when(mockRepositoryDetails.getName()).thenReturn(mockRepositoryType);
        when(mockFieldMetadata.getFieldType()).thenReturn(mockIdType);
        when(mockRepositoryLocator.getRepositories(mockTargetEntity))
                .thenReturn(Arrays.asList(mockRepositoryDetails));
    }

    @Test
    public void testGetAdditionsForNonRepositoryLayerMethod() {
        // Invoke
        final MemberTypeAdditions additions = layerProvider
                .getMemberTypeAdditions(CALLER_MID, "bogus", mockTargetEntity,
                        mockIdType);

        // Check
        assertNull(additions);
    }

    @Test
    public void testGetAdditionsWhenNoRepositoriesExist() {
        // Invoke
        final MemberTypeAdditions additions = layerProvider
                .getMemberTypeAdditions(CALLER_MID, FIND_ALL_METHOD.name(),
                        mockTargetEntity, mockIdType);

        // Check
        assertNull(additions);
    }

    @Test
    public void testGetFindAllAdditions() {
        assertMethodCall("clinicRepo.findAll()", FIND_ALL_METHOD);
    }

    @Test
    public void testGetFlushAdditions() {
        final MethodParameter entityParameter = new MethodParameter(
                mockTargetEntity, "anything");
        assertMethodCall("clinicRepo.flush()", FLUSH_METHOD, entityParameter);
    }
}
