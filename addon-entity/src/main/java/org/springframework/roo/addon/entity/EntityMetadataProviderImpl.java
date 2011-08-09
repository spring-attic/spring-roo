package org.springframework.roo.addon.entity;

import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.CLEAR_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FLUSH_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.IDENTIFIER_FIELD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.IDENTIFIER_MUTATOR_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.IDENTIFIER_TYPE;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.PERSISTENT_TYPE;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.VERSION_ACCESSOR_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.VERSION_MUTATOR_METHOD;
import static org.springframework.roo.classpath.customdata.taggers.FieldMatcher.JPA_COLUMN;
import static org.springframework.roo.classpath.customdata.taggers.FieldMatcher.JPA_EMBEDDED;
import static org.springframework.roo.classpath.customdata.taggers.FieldMatcher.JPA_ENUMERATED;
import static org.springframework.roo.classpath.customdata.taggers.FieldMatcher.JPA_LOB;
import static org.springframework.roo.classpath.customdata.taggers.FieldMatcher.JPA_MANY_TO_MANY;
import static org.springframework.roo.classpath.customdata.taggers.FieldMatcher.JPA_MANY_TO_ONE;
import static org.springframework.roo.classpath.customdata.taggers.FieldMatcher.JPA_ONE_TO_MANY;
import static org.springframework.roo.classpath.customdata.taggers.FieldMatcher.JPA_ONE_TO_ONE;
import static org.springframework.roo.classpath.customdata.taggers.FieldMatcher.JPA_TRANSIENT;
import static org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder.JPA_EMBEDDED_ID_ANNOTATION;
import static org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder.JPA_ID_ANNOTATION;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.addon.plural.PluralMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.taggers.ConstructorMatcher;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.FieldMatcher;
import org.springframework.roo.classpath.customdata.taggers.Matcher;
import org.springframework.roo.classpath.customdata.taggers.MethodMatcher;
import org.springframework.roo.classpath.customdata.taggers.TypeMatcher;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.MemberHoldingTypeDetailsMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.CustomDataAccessor;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;

