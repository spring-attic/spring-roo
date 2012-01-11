package org.springframework.roo.addon.layers.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.roo.addon.layers.service.ServiceLayerMethod.FIND_ALL;
import static org.springframework.roo.addon.layers.service.ServiceLayerMethod.FIND_ENTRIES;
import static org.springframework.roo.addon.layers.service.ServiceLayerMethod.SAVE;
import static org.springframework.roo.addon.layers.service.ServiceLayerMethod.UPDATE;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Unit test of {@link ServiceLayerProvider}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class ServiceLayerProviderTest {

    private static final String BOGUS_METHOD = "bogus";
    private static final String CALLER_MID = "MID:anything#com.example.web.PersonController";
    private static final String SERVICE_MID = "MID:anything#com.example.serv.PersonService";
    private static final MethodParameter SIZE_PARAMETER = new MethodParameter(
            JavaType.INT_PRIMITIVE, "count");
    private static final MethodParameter START_PARAMETER = new MethodParameter(
            JavaType.INT_PRIMITIVE, "start");

    // Fixture

    @Mock private JavaType mockIdType;
    @Mock private MetadataService mockMetadataService;
    @Mock private ServiceAnnotationValuesFactory mockServiceAnnotationValuesFactory;
    @Mock private ServiceInterfaceLocator mockServiceInterfaceLocator;
    // -- Mocks
    @Mock private JavaType mockTargetType;
    @Mock private TypeLocationService mockTypeLocationService;

    private String pluralId;
    // -- Others
    private ServiceLayerProvider provider;

    /**
     * Asserts that asking the {@link ServiceLayerProvider} for a method with
     * the given name and parameters results in the given method signature
     * 
     * @param plural
     * @param mockServiceInterfaces can be empty
     * @param methodId
     * @param expectedMethodSignature <code>null</code> means no additions are
     *            expected
     * @param methodParameters
     */
    private void assertAdditions(final String plural,
            final List<ClassOrInterfaceTypeDetails> mockServiceInterfaces,
            final String methodId, final String expectedMethodSignature,
            final MethodParameter... methodParameters) {
        // Set up
        setUpPluralMetadata(plural);
        when(mockServiceInterfaceLocator.getServiceInterfaces(mockTargetType))
                .thenReturn(mockServiceInterfaces);

        // Invoke
        final MemberTypeAdditions additions = provider.getMemberTypeAdditions(
                CALLER_MID, methodId, mockTargetType, mockIdType,
                methodParameters);

        // Check
        if (expectedMethodSignature == null) {
            assertNull("Expected no additions but found: " + additions,
                    additions);
        }
        else {
            assertNotNull("Expected some additions but was null", additions);
            assertEquals(expectedMethodSignature, additions.getMethodCall());
        }
    }

    /**
     * Sets up a mock {@link ClassOrInterfaceTypeDetails} for a service
     * interface whose {@link RooService} annotation specifies the following
     * method names
     * 
     * @param findAllMethod can be blank
     * @param saveMethod can be blank
     * @param updateMethod can be blank
     * @param findEntriesMethod can be blank
     * @return a non-<code>null</code> mock
     */
    private ClassOrInterfaceTypeDetails getMockService(
            final String findAllMethod, final String saveMethod,
            final String updateMethod, final String findEntriesMethod) {
        final ClassOrInterfaceTypeDetails mockServiceInterface = mock(ClassOrInterfaceTypeDetails.class);
        final JavaType mockServiceType = mock(JavaType.class);
        final ServiceAnnotationValues mockServiceAnnotationValues = mock(ServiceAnnotationValues.class);

        when(mockServiceType.getSimpleTypeName()).thenReturn("PersonService");
        when(mockServiceInterface.getName()).thenReturn(mockServiceType);
        when(mockServiceInterface.getDeclaredByMetadataId()).thenReturn(
                SERVICE_MID);
        when(mockServiceAnnotationValues.getFindAllMethod()).thenReturn(
                findAllMethod);
        when(mockServiceAnnotationValues.getFindEntriesMethod()).thenReturn(
                findEntriesMethod);
        when(mockServiceAnnotationValues.getSaveMethod())
                .thenReturn(saveMethod);
        when(mockServiceAnnotationValues.getUpdateMethod()).thenReturn(
                updateMethod);
        when(
                mockServiceAnnotationValuesFactory
                        .getInstance(mockServiceInterface)).thenReturn(
                mockServiceAnnotationValues);

        return mockServiceInterface;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        provider = new ServiceLayerProvider();
        provider.setMetadataService(mockMetadataService);
        provider.setServiceAnnotationValuesFactory(mockServiceAnnotationValuesFactory);
        provider.setServiceInterfaceLocator(mockServiceInterfaceLocator);
        provider.typeLocationService = mockTypeLocationService;

        when(mockTargetType.getFullyQualifiedTypeName()).thenReturn(
                "com.example.domain.Person");
        when(mockIdType.getFullyQualifiedTypeName()).thenReturn(
                Long.class.getName());
        when(mockTargetType.getSimpleTypeName()).thenReturn("Person");
        when(mockTypeLocationService.getTypePath(mockTargetType)).thenReturn(
                Path.SRC_MAIN_JAVA.getModulePathId(""));
        pluralId = PluralMetadata.createIdentifier(mockTargetType,
                Path.SRC_MAIN_JAVA.getModulePathId(""));
    }

    /**
     * Sets up the mock {@link MetadataService} to return the given plural text
     * for our test entity type.
     * 
     * @param plural can be <code>null</code>
     */
    private void setUpPluralMetadata(final String plural) {
        final PluralMetadata mockPluralMetadata = mock(PluralMetadata.class);
        when(mockPluralMetadata.getPlural()).thenReturn(plural);
        when(mockMetadataService.get(pluralId)).thenReturn(mockPluralMetadata);
    }

    @Test
    public void testGetAdditionsForBogusMethod() {
        final ClassOrInterfaceTypeDetails mockServiceInterface = mock(ClassOrInterfaceTypeDetails.class);
        assertAdditions("x", Arrays.asList(mockServiceInterface), BOGUS_METHOD,
                null);
    }

    @Test
    public void testGetAdditionsForEntityWithNoServices() {
        assertAdditions("x", Arrays.<ClassOrInterfaceTypeDetails> asList(),
                BOGUS_METHOD, null);
    }

    @Test
    public void testGetAdditionsForEntityWithNullPluralMetadata() {
        // Set up
        when(mockMetadataService.get(pluralId)).thenReturn(null);

        // Invoke
        final MemberTypeAdditions additions = provider.getMemberTypeAdditions(
                CALLER_MID, BOGUS_METHOD, mockTargetType, mockIdType);

        // Check
        assertNull(additions);
    }

    @Test
    public void testGetAdditionsForEntityWithNullPluralText() {
        assertAdditions(null, Arrays.<ClassOrInterfaceTypeDetails> asList(),
                FIND_ALL.getKey(), null);
    }

    @Test
    public void testGetAdditionsForFindAllMethodWhenServiceDoesNotProvideIt() {
        final ClassOrInterfaceTypeDetails mockServiceInterface = getMockService(
                "", "x", "x", "x");
        assertAdditions("x", Arrays.asList(mockServiceInterface),
                FIND_ALL.getKey(), null);
    }

    @Test
    public void testGetAdditionsForFindAllMethodWhenServiceProvidesIt() {
        final ClassOrInterfaceTypeDetails mockServiceInterface = getMockService(
                "findPerson", "", "x", "x");
        assertAdditions("s", Arrays.asList(mockServiceInterface),
                FIND_ALL.getKey(), "personService.findPersons()");
    }

    @Test
    public void testGetAdditionsForFindEntriesMethodWhenServiceDoesNotProvideIt() {
        final ClassOrInterfaceTypeDetails mockServiceInterface = getMockService(
                "x", "x", "x", "");
        assertAdditions("x", Arrays.asList(mockServiceInterface),
                FIND_ENTRIES.getKey(), null, START_PARAMETER, SIZE_PARAMETER);
    }

    @Test
    public void testGetAdditionsForFindEntriesMethodWhenServiceProvidesIt() {
        final ClassOrInterfaceTypeDetails mockServiceInterface = getMockService(
                "x", "x", "x", "locate");
        assertAdditions("z", Arrays.asList(mockServiceInterface),
                FIND_ENTRIES.getKey(),
                "personService.locatePersonEntries(start, count)",
                START_PARAMETER, SIZE_PARAMETER);
    }

    @Test
    public void testGetAdditionsForSaveMethodWhenServiceDoesNotProvideIt() {
        final ClassOrInterfaceTypeDetails mockServiceInterface = getMockService(
                "x", null, "x", "x");
        final MethodParameter methodParameter = new MethodParameter(
                mockTargetType, "anything");
        assertAdditions("x", Arrays.asList(mockServiceInterface),
                SAVE.getKey(), null, methodParameter);
    }

    @Test
    public void testGetAdditionsForSaveMethodWhenServiceProvidesIt() {
        final ClassOrInterfaceTypeDetails mockServiceInterface = getMockService(
                "x", "save", "x", "x");
        final MethodParameter methodParameter = new MethodParameter(
                mockTargetType, "user");
        assertAdditions("x", Arrays.asList(mockServiceInterface),
                SAVE.getKey(), "personService.savePerson(user)",
                methodParameter);
    }

    @Test
    public void testGetAdditionsForUpdateMethodWhenServiceDoesNotProvideIt() {
        final ClassOrInterfaceTypeDetails mockServiceInterface = getMockService(
                "x", "x", "", "x");
        final MethodParameter methodParameter = new MethodParameter(
                mockTargetType, "employee");
        assertAdditions("x", Arrays.asList(mockServiceInterface),
                UPDATE.getKey(), null, methodParameter);
    }

    @Test
    public void testGetAdditionsForUpdateMethodWhenServiceProvidesIt() {
        final ClassOrInterfaceTypeDetails mockServiceInterface = getMockService(
                "x", "x", "change", "x");
        final MethodParameter methodParameter = new MethodParameter(
                mockTargetType, "bob");
        assertAdditions("x", Arrays.asList(mockServiceInterface),
                UPDATE.getKey(), "personService.changePerson(bob)",
                methodParameter);
    }

    @Test
    public void testGetAdditionsWhenServiceAnnotationValuesUnavailable() {
        final ClassOrInterfaceTypeDetails mockServiceInterface = mock(ClassOrInterfaceTypeDetails.class);
        assertAdditions("anything", Arrays.asList(mockServiceInterface),
                BOGUS_METHOD, null);
    }
}
