package org.springframework.roo.addon.entity;

import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.COLUMN_FIELD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.EMBEDDED_FIELD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.EMBEDDED_ID_FIELD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.ENUMERATED_FIELD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.IDENTIFIER_FIELD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.IDENTIFIER_MUTATOR_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.IDENTIFIER_TYPE;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.LOB_FIELD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.MANY_TO_MANY_FIELD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.MANY_TO_ONE_FIELD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.ONE_TO_MANY_FIELD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.ONE_TO_ONE_FIELD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.PERSISTENT_TYPE;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.TRANSIENT_FIELD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.VERSION_ACCESSOR_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.VERSION_FIELD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.VERSION_MUTATOR_METHOD;
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
import static org.springframework.roo.model.RooJavaType.ROO_ENTITY;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ENTITY;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.AnnotatedTypeMatcher;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.FieldMatcher;
import org.springframework.roo.classpath.customdata.taggers.MethodMatcher;
import org.springframework.roo.classpath.customdata.taggers.MidTypeMatcher;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;

/**
 * The {@link JpaEntityMetadataProvider} implementation.
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component(immediate = true)
@Service
public class JpaEntityMetadataProviderImpl extends AbstractIdentifierServiceAwareMetadataProvider implements JpaEntityMetadataProvider {
	
	// JPA-related field matchers
	private static final FieldMatcher JPA_COLUMN_FIELD_MATCHER = new FieldMatcher(COLUMN_FIELD, AnnotationMetadataBuilder.getInstance(COLUMN));
	private static final FieldMatcher JPA_EMBEDDED_FIELD_MATCHER = new FieldMatcher(EMBEDDED_FIELD, AnnotationMetadataBuilder.getInstance(EMBEDDED));
	private static final FieldMatcher JPA_EMBEDDED_ID_FIELD_MATCHER = new FieldMatcher(EMBEDDED_ID_FIELD, AnnotationMetadataBuilder.getInstance(EMBEDDED_ID));
	private static final FieldMatcher JPA_ENUMERATED_FIELD_MATCHER = new FieldMatcher(ENUMERATED_FIELD, AnnotationMetadataBuilder.getInstance(ENUMERATED));
	private static final FieldMatcher JPA_ID_AND_EMBEDDED_ID_FIELD_MATCHER = new FieldMatcher(IDENTIFIER_FIELD, AnnotationMetadataBuilder.getInstance(ID), AnnotationMetadataBuilder.getInstance(EMBEDDED_ID));
	private static final FieldMatcher JPA_ID_FIELD_MATCHER = new FieldMatcher(IDENTIFIER_FIELD, AnnotationMetadataBuilder.getInstance(ID));
	private static final FieldMatcher JPA_LOB_FIELD_MATCHER = new FieldMatcher(LOB_FIELD, AnnotationMetadataBuilder.getInstance(LOB));
	private static final FieldMatcher JPA_MANY_TO_MANY_FIELD_MATCHER = new FieldMatcher(MANY_TO_MANY_FIELD, AnnotationMetadataBuilder.getInstance(MANY_TO_MANY));
	private static final FieldMatcher JPA_MANY_TO_ONE_FIELD_MATCHER = new FieldMatcher(MANY_TO_ONE_FIELD, AnnotationMetadataBuilder.getInstance(MANY_TO_ONE));
	private static final FieldMatcher JPA_ONE_TO_MANY_FIELD_MATCHER = new FieldMatcher(ONE_TO_MANY_FIELD, AnnotationMetadataBuilder.getInstance(ONE_TO_MANY));
	private static final FieldMatcher JPA_ONE_TO_ONE_FIELD_MATCHER = new FieldMatcher(ONE_TO_ONE_FIELD, AnnotationMetadataBuilder.getInstance(ONE_TO_ONE));
	private static final FieldMatcher JPA_TRANSIENT_FIELD_MATCHER = new FieldMatcher(TRANSIENT_FIELD, AnnotationMetadataBuilder.getInstance(TRANSIENT));
	private static final FieldMatcher JPA_VERSION_FIELD_MATCHER = new FieldMatcher(VERSION_FIELD, AnnotationMetadataBuilder.getInstance(VERSION));

	// The order of this array is the order in which we look for annotations. We
	// use the values of the first one found.
	private static final JavaType[] TRIGGER_ANNOTATIONS = {
		// We trigger off RooJpaEntity in case the user doesn't want Active Record methods
		ROO_JPA_ENTITY,
		// We trigger off RooEntity so that existing projects don't need to add RooJpaEntity
		ROO_ENTITY,
	};

	private static final String PROVIDES_TYPE_STRING = JpaEntityMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	
	// Fields
	@Reference private CustomDataKeyDecorator customDataKeyDecorator;
	
	// ------------- Mandatory AbstractItdMetadataProvider methods -------------
	
	@Override
	protected String createLocalIdentifier(final JavaType javaType, final Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
		final JavaType javaType = getType(metadataIdentificationString);
		final Path path = PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	private JavaType getType(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "Jpa_Entity";
	}
	
	public String getProvidesType() {
		return PROVIDES_TYPE;
	}
	
	// ------------- Optional AbstractItdMetadataProvider methods --------------

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), PROVIDES_TYPE);
		addMetadataTriggers(TRIGGER_ANNOTATIONS);
		registerMatchers();
	}
	
	@SuppressWarnings("unchecked")
	private void registerMatchers() {
		customDataKeyDecorator.registerMatchers(
				getClass(),
				// Type matchers
				new MidTypeMatcher(IDENTIFIER_TYPE, "org.springframework.roo.addon.entity.IdentifierMetadata"),
				new AnnotatedTypeMatcher(PERSISTENT_TYPE, RooJavaType.ROO_ENTITY, RooJavaType.ROO_JPA_ENTITY),
				// Field matchers
				JPA_COLUMN_FIELD_MATCHER,
				JPA_EMBEDDED_FIELD_MATCHER,
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
				new MethodMatcher(Arrays.asList(JPA_ID_AND_EMBEDDED_ID_FIELD_MATCHER), IDENTIFIER_ACCESSOR_METHOD, true),
				new MethodMatcher(Arrays.asList(JPA_ID_AND_EMBEDDED_ID_FIELD_MATCHER), IDENTIFIER_MUTATOR_METHOD, false),
				new MethodMatcher(Arrays.asList(JPA_VERSION_FIELD_MATCHER), VERSION_ACCESSOR_METHOD, true),
				new MethodMatcher(Arrays.asList(JPA_VERSION_FIELD_MATCHER), VERSION_MUTATOR_METHOD, false)
		);
	}
	
	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), PROVIDES_TYPE);
		removeMetadataTriggers(TRIGGER_ANNOTATIONS);
		customDataKeyDecorator.unregisterMatchers(getClass());
	}
	
	// ---------------- The meat of this provider starts here ------------------

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataId, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalType, final String itdFilename) {
		// Find out the entity-level JPA details from the trigger annotation
		final JpaEntityAnnotationValues jpaEntityAnnotationValues = getJpaEntityAnnotationValues(governorPhysicalType);
		
		/*
		 * Walk the inheritance hierarchy for any existing JpaEntityMetadata. We
		 * don't need to monitor any such parent, as any changes to its Java
		 * type will trickle down to the governing java type.
		 */
		final JpaEntityMetadata parentEntity = getParentMetadata((ClassOrInterfaceTypeDetails) governorPhysicalType.getMemberHoldingTypeDetails());
		
		// Get the governor's members
		final MemberDetails governorDetails = getMemberDetails(governorPhysicalType);

		// Get the governor's ID field, if any
		final Identifier identifier = getIdentifier(metadataId);

		boolean isGaeEnabled = false;
		boolean isDatabaseDotComEnabled = false;

		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.PROJECT_IDENTIFIER);
		if (projectMetadata != null) {
			// If the project itself changes, we want a chance to refresh this item
			metadataDependencyRegistry.registerDependency(ProjectMetadata.PROJECT_IDENTIFIER, metadataId);
			isGaeEnabled = projectMetadata.isGaeEnabled();
			isDatabaseDotComEnabled = projectMetadata.isDatabaseDotComEnabled();
		}

		return new JpaEntityMetadata(metadataId, aspectName, governorPhysicalType, parentEntity, projectMetadata, governorDetails, identifier, jpaEntityAnnotationValues, isGaeEnabled, isDatabaseDotComEnabled);
	}

	/**
	 * Returns the {@link JpaEntityAnnotationValues} for the given domain type
	 * 
	 * @param governorPhysicalType (required)
	 * @return a non-<code>null</code> instance
	 */
	private JpaEntityAnnotationValues getJpaEntityAnnotationValues(final PhysicalTypeMetadata governorPhysicalType) {
		for (final JavaType triggerAnnotation : TRIGGER_ANNOTATIONS) {
			final JpaEntityAnnotationValues annotationValues = new JpaEntityAnnotationValues(governorPhysicalType, triggerAnnotation);
			if (annotationValues.isAnnotationFound()) {
				return annotationValues;
			}
		}
		throw new IllegalStateException(getClass().getSimpleName() + " was triggered but not by any of " + Arrays.toString(TRIGGER_ANNOTATIONS));
	}
	
	/**
	 * Returns the {@link Identifier} for the entity identified by the given metadata ID.
	 * 
	 * @param metadataId
	 * @return <code>null</code> if there isn't one
	 */
	private Identifier getIdentifier(final String metadataId) {
		final JavaType entity = getType(metadataId);
		final List<Identifier> identifiers = getIdentifiersForType(entity);
		if (identifiers == null || identifiers.isEmpty()) {
			return null;
		}
		// We have potential identifier information from an IdentifierService.
		// We only use this identifier information if the user did NOT provide ANY identifier-related attributes on @RooEntity....
		Assert.isTrue(identifiers.size() == 1, "Identifier service indicates " + identifiers.size() + " fields illegally for the entity '" + entity.getSimpleTypeName() + "' (should only be one identifier field given this is an entity, not an Identifier class)");
		return identifiers.iterator().next();
	}
}
