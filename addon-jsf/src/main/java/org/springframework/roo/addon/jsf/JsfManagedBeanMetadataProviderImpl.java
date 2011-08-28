package org.springframework.roo.addon.jsf;

import static org.springframework.roo.model.RooJavaType.ROO_JSF_MANAGED_BEAN;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.CustomDataBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.Pair;
import org.springframework.roo.support.util.PairList;

/**
 * Implementation of {@link JsfManagedBeanMetadataProvider}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component(immediate = true) 
@Service 
public final class JsfManagedBeanMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider implements JsfManagedBeanMetadataProvider {
	
	// Constants
	private static final String COUNT_ALL_METHOD = PersistenceCustomDataKeys.COUNT_ALL_METHOD.name();
	private static final String DELETE_METHOD = PersistenceCustomDataKeys.REMOVE_METHOD.name();
	private static final String FIND_METHOD = PersistenceCustomDataKeys.FIND_METHOD.name();
	private static final String FIND_ALL_METHOD = PersistenceCustomDataKeys.FIND_ALL_METHOD.name();
	private static final String FIND_ENTRIES_METHOD = PersistenceCustomDataKeys.FIND_ENTRIES_METHOD.name();
	private static final String MERGE_METHOD = PersistenceCustomDataKeys.MERGE_METHOD.name();
	private static final String PERSIST_METHOD = PersistenceCustomDataKeys.PERSIST_METHOD.name();
	private static final int LAYER_POSITION = LayerType.HIGHEST.getPosition();

	// Fields
	@Reference private LayerService layerService;
	@Reference private PersistenceMemberLocator persistenceMemberLocator;
	private Map<JavaType, String> entityToManagedBeandMidMap = new LinkedHashMap<JavaType, String>();
	private Map<String, JavaType> managedBeanMidToEntityMap = new LinkedHashMap<String, JavaType>();


	protected void activate(ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_JSF_MANAGED_BEAN);
	}

	protected void deactivate(ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_JSF_MANAGED_BEAN);
	}

	protected String getLocalMidToRequest(ItdTypeDetails itdTypeDetails) {
		// Determine the governor for this ITD, and whether any metadata is even hoping to hear about changes to that JavaType and its ITDs
		String localMid = entityToManagedBeandMidMap.get(itdTypeDetails.getName());
		return localMid == null ? null : localMid;
	}

	protected ItdTypeDetailsProvidingMetadataItem getMetadata(String metadataIdentificationString, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, String itdFilename) {
		// We need to parse the annotation, which we expect to be present
		JsfManagedBeanAnnotationValues annotationValues = new JsfManagedBeanAnnotationValues(governorPhysicalTypeMetadata);
		JavaType entityType = annotationValues.getEntity();
		if (!annotationValues.isAnnotationFound() || entityType == null) {
			return null;
		}

		// Lookup the entity's metadata
		MemberDetails memberDetails = getMemberDetails(entityType);
		if (memberDetails == null) {
			return null;
		}

		MemberHoldingTypeDetails persistenceMemberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(memberDetails, PersistenceCustomDataKeys.PERSISTENT_TYPE);
		if (persistenceMemberHoldingTypeDetails == null) {
			return null;
		}

		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(persistenceMemberHoldingTypeDetails.getDeclaredByMetadataId(), metadataIdentificationString);

		// Remember that this entity JavaType matches up with this metadata identification string
		// Start by clearing the previous association
		JavaType oldEntity = managedBeanMidToEntityMap.get(metadataIdentificationString);
		if (oldEntity != null) {
			entityToManagedBeandMidMap.remove(oldEntity);
		}
		entityToManagedBeandMidMap.put(entityType, metadataIdentificationString);
		managedBeanMidToEntityMap.put(metadataIdentificationString, entityType);

		PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(entityType, Path.SRC_MAIN_JAVA));
		Assert.notNull(pluralMetadata, "Could not determine plural for '" + entityType.getSimpleTypeName() + "'");
		String plural = pluralMetadata.getPlural();

		Map<String, MemberTypeAdditions> crudAdditions = getCrudAdditions(entityType, memberDetails, metadataIdentificationString);
		Map<FieldMetadata, MethodMetadata> locatedFieldsAndAccessors = locateFieldsAndAccessors(entityType, memberDetails, metadataIdentificationString);

		return new JsfManagedBeanMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, memberDetails, plural, crudAdditions, locatedFieldsAndAccessors);
	}

	@SuppressWarnings("unchecked") 
	private Map<String, MemberTypeAdditions> getCrudAdditions(final JavaType javaType, final MemberDetails memberDetails, String metadataIdentificationString) {
		Map<String, MemberTypeAdditions> additions = new HashMap<String, MemberTypeAdditions>();
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA), metadataIdentificationString);
		List<FieldMetadata> idFields = persistenceMemberLocator.getIdentifierFields(javaType);
		if (idFields.isEmpty()) {
			return additions;
		}
		FieldMetadata identifierField = idFields.get(0);
		JavaType idType = persistenceMemberLocator.getIdentifierType(javaType);
		if (idType == null) {
			return additions;
		}
		metadataDependencyRegistry.registerDependency(identifierField.getDeclaredByMetadataId(), metadataIdentificationString);

		JavaSymbolName entityName = JavaSymbolName.getReservedWordSaveName(javaType);
		final Pair<JavaType, JavaSymbolName> entityParameter = new Pair<JavaType, JavaSymbolName>(javaType, entityName);
		final Pair<JavaType, JavaSymbolName> idParameter = new Pair<JavaType, JavaSymbolName>(idType, new JavaSymbolName("id"));
		final PairList<JavaType, JavaSymbolName> findEntriesParameters = new PairList<JavaType, JavaSymbolName>(Arrays.asList(JavaType.INT_PRIMITIVE, JavaType.INT_PRIMITIVE), Arrays.asList(new JavaSymbolName("firstResult"), new JavaSymbolName("sizeNo")));

		MemberTypeAdditions persistMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, PERSIST_METHOD, javaType, idType, LAYER_POSITION, entityParameter);
		MemberTypeAdditions removeMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, DELETE_METHOD, javaType, idType, LAYER_POSITION, entityParameter);
		MemberTypeAdditions mergeMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, MERGE_METHOD, javaType, idType, LAYER_POSITION, entityParameter);
		MemberTypeAdditions findAllMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, FIND_ALL_METHOD, javaType, idType, LAYER_POSITION);
		MemberTypeAdditions findMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, FIND_METHOD, javaType, idType, LAYER_POSITION, idParameter);
		MemberTypeAdditions countMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, COUNT_ALL_METHOD, javaType, idType, LAYER_POSITION);
		MemberTypeAdditions findEntriesMethod = layerService.getMemberTypeAdditions(metadataIdentificationString, FIND_ENTRIES_METHOD, javaType, idType, LAYER_POSITION, findEntriesParameters.toArray());

		additions.put(COUNT_ALL_METHOD, countMethod);
		additions.put(DELETE_METHOD, removeMethod);
		additions.put(FIND_METHOD, findMethod);
		additions.put(FIND_ALL_METHOD, findAllMethod);
		additions.put(FIND_ENTRIES_METHOD, findEntriesMethod);
		additions.put(MERGE_METHOD, mergeMethod);
		additions.put(PERSIST_METHOD, persistMethod);
		return Collections.unmodifiableMap(additions);
	}
	
	private Map<FieldMetadata, MethodMetadata> locateFieldsAndAccessors(JavaType entityType, MemberDetails memberDetails, String metadataIdentificationString) {
		Map<FieldMetadata, MethodMetadata> locatedFieldsAndAccessors = new LinkedHashMap<FieldMetadata, MethodMetadata>();
		
		MethodMetadata identifierAccessor = persistenceMemberLocator.getIdentifierAccessor(entityType);
		MethodMetadata versionAccessor = persistenceMemberLocator.getVersionAccessor(entityType);

		int counter = 0;
		for (MethodMetadata method : MemberFindingUtils.getMethods(memberDetails)) {
			if (!BeanInfoUtils.isAccessorMethod(method)) {
				continue;
			}
			if (isPersistenceIdentifierOrVersionMethod(method, identifierAccessor, versionAccessor)) {
				continue;
			}
			FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, BeanInfoUtils.getPropertyNameForJavaBeanMethod(method));
			if (field == null) {
				continue;
			}
			metadataDependencyRegistry.registerDependency(field.getDeclaredByMetadataId(), metadataIdentificationString);

			if (counter < 4 && isFieldOfInterest(field)) {
				counter++;
				CustomDataBuilder customDataBuilder = new CustomDataBuilder();
				customDataBuilder.put("converterField", "true");
				FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(field);
				fieldBuilder.append(customDataBuilder.build());
				field = fieldBuilder.build();
			}
			
			locatedFieldsAndAccessors.put(field, method);
		}
		return locatedFieldsAndAccessors;
	}

	private boolean isPersistenceIdentifierOrVersionMethod(MethodMetadata method, MethodMetadata idMethod, MethodMetadata versionMethod) {
		Assert.notNull(method, "Method metadata required");
		return (idMethod != null && method.getMethodName().equals(idMethod.getMethodName())) || (versionMethod != null && method.getMethodName().equals(versionMethod.getMethodName()));
	}
	
	private boolean isFieldOfInterest(FieldMetadata field) {
		JavaType fieldType = field.getFieldType();
		if (fieldType.isCommonCollectionType() || fieldType.isArray() // Exclude collections and arrays
				|| isApplicationType(fieldType) // Exclude references to other domain objects as they are too verbose
				|| fieldType.equals(JavaType.BOOLEAN_PRIMITIVE) || fieldType.equals(JavaType.BOOLEAN_OBJECT) // Exclude boolean values as they would not be meaningful in this presentation
				|| fieldType.equals(JavaType.BYTE_ARRAY_PRIMITIVE) // Exclude byte[] fields
				|| field.getCustomData().keySet().contains(PersistenceCustomDataKeys.EMBEDDED_FIELD) /* Not interested in embedded types */) {
			return false;
		}
		return true;
	}

	public boolean isApplicationType(JavaType javaType) {
		Assert.notNull(javaType, "Java type required");
		return metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA)) != null;
	}

	public String getItdUniquenessFilenameSuffix() {
		return "ManagedBean";
	}

	protected String getGovernorPhysicalTypeIdentifier(String metadataIdentificationString) {
		JavaType javaType = JsfManagedBeanMetadata.getJavaType(metadataIdentificationString);
		Path path = JsfManagedBeanMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	protected String createLocalIdentifier(JavaType javaType, Path path) {
		return JsfManagedBeanMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return JsfManagedBeanMetadata.getMetadataIdentiferType();
	}
}