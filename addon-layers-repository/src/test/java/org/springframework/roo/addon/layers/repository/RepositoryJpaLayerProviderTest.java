package org.springframework.roo.addon.layers.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FLUSH_METHOD;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.layers.MemberTypeAdditions;
import org.springframework.roo.support.util.Pair;


/**
 * Unit test of {@link RepositoryJpaLayerProvider}
 *
 * @author Andrew Swan
 * @author Stefan Schmidt
 * @since 1.2
 */
public class RepositoryJpaLayerProviderTest {

	// Constants
	private static final String CALLER_MID = "MID:anything#com.example.PetService";
	
	// Fixture
	private RepositoryJpaLayerProvider layerProvider;
	@Mock private JavaType mockTargetEntity;
	@Mock private JavaType mockIdType;
	@Mock private PersistenceMemberLocator persistenceMemberLocator;
	@Mock private RepositoryJpaLocator mockRepositoryLocator;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		this.layerProvider = new RepositoryJpaLayerProvider();
		this.layerProvider.setRepositoryLocator(mockRepositoryLocator);
		this.layerProvider.setPersistenceMemberLocator(persistenceMemberLocator);
	}
	
	/**
	 * Sets up the mock {@link RepositoryJpaLocator} and {@link PersistenceMemberLocator} to return a mock repository
	 * for our test entity.
	 */
	private void setUpMockRepository() {
		final ClassOrInterfaceTypeDetails mockRepositoryDetails = mock(ClassOrInterfaceTypeDetails.class);
		final FieldMetadata mockFieldMetadata = mock(FieldMetadata.class);
		final JavaType mockRepositoryType = mock(JavaType.class);
		when(mockRepositoryType.getSimpleTypeName()).thenReturn("ClinicRepo");
		when(mockIdType.getFullyQualifiedTypeName()).thenReturn(Long.class.getName());
		when(mockRepositoryDetails.getName()).thenReturn(mockRepositoryType);
		when(mockFieldMetadata.getFieldType()).thenReturn(mockIdType);
		when(mockRepositoryLocator.getRepositories(mockTargetEntity)).thenReturn(Arrays.asList(mockRepositoryDetails));
		when(persistenceMemberLocator.getIdentifierFields(mockTargetEntity)).thenReturn(Arrays.asList(mockFieldMetadata));
	}
	
	@Test
	public void testGetAdditionsForNonRepositoryLayerMethod() {
		// Invoke
		final MemberTypeAdditions additions = this.layerProvider.getMemberTypeAdditions(CALLER_MID, "bogus", mockTargetEntity, mockIdType);
		
		// Check
		assertNull(additions);
	}
	
	@Test
	public void testGetAdditionsWhenNoRepositoriesExist() {
		// Invoke
		final MemberTypeAdditions additions = this.layerProvider.getMemberTypeAdditions(CALLER_MID, FIND_ALL_METHOD.name(), mockTargetEntity, mockIdType);
		
		// Check
		assertNull(additions);
	}
	
	/**
	 * Asserts that the {@link RepositoryJpaLayerProvider} generates the
	 * expected call for the given method with the given parameters
	 * 
	 * @param expectedMethodCall
	 * @param methodKey
	 * @param callerParameters
	 */
	private void assertMethodCall(final String expectedMethodCall, final MethodMetadataCustomDataKey methodKey, final Pair<JavaType, JavaSymbolName>... callerParameters) {
		// Set up
		setUpMockRepository();
		
		// Invoke
		final MemberTypeAdditions additions = this.layerProvider.getMemberTypeAdditions(CALLER_MID, methodKey.name(), mockTargetEntity, mockIdType, callerParameters);
		
		// Check
		assertEquals(expectedMethodCall, additions.getMethodCall());
	}
	
	@Test
	public void testGetFindAllAdditions() {
		assertMethodCall("clinicRepo.findAll()", FIND_ALL_METHOD);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testGetFlushAdditions() {
		final Pair<JavaType, JavaSymbolName> entityParameter = new Pair<JavaType, JavaSymbolName>(mockTargetEntity, new JavaSymbolName("anything"));
		assertMethodCall("clinicRepo.flush()", FLUSH_METHOD, entityParameter);
	}
}
