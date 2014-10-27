package org.springframework.roo.addon.jpa.activerecord;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.CLEAR_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_SORTED_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ENTRIES_SORTED_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FLUSH_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.REMOVE_METHOD;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Unit test of {@link EntityLayerProvider}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class EntityLayerProviderTest {

    private static final String CALLER_MID = "MID:caller#com.example.MyService";

    // Maps the supported entity methods to their test parameter names
    private static final Map<MethodMetadataCustomDataKey, List<String>> METHODS = new HashMap<MethodMetadataCustomDataKey, List<String>>();

    static {
        METHODS.put(CLEAR_METHOD, Collections.<String> emptyList());
        METHODS.put(COUNT_ALL_METHOD, Collections.<String> emptyList());
        METHODS.put(FIND_ALL_METHOD, Collections.<String> emptyList());
        METHODS.put(FIND_ENTRIES_METHOD, Arrays.asList("x", "y"));
        METHODS.put(FIND_ALL_SORTED_METHOD, Arrays.asList("x", "y"));
        METHODS.put(FIND_ENTRIES_SORTED_METHOD, Arrays.asList("w", "x", "y", "z"));
        METHODS.put(FIND_METHOD, Arrays.asList("id"));
        METHODS.put(FLUSH_METHOD, Collections.<String> emptyList());
        METHODS.put(MERGE_METHOD, Collections.<String> emptyList());
        METHODS.put(PERSIST_METHOD, Collections.<String> emptyList());
        METHODS.put(REMOVE_METHOD, Collections.<String> emptyList());
    }

    // Fixture
    private EntityLayerProvider layerProvider;
    @Mock private JpaCrudAnnotationValues mockAnnotationValues;

    @Mock private JavaType mockIdType;
    @Mock private JpaActiveRecordMetadataProvider mockJpaActiveRecordMetadataProvider;
    @Mock private MetadataService mockMetadataService;
    @Mock private PluralMetadata mockPluralMetadata;
    @Mock private JavaType mockTargetEntity;
    @Mock private TypeLocationService mockTypeLocationService;
    private String pluralId;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockTargetEntity.getFullyQualifiedTypeName()).thenReturn(
                "com.example.Pizza");
        when(mockIdType.getFullyQualifiedTypeName()).thenReturn(
                Long.class.getName());
        when(mockTypeLocationService.getTypePath(mockTargetEntity)).thenReturn(
                Path.SRC_MAIN_JAVA.getModulePathId(""));

        pluralId = PluralMetadata.createIdentifier(mockTargetEntity,
                Path.SRC_MAIN_JAVA.getModulePathId(""));
        layerProvider = new EntityLayerProvider();
        layerProvider.typeLocationService = mockTypeLocationService;
        layerProvider
                .setJpaActiveRecordMetadataProvider(mockJpaActiveRecordMetadataProvider);
        layerProvider.setMetadataService(mockMetadataService);
    }

    private void setUpMockAnnotationValues() {
        when(
                mockJpaActiveRecordMetadataProvider
                        .getAnnotationValues(mockTargetEntity)).thenReturn(
                mockAnnotationValues);
    }

    private void setUpPlural(final String plural) {
        when(mockMetadataService.get(pluralId)).thenReturn(mockPluralMetadata);
        when(mockPluralMetadata.getPlural()).thenReturn(plural);
    }

    @Test
    public void testGetAdditionsForBogusMethod() {
        // Set up
        setUpMockAnnotationValues();
        setUpPlural("anything");

        // Invoke
        final MemberTypeAdditions additions = layerProvider
                .getMemberTypeAdditions(CALLER_MID, "bogus", mockTargetEntity,
                        mockIdType);

        // Check
        assertNull(additions);
    }

    @Test
    public void testGetAdditionsForMethodAnnotatedWithEmptyName() {
        // Set up
        setUpMockAnnotationValues();
        when(mockAnnotationValues.getFindAllMethod()).thenReturn("");
        setUpPlural("anything");

        // Invoke
        final MemberTypeAdditions additions = layerProvider
                .getMemberTypeAdditions(CALLER_MID, FIND_ALL_METHOD.name(),
                        mockTargetEntity, mockIdType);

        // Check
        assertNull(additions);
    }

    @Test
    public void testGetAdditionsForMethodAnnotatedWithNonEmptyName() {
        // Set up
        setUpMockAnnotationValues();
        when(mockAnnotationValues.getFindAllMethod()).thenReturn("getAll");
        setUpPlural("Pizzas");

        // Invoke
        final MemberTypeAdditions additions = layerProvider
                .getMemberTypeAdditions(CALLER_MID, FIND_ALL_METHOD.name(),
                        mockTargetEntity, mockIdType);

        // Check
        assertEquals("getAllPizzas", additions.getMethodName());
    }

    @Test
    public void testGetAdditionsWhenEntityAnnotationValuesNotAvailable() {
        // Set up
        when(
                mockJpaActiveRecordMetadataProvider
                        .getAnnotationValues(mockTargetEntity))
                .thenReturn(null);

        // Invoke
        final MemberTypeAdditions additions = layerProvider
                .getMemberTypeAdditions(CALLER_MID, FIND_ALL_METHOD.name(),
                        mockTargetEntity, mockIdType);

        // Check
        assertNull(additions);
    }

    @Test
    public void testGetAdditionsWhenGovernorPluralIsEmpty() {
        // Set up
        setUpMockAnnotationValues();
        setUpPlural("");

        // Invoke
        final MemberTypeAdditions additions = layerProvider
                .getMemberTypeAdditions(CALLER_MID, FIND_ALL_METHOD.name(),
                        mockTargetEntity, mockIdType);

        // Check
        assertNull(additions);
    }

    @Test
    public void testGetAdditionsWhenGovernorPluralMetadataIsNull() {
        setUpMockAnnotationValues();
        when(mockMetadataService.get(pluralId)).thenReturn(null);

        // Invoke
        final MemberTypeAdditions additions = layerProvider
                .getMemberTypeAdditions(CALLER_MID, FIND_ALL_METHOD.name(),
                        mockTargetEntity, mockIdType);

        // Check
        assertNull(additions);
    }
}
