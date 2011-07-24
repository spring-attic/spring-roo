package org.springframework.roo.addon.layers.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ALL_METHOD;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.layers.MemberTypeAdditions;


/**
 * Unit test of {@link RepositoryJpaLayerProvider}
 *
 * @author Andrew Swan
 * @since 1.2
 */
public class RepositoryJpaLayerProviderTest {

	// Constants
	private static final String CALLER_MID = "MID:anything#com.example.PetService";
	
	// Fixture
	private RepositoryJpaLayerProvider layerProvider;
	@Mock private JavaType mockTargetEntity;
	@Mock private MetadataDependencyRegistry mockMetadataDependencyRegistry;
	@Mock private RepositoryJpaLocator mockRepositoryLocator;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		this.layerProvider = new RepositoryJpaLayerProvider();
		this.layerProvider.setMetadataDependencyRegistry(mockMetadataDependencyRegistry);
		this.layerProvider.setRepositoryLocator(mockRepositoryLocator);
	}
	
	@Test
	public void testGetAdditionsForNonRepositoryLayerMethod() {
		// Invoke
		final MemberTypeAdditions additions = this.layerProvider.getMemberTypeAdditions(CALLER_MID, "bogus", mockTargetEntity);
		
		// Check
		assertNull(additions);
	}
	
	@Test
	public void testGetAdditionsWhenNoRepositoriesExist() {
		// Invoke
		final MemberTypeAdditions additions = this.layerProvider.getMemberTypeAdditions(CALLER_MID, FIND_ALL_METHOD.name(), mockTargetEntity);
		
		// Check
		assertNull(additions);
	}
	
	@Test
	public void testGetAdditionsWhenRepositoryExists() {
		// Set up
		final ClassOrInterfaceTypeDetails mockRepositoryDetails = mock(ClassOrInterfaceTypeDetails.class);
		final JavaType mockRepositoryType = mock(JavaType.class);
		when(mockRepositoryType.getSimpleTypeName()).thenReturn("ClinicRepo");
		when(mockRepositoryDetails.getName()).thenReturn(mockRepositoryType);
		when(mockRepositoryLocator.getRepositories(mockTargetEntity)).thenReturn(Arrays.asList(mockRepositoryDetails));
		
		// Invoke
		final MemberTypeAdditions additions = this.layerProvider.getMemberTypeAdditions(CALLER_MID, FIND_ALL_METHOD.name(), mockTargetEntity);
		
		// Check
		assertEquals("clinicRepo.findAll()", additions.getMethodSignature());	
	}
}
