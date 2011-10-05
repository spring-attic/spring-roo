package org.springframework.roo.classpath.details;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.roo.model.JavaType;

/**
 * Unit test of {@link AbstractMemberHoldingTypeDetailsBuilder}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class AbstractMemberHoldingTypeDetailsBuilderTest {

	/**
	 * Testable subclass of {@link AbstractMemberHoldingTypeDetailsBuilder}, see
	 * http://stackoverflow.com/questions/1087339/using-mockito-to-test-abstract-classes
	 */
	private static class TestableBuilder extends AbstractMemberHoldingTypeDetailsBuilder<ClassOrInterfaceTypeDetails> {

		/**
		 * Constructor
		 */
		protected TestableBuilder() {
			super(DECLARED_BY_MID);
		}

		public ClassOrInterfaceTypeDetails build() {
			// This method can be spied upon, see the StackOverflow link above
			return null;
		}
	}

	// Constants
	private static final String DECLARED_BY_MID = "MID:foo#bar";

	// Fixture
	private AbstractMemberHoldingTypeDetailsBuilder<ClassOrInterfaceTypeDetails> builder;

	@Before
	public void setUp() {
		this.builder = new TestableBuilder();
	}

	@Test
	public void testAddConstructorAfterSettingUnmodifiableCollection() {
		// Set up
		final ConstructorMetadataBuilder mockConstructor1 = mock(ConstructorMetadataBuilder.class);
		final ConstructorMetadataBuilder mockConstructor2 = mock(ConstructorMetadataBuilder.class);
		when(mockConstructor2.getDeclaredByMetadataId()).thenReturn(DECLARED_BY_MID);

		// Invoke
		this.builder.setDeclaredConstructors(Collections.singleton(mockConstructor1));
		final boolean added = this.builder.addConstructor(mockConstructor2);

		// Check
		assertTrue(added);
		assertEquals(Arrays.asList(mockConstructor1, mockConstructor2), this.builder.getDeclaredConstructors());
	}

	@Test
	public void testAddExtendsTypeAfterSettingUnmodifiableCollection() {
		// Set up
		final JavaType mockType1 = mock(JavaType.class);
		final JavaType mockType2 = mock(JavaType.class);

		// Invoke
		this.builder.setExtendsTypes(Collections.singleton(mockType1));
		final boolean added = this.builder.addExtendsTypes(mockType2);

		// Check
		assertTrue(added);
		assertEquals(Arrays.asList(mockType1, mockType2), this.builder.getExtendsTypes());
	}

	@Test
	public void testAddFieldAfterSettingUnmodifiableCollection() {
		// Set up
		final FieldMetadataBuilder mockField1 = mock(FieldMetadataBuilder.class);
		final FieldMetadataBuilder mockField2 = mock(FieldMetadataBuilder.class);
		when(mockField2.getDeclaredByMetadataId()).thenReturn(DECLARED_BY_MID);

		// Invoke
		this.builder.setDeclaredFields(Collections.singleton(mockField1));
		final boolean added = this.builder.addField(mockField2);

		// Check
		assertTrue(added);
		assertEquals(Arrays.asList(mockField1, mockField2), this.builder.getDeclaredFields());
	}

	@Test
	public void testAddImplementsTypeAfterSettingUnmodifiableCollection() {
		// Set up
		final JavaType mockType1 = mock(JavaType.class);
		final JavaType mockType2 = mock(JavaType.class);

		// Invoke
		this.builder.setImplementsTypes(Collections.singleton(mockType1));
		final boolean added = this.builder.addImplementsType(mockType2);

		// Check
		assertTrue(added);
		assertEquals(Arrays.asList(mockType1, mockType2), this.builder.getImplementsTypes());
	}

	@Test
	public void testAddInitializerAfterSettingUnmodifiableCollection() {
		// Set up
		final InitializerMetadataBuilder mockInitializer1 = mock(InitializerMetadataBuilder.class);
		final InitializerMetadataBuilder mockInitializer2 = mock(InitializerMetadataBuilder.class);
		when(mockInitializer2.getDeclaredByMetadataId()).thenReturn(DECLARED_BY_MID);

		// Invoke
		this.builder.setDeclaredInitializers(Collections.singleton(mockInitializer1));
		final boolean added = this.builder.addInitializer(mockInitializer2);

		// Check
		assertTrue(added);
		assertEquals(Arrays.asList(mockInitializer1, mockInitializer2), this.builder.getDeclaredInitializers());
	}

	@Test
	public void testAddInnerTypeAfterSettingUnmodifiableCollection() {
		// Set up
		final ClassOrInterfaceTypeDetailsBuilder mockInnerType1 = mock(ClassOrInterfaceTypeDetailsBuilder.class);
		final ClassOrInterfaceTypeDetailsBuilder mockInnerType2 = mock(ClassOrInterfaceTypeDetailsBuilder.class);

		// Invoke
		this.builder.setDeclaredInnerTypes(Collections.singleton(mockInnerType1));
		final boolean added = this.builder.addInnerType(mockInnerType2);

		// Check
		assertTrue(added);
		assertEquals(Arrays.asList(mockInnerType1, mockInnerType2), this.builder.getDeclaredInnerTypes());
	}
}
