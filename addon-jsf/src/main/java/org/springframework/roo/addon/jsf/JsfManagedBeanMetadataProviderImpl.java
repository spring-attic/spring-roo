package org.springframework.roo.addon.jsf;

import static org.springframework.roo.classpath.PhysicalTypeCategory.ENUMERATION;
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
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
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
import org.springframework.roo.classpath.layers.LayerCustomDataKeys;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.CustomDataBuilder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;

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
	// -- The maximum number of entity fields to show in a list view.
	private static final int MAX_LIST_VIEW_FIELDS = 4;

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
		JavaType governor = itdTypeDetails.getName();
		String localMid = entityToManagedBeanMidMap.get(governor);
		if (localMid != null) {
			return localMid;
		}

		MemberHoldingTypeDetails memberHoldingTypeDetails = typeLocationService.findClassOrInterface(itdTypeDetails.getGovernor().getName());
		if (memberHoldingTypeDetails != null && memberHoldingTypeDetails.getCustomData().get(LayerCustomDataKeys.LAYER_TYPE) != null) {
			@SuppressWarnings("unchecked")
			List<JavaType> domainTypes = (List<JavaType>) memberHoldingTypeDetails.getCustomData().get(LayerCustomDataKeys.LAYER_TYPE);
			if (domainTypes != null) {
				for (JavaType type : domainTypes) {
					String localMidType = entityToManagedBeanMidMap.get(type);
					if (localMidType != null) {
						return localMidType;
					}
				}
			}
		}
		return null;
	}

	@Override
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataId, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
		// We need to parse the annotation, which we expect to be present
		final JsfManagedBeanAnnotationValues annotationValues = new JsfManagedBeanAnnotationValues(governorPhysicalTypeMetadata);
		final JavaType entity = annotationValues.getEntity();
		if (!annotationValues.isAnnotationFound() || entity == null) {
			return null;
		}

		final MemberDetails memberDetails = getMemberDetails(entity);
		if (memberDetails == null) {
			return null;
		}
		
		final MemberHoldingTypeDetails persistenceMemberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(memberDetails, PERSISTENT_TYPE);
		if (persistenceMemberHoldingTypeDetails == null) {
			return null;
		}

		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(persistenceMemberHoldingTypeDetails.getDeclaredByMetadataId(), metadataId);

		// Remember that this entity JavaType matches up with this metadata identification string
		// Start by clearing any previous association
		final JavaType oldEntity = managedBeanMidToEntityMap.get(metadataId);
		if (oldEntity != null) {
			entityToManagedBeanMidMap.remove(oldEntity);
		}
		entityToManagedBeanMidMap.put(entity, metadataId);
		managedBeanMidToEntityMap.put(metadataId, entity);

		final PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(entity));
		Assert.notNull(pluralMetadata, "Could not determine plural for '" + entity.getSimpleTypeName() + "'");
		final String plural = pluralMetadata.getPlural();

		final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions = getCrudAdditions(entity, metadataId);
		final Map<FieldMetadata, MethodMetadata> locatedFieldsAndAccessors = locateFieldsAndAccessors(entity, memberDetails, metadataId);
		final MethodMetadata identifierAccessor = persistenceMemberLocator.getIdentifierAccessor(entity);

		return new JsfManagedBeanMetadata(metadataId, aspectName, governorPhysicalTypeMetadata, annotationValues, plural, crudAdditions, locatedFieldsAndAccessors, identifierAccessor);
	}

	/**
	 * Returns the additions to make to the generated ITD in order to invoke the
	 * various CRUD methods of the given entity
	 * 
	 * @param entity the target entity type (required)
	 * @param metadataId the ID of the metadata that's being created (required)
	 * @return a non-<code>null</code> map (may be empty if the CRUD methods are indeterminable)
	 */
	private Map<MethodMetadataCustomDataKey, MemberTypeAdditions> getCrudAdditions(final JavaType entity, final String metadataId) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.createIdentifier(entity), metadataId);
		final List<FieldMetadata> idFields = persistenceMemberLocator.getIdentifierFields(entity);
		if (idFields.isEmpty()) {
			return Collections.emptyMap();
		}
		final FieldMetadata identifierField = idFields.get(0);
		final JavaType identifierType = persistenceMemberLocator.getIdentifierType(entity);
		if (identifierType == null) {
			return Collections.emptyMap();
		}
		metadataDependencyRegistry.registerDependency(identifierField.getDeclaredByMetadataId(), metadataId);

		final JavaSymbolName entityName = JavaSymbolName.getReservedWordSafeName(entity);
		final MethodParameter entityParameter = new MethodParameter(entity, entityName);
		final MethodParameter idParameter = new MethodParameter(identifierType, "id");
		final MethodParameter firstResultParameter = new MethodParameter(INT_PRIMITIVE, "firstResult");
		final MethodParameter maxResultsParameter = new MethodParameter(INT_PRIMITIVE, "sizeNo");
		
		final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> additions = new HashMap<MethodMetadataCustomDataKey, MemberTypeAdditions>();
		additions.put(COUNT_ALL_METHOD, layerService.getMemberTypeAdditions(metadataId, COUNT_ALL_METHOD.name(), entity, identifierType, LAYER_POSITION));
		additions.put(FIND_ALL_METHOD, layerService.getMemberTypeAdditions(metadataId, FIND_ALL_METHOD.name(), entity, identifierType, LAYER_POSITION));
		additions.put(FIND_ENTRIES_METHOD, layerService.getMemberTypeAdditions(metadataId, FIND_ENTRIES_METHOD.name(), entity, identifierType, LAYER_POSITION, firstResultParameter, maxResultsParameter));
		additions.put(FIND_METHOD, layerService.getMemberTypeAdditions(metadataId, FIND_METHOD.name(), entity, identifierType, LAYER_POSITION, idParameter));
		additions.put(MERGE_METHOD, layerService.getMemberTypeAdditions(metadataId, MERGE_METHOD.name(), entity, identifierType, LAYER_POSITION, entityParameter));
		additions.put(PERSIST_METHOD, layerService.getMemberTypeAdditions(metadataId, PERSIST_METHOD.name(), entity, identifierType, LAYER_POSITION, entityParameter));
		additions.put(REMOVE_METHOD, layerService.getMemberTypeAdditions(metadataId, REMOVE_METHOD.name(), entity, identifierType, LAYER_POSITION, entityParameter));
		return additions;
	}

	/**
	 * Returns a map of the given entity's fields to their accessor methods,
	 * excluding any ID or version field; along the way, flags the first
	 * {@value #MAX_LIST_VIEW_FIELDS} non ID/version fields as being displayable in
	 * the list view for this entity type.
	 * 
	 * @param entity the entity for which to find the fields and accessors (required)
	 * @param memberDetails the entity's members (required)
	 * @param metadataIdentificationString the ID of the metadata being generated (required)
	 * @return a non-<code>null</code> map
	 */
	private Map<FieldMetadata, MethodMetadata> locateFieldsAndAccessors(final JavaType entity, final MemberDetails memberDetails, final String metadataIdentificationString) {
		final Map<FieldMetadata, MethodMetadata> locatedFieldsAndAccessors = new LinkedHashMap<FieldMetadata, MethodMetadata>();
		
		final MethodMetadata identifierAccessor = persistenceMemberLocator.getIdentifierAccessor(entity);
		final MethodMetadata versionAccessor = persistenceMemberLocator.getVersionAccessor(entity);

		int listViewFields = 0;
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

			// Check field is an enum type
			if (!field.getCustomData().keySet().contains(PersistenceCustomDataKeys.ENUMERATED_FIELD)) {
				final ClassOrInterfaceTypeDetails cid = typeLocationService.findClassOrInterface(field.getFieldType());
				if (cid != null && ENUMERATION.equals(cid.getPhysicalTypeCategory())) {
					final CustomDataBuilder customDataBuilder = new CustomDataBuilder();
					customDataBuilder.put(PersistenceCustomDataKeys.ENUMERATED_FIELD, "true");
					final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(field);
					fieldBuilder.append(customDataBuilder.build());
					field = fieldBuilder.build();
				}
			}

			if (listViewFields <= MAX_LIST_VIEW_FIELDS && isDisplayableInListView(field)) {
				listViewFields++;
				// Flag this field as being displayable in the entity's list view
				final CustomDataBuilder customDataBuilder = new CustomDataBuilder();
				customDataBuilder.put(JsfManagedBeanMetadataProvider.LIST_VIEW_FIELD_CUSTOM_DATA_KEY, "true");
				final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(field);
				fieldBuilder.append(customDataBuilder.build());
				field = fieldBuilder.build();
			}
			
			locatedFieldsAndAccessors.put(field, method);
		}
		return locatedFieldsAndAccessors;
	}
	
	/**
	 * Indicates whether the given method is the ID or version accessor
	 * 
	 * @param method the method to check (required)
	 * @param idMethod the ID accessor method (can be <code>null</code>)
	 * @param versionMethod the version accessor method (can be <code>null</code>)
	 * @return see above
	 */
	private boolean isPersistenceIdentifierOrVersionMethod(final MethodMetadata method, final MethodMetadata idMethod, final MethodMetadata versionMethod) {
		return MemberFindingUtils.hasSameName(method, idMethod, versionMethod);
	}
	
	/**
	 * Indicates whether the given field is for display in the entity's list view.
	 * 
	 * @param field the field to check (required)
	 * @return see above
	 */
	private boolean isDisplayableInListView(final FieldMetadata field) {
		final JavaType fieldType = field.getFieldType();
		return !fieldType.isCommonCollectionType()
			&& !fieldType.isArray()
			// Boolean values would not be meaningful in this presentation
			&& !fieldType.equals(BOOLEAN_PRIMITIVE)
			&& !fieldType.equals(BOOLEAN_OBJECT)
			&& !fieldType.equals(BYTE_ARRAY_PRIMITIVE)
			&& !field.getCustomData().keySet().contains(EMBEDDED_FIELD)
			// References to other domain objects would be too verbose
			&& !isApplicationType(fieldType);
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