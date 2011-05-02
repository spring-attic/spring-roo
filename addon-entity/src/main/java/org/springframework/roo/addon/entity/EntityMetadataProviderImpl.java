package org.springframework.roo.addon.entity;

import java.util.ArrayList;
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
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.customdata.taggers.ConstructorMatcher;
import org.springframework.roo.classpath.customdata.taggers.FieldMatcher;
import org.springframework.roo.classpath.customdata.taggers.MethodMatcher;
import org.springframework.roo.classpath.customdata.taggers.CustomDataKeyDecorator;
import org.springframework.roo.classpath.customdata.taggers.TypeMatcher;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;

/**
 * Provides {@link EntityMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public final class EntityMetadataProviderImpl extends AbstractIdentifierServiceAwareMetadataProvider implements EntityMetadataProvider {
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	@Reference private PluralMetadataProvider pluralMetadataProvider;
	@Reference private CustomDataKeyDecorator customDataKeyDecorator;
	private boolean noArgConstructor = true;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		configurableMetadataProvider.addMetadataTrigger(new JavaType(RooEntity.class.getName()));
		pluralMetadataProvider.addMetadataTrigger(new JavaType(RooEntity.class.getName()));
		addMetadataTrigger(new JavaType(RooEntity.class.getName()));
		registerMatchers();
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		configurableMetadataProvider.removeMetadataTrigger(new JavaType(RooEntity.class.getName()));
		pluralMetadataProvider.removeMetadataTrigger(new JavaType(RooEntity.class.getName()));
		removeMetadataTrigger(new JavaType(RooEntity.class.getName()));
		unregisterMatchers();
	}

	private void registerMatchers() {
		customDataKeyDecorator.registerMatcher(getClass().getName(), new TypeMatcher(PersistenceCustomDataKeys.PERSISTENT_TYPE, "org.springframework.roo.addon.entity.EntityMetadata"));
		customDataKeyDecorator.registerMatcher(getClass().getName(), new TypeMatcher(PersistenceCustomDataKeys.IDENTIFIER_TYPE, "org.springframework.roo.addon.entity.IdentifierMetadata"));

		customDataKeyDecorator.registerMatcher(getClass().getName(), new ConstructorMatcher(PersistenceCustomDataKeys.NO_ARG_CONSTRUCTOR, new ArrayList<JavaType>()));

		AnnotationMetadata idAnnotation = new AnnotationMetadataBuilder(new JavaType("javax.persistence.Id")).build();
		AnnotationMetadata embeddedIdAnnotation = new AnnotationMetadataBuilder(new JavaType("javax.persistence.EmbeddedId")).build();
		FieldMatcher idAndEmbeddedIdFieldTagger = new FieldMatcher(PersistenceCustomDataKeys.IDENTIFIER_FIELD, Arrays.asList(idAnnotation, embeddedIdAnnotation));
		customDataKeyDecorator.registerMatcher(getClass().getName(), new MethodMatcher(Arrays.asList(idAndEmbeddedIdFieldTagger), PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD, true));
		customDataKeyDecorator.registerMatcher(getClass().getName(), new MethodMatcher(Arrays.asList(idAndEmbeddedIdFieldTagger), PersistenceCustomDataKeys.IDENTIFIER_MUTATOR_METHOD, false));

		FieldMatcher identifierFieldTagger = new FieldMatcher(PersistenceCustomDataKeys.IDENTIFIER_FIELD, Arrays.asList(idAnnotation));
		customDataKeyDecorator.registerMatcher(getClass().getName(), identifierFieldTagger);

		FieldMatcher embeddedIdFieldTagger = new FieldMatcher(PersistenceCustomDataKeys.EMBEDDED_ID_FIELD, Arrays.asList(embeddedIdAnnotation));
		customDataKeyDecorator.registerMatcher(getClass().getName(), embeddedIdFieldTagger);

		AnnotationMetadata annotationMetadata = new AnnotationMetadataBuilder(new JavaType("javax.persistence.Version")).build();
		FieldMatcher versionFieldTagger = new FieldMatcher(PersistenceCustomDataKeys.VERSION_FIELD, Arrays.asList(annotationMetadata));
		customDataKeyDecorator.registerMatcher(getClass().getName(), versionFieldTagger);
		customDataKeyDecorator.registerMatcher(getClass().getName(), new MethodMatcher(Arrays.asList(versionFieldTagger), PersistenceCustomDataKeys.VERSION_ACCESSOR_METHOD, true));
		customDataKeyDecorator.registerMatcher(getClass().getName(), new MethodMatcher(Arrays.asList(versionFieldTagger), PersistenceCustomDataKeys.VERSION_MUTATOR_METHOD, false));

		customDataKeyDecorator.registerMatcher(getClass().getName(), new MethodMatcher(PersistenceCustomDataKeys.PERSIST_METHOD, new JavaType(RooEntity.class.getName()), new JavaSymbolName("persistMethod"), "persist"));
		customDataKeyDecorator.registerMatcher(getClass().getName(), new MethodMatcher(PersistenceCustomDataKeys.REMOVE_METHOD, new JavaType(RooEntity.class.getName()), new JavaSymbolName("removeMethod"), "remove"));
		customDataKeyDecorator.registerMatcher(getClass().getName(), new MethodMatcher(PersistenceCustomDataKeys.FLUSH_METHOD, new JavaType(RooEntity.class.getName()), new JavaSymbolName("flushMethod"), "flush"));
		customDataKeyDecorator.registerMatcher(getClass().getName(), new MethodMatcher(PersistenceCustomDataKeys.CLEAR_METHOD, new JavaType(RooEntity.class.getName()), new JavaSymbolName("clearMethod"), "clear"));
		customDataKeyDecorator.registerMatcher(getClass().getName(), new MethodMatcher(PersistenceCustomDataKeys.MERGE_METHOD, new JavaType(RooEntity.class.getName()), new JavaSymbolName("mergeMethod"), "merge"));

		customDataKeyDecorator.registerMatcher(getClass().getName(), new MethodMatcher(PersistenceCustomDataKeys.COUNT_ALL_METHOD, new JavaType(RooEntity.class.getName()), new JavaSymbolName("countMethod"), "count", true, false));
		customDataKeyDecorator.registerMatcher(getClass().getName(), new MethodMatcher(PersistenceCustomDataKeys.FIND_ALL_METHOD, new JavaType(RooEntity.class.getName()), new JavaSymbolName("findAllMethod"), "findAll", true, false));
		customDataKeyDecorator.registerMatcher(getClass().getName(), new MethodMatcher(PersistenceCustomDataKeys.FIND_METHOD, new JavaType(RooEntity.class.getName()), new JavaSymbolName("findMethod"), "find", false, true));
		customDataKeyDecorator.registerMatcher(getClass().getName(), new MethodMatcher(PersistenceCustomDataKeys.FIND_ENTRIES_METHOD, new JavaType(RooEntity.class.getName()), new JavaSymbolName("findEntriesMethod"), "find", false, true, "Entries"));

		customDataKeyDecorator.registerMatcher(getClass().getName(), new FieldMatcher(PersistenceCustomDataKeys.MANY_TO_MANY_FIELD, getAnnotationMetadataList("javax.persistence.ManyToMany")));
		customDataKeyDecorator.registerMatcher(getClass().getName(), new FieldMatcher(PersistenceCustomDataKeys.ENUMERATED_FIELD, getAnnotationMetadataList("javax.persistence.Enumerated")));
		customDataKeyDecorator.registerMatcher(getClass().getName(), new FieldMatcher(PersistenceCustomDataKeys.ONE_TO_MANY_FIELD, getAnnotationMetadataList("javax.persistence.OneToMany")));

		customDataKeyDecorator.registerMatcher(getClass().getName(), new FieldMatcher(PersistenceCustomDataKeys.MANY_TO_ONE_FIELD, getAnnotationMetadataList("javax.persistence.ManyToOne")));
		customDataKeyDecorator.registerMatcher(getClass().getName(), new FieldMatcher(PersistenceCustomDataKeys.ONE_TO_ONE_FIELD, getAnnotationMetadataList("javax.persistence.OneToOne")));
		customDataKeyDecorator.registerMatcher(getClass().getName(), new FieldMatcher(PersistenceCustomDataKeys.LOB_FIELD, getAnnotationMetadataList("javax.persistence.Lob")));
		customDataKeyDecorator.registerMatcher(getClass().getName(), new FieldMatcher(PersistenceCustomDataKeys.TRANSIENT_FIELD, getAnnotationMetadataList("javax.persistence.Transient")));

		customDataKeyDecorator.registerMatcher(getClass().getName(), new FieldMatcher(PersistenceCustomDataKeys.EMBEDDED_FIELD, getAnnotationMetadataList("javax.persistence.Embedded")));
		customDataKeyDecorator.registerMatcher(getClass().getName(), new FieldMatcher(PersistenceCustomDataKeys.COLUMN_FIELD, getAnnotationMetadataList("javax.persistence.Column")));
	}

	private void unregisterMatchers() {
		customDataKeyDecorator.unregisterMatchers(getClass().getName());
	}

	private AnnotationMetadata getAnnotationMetadata(String type) {
		return new AnnotationMetadataBuilder(new JavaType(type)).build();
	}

	private List<AnnotationMetadata> getAnnotationMetadataList(String type) {
		return Arrays.asList(getAnnotationMetadata(type));
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
}
