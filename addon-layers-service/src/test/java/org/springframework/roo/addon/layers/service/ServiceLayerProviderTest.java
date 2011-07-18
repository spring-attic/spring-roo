package org.springframework.roo.addon.layers.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.roo.addon.layers.service.ServiceLayerProvider.FIND_ALL_METHOD;
import static org.springframework.roo.addon.layers.service.ServiceLayerProvider.SAVE_METHOD;
import static org.springframework.roo.addon.layers.service.ServiceLayerProvider.UPDATE_METHOD;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.layers.MemberTypeAdditions;
import org.springframework.roo.support.util.Pair;

/**
 * Unit test of {@link ServiceLayerProvider}
 *
 * @author Andrew Swan
 * @since 1.2
 */
public class ServiceLayerProviderTest {

	// Constants
	private static final String BOGUS_METHOD = "bogus";
	private static final String CALLER_MID = "MID:anything#com.example.web.PersonController";
	private static final String SERVICE_MID = "MID:anything#com.example.serv.PersonService";
	
	// Fixture
	
	// -- Mocks
	@Mock private JavaType mockTargetType;
	@Mock private MetadataDependencyRegistry mockMetadataDependencyRegistry;
	@Mock private MetadataService mockMetadataService;
	@Mock private ServiceAnnotationValuesFactory mockServiceAnnotationValuesFactory;
	@Mock private ServiceInterfaceLocator mockServiceInterfaceLocator;
	
	// -- Others
	private ServiceLayerProvider provider;
	private String pluralId;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		this.provider = new ServiceLayerProvider();
		this.provider.setMetadataDependencyRegistry(mockMetadataDependencyRegistry);
		this.provider.setMetadataService(mockMetadataService);
		this.provider.setServiceAnnotationValuesFactory(mockServiceAnnotationValuesFactory);
		this.provider.setServiceInterfaceLocator(mockServiceInterfaceLocator);
		
