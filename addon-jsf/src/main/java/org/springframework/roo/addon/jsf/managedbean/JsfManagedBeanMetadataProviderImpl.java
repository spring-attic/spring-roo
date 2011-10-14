package org.springframework.roo.addon.jsf.managedbean;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.EMBEDDED_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSISTENT_TYPE;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.model.JavaType.BOOLEAN_OBJECT;
import static org.springframework.roo.model.JavaType.BOOLEAN_PRIMITIVE;
import static org.springframework.roo.model.JavaType.BYTE_ARRAY_PRIMITIVE;
import static org.springframework.roo.model.JavaType.INT_PRIMITIVE;
import static org.springframework.roo.model.RooJavaType.ROO_JSF_MANAGED_BEAN;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.configurable.ConfigurableMetadataProvider;
import org.springframework.roo.addon.jsf.model.JsfFieldHolder;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.layers.LayerService;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
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
public class JsfManagedBeanMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider implements JsfManagedBeanMetadataProvider {

	// Constants
	private static final int LAYER_POSITION = LayerType.HIGHEST.getPosition();
	// -- The maximum number of entity fields to show in a list view.
	private static final int MAX_LIST_VIEW_FIELDS = 4;

	// Fields
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	@Reference private LayerService layerService;
	@Reference private TypeLocationService typeLocationService;
	private final Map<JavaType, String> entityToManagedBeanMidMap = new LinkedHashMap<JavaType, String>();
	private final Map<String, JavaType> managedBeanMidToEntityMap = new LinkedHashMap<String, JavaType>();

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_JSF_MANAGED_BEAN);
		configurableMetadataProvider.addMetadataTrigger(ROO_JSF_MANAGED_BEAN);
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_JSF_MANAGED_BEAN);
		configurableMetadataProvider.removeMetadataTrigger(ROO_JSF_MANAGED_BEAN);
	}

	@Override
	protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
		// Determine the governor for this ITD, and whether any metadata is even hoping to hear about changes to that JavaType and its ITDs
		JavaType governor = itdTypeDetails.getName();
		String localMid = entityToManagedBeanMidMap.get(governor);
		if (localMid != null) {
			return localMid;
		}

		final MemberHoldingTypeDetails memberHoldingTypeDetails = typeLocationService.findClassOrInterface(itdTypeDetails.getGovernor().getName());
		if (memberHoldingTypeDetails != null) {
			for (final JavaType type : memberHoldingTypeDetails.getLayerEntities()) {
				final String localMidType = entityToManagedBeanMidMap.get(type);
				if (localMidType != null) {
					return localMidType;
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

		final MethodMetadata identifierAccessor = persistenceMemberLocator.getIdentifierAccessor(entity);
		final MethodMetadata versionAccessor = persistenceMemberLocator.getVersionAccessor(entity);
		final Set<JsfFieldHolder> locatedFields = locateFields(entity, memberDetails, metadataId, identifierAccessor, versionAccessor);
		if (locatedFields.isEmpty()) {
			return null;
		}

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

		return new JsfManagedBeanMetadata(metadataId, aspectName, governorPhysicalTypeMetadata, annotationValues, plural, crudAdditions, locatedFields, identifierAccessor);
	}

	/**
	 * Returns an iterable collection of the given entity's fields
	 * excluding any ID or version field; along the way, flags the first
	 * {@value #MAX_LIST_VIEW_FIELDS} non ID/version fields as being displayable in
	 * the list view for this entity type.
	 *
	 * @param entity the entity for which to find the fields and accessors (required)
	 * @param memberDetails the entity's members (required)
	 * @param metadataIdentificationString the ID of the metadata being generated (required)
	 * @param versionAccessor
	 * @param identifierAccessor
	 * @return a non-<code>null</code> iterable collection
	 */
	private Set<JsfFieldHolder> locateFields(final JavaType entity, final MemberDetails memberDetails, final String metadataId, final MethodMetadata identifierAccessor, final MethodMetadata versionAccessor) {
		final Set<JsfFieldHolder> locatedFields = new LinkedHashSet<JsfFieldHolder>();
		Set<ClassOrInterfaceTypeDetails> managedBeans = typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ROO_JSF_MANAGED_BEAN);

		int listViewFields = 0;
		for (final MethodMetadata method : memberDetails.getMethods()) {
			if (!BeanInfoUtils.isAccessorMethod(method)) {
				continue;
			}
			if (method.hasSameName(identifierAccessor, versionAccessor)) {
				continue;
			}
			FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(memberDetails, BeanInfoUtils.getPropertyNameForJavaBeanMethod(method));
			if (field == null) {
				continue;
			}
			metadataDependencyRegistry.registerDependency(field.getDeclaredByMetadataId(), metadataId);

			// Check field is to be displayed in the entity's list view
			if (listViewFields < MAX_LIST_VIEW_FIELDS && isDisplayableInListView(field)) {
				listViewFields++;
				final CustomDataBuilder customDataBuilder = new CustomDataBuilder();
				customDataBuilder.put(JsfManagedBeanMetadataProvider.LIST_VIEW_FIELD_CUSTOM_DATA_KEY, "true");
				final FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(field);
				fieldBuilder.append(customDataBuilder.build());
				field = fieldBuilder.build();
			}

			final JavaType fieldType = field.getFieldType();
			final boolean enumerated = field.getCustomData().keySet().contains(CustomDataKeys.ENUMERATED_FIELD) || isEnum(fieldType);
			final Map<JavaType, String> genericTypes = new LinkedHashMap<JavaType, String>();
			String genericTypePlural = null;
			MemberDetails applicationTypeMemberDetails = null;
			final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions = new LinkedHashMap<MethodMetadataCustomDataKey, MemberTypeAdditions>();
			
			if (!enumerated) {
				if (fieldType.isCommonCollectionType()) {
					genericTypeLoop: for (JavaType genericType : fieldType.getParameters()) {
						if (isApplicationType(genericType)) {
							for (ClassOrInterfaceTypeDetails managedBean : managedBeans) {
								AnnotationMetadata managedBeanAnnotation = managedBean.getAnnotation(ROO_JSF_MANAGED_BEAN);
								AnnotationAttributeValue<?> entityAttribute = managedBeanAnnotation.getAttribute("entity");
								if (entityAttribute != null) {
									JavaType attrValue = (JavaType) entityAttribute.getValue();
									if (attrValue.equals(genericType)) {
										AnnotationAttributeValue<?> beanNameAttribute = managedBeanAnnotation.getAttribute("beanName");
										genericTypes.put(genericType, (String) beanNameAttribute.getValue());
										final PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(genericType));
										genericTypePlural = pluralMetadata.getPlural();
										break genericTypeLoop; // Only support one generic type parameter
									}
								}
							}
							// Generic type is not an entity - test for an enum
							if (isEnum(genericType)) {
								genericTypes.put(genericType, "");
							}
						}
					}
				} else {
					if (isApplicationType(fieldType) && !field.getCustomData().keySet().contains(CustomDataKeys.EMBEDDED_FIELD)) {
						applicationTypeMemberDetails = getMemberDetails(fieldType);
						crudAdditions.putAll(getCrudAdditions(fieldType, metadataId));
					}
				}
			}

			final JsfFieldHolder jsfFieldHolder = new JsfFieldHolder(field, enumerated, genericTypePlural, genericTypes, applicationTypeMemberDetails, crudAdditions);
			locatedFields.add(jsfFieldHolder);
		}

		return locatedFields;
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

	private boolean isApplicationType(final JavaType fieldType) {
		return metadataService.get(PhysicalTypeIdentifier.createIdentifier(fieldType)) != null;
	}
	
	private boolean isEnum(final JavaType fieldType) {
		ClassOrInterfaceTypeDetails cid = typeLocationService.findClassOrInterface(fieldType);
		return cid != null && cid.getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION;
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
			&& !field.getCustomData().keySet().contains(EMBEDDED_FIELD);
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