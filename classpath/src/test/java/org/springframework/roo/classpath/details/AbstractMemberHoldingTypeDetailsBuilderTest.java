package org.springframework.roo.classpath.details;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
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
     * http
     * ://stackoverflow.com/questions/1087339/using-mockito-to-test-abstract-
     * classes
     */
    private static class TestableBuilder
            extends
            AbstractMemberHoldingTypeDetailsBuilder<ClassOrInterfaceTypeDetails> {

        /**
         * Constructor
         */
        protected TestableBuilder() {
            super(DECLARED_BY_MID);
        }

        @Override
        public void addImports(final Collection<ImportMetadata> imports) {
        }

        public ClassOrInterfaceTypeDetails build() {
            // This method can be spied upon, see the StackOverflow link above
            return null;
        }
    }

    private static final String DECLARED_BY_MID = "MID:foo#bar";

    // Fixture
    private AbstractMemberHoldingTypeDetailsBuilder<ClassOrInterfaceTypeDetails> builder;

    @Before
    public void setUp() {
        builder = new TestableBuilder();
    }

    @Test
    public void testAddConstructorAfterSettingUnmodifiableCollection() {
        // Set up
        final ConstructorMetadataBuilder mockConstructor1 = mock(ConstructorMetadataBuilder.class);
        final ConstructorMetadataBuilder mockConstructor2 = mock(ConstructorMetadataBuilder.class);
        when(mockConstructor2.getDeclaredByMetadataId()).thenReturn(
                DECLARED_BY_MID);

        // Invoke
        builder.setDeclaredConstructors(Collections.singleton(mockConstructor1));
        final boolean added = builder.addConstructor(mockConstructor2);

        // Check
        assertTrue(added);
        assertEquals(Arrays.asList(mockConstructor1, mockConstructor2),
                builder.getDeclaredConstructors());
    }

    @Test
    public void testAddExtendsTypeAfterSettingUnmodifiableCollection() {
        // Set up
        final JavaType mockType1 = mock(JavaType.class);
        final JavaType mockType2 = mock(JavaType.class);

        // Invoke
        builder.setExtendsTypes(Collections.singleton(mockType1));
        final boolean added = builder.addExtendsTypes(mockType2);

        // Check
        assertTrue(added);
        assertEquals(Arrays.asList(mockType1, mockType2),
                builder.getExtendsTypes());
    }

    @Test
    public void testAddFieldAfterSettingUnmodifiableCollection() {
        // Set up
        final FieldMetadataBuilder mockField1 = mock(FieldMetadataBuilder.class);
        final FieldMetadataBuilder mockField2 = mock(FieldMetadataBuilder.class);
        when(mockField2.getDeclaredByMetadataId()).thenReturn(DECLARED_BY_MID);

        // Invoke
        builder.setDeclaredFields(Collections.singleton(mockField1));
        final boolean added = builder.addField(mockField2);

        // Check
        assertTrue(added);
        assertEquals(Arrays.asList(mockField1, mockField2),
                builder.getDeclaredFields());
    }

    @Test
    public void testAddImplementsTypeAfterSettingUnmodifiableCollection() {
        // Set up
        final JavaType mockType1 = mock(JavaType.class);
        final JavaType mockType2 = mock(JavaType.class);

        // Invoke
        builder.setImplementsTypes(Collections.singleton(mockType1));
        final boolean added = builder.addImplementsType(mockType2);

        // Check
        assertTrue(added);
        assertEquals(Arrays.asList(mockType1, mockType2),
                builder.getImplementsTypes());
    }

    @Test
    public void testAddInitializerAfterSettingUnmodifiableCollection() {
        // Set up
        final InitializerMetadataBuilder mockInitializer1 = mock(InitializerMetadataBuilder.class);
        final InitializerMetadataBuilder mockInitializer2 = mock(InitializerMetadataBuilder.class);
        when(mockInitializer2.getDeclaredByMetadataId()).thenReturn(
                DECLARED_BY_MID);

        // Invoke
        builder.setDeclaredInitializers(Collections.singleton(mockInitializer1));
        final boolean added = builder.addInitializer(mockInitializer2);

        // Check
        assertTrue(added);
        assertEquals(Arrays.asList(mockInitializer1, mockInitializer2),
                builder.getDeclaredInitializers());
    }

    @Test
    public void testAddInnerTypeAfterSettingUnmodifiableCollection() {
        // Set up
        final ClassOrInterfaceTypeDetailsBuilder mockInnerType1 = mock(ClassOrInterfaceTypeDetailsBuilder.class);
        final ClassOrInterfaceTypeDetailsBuilder mockInnerType2 = mock(ClassOrInterfaceTypeDetailsBuilder.class);

        // Invoke
        builder.setDeclaredInnerTypes(Collections.singleton(mockInnerType1));
        final boolean added = builder.addInnerType(mockInnerType2);

        // Check
        assertTrue(added);
        assertEquals(Arrays.asList(mockInnerType1, mockInnerType2),
                builder.getDeclaredInnerTypes());
    }
}