		when(mockTargetType.getFullyQualifiedTypeName()).thenReturn("com.example.domain.Person");
		when(mockTargetType.getSimpleTypeName()).thenReturn("Person");
		this.pluralId = PluralMetadata.createIdentifier(mockTargetType);
	}
	
	/**
	 * Sets up a mock {@link ClassOrInterfaceTypeDetails} for a service
	 * interface whose {@link RooService} annotation specifies the following
	 * method names
	 * 
	 * @param findAllMethod can be blank
	 * @param saveMethod can be blank
	 * @param updateMethod can be blank
	 * @return a non-<code>null</code> mock
	 */
	private ClassOrInterfaceTypeDetails getMockService(final String findAllMethod, final String saveMethod, final String updateMethod) {
		final ClassOrInterfaceTypeDetails mockServiceInterface = mock(ClassOrInterfaceTypeDetails.class);
		final JavaType mockServiceType = mock(JavaType.class);
		final ServiceAnnotationValues mockServiceAnnotationValues = mock(ServiceAnnotationValues.class);
		
		when(mockServiceType.getSimpleTypeName()).thenReturn("PersonService");
		when(mockServiceInterface.getName()).thenReturn(mockServiceType);
		when(mockServiceInterface.getDeclaredByMetadataId()).thenReturn(SERVICE_MID);
		when(mockServiceAnnotationValues.getFindAllMethod()).thenReturn(findAllMethod);
		when(mockServiceAnnotationValues.getSaveMethod()).thenReturn(saveMethod);
		when(mockServiceAnnotationValues.getUpdateMethod()).thenReturn(updateMethod);
		when(mockServiceAnnotationValuesFactory.getInstance(mockServiceInterface)).thenReturn(mockServiceAnnotationValues);
		
		return mockServiceInterface;
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
	
	/**
	 * Asserts that asking the {@link ServiceLayerProvider} for a method with
	 * the given name and parameters results in the given method signature
	 * 
	 * @param plural
	 * @param mockServiceInterfaces can be empty
	 * @param methodId
	 * @param expectedMethodSignature <code>null</code> means no additions are expected
	 * @param methodParameters
	 */
	private void assertAdditions(final String plural, final List<ClassOrInterfaceTypeDetails> mockServiceInterfaces, final String methodId, final String expectedMethodSignature, final Pair<JavaType, JavaSymbolName>... methodParameters) {
		// Set up
		setUpPluralMetadata(plural);
		when(mockServiceInterfaceLocator.getServiceInterfaces(mockTargetType)).thenReturn(mockServiceInterfaces);
		
		// Invoke
		final MemberTypeAdditions additions = this.provider.getMemberTypeAdditions(CALLER_MID, methodId, mockTargetType, methodParameters);
		
		// Check
		if (expectedMethodSignature == null) {
			assertNull("Expected no additions but found: " + additions, additions);
		} else {
			assertNotNull("Expected some additions but was null", additions);
			assertEquals(expectedMethodSignature, additions.getMethodSignature());
			verify(mockMetadataDependencyRegistry).registerDependency(SERVICE_MID, CALLER_MID);
			verify(mockMetadataDependencyRegistry).registerDependency(this.pluralId, CALLER_MID);
		}
	}
	
	@Test
	public void testGetAdditionsForEntityWithNullPluralMetadata() {
		// Set up
		when(mockMetadataService.get(pluralId)).thenReturn(null);
		
		// Invoke
		final MemberTypeAdditions additions = this.provider.getMemberTypeAdditions(CALLER_MID, BOGUS_METHOD, mockTargetType);
		
		// Check
		assertNull(additions);		
	}

	@Test
	public void testGetAdditionsForEntityWithNullPluralText() {
		assertAdditions(null, Arrays.<ClassOrInterfaceTypeDetails>asList(), FIND_ALL_METHOD, null);
	}
	
	@Test
	public void testGetAdditionsForEntityWithNoServices() {
		assertAdditions("x", Arrays.<ClassOrInterfaceTypeDetails>asList(), BOGUS_METHOD, null);
	}
	
	@Test
	public void testGetAdditionsWhenServiceAnnotationValuesUnavailable() {
		final ClassOrInterfaceTypeDetails mockServiceInterface = mock(ClassOrInterfaceTypeDetails.class);
		assertAdditions("anything", Arrays.asList(mockServiceInterface), BOGUS_METHOD, null);
	}	
	
	@Test
	public void testGetAdditionsForBogusMethod() {
		final ClassOrInterfaceTypeDetails mockServiceInterface = mock(ClassOrInterfaceTypeDetails.class);		
		assertAdditions("x", Arrays.asList(mockServiceInterface), BOGUS_METHOD, null);
	}
	
	@Test
	public void testGetAdditionsForFindAllMethodWhenServiceDoesNotProvideIt() {
		final ClassOrInterfaceTypeDetails mockServiceInterface = getMockService("", "x", "x");
		assertAdditions("x", Arrays.asList(mockServiceInterface), FIND_ALL_METHOD, null);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testGetAdditionsForSaveMethodWhenServiceDoesNotProvideIt() {
		final ClassOrInterfaceTypeDetails mockServiceInterface = getMockService("x", null, "x");
		final Pair<JavaType, JavaSymbolName> methodParameter = new Pair<JavaType, JavaSymbolName>(mockTargetType, new JavaSymbolName("anything"));
		assertAdditions("x", Arrays.asList(mockServiceInterface), SAVE_METHOD, null, methodParameter);
	}
	
	@Test
	public void testGetAdditionsForFindAllMethodWhenServiceProvidesIt() {
		final ClassOrInterfaceTypeDetails mockServiceInterface = getMockService("findPerson", "", "x");
		assertAdditions("s", Arrays.asList(mockServiceInterface), FIND_ALL_METHOD, "personService.findPersons()");		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testGetAdditionsForSaveMethodWhenServiceProvidesIt() {
		final ClassOrInterfaceTypeDetails mockServiceInterface = getMockService("x", "save", "x");
		final Pair<JavaType, JavaSymbolName> methodParameters = new Pair<JavaType, JavaSymbolName>(mockTargetType, new JavaSymbolName("user"));
		assertAdditions("x", Arrays.asList(mockServiceInterface), SAVE_METHOD, "personService.savePerson(user)", methodParameters);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testGetAdditionsForUpdateMethodWhenServiceDoesNotProvideIt() {
		final ClassOrInterfaceTypeDetails mockServiceInterface = getMockService("x", "x", "");
		final Pair<JavaType, JavaSymbolName> methodParameter = new Pair<JavaType, JavaSymbolName>(mockTargetType, new JavaSymbolName("employee"));
		assertAdditions("x", Arrays.asList(mockServiceInterface), UPDATE_METHOD, null, methodParameter);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testGetAdditionsForUpdateMethodWhenServiceProvidesIt() {
		final ClassOrInterfaceTypeDetails mockServiceInterface = getMockService("x", "x", "change");
		final Pair<JavaType, JavaSymbolName> methodParameters = new Pair<JavaType, JavaSymbolName>(mockTargetType, new JavaSymbolName("bob"));
		assertAdditions("x", Arrays.asList(mockServiceInterface), UPDATE_METHOD, "personService.changePerson(bob)", methodParameters);
	}
}
