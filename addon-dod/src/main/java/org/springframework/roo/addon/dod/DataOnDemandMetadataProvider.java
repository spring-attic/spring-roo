package org.springframework.roo.addon.dod;

import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;

/**
 * Provides {@link DataOnDemandMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component(immediate = true)
@Service
public final class DataOnDemandMetadataProvider extends AbstractMemberDiscoveringItdMetadataProvider {
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	private Map<JavaType, String> entityToDodMidMap = new LinkedHashMap<JavaType, String>();
	private Map<String, JavaType> dodMidToEntityMap = new LinkedHashMap<String, JavaType>();
	
	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		
		// DOD classes are @Configurable because they may need DI of other DOD classes that provide M:1 relationships
		configurableMetadataProvider.addMetadataTrigger(new JavaType(RooDataOnDemand.class.getName()));
		addMetadataTrigger(new JavaType(RooDataOnDemand.class.getName()));
	}
	
	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		configurableMetadataProvider.removeMetadataTrigger(new JavaType(RooDataOnDemand.class.getName()));
		removeMetadataTrigger(new JavaType(RooDataOnDemand.class.getName()));
	}
	
	protected String getLocalMidToRequest(ItdTypeDetails itdTypeDetails) {
		// Determine the governor for this ITD, and whether any DOD metadata is even hoping to hear about changes to that JavaType and its ITDs
		JavaType governor = itdTypeDetails.getName();
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
		
		MemberDetails memberDetails = getMemberDetails(annotationValues.getEntity());
		if (memberDetails == null) {
			return null;
		}
		
		MemberHoldingTypeDetails persistenceMemberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(memberDetails, PersistenceCustomDataKeys.PERSISTENT_TYPE);
		if (persistenceMemberHoldingTypeDetails == null) {
			return null;
		}

		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(persistenceMemberHoldingTypeDetails.getDeclaredByMetadataId(), metadataIdentificationString);
		
		MethodMetadata findEntriesMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.FIND_ENTRIES_METHOD);
		MethodMetadata persistMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.PERSIST_METHOD);
		MethodMetadata flushMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.FLUSH_METHOD);
		MethodMetadata findMethod = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.FIND_METHOD);
		MethodMetadata identifierAccessor = MemberFindingUtils.getMostConcreteMethodWithTag(memberDetails, PersistenceCustomDataKeys.IDENTIFIER_ACCESSOR_METHOD);
		if (findEntriesMethod == null || persistMethod == null || flushMethod == null || findMethod == null || identifierAccessor == null) {
			return null;
		}
		
		// Identify all the mutators we care about on the entity
		Map<MethodMetadata, CollaboratingDataOnDemandMetadataHolder> locatedMutators = getLocatedMutators(memberDetails, metadataIdentificationString);
		
		// Get the embedded identifier metadata holder - may be null if no embedded identifier exists
		EmbeddedIdentifierHolder embeddedIdentifierHolder = getEmbeddedIdentifierHolder(memberDetails, metadataIdentificationString);

		return new DataOnDemandMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, identifierAccessor, findMethod, findEntriesMethod, persistMethod, flushMethod, locatedMutators, persistenceMemberHoldingTypeDetails.getName(), embeddedIdentifierHolder);
	}

	private Map<MethodMetadata, CollaboratingDataOnDemandMetadataHolder> getLocatedMutators(MemberDetails memberDetails, String metadataIdentificationString) {
		Map<MethodMetadata, CollaboratingDataOnDemandMetadataHolder> locatedMutators = new LinkedHashMap<MethodMetadata, CollaboratingDataOnDemandMetadataHolder>();

		// Add the methods we care to the locatedMutators
		for (MethodMetadata method : MemberFindingUtils.getMethods(memberDetails)) {
			if (!BeanInfoUtils.isMutatorMethod(method)) {
				continue;
			}
			
			JavaSymbolName propertyName = BeanInfoUtils.getPropertyNameForJavaBeanMethod(method);
			FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, propertyName);
			if (field == null) {
				continue;
			}
			
			Set<Object> fieldCustomDataKeys = field.getCustomData().keySet();

			// Never include id or version fields (they shouldn't normally have a mutator anyway, but the user might have added one)
			if (fieldCustomDataKeys.contains(PersistenceCustomDataKeys.IDENTIFIER_FIELD) || fieldCustomDataKeys.contains(PersistenceCustomDataKeys.EMBEDDED_ID_FIELD) || fieldCustomDataKeys.contains(PersistenceCustomDataKeys.VERSION_FIELD)) {
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
		List<FieldMetadata> identifierFields = new LinkedList<FieldMetadata>();
		List<FieldMetadata> fields = MemberFindingUtils.getFieldsWithTag(memberDetails, PersistenceCustomDataKeys.EMBEDDED_ID_FIELD);
		if (fields.isEmpty()) {
			return null;
		}
		FieldMetadata embeddedIdentifierField = fields.get(0);
		MemberDetails identifierMemberDetails = getMemberDetails(embeddedIdentifierField.getFieldType());
		if (identifierMemberDetails == null) {
			return null;
		}
		
		MemberHoldingTypeDetails identifierMemberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(identifierMemberDetails, PersistenceCustomDataKeys.IDENTIFIER_TYPE);
		if (identifierMemberHoldingTypeDetails == null) {
			return null;
		}
		
		for (FieldMetadata field : MemberFindingUtils.getFields(identifierMemberDetails)) {
			if (!(Modifier.isStatic(field.getModifier()) || Modifier.isFinal(field.getModifier()) || Modifier.isTransient(field.getModifier()))) {
				metadataDependencyRegistry.registerDependency(field.getDeclaredByMetadataId(), metadataIdentificationString);
				identifierFields.add(field);
			}
		}
		List<ConstructorMetadata> constructors = MemberFindingUtils.getConstructors(identifierMemberDetails);
		for (ConstructorMetadata constructor : constructors) {
			metadataDependencyRegistry.registerDependency(constructor.getDeclaredByMetadataId(), metadataIdentificationString);
			if (hasExactFields(constructor, identifierFields)) {
				return new EmbeddedIdentifierHolder(embeddedIdentifierField, identifierFields, constructor);
			}
		}
		return null;
	}
	
	private boolean hasExactFields(ConstructorMetadata constructor, List<FieldMetadata> identifierFields) {
		List<JavaType> parameterTypes = AnnotatedJavaType.convertFromAnnotatedJavaTypes(constructor.getParameterTypes());
		List<JavaType> fieldTypes = new LinkedList<JavaType> ();
		List<JavaSymbolName> fieldNames = new LinkedList<JavaSymbolName>();
		for (FieldMetadata identifierField : identifierFields) {
			fieldTypes.add(identifierField.getFieldType());
			fieldNames.add(identifierField.getFieldName());
		}
		return parameterTypes.size() == identifierFields.size() && parameterTypes.containsAll(fieldTypes) && constructor.getParameterNames().containsAll(fieldNames);
	}

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
			return null;
		}
		
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
