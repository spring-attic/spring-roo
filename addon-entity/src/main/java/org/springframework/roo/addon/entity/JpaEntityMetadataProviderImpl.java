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

import java.util.Arrays;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.FieldMatcher;
import org.springframework.roo.classpath.customdata.taggers.MethodMatcher;
import org.springframework.roo.classpath.customdata.taggers.TypeMatcher;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;

/**
 * The {@link JpaEntityMetadataProvider} implementation.
 * 
 * @author Andrew Swan
 * @since 1.2
 */
@Component(immediate = true)
@Service
public class JpaEntityMetadataProviderImpl extends AbstractIdentifierServiceAwareMetadataProvider implements JpaEntityMetadataProvider {
	
	// ------------------------------ Constants --------------------------------

	// Value-less JPA annotations (using literal class names so Roo does not depend on JPA)
	private static final AnnotationMetadata JPA_EMBEDDED_ID_ANNOTATION =  AnnotationMetadataBuilder.getInstance("javax.persistence.EmbeddedId");
	private static final AnnotationMetadata JPA_ID_ANNOTATION =  AnnotationMetadataBuilder.getInstance("javax.persistence.Id");
	
	// JPA-related field matchers
	private static final FieldMatcher JPA_COLUMN_FIELD_MATCHER = new FieldMatcher(COLUMN_FIELD, AnnotationMetadataBuilder.getInstance("javax.persistence.Column"));
	private static final FieldMatcher JPA_EMBEDDED_FIELD_MATCHER = new FieldMatcher(EMBEDDED_FIELD, AnnotationMetadataBuilder.getInstance("javax.persistence.Embedded"));
	private static final FieldMatcher JPA_EMBEDDED_ID_FIELD_MATCHER = new FieldMatcher(EMBEDDED_ID_FIELD, JPA_EMBEDDED_ID_ANNOTATION);
	private static final FieldMatcher JPA_ENUMERATED_FIELD_MATCHER = new FieldMatcher(ENUMERATED_FIELD, AnnotationMetadataBuilder.getInstance("javax.persistence.Enumerated"));
	private static final FieldMatcher JPA_ID_AND_EMBEDDED_ID_FIELD_MATCHER = new FieldMatcher(IDENTIFIER_FIELD, JPA_ID_ANNOTATION, JPA_EMBEDDED_ID_ANNOTATION);
	private static final FieldMatcher JPA_ID_FIELD_MATCHER = new FieldMatcher(IDENTIFIER_FIELD, JPA_ID_ANNOTATION);
	private static final FieldMatcher JPA_LOB_FIELD_MATCHER = new FieldMatcher(LOB_FIELD, AnnotationMetadataBuilder.getInstance("javax.persistence.Lob"));
	private static final FieldMatcher JPA_MANY_TO_MANY_FIELD_MATCHER = new FieldMatcher(MANY_TO_MANY_FIELD, AnnotationMetadataBuilder.getInstance("javax.persistence.ManyToMany"));
	private static final FieldMatcher JPA_MANY_TO_ONE_FIELD_MATCHER = new FieldMatcher(MANY_TO_ONE_FIELD, AnnotationMetadataBuilder.getInstance("javax.persistence.ManyToOne"));
	private static final FieldMatcher JPA_ONE_TO_MANY_FIELD_MATCHER = new FieldMatcher(ONE_TO_MANY_FIELD, AnnotationMetadataBuilder.getInstance("javax.persistence.OneToMany"));
	private static final FieldMatcher JPA_ONE_TO_ONE_FIELD_MATCHER = new FieldMatcher(ONE_TO_ONE_FIELD, AnnotationMetadataBuilder.getInstance("javax.persistence.OneToOne"));
	private static final FieldMatcher JPA_TRANSIENT_FIELD_MATCHER = new FieldMatcher(TRANSIENT_FIELD, AnnotationMetadataBuilder.getInstance("javax.persistence.Transient"));
	private static final FieldMatcher JPA_VERSION_FIELD_MATCHER = new FieldMatcher(VERSION_FIELD, AnnotationMetadataBuilder.getInstance("javax.persistence.Version"));

	// The order of this array is the order in which we look for annotations. We
	// use the values of the first one found.
	private static final JavaType[] TRIGGER_ANNOTATIONS = {
		// We trigger off RooJpaEntity in case the user doesn't want Active Record methods
		new JavaType(RooJpaEntity.class.getName()),
		// We trigger off RooEntity so that existing projects don't need to add RooJpaEntity
		new JavaType(RooEntity.class.getName()),
	};

	private static final String PROVIDES_TYPE_STRING = JpaEntityMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	
	// ------------------------------- Fields ----------------------------------
	
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
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		for (final JavaType triggerAnnotation : TRIGGER_ANNOTATIONS) {
			addMetadataTrigger(triggerAnnotation);
		}
		registerMatchers();
	}
	
	@SuppressWarnings("unchecked")
	private void registerMatchers() {
		customDataKeyDecorator.registerMatchers(
				getClass(),
				// Type matchers
				new TypeMatcher(IDENTIFIER_TYPE, "org.springframework.roo.addon.entity.IdentifierMetadata"),
				new TypeMatcher(PERSISTENT_TYPE, "org.springframework.roo.addon.entity.JpaEntityMetadata"),
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
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		for (final JavaType triggerAnnotation : TRIGGER_ANNOTATIONS) {
			removeMetadataTrigger(triggerAnnotation);
		}
		customDataKeyDecorator.unregisterMatchers(getClass());
	}
	
	// ---------------- The meat of this provider starts here ------------------

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataId, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalType, final String itdFilename) {
		// Find out the entity-level JPA details from the trigger annotation
		final JpaEntityAnnotationValues jpaEntityAnnotationValues = getJpaEntityAnnotationValues(governorPhysicalType);
		
		// Look up the inheritance hierarchy for any existing JpaEntityMetadata
		final JpaEntityMetadata parentEntity = getParentMetadata((ClassOrInterfaceTypeDetails) governorPhysicalType.getMemberHoldingTypeDetails());

		// If the project itself changes, we want a chance to refresh this item
		metadataDependencyRegistry.registerDependency(ProjectMetadata.PROJECT_IDENTIFIER, metadataId);
		final ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.PROJECT_IDENTIFIER);
		
		// Get the governor's members
		final MemberDetails governorDetails = getMemberDetails(governorPhysicalType);

		// Get the governor's ID field, if any
		final Identifier identifier = getIdentifier(metadataId);

		return new JpaEntityMetadata(metadataId, aspectName, governorPhysicalType, parentEntity, projectMetadata, governorDetails, identifier, jpaEntityAnnotationValues);
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
	 * Returns the {@link Identifier} for the entity identified by the given
	 * metadata ID.
	 * 
	 * @param metadataId
	 * @return <code>null</code> if there isn't one
	 */
	private Identifier getIdentifier(final String metadataId) {
		final JavaType entityType = getType(metadataId);
		final List<Identifier> identifiers = getIdentifiersForType(entityType);
		if (identifiers == null || identifiers.isEmpty()) {
			return null;
		}
		// We have potential identifier information from an IdentifierService.
		// We only use this identifier information if the user did NOT provide ANY identifier-related attributes on @RooEntity....
		Assert.isTrue(identifiers.size() == 1, "Identifier service indicates " + identifiers.size() + " fields illegally for the entity '" + entityType.getSimpleTypeName() + "' (should only be one identifier field given this is an entity, not an Identifier class)");
		return identifiers.iterator().next();
	}
}
