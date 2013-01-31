package org.springframework.roo.addon.jpa.entity;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.COLUMN_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.EMBEDDED_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.EMBEDDED_ID_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.ENUMERATED_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_ACCESSOR_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_MUTATOR_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.IDENTIFIER_TYPE;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.LOB_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MANY_TO_MANY_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MANY_TO_ONE_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.ONE_TO_MANY_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.ONE_TO_ONE_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSISTENT_TYPE;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.TRANSIENT_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.VERSION_ACCESSOR_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.VERSION_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.VERSION_MUTATOR_METHOD;
import static org.springframework.roo.model.JpaJavaType.COLUMN;
import static org.springframework.roo.model.JpaJavaType.EMBEDDED;
import static org.springframework.roo.model.JpaJavaType.EMBEDDED_ID;
import static org.springframework.roo.model.JpaJavaType.ENUMERATED;
import static org.springframework.roo.model.JpaJavaType.ID;
import static org.springframework.roo.model.JpaJavaType.LOB;
import static org.springframework.roo.model.JpaJavaType.MANY_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.MANY_TO_ONE;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_ONE;
import static org.springframework.roo.model.JpaJavaType.TRANSIENT;
import static org.springframework.roo.model.JpaJavaType.VERSION;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ACTIVE_RECORD;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ENTITY;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.AbstractIdentifierServiceAwareMetadataProvider;
import org.springframework.roo.addon.jpa.identifier.Identifier;
import org.springframework.roo.addon.jpa.identifier.IdentifierMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.AnnotatedTypeMatcher;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.FieldMatcher;
import org.springframework.roo.classpath.customdata.taggers.MethodMatcher;
import org.springframework.roo.classpath.customdata.taggers.MidTypeMatcher;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.FeatureNames;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * The {@link JpaEntityMetadataProvider} implementation.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component(immediate = true)
@Service
public class JpaEntityMetadataProviderImpl extends
        AbstractIdentifierServiceAwareMetadataProvider implements
        JpaEntityMetadataProvider {

    // JPA-related field matchers
    private static final FieldMatcher JPA_COLUMN_FIELD_MATCHER = new FieldMatcher(
            COLUMN_FIELD, AnnotationMetadataBuilder.getInstance(COLUMN));
    private static final FieldMatcher JPA_EMBEDDED_FIELD_MATCHER = new FieldMatcher(
            EMBEDDED_FIELD, AnnotationMetadataBuilder.getInstance(EMBEDDED));
    private static final FieldMatcher JPA_EMBEDDED_ID_FIELD_MATCHER = new FieldMatcher(
            EMBEDDED_ID_FIELD,
            AnnotationMetadataBuilder.getInstance(EMBEDDED_ID));
    private static final FieldMatcher JPA_ENUMERATED_FIELD_MATCHER = new FieldMatcher(
            ENUMERATED_FIELD, AnnotationMetadataBuilder.getInstance(ENUMERATED));
    private static final FieldMatcher JPA_ID_AND_EMBEDDED_ID_FIELD_MATCHER = new FieldMatcher(
            IDENTIFIER_FIELD, AnnotationMetadataBuilder.getInstance(ID),
            AnnotationMetadataBuilder.getInstance(EMBEDDED_ID));
    private static final FieldMatcher JPA_ID_FIELD_MATCHER = new FieldMatcher(
            IDENTIFIER_FIELD, AnnotationMetadataBuilder.getInstance(ID));
    private static final FieldMatcher JPA_LOB_FIELD_MATCHER = new FieldMatcher(
            LOB_FIELD, AnnotationMetadataBuilder.getInstance(LOB));
    private static final FieldMatcher JPA_MANY_TO_MANY_FIELD_MATCHER = new FieldMatcher(
            MANY_TO_MANY_FIELD,
            AnnotationMetadataBuilder.getInstance(MANY_TO_MANY));
    private static final FieldMatcher JPA_MANY_TO_ONE_FIELD_MATCHER = new FieldMatcher(
            MANY_TO_ONE_FIELD,
            AnnotationMetadataBuilder.getInstance(MANY_TO_ONE));
    private static final FieldMatcher JPA_ONE_TO_MANY_FIELD_MATCHER = new FieldMatcher(
            ONE_TO_MANY_FIELD,
            AnnotationMetadataBuilder.getInstance(ONE_TO_MANY));
    private static final FieldMatcher JPA_ONE_TO_ONE_FIELD_MATCHER = new FieldMatcher(
            ONE_TO_ONE_FIELD, AnnotationMetadataBuilder.getInstance(ONE_TO_ONE));
    private static final FieldMatcher JPA_TRANSIENT_FIELD_MATCHER = new FieldMatcher(
            TRANSIENT_FIELD, AnnotationMetadataBuilder.getInstance(TRANSIENT));
    private static final FieldMatcher JPA_VERSION_FIELD_MATCHER = new FieldMatcher(
            VERSION_FIELD, AnnotationMetadataBuilder.getInstance(VERSION));
    private static final String PROVIDES_TYPE_STRING = JpaEntityMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);

    // The order of this array is the order in which we look for annotations. We
    // use the values of the first one found.
    private static final JavaType[] TRIGGER_ANNOTATIONS = {
            // We trigger off RooJpaEntity in case the user doesn't want Active
            // Record methods
            ROO_JPA_ENTITY,
            // We trigger off RooJpaActiveRecord so that existing projects don't
            // need to add RooJpaEntity
            ROO_JPA_ACTIVE_RECORD, };

    @Reference private CustomDataKeyDecorator customDataKeyDecorator;
    @Reference private ProjectOperations projectOperations;

    protected void activate(final ComponentContext context) {
        metadataDependencyRegistry.registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                PROVIDES_TYPE);
        addMetadataTriggers(TRIGGER_ANNOTATIONS);
        registerMatchers();
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(
                PROVIDES_TYPE_STRING, javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        metadataDependencyRegistry.deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                PROVIDES_TYPE);
        removeMetadataTriggers(TRIGGER_ANNOTATIONS);
        customDataKeyDecorator.unregisterMatchers(getClass());
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = getType(metadataIdentificationString);
        final LogicalPath path = PhysicalTypeIdentifierNamingUtils.getPath(
                PROVIDES_TYPE_STRING, metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    /**
     * Returns the {@link Identifier} for the entity identified by the given
     * metadata ID.
     * 
     * @param metadataIdentificationString
     * @return <code>null</code> if there isn't one
     */
    private Identifier getIdentifier(final String metadataIdentificationString) {
        final JavaType entity = getType(metadataIdentificationString);
        final List<Identifier> identifiers = getIdentifiersForType(entity);
        if (CollectionUtils.isEmpty(identifiers)) {
            return null;
        }
        // We have potential identifier information from an IdentifierService.
        // We only use this identifier information if the user did NOT provide
        // ANY identifier-related attributes on @RooJpaEntity....
        Validate.isTrue(
                identifiers.size() == 1,
                "Identifier service indicates %d fields illegally for the entity '%s' (should only be one identifier field given this is an entity, not an Identifier class)",
                identifiers.size(), entity.getSimpleTypeName());
        return identifiers.iterator().next();
    }

    public String getItdUniquenessFilenameSuffix() {
        return "Jpa_Entity";
    }

    /**
     * Returns the {@link JpaEntityAnnotationValues} for the given domain type
     * 
     * @param governorPhysicalType (required)
     * @return a non-<code>null</code> instance
     */
    private JpaEntityAnnotationValues getJpaEntityAnnotationValues(
            final PhysicalTypeMetadata governorPhysicalType) {
        for (final JavaType triggerAnnotation : TRIGGER_ANNOTATIONS) {
            final JpaEntityAnnotationValues annotationValues = new JpaEntityAnnotationValues(
                    governorPhysicalType, triggerAnnotation);
            if (annotationValues.isAnnotationFound()) {
                return annotationValues;
            }
        }
        throw new IllegalStateException(getClass().getSimpleName()
                + " was triggered but not by any of "
                + Arrays.toString(TRIGGER_ANNOTATIONS));
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalType,
            final String itdFilename) {
        // Find out the entity-level JPA details from the trigger annotation
        final JpaEntityAnnotationValues jpaEntityAnnotationValues = getJpaEntityAnnotationValues(governorPhysicalType);

        /*
         * Walk the inheritance hierarchy for any existing JpaEntityMetadata. We
         * don't need to monitor any such parent, as any changes to its Java
         * type will trickle down to the governing java type.
         */
        final JpaEntityMetadata parent = getParentMetadata(governorPhysicalType
                .getMemberHoldingTypeDetails());

        // Get the governor's members
        final MemberDetails governorMemberDetails = getMemberDetails(governorPhysicalType);

        // Get the governor's ID field, if any
        final Identifier identifier = getIdentifier(metadataIdentificationString);

        boolean isGaeEnabled = false;
        boolean isDatabaseDotComEnabled = false;
        final String moduleName = PhysicalTypeIdentifierNamingUtils.getPath(
                metadataIdentificationString).getModule();
        if (projectOperations.isProjectAvailable(moduleName)) {
            // If the project itself changes, we want a chance to refresh this
            // item
            metadataDependencyRegistry.registerDependency(
                    ProjectMetadata.getProjectIdentifier(moduleName),
                    metadataIdentificationString);
            isGaeEnabled = projectOperations.isFeatureInstalledInModule(
                    FeatureNames.GAE, moduleName);
            isDatabaseDotComEnabled = projectOperations
                    .isFeatureInstalledInFocusedModule(FeatureNames.DATABASE_DOT_COM);
        }

        return new JpaEntityMetadata(metadataIdentificationString, aspectName,
                governorPhysicalType, parent, governorMemberDetails,
                identifier, jpaEntityAnnotationValues, isGaeEnabled,
                isDatabaseDotComEnabled);
    }

    public String getProvidesType() {
        return PROVIDES_TYPE;
    }

    private JavaType getType(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getJavaType(
                PROVIDES_TYPE_STRING, metadataIdentificationString);
    }

    @SuppressWarnings("unchecked")
    private void registerMatchers() {
        customDataKeyDecorator.registerMatchers(
                getClass(),
                // Type matchers
                new MidTypeMatcher(IDENTIFIER_TYPE, IdentifierMetadata.class
                        .getName()),
                new AnnotatedTypeMatcher(PERSISTENT_TYPE,
                        RooJavaType.ROO_JPA_ACTIVE_RECORD, ROO_JPA_ENTITY),
                // Field matchers
                JPA_COLUMN_FIELD_MATCHER, JPA_EMBEDDED_FIELD_MATCHER,
                JPA_EMBEDDED_ID_FIELD_MATCHER,
                JPA_ENUMERATED_FIELD_MATCHER,
                JPA_ID_FIELD_MATCHER,
                JPA_LOB_FIELD_MATCHER,
                JPA_MANY_TO_MANY_FIELD_MATCHER,
                JPA_MANY_TO_ONE_FIELD_MATCHER,
                JPA_ONE_TO_MANY_FIELD_MATCHER,
                JPA_ONE_TO_ONE_FIELD_MATCHER,
                JPA_TRANSIENT_FIELD_MATCHER,
                JPA_VERSION_FIELD_MATCHER,
                // Method matchers
                new MethodMatcher(Arrays
                        .asList(JPA_ID_AND_EMBEDDED_ID_FIELD_MATCHER),
                        IDENTIFIER_ACCESSOR_METHOD, true), new MethodMatcher(
                        Arrays.asList(JPA_ID_AND_EMBEDDED_ID_FIELD_MATCHER),
                        IDENTIFIER_MUTATOR_METHOD, false), new MethodMatcher(
                        Arrays.asList(JPA_VERSION_FIELD_MATCHER),
                        VERSION_ACCESSOR_METHOD, true), new MethodMatcher(
                        Arrays.asList(JPA_VERSION_FIELD_MATCHER),
                        VERSION_MUTATOR_METHOD, false));
    }
}
