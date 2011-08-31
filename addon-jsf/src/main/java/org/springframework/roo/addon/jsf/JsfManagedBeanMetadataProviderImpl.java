package org.springframework.roo.addon.jsf;

import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.EMBEDDED_FIELD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.PERSISTENT_TYPE;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.model.JavaType.BOOLEAN_OBJECT;
import static org.springframework.roo.model.JavaType.BOOLEAN_PRIMITIVE;
import static org.springframework.roo.model.JavaType.BYTE_ARRAY_PRIMITIVE;
import static org.springframework.roo.model.JavaType.INT_PRIMITIVE;
import static org.springframework.roo.model.RooJavaType.ROO_JSF_MANAGED_BEAN;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
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
	private static final int LAYER_POSITION = LayerType.HIGHEST.getPosition();

	// Fields
	@Reference private LayerService layerService;
	@Reference private PersistenceMemberLocator persistenceMemberLocator;
	@Reference private TypeLocationService typeLocationService;
	private final Map<JavaType, String> entityToManagedBeanMidMap = new LinkedHashMap<JavaType, String>();
	private final Map<String, JavaType> managedBeanMidToEntityMap = new LinkedHashMap<String, JavaType>();


	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_JSF_MANAGED_BEAN);
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_JSF_MANAGED_BEAN);
	}

	@Override
	protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
		// Determine the governor for this ITD, and whether any metadata is even hoping to hear about changes to that JavaType and its ITDs
		return entityToManagedBeanMidMap.get(itdTypeDetails.getName());
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataIdentificationString, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
		// We need to parse the annotation, which we expect to be present
		final JsfManagedBeanAnnotationValues annotationValues = new JsfManagedBeanAnnotationValues(governorPhysicalTypeMetadata);
		final JavaType entityType = annotationValues.getEntity();
		if (!annotationValues.isAnnotationFound() || entityType == null) {
			return null;
		}

		// Lookup the entity's metadata
		final MemberDetails memberDetails = getMemberDetails(entityType);
		if (memberDetails == null) {
			return null;
		}

		final MemberHoldingTypeDetails persistenceMemberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(memberDetails, PERSISTENT_TYPE);
		if (persistenceMemberHoldingTypeDetails == null) {
			return null;
		}

		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(persistenceMemberHoldingTypeDetails.getDeclaredByMetadataId(), metadataIdentificationString);

		// Remember that this entity JavaType matches up with this metadata identification string
		// Start by clearing the previous association
		final JavaType oldEntity = managedBeanMidToEntityMap.get(metadataIdentificationString);
		if (oldEntity != null) {
			entityToManagedBeanMidMap.remove(oldEntity);
		}
		entityToManagedBeanMidMap.put(entityType, metadataIdentificationString);
		managedBeanMidToEntityMap.put(metadataIdentificationString, entityType);

		final PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(entityType));
		Assert.notNull(pluralMetadata, "Could not determine plural for '" + entityType.getSimpleTypeName() + "'");
		final String plural = pluralMetadata.getPlural();

		final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions = getCrudAdditions(entityType, metadataIdentificationString);
		final Map<FieldMetadata, MethodMetadata> locatedFieldsAndAccessors = locateFieldsAndAccessors(entityType, memberDetails, metadataIdentificationString);
		final Set<FieldMetadata> enumTypes = getEnumTypes(locatedFieldsAndAccessors);
		final MethodMetadata identifierAccessor = persistenceMemberLocator.getIdentifierAccessor(entityType);

		return new JsfManagedBeanMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, plural, crudAdditions, locatedFieldsAndAccessors, enumTypes, identifierAccessor);
	}

	/**
	 * Returns the additions to make to the generated ITD in order to invoke the
	 * various CRUD methods of the given entity
	 * 
	 * @param entity the target entity type (required)
	 * @param metadataId the ID of the metadata that's being created (required)
	 * @return a non-<code>null</code> map (may be empty if the CRUD methods are indeterminable)
	 */
	@SuppressWarnings("unchecked") 
	private Map<MethodMetadataCustomDataKey, MemberTypeAdditions> getCrudAdditions(final JavaType entity, final String metadataId) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.createIdentifier(entity), metadataId);
		final List<FieldMetadata> idFields = persistenceMemberLocator.getIdentifierFields(entity);
		if (idFields.isEmpty()) {
			return Collections.emptyMap();
		}
		final FieldMetadata identifierField = idFields.get(0);
		final JavaType idType = persistenceMemberLocator.getIdentifierType(entity);
		if (idType == null) {
			return Collections.emptyMap();
		}
		metadataDependencyRegistry.registerDependency(identifierField.getDeclaredByMetadataId(), metadataId);

		final JavaSymbolName entityName = JavaSymbolName.getReservedWordSafeName(entity);
		final Pair<JavaType, JavaSymbolName> entityParameter = new Pair<JavaType, JavaSymbolName>(entity, entityName);
		final Pair<JavaType, JavaSymbolName> idParameter = new Pair<JavaType, JavaSymbolName>(idType, new JavaSymbolName("id"));
		final PairList<JavaType, JavaSymbolName> findEntriesParameters = new PairList<JavaType, JavaSymbolName>(Arrays.asList(INT_PRIMITIVE, INT_PRIMITIVE), Arrays.asList(new JavaSymbolName("firstResult"), new JavaSymbolName("sizeNo")));

		final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> additions = new HashMap<MethodMetadataCustomDataKey, MemberTypeAdditions>();
		additions.put(COUNT_ALL_METHOD, layerService.getMemberTypeAdditions(metadataId, COUNT_ALL_METHOD.name(), entity, idType, LAYER_POSITION));
		additions.put(FIND_ALL_METHOD, layerService.getMemberTypeAdditions(metadataId, FIND_ALL_METHOD.name(), entity, idType, LAYER_POSITION));
		additions.put(FIND_ENTRIES_METHOD, layerService.getMemberTypeAdditions(metadataId, FIND_ENTRIES_METHOD.name(), entity, idType, LAYER_POSITION, findEntriesParameters.toArray()));
		additions.put(FIND_METHOD, layerService.getMemberTypeAdditions(metadataId, FIND_METHOD.name(), entity, idType, LAYER_POSITION, idParameter));
		additions.put(MERGE_METHOD, layerService.getMemberTypeAdditions(metadataId, MERGE_METHOD.name(), entity, idType, LAYER_POSITION, entityParameter));
		additions.put(PERSIST_METHOD, layerService.getMemberTypeAdditions(metadataId, PERSIST_METHOD.name(), entity, idType, LAYER_POSITION, entityParameter));
		additions.put(REMOVE_METHOD, layerService.getMemberTypeAdditions(metadataId, REMOVE_METHOD.name(), entity, idType, LAYER_POSITION, entityParameter));
		return additions;
	}
	
	private Map<FieldMetadata, MethodMetadata> locateFieldsAndAccessors(final JavaType entityType, final MemberDetails memberDetails, final String metadataIdentificationString) {
		final Map<FieldMetadata, MethodMetadata> locatedFieldsAndAccessors = new LinkedHashMap<FieldMetadata, MethodMetadata>();
		
		final MethodMetadata identifierAccessor = persistenceMemberLocator.getIdentifierAccessor(entityType);
		final MethodMetadata versionAccessor = persistenceMemberLocator.getVersionAccessor(entityType);

		int counter = 0;
		for (final MethodMetadata method : MemberFindingUtils.getMethods(memberDetails)) {
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
				final CustomDataBuilder customDataBuilder = new CustomDataBuilder();
				customDataBuilder.put("converterField", "true");
				final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(field);
				fieldBuilder.append(customDataBuilder.build());
				field = fieldBuilder.build();
			}
			
			locatedFieldsAndAccessors.put(field, method);
		}
		return locatedFieldsAndAccessors;
	}
	
	private Set<FieldMetadata> getEnumTypes(final Map<FieldMetadata, MethodMetadata> locatedFieldsAndAccessors) {
		final Set<FieldMetadata> enumTypes = new HashSet<FieldMetadata>();
		for (final FieldMetadata field : locatedFieldsAndAccessors.keySet()) {
			final ClassOrInterfaceTypeDetails cid = typeLocationService.findClassOrInterface(field.getFieldType());
			if (cid != null && cid.getPhysicalTypeCategory().equals(PhysicalTypeCategory.ENUMERATION)) {
				enumTypes.add(field);
			}
		}
		return enumTypes;
	}

	private boolean isPersistenceIdentifierOrVersionMethod(final MethodMetadata method, final MethodMetadata idMethod, final MethodMetadata versionMethod) {
		Assert.notNull(method, "Method metadata required");
		return (idMethod != null && method.getMethodName().equals(idMethod.getMethodName())) || (versionMethod != null && method.getMethodName().equals(versionMethod.getMethodName()));
	}
	
	private boolean isFieldOfInterest(final FieldMetadata field) {
		final JavaType fieldType = field.getFieldType();
		if (fieldType.isCommonCollectionType() || fieldType.isArray() // Exclude collections and arrays
				|| isApplicationType(fieldType) // Exclude references to other domain objects as they are too verbose
				|| fieldType.equals(BOOLEAN_PRIMITIVE) || fieldType.equals(BOOLEAN_OBJECT) // Exclude boolean values as they would not be meaningful in this presentation
				|| fieldType.equals(BYTE_ARRAY_PRIMITIVE) // Exclude byte[] fields
				|| field.getCustomData().keySet().contains(EMBEDDED_FIELD) /* Not interested in embedded types */) {
			return false;
		}
		return true;
	}

	public boolean isApplicationType(final JavaType javaType) {
		Assert.notNull(javaType, "Java type required");
		return metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType)) != null;
	}

	public String getItdUniquenessFilenameSuffix() {
		return "ManagedBean";
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
		final JavaType javaType = JsfManagedBeanMetadata.getJavaType(metadataIdentificationString);
		final Path path = JsfManagedBeanMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	@Override
	protected String createLocalIdentifier(final JavaType javaType, final Path path) {
		return JsfManagedBeanMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return JsfManagedBeanMetadata.getMetadataIdentiferType();
	}
}