/**
 * Implementation of {@link EntityMetadataProvider}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public final class EntityMetadataProviderImpl extends AbstractIdentifierServiceAwareMetadataProvider implements EntityMetadataProvider {
	
	// Fields
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	@Reference private CustomDataKeyDecorator customDataKeyDecorator;
	// @Reference private IdentifierMetadataProvider identifierMetadataProvider;
	@Reference private PluralMetadataProvider pluralMetadataProvider;
	private boolean noArgConstructor = true;

	protected void activate(ComponentContext context) {
		configurableMetadataProvider.addMetadataTrigger(ENTITY_ANNOTATION);
		// Now that active record is optional, the ID field is managed by the IdentifierMetadataProvider, not this one
		// identifierMetadataProvider.addMetadataTrigger(ENTITY_ANNOTATION);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		pluralMetadataProvider.addMetadataTrigger(ENTITY_ANNOTATION);
		addMetadataTrigger(ENTITY_ANNOTATION);
		registerMatchers();
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		configurableMetadataProvider.removeMetadataTrigger(ENTITY_ANNOTATION);
		// identifierMetadataProvider.removeMetadataTrigger(ENTITY_ANNOTATION);
		pluralMetadataProvider.removeMetadataTrigger(ENTITY_ANNOTATION);
		removeMetadataTrigger(ENTITY_ANNOTATION);
		customDataKeyDecorator.unregisterMatchers(getClass());
	}

	@SuppressWarnings("unchecked")
	private void registerMatchers() {
		registerMatchers(new TypeMatcher(PERSISTENT_TYPE, EntityMetadata.class));
		registerMatchers(new TypeMatcher(IDENTIFIER_TYPE, IdentifierMetadata.class));

		registerMatchers(ConstructorMatcher.NO_ARG);

		final FieldMatcher idAndEmbeddedIdFieldTagger = new FieldMatcher(IDENTIFIER_FIELD, JPA_ID_ANNOTATION, JPA_EMBEDDED_ID_ANNOTATION);
		registerMatchers(new MethodMatcher(Arrays.asList(idAndEmbeddedIdFieldTagger), IDENTIFIER_ACCESSOR_METHOD, true));
		registerMatchers(new MethodMatcher(Arrays.asList(idAndEmbeddedIdFieldTagger), IDENTIFIER_MUTATOR_METHOD, false));

		registerMatchers(FieldMatcher.JPA_ID, FieldMatcher.JPA_EMBEDDED_ID, FieldMatcher.JPA_VERSION);
		registerMatchers(new MethodMatcher(Arrays.asList(FieldMatcher.JPA_VERSION), VERSION_ACCESSOR_METHOD, true));
		registerMatchers(new MethodMatcher(Arrays.asList(FieldMatcher.JPA_VERSION), VERSION_MUTATOR_METHOD, false));

		registerMatchers(new MethodMatcher(CLEAR_METHOD, ENTITY_ANNOTATION, new JavaSymbolName("clearMethod"), EntityAnnotationValues.CLEAR_METHOD_DEFAULT));
		registerMatchers(new MethodMatcher(COUNT_ALL_METHOD, ENTITY_ANNOTATION, new JavaSymbolName("countMethod"), EntityAnnotationValues.COUNT_METHOD_DEFAULT, true, false));
		registerMatchers(new MethodMatcher(FIND_ALL_METHOD, ENTITY_ANNOTATION, new JavaSymbolName("findAllMethod"), EntityAnnotationValues.FIND_ALL_METHOD_DEFAULT, true, false));
		registerMatchers(new MethodMatcher(FIND_ENTRIES_METHOD, ENTITY_ANNOTATION, new JavaSymbolName("findEntriesMethod"), "find", false, true, "Entries"));
		registerMatchers(new MethodMatcher(FIND_METHOD, ENTITY_ANNOTATION, new JavaSymbolName("findMethod"), EntityAnnotationValues.FIND_METHOD_DEFAULT, false, true));
		registerMatchers(new MethodMatcher(FLUSH_METHOD, ENTITY_ANNOTATION, new JavaSymbolName("flushMethod"), EntityAnnotationValues.FLUSH_METHOD_DEFAULT));
		registerMatchers(new MethodMatcher(MERGE_METHOD, ENTITY_ANNOTATION, new JavaSymbolName("mergeMethod"), EntityAnnotationValues.MERGE_METHOD_DEFAULT));
		registerMatchers(new MethodMatcher(PERSIST_METHOD, ENTITY_ANNOTATION, new JavaSymbolName("persistMethod"), EntityAnnotationValues.PERSIST_METHOD_DEFAULT));
		registerMatchers(new MethodMatcher(REMOVE_METHOD, ENTITY_ANNOTATION, new JavaSymbolName("removeMethod"), EntityAnnotationValues.REMOVE_METHOD_DEFAULT));

		registerMatchers(JPA_COLUMN, JPA_EMBEDDED, JPA_ENUMERATED, JPA_LOB, JPA_MANY_TO_MANY, JPA_MANY_TO_ONE, JPA_ONE_TO_MANY, JPA_ONE_TO_ONE, JPA_TRANSIENT);
	}
	
	/**
	 * Registers the given matchers on behalf of this class
	 * 
	 * @param matchers the matchers to register
	 */
	private void registerMatchers(final Matcher<? extends CustomDataAccessor>... matchers) {
		this.customDataKeyDecorator.registerMatchers(getClass(), matchers);
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We know governor type details are non-null and can be safely cast
		
		// We need to parse the annotation, which we expect to be present
		EntityAnnotationValues annotationValues = new EntityAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound()) {
			return null;
		}

		// Now we walk the inheritance hierarchy until we find some existing EntityMetadata
		EntityMetadata parent = null;
		ClassOrInterfaceTypeDetails superCid = ((ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails()).getSuperclass();
		while (superCid != null && parent == null) {
			String superCidPhysicalTypeIdentifier = superCid.getDeclaredByMetadataId();
			Path path = PhysicalTypeIdentifier.getPath(superCidPhysicalTypeIdentifier);
			String superCidLocalIdentifier = createLocalIdentifier(superCid.getName(), path);
			parent = (EntityMetadata) metadataService.get(superCidLocalIdentifier);
			superCid = superCid.getSuperclass();
		}
		
		// We also need the plural
		JavaType javaType = EntityMetadata.getJavaType(metadataIdentificationString);
		Path path = EntityMetadata.getPath(metadataIdentificationString);
		String key = PluralMetadata.createIdentifier(javaType, path);
		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(key);
		if (pluralMetadata == null) {
			// Can't acquire the plural
			return null;
		}
		metadataDependencyRegistry.registerDependency(key, metadataIdentificationString);
	
		// If the project itself changes, we want a chance to refresh this item
		metadataDependencyRegistry.registerDependency(ProjectMetadata.getProjectIdentifier(), metadataIdentificationString);
		
		// We do not need to monitor the parent, as any changes to the java type associated with the parent will trickle down to
		// the governing java type
		
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());

		MemberDetails memberDetails = getMemberDetails(governorPhysicalTypeMetadata);

		List<Identifier> identifierServiceResult = getIdentifiersForType(javaType);
		return new EntityMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, parent, projectMetadata, annotationValues, noArgConstructor, pluralMetadata.getPlural(), memberDetails, identifierServiceResult);
	}
	
	public String getItdUniquenessFilenameSuffix() {
		return "Entity";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = EntityMetadata.getJavaType(metadataIdentificationString);
		Path path = EntityMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return EntityMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return EntityMetadata.getMetadataIdentifierType();
	}

	/**
	 * Allows disabling the automated creation of no arg constructors. This might be appropriate, for example, if another add-on is providing
	 * more sophisticated constructor creation facilities.
	 * 
	 * @param noArgConstructor automatically causes any {@link EntityMetadata} to have a no-arg constructor added if there are zero no-arg
	 * constructors defined in the {@link PhysicalTypeMetadata} (defaults to true).
	 */
	public void setNoArgConstructor(boolean noArgConstructor) {
		this.noArgConstructor = noArgConstructor;
	}

	public EntityAnnotationValues getAnnotationValues(final JavaType javaType) {
		Assert.notNull(javaType, "JavaType required");
		final MemberHoldingTypeDetailsMetadataItem<?> governor = (MemberHoldingTypeDetailsMetadataItem<?>) metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType));
		if (MemberFindingUtils.getAnnotationOfType(governor, ENTITY_ANNOTATION) == null) {
			// The type can't be found or it's not annotated with @RooEntity
			return null;
		}
		return new EntityAnnotationValues(governor);
	}
}
