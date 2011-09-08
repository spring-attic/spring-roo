package org.springframework.roo.addon.dod;

import static org.springframework.roo.model.RooJavaType.ROO_DATA_ON_DEMAND;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerCustomDataKeys;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.shell.NaturalOrderComparator;

/**
 * Implementation of {@link DataOnDemandMetadataProvider}.
 * 
 * @author Ben Alex
 * @author Greg Turnquist
 * @author Andrew Swan
 * @since 1.0
 */
@Component(immediate = true)
@Service
public final class DataOnDemandMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider implements DataOnDemandMetadataProvider {
	
	// Constants
	private static final String FLUSH_METHOD = PersistenceCustomDataKeys.FLUSH_METHOD.name();
	private static final String PERSIST_METHOD = PersistenceCustomDataKeys.PERSIST_METHOD.name();
	
	// Fields
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	@Reference private LayerService layerService;
	@Reference private PersistenceMemberLocator persistenceMemberLocator;
	
	private final Map<JavaType, String> entityToDodMidMap = new LinkedHashMap<JavaType, String>();
	private final Map<String, JavaType> dodMidToEntityMap = new LinkedHashMap<String, JavaType>();
	
	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		
		// DOD classes are @Configurable because they may need DI of other DOD classes that provide M:1 relationships
		configurableMetadataProvider.addMetadataTrigger(ROO_DATA_ON_DEMAND);
		addMetadataTrigger(ROO_DATA_ON_DEMAND);
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		configurableMetadataProvider.removeMetadataTrigger(ROO_DATA_ON_DEMAND);
		removeMetadataTrigger(ROO_DATA_ON_DEMAND);
	}
	
	protected String getLocalMidToRequest(ItdTypeDetails itdTypeDetails) {
		// Determine the governor for this ITD, and whether any DOD metadata is even hoping to hear about changes to that JavaType and its ITDs
		JavaType governor = itdTypeDetails.getName();
		
		Object layerTypeValue = itdTypeDetails.getGovernor().getCustomData().get(LayerCustomDataKeys.LAYER_TYPE);
		if (layerTypeValue != null) {
			@SuppressWarnings("unchecked")
			List<JavaType> domainTypes = (List<JavaType>) layerTypeValue;
			for (JavaType type : domainTypes) {
				String localMidType = entityToDodMidMap.get(type);
				if (localMidType != null) {
					return localMidType;
				}
			}
		}
		
		String localMid = entityToDodMidMap.get(governor);
		if (localMid == null) {
			// No DOD is interested in this JavaType, so let's move on
			return null;
		}
		
		// We have some DOD metadata, so let's check if we care if any methods match our requirements
		for (MethodMetadata method : itdTypeDetails.getDeclaredMethods()) {
			if (BeanInfoUtils.isMutatorMethod(method)) {
				// A DOD cares about the JavaType, and an ITD offers a method likely of interest, so let's formally trigger it to run.
				// Note that it will re-scan and discover this ITD, and register a direct dependency on it for the future.
				return localMid;
			}
		}
		
		return null;
	}
	
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We need to parse the annotation, which we expect to be present
		DataOnDemandAnnotationValues annotationValues = new DataOnDemandAnnotationValues(governorPhysicalTypeMetadata);
		if (!annotationValues.isAnnotationFound() || annotationValues.getEntity() == null) {
			return null;
		}
		
		// Remember that this entity JavaType matches up with this DOD's metadata identification string
		// Start by clearing the previous association
		JavaType oldEntity = dodMidToEntityMap.get(metadataIdentificationString);
		if (oldEntity != null) {
			entityToDodMidMap.remove(oldEntity);
		}
		entityToDodMidMap.put(annotationValues.getEntity(), metadataIdentificationString);
		dodMidToEntityMap.put(metadataIdentificationString, annotationValues.getEntity());
		
		JavaType entity = annotationValues.getEntity();
		
		MemberDetails memberDetails = getMemberDetails(entity);
		if (memberDetails == null) {
			return null;
		}
		
		MemberHoldingTypeDetails persistenceMemberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(memberDetails, PersistenceCustomDataKeys.PERSISTENT_TYPE);
		if (persistenceMemberHoldingTypeDetails == null) {
			return null;
		}

		JavaType idType = persistenceMemberLocator.getIdentifierType(entity);
		if (idType == null) {
			return null;
		}

		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(persistenceMemberHoldingTypeDetails.getDeclaredByMetadataId(), metadataIdentificationString);
		
		// Get the additions to make for each required method
		final MethodParameter fromParameter = new MethodParameter(JavaType.INT_PRIMITIVE, "from");
		final MethodParameter toParameter = new MethodParameter(JavaType.INT_PRIMITIVE, "to");
		final MemberTypeAdditions findEntriesMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, PersistenceCustomDataKeys.FIND_ENTRIES_METHOD.name(), entity, idType, LayerType.HIGHEST.getPosition(), fromParameter, toParameter);
		MemberTypeAdditions findMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, PersistenceCustomDataKeys.FIND_METHOD.name(), entity, idType, LayerType.HIGHEST.getPosition(), new MethodParameter(idType, "id"));
		final MethodParameter entityParameter = new MethodParameter(entity, "obj");
		MemberTypeAdditions flushMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, FLUSH_METHOD, entity, idType, LayerType.HIGHEST.getPosition(), entityParameter);
		MethodMetadata identifierAccessor = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD);
		MemberTypeAdditions persistMethodAdditions = layerService.getMemberTypeAdditions(metadataIdentificationString, PERSIST_METHOD, entity, idType, LayerType.HIGHEST.getPosition(), entityParameter);
		
		if (findEntriesMethod == null || findMethodAdditions == null || identifierAccessor == null || persistMethodAdditions == null) {
			return null;
		}
		
		// Identify all the mutators we care about on the entity
		Map<MethodMetadata, CollaboratingDataOnDemandMetadataHolder> locatedMutators = getLocatedMutators(memberDetails, metadataIdentificationString);
		
		// Get the embedded identifier metadata holder - may be null if no embedded identifier exists
		EmbeddedIdentifierHolder embeddedIdentifierHolder = getEmbeddedIdentifierHolder(memberDetails, metadataIdentificationString);
		
		// Get the list of embedded metadata holders - may be an empty list if no embedded identifier exists
		List<EmbeddedHolder> embeddedHolders = getEmbeddedHolders(memberDetails, metadataIdentificationString);

		return new DataOnDemandMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, identifierAccessor, findMethodAdditions, findEntriesMethod, persistMethodAdditions, flushMethod, locatedMutators, entity, idType, embeddedIdentifierHolder, embeddedHolders);
	}

	private Map<MethodMetadata, CollaboratingDataOnDemandMetadataHolder> getLocatedMutators(MemberDetails memberDetails, String metadataIdentificationString) {
		Map<MethodMetadata, CollaboratingDataOnDemandMetadataHolder> locatedMutators = new LinkedHashMap<MethodMetadata, CollaboratingDataOnDemandMetadataHolder>();

		List<MethodMetadata> mutatorMethods = MemberFindingUtils.getMethods(memberDetails);
		// To avoid unnecessary rewriting of the DoD ITD we sort the mutators by method name to provide a consistent ordering
		Collections.sort(mutatorMethods, new NaturalOrderComparator<MethodMetadata>() {
			@Override
			protected String stringify(MethodMetadata object) {
				return object.getMethodName().getSymbolName();
			}
		});

		// Add the methods we care to the locatedMutators
		for (MethodMetadata method : mutatorMethods) {
			if (!BeanInfoUtils.isMutatorMethod(method)) {
				continue;
			}
			
			JavaSymbolName propertyName = BeanInfoUtils.getPropertyNameForJavaBeanMethod(method);
			FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, propertyName);
			if (field == null) {
				continue;
			}
			
			Set<Object> fieldCustomDataKeys = field.getCustomData().keySet();

			// Never include id or version fields (they shouldn't normally have a mutator anyway, but the user might have added one), or embedded types
			if (fieldCustomDataKeys.contains(PersistenceCustomDataKeys.IDENTIFIER_FIELD) || fieldCustomDataKeys.contains(PersistenceCustomDataKeys.EMBEDDED_ID_FIELD) || fieldCustomDataKeys.contains(PersistenceCustomDataKeys.EMBEDDED_FIELD) || fieldCustomDataKeys.contains(PersistenceCustomDataKeys.VERSION_FIELD)) {
				continue;
			}

			// Never include persistence transient fields
			if (fieldCustomDataKeys.contains(PersistenceCustomDataKeys.TRANSIENT_FIELD)) {
				continue;
			}

			// Never include any sort of collection; user has to make such entities by hand
			if (field.getFieldType().isCommonCollectionType() || fieldCustomDataKeys.contains(PersistenceCustomDataKeys.ONE_TO_MANY_FIELD) || fieldCustomDataKeys.contains(PersistenceCustomDataKeys.MANY_TO_MANY_FIELD)) {
				continue;
			}
			
			// Look up collaborating metadata
			DataOnDemandMetadata otherMetadata = locateCollaboratingMetadata(metadataIdentificationString, field);
			locatedMutators.put(method, new CollaboratingDataOnDemandMetadataHolder(field, otherMetadata));
			
			// Track any changes to that method (eg it goes away)
			metadataDependencyRegistry.registerDependency(method.getDeclaredByMetadataId(), metadataIdentificationString);
		}
		
		return locatedMutators;
	}

	private EmbeddedIdentifierHolder getEmbeddedIdentifierHolder(MemberDetails memberDetails, String metadataIdentificationString) {
		final List<FieldMetadata> identifierFields = new ArrayList<FieldMetadata>();
		List<FieldMetadata> fields = MemberFindingUtils.getFieldsWithTag(memberDetails, PersistenceCustomDataKeys.EMBEDDED_ID_FIELD);
		if (fields.isEmpty()) {
			return null;
		}
		FieldMetadata embeddedIdentifierField = fields.get(0);
		MemberDetails identifierMemberDetails = getMemberDetails(embeddedIdentifierField.getFieldType());
		if (identifierMemberDetails == null) {
			return null;
		}
		
		for (FieldMetadata field : MemberFindingUtils.getFields(identifierMemberDetails)) {
			if (!(Modifier.isStatic(field.getModifier()) || Modifier.isFinal(field.getModifier()) || Modifier.isTransient(field.getModifier()))) {
				metadataDependencyRegistry.registerDependency(field.getDeclaredByMetadataId(), metadataIdentificationString);
				identifierFields.add(field);
			}
		}
		
		return new EmbeddedIdentifierHolder(embeddedIdentifierField, identifierFields);
	}
	
	private List<EmbeddedHolder> getEmbeddedHolders(MemberDetails memberDetails, String metadataIdentificationString) {
		final List<EmbeddedHolder> embeddedHolders = new ArrayList<EmbeddedHolder>();

		List<FieldMetadata> embeddedFields = MemberFindingUtils.getFieldsWithTag(memberDetails, PersistenceCustomDataKeys.EMBEDDED_FIELD);
		if (embeddedFields.isEmpty()) {
			return embeddedHolders;
		}
		
		for (FieldMetadata embeddedField : embeddedFields) {
			MemberDetails embeddedMemberDetails = getMemberDetails(embeddedField.getFieldType());
			if (embeddedMemberDetails == null) {
				continue;
			}

			final List<FieldMetadata> fields = new ArrayList<FieldMetadata>();

			for (FieldMetadata field : MemberFindingUtils.getFields(embeddedMemberDetails)) {
				if (!(Modifier.isStatic(field.getModifier()) || Modifier.isFinal(field.getModifier()) || Modifier.isTransient(field.getModifier()))) {
					metadataDependencyRegistry.registerDependency(field.getDeclaredByMetadataId(), metadataIdentificationString);
					fields.add(field);
				}
			}
			embeddedHolders.add(new EmbeddedHolder(embeddedField, fields));
		}

		return embeddedHolders;
	}

	/**
	 * Returns the data-on-demand metadata for the entity that's the target of
	 * the given reference field. Registers a metadata dependency on that entity
	 * type if appropriate.
	 * 
	 * @param metadataIdentificationString
	 * @param field
	 * @return <code>null</code> if it's not an n:1 or 1:1 field, or the DoD
	 * metadata is simply not available
	 */
	private DataOnDemandMetadata locateCollaboratingMetadata(String metadataIdentificationString, FieldMetadata field) {
		// Check field type to ensure it is a persistent type and is not abstract
		MemberDetails memberDetails = getMemberDetails(field.getFieldType());
		if (memberDetails == null) {
			return null;
		}
		
		MemberHoldingTypeDetails persistenceMemberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(memberDetails, PersistenceCustomDataKeys.PERSISTENT_TYPE);
		if (persistenceMemberHoldingTypeDetails == null) {
			return null;
		}
		
		// Check field for @ManyToOne or @OneToOne annotation
		if (!field.getCustomData().keySet().contains(PersistenceCustomDataKeys.MANY_TO_ONE_FIELD) && !field.getCustomData().keySet().contains(PersistenceCustomDataKeys.ONE_TO_ONE_FIELD)) {
			return null;
		}
		
		// Look up the metadata we are relying on
		String otherProvider = DataOnDemandMetadata.createIdentifier(new JavaType(field.getFieldType() + "DataOnDemand"), Path.SRC_TEST_JAVA);
		if (otherProvider.equals(metadataIdentificationString)) {
			return null; // Ignore self-references
		}
		
		// The field points to a single instance of another domain entity - register for changes to it
		metadataDependencyRegistry.registerDependency(persistenceMemberHoldingTypeDetails.getDeclaredByMetadataId(), metadataIdentificationString);
		metadataDependencyRegistry.registerDependency(otherProvider, metadataIdentificationString);
		return (DataOnDemandMetadata) metadataService.get(otherProvider);
	}

	public String getItdUniquenessFilenameSuffix() {
		return "DataOnDemand";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = DataOnDemandMetadata.getJavaType(metadataIdentificationString);
		Path path = DataOnDemandMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return DataOnDemandMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return DataOnDemandMetadata.getMetadataIdentiferType();
	}
}
