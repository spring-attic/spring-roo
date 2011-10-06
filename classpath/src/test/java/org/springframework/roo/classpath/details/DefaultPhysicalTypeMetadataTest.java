package org.springframework.roo.classpath.details;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.itd.ItdMetadataProvider;
import org.springframework.roo.model.JavaType;

/**
 * Unit test of {@link DefaultPhysicalTypeMetadata}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class DefaultPhysicalTypeMetadataTest {
	
	// Constants
	private static final String CANONICAL_PATH = "/usr/bob/projects/foo/Foo.java";
	private static final String METADATA_ID = PhysicalTypeIdentifier.createIdentifier(new JavaType("com.example.Bar"));
	
	// Fixture
	private DefaultPhysicalTypeMetadata metadata;
	@Mock private MemberHoldingTypeDetails mockMemberHoldingTypeDetails;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		this.metadata = new DefaultPhysicalTypeMetadata(METADATA_ID, CANONICAL_PATH, mockMemberHoldingTypeDetails);
	}

	@Test
	public void testGetItdCanoncialPath() {
		// Set up
		final ItdMetadataProvider mockItdMetadataProvider = mock(ItdMetadataProvider.class);
		when(mockItdMetadataProvider.getItdUniquenessFilenameSuffix()).thenReturn("MySuffix");
		
		// Invoke
		final String itdCanoncialPath = metadata.getItdCanoncialPath(mockItdMetadataProvider);
		
		// Check
		assertEquals("/usr/bob/projects/foo/Foo_Roo_MySuffix.aj", itdCanoncialPath);
	}
}
