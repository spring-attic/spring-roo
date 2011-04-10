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
import org.springframework.roo.classpath.customdata.CustomDataPersistenceTags;
import org.springframework.roo.classpath.customdata.taggers.ConstructorTagger;
import org.springframework.roo.classpath.customdata.taggers.FieldTagger;
import org.springframework.roo.classpath.customdata.taggers.MethodTagger;
import org.springframework.roo.classpath.customdata.taggers.TaggerRegistry;
import org.springframework.roo.classpath.customdata.taggers.TypeTagger;
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
	@Reference private TaggerRegistry taggerRegistry;
	
	private boolean noArgConstructor = true;

	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		configurableMetadataProvider.addMetadataTrigger(new JavaType(RooEntity.class.getName()));
		pluralMetadataProvider.addMetadataTrigger(new JavaType(RooEntity.class.getName()));
		addMetadataTrigger(new JavaType(RooEntity.class.getName()));
		helperDotHelp();
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		configurableMetadataProvider.removeMetadataTrigger(new JavaType(RooEntity.class.getName()));
		pluralMetadataProvider.removeMetadataTrigger(new JavaType(RooEntity.class.getName()));
		removeMetadataTrigger(new JavaType(RooEntity.class.getName()));
		helperDotHelpHelp();
	}

	private void helperDotHelp() {
		taggerRegistry.registerTagger(getClass(), new TypeTagger(CustomDataPersistenceTags.PERSISTENT_TYPE, "org.springframework.roo.addon.entity.EntityMetadata"));
		taggerRegistry.registerTagger(getClass(), new TypeTagger(CustomDataPersistenceTags.IDENTIFIER_TYPE, "org.springframework.roo.addon.entity.IdentifierMetadata"));

		taggerRegistry.registerTagger(getClass(), new ConstructorTagger(CustomDataPersistenceTags.NO_ARG_CONSTRUCTOR, new ArrayList<JavaType>()));

		AnnotationMetadata idAnnotation = new AnnotationMetadataBuilder(new JavaType("javax.persistence.Id")).build();
		AnnotationMetadata embeddedIdAnnotation = new AnnotationMetadataBuilder(new JavaType("javax.persistence.EmbeddedId")).build();
		FieldTagger idAndEmbeddedIdFieldTagger = new FieldTagger(CustomDataPersistenceTags.IDENTIFIER_FIELD, Arrays.asList(idAnnotation, embeddedIdAnnotation));
		taggerRegistry.registerTagger(getClass(), new MethodTagger(Arrays.asList(idAndEmbeddedIdFieldTagger), CustomDataPersistenceTags.IDENTIFIER_ACCESSOR_METHOD, true));
		taggerRegistry.registerTagger(getClass(), new MethodTagger(Arrays.asList(idAndEmbeddedIdFieldTagger), CustomDataPersistenceTags.IDENTIFIER_MUTATOR_METHOD, false));

		FieldTagger identifierFieldTagger = new FieldTagger(CustomDataPersistenceTags.IDENTIFIER_FIELD, Arrays.asList(idAnnotation));
		taggerRegistry.registerTagger(getClass(), identifierFieldTagger);

		FieldTagger embeddedIdFieldTagger = new FieldTagger(CustomDataPersistenceTags.EMBEDDED_ID_FIELD, Arrays.asList(embeddedIdAnnotation));
		taggerRegistry.registerTagger(getClass(), embeddedIdFieldTagger);

		AnnotationMetadata annotationMetadata = new AnnotationMetadataBuilder(new JavaType("javax.persistence.Version")).build();
		FieldTagger versionFieldTagger = new FieldTagger(CustomDataPersistenceTags.VERSION_FIELD, Arrays.asList(annotationMetadata));
		taggerRegistry.registerTagger(getClass(), versionFieldTagger);
		taggerRegistry.registerTagger(getClass(), new MethodTagger(Arrays.asList(versionFieldTagger), CustomDataPersistenceTags.VERSION_ACCESSOR_METHOD, true));
		taggerRegistry.registerTagger(getClass(), new MethodTagger(Arrays.asList(versionFieldTagger), CustomDataPersistenceTags.VERSION_MUTATOR_METHOD, false));

		taggerRegistry.registerTagger(getClass(), new MethodTagger(CustomDataPersistenceTags.PERSIST_METHOD, new JavaType(RooEntity.class.getName()), new JavaSymbolName("persistMethod"), "persist"));
		taggerRegistry.registerTagger(getClass(), new MethodTagger(CustomDataPersistenceTags.REMOVE_METHOD, new JavaType(RooEntity.class.getName()), new JavaSymbolName("removeMethod"), "remove"));
		taggerRegistry.registerTagger(getClass(), new MethodTagger(CustomDataPersistenceTags.FLUSH_METHOD, new JavaType(RooEntity.class.getName()), new JavaSymbolName("flushMethod"), "flush"));
		taggerRegistry.registerTagger(getClass(), new MethodTagger(CustomDataPersistenceTags.CLEAR_METHOD, new JavaType(RooEntity.class.getName()), new JavaSymbolName("clearMethod"), "clear"));
		taggerRegistry.registerTagger(getClass(), new MethodTagger(CustomDataPersistenceTags.MERGE_METHOD, new JavaType(RooEntity.class.getName()), new JavaSymbolName("mergeMethod"), "merge"));

		taggerRegistry.registerTagger(getClass(), new MethodTagger(CustomDataPersistenceTags.COUNT_ALL_METHOD, new JavaType(RooEntity.class.getName()), new JavaSymbolName("countMethod"), "count", true, false));
		taggerRegistry.registerTagger(getClass(), new MethodTagger(CustomDataPersistenceTags.FIND_ALL_METHOD, new JavaType(RooEntity.class.getName()), new JavaSymbolName("findAllMethod"), "findAll", true, false));
		taggerRegistry.registerTagger(getClass(), new MethodTagger(CustomDataPersistenceTags.FIND_METHOD, new JavaType(RooEntity.class.getName()), new JavaSymbolName("findMethod"), "find", false, true));
		taggerRegistry.registerTagger(getClass(), new MethodTagger(CustomDataPersistenceTags.FIND_ENTRIES_METHOD, new JavaType(RooEntity.class.getName()), new JavaSymbolName("findEntriesMethod"), "find", false, true, "Entries"));

		taggerRegistry.registerTagger(getClass(), new FieldTagger(CustomDataPersistenceTags.MANY_TO_MANY_FIELD, getAnnotationMetadataList("javax.persistence.ManyToMany")));
		taggerRegistry.registerTagger(getClass(), new FieldTagger(CustomDataPersistenceTags.ENUMERATED_FIELD, getAnnotationMetadataList("javax.persistence.Enumerated")));
		taggerRegistry.registerTagger(getClass(), new FieldTagger(CustomDataPersistenceTags.ONE_TO_MANY_FIELD, getAnnotationMetadataList("javax.persistence.OneToMany")));

		taggerRegistry.registerTagger(getClass(), new FieldTagger(CustomDataPersistenceTags.MANY_TO_ONE_FIELD, getAnnotationMetadataList("javax.persistence.ManyToOne")));
		taggerRegistry.registerTagger(getClass(), new FieldTagger(CustomDataPersistenceTags.ONE_TO_ONE_FIELD, getAnnotationMetadataList("javax.persistence.OneToOne")));
		taggerRegistry.registerTagger(getClass(), new FieldTagger(CustomDataPersistenceTags.LOB_FIELD, getAnnotationMetadataList("javax.persistence.Lob")));
		taggerRegistry.registerTagger(getClass(), new FieldTagger(CustomDataPersistenceTags.TRANSIENT_FIELD, getAnnotationMetadataList("javax.persistence.Transient")));

		taggerRegistry.registerTagger(getClass(), new FieldTagger(CustomDataPersistenceTags.EMBEDDED_FIELD, getAnnotationMetadataList("javax.persistence.Embedded")));
		taggerRegistry.registerTagger(getClass(), new FieldTagger(CustomDataPersistenceTags.COLUMN_FIELD, getAnnotationMetadataList("javax.persistence.Column")));
	}

	private void helperDotHelpHelp() {
		taggerRegistry.unregisterTaggers(getClass());
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

		ClassOrInterfaceTypeDetails cid = (ClassOrInterfaceTypeDetails) governorPhysicalTypeMetadata.getMemberHoldingTypeDetails();
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(this.getClass().getName(), cid);

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
