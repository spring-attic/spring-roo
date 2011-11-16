package org.springframework.roo.addon.jsf.managedbean;

import static org.springframework.roo.classpath.customdata.CustomDataKeys.COUNT_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.EMBEDDED_FIELD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_ENTRIES_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.FIND_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.MERGE_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.PERSIST_METHOD;
import static org.springframework.roo.classpath.customdata.CustomDataKeys.REMOVE_METHOD;
import static org.springframework.roo.model.JavaType.BOOLEAN_OBJECT;
import static org.springframework.roo.model.JavaType.BOOLEAN_PRIMITIVE;
import static org.springframework.roo.model.JavaType.BYTE_ARRAY_PRIMITIVE;
import static org.springframework.roo.model.JavaType.INT_PRIMITIVE;
import static org.springframework.roo.model.RooJavaType.ROO_JSF_MANAGED_BEAN;

import java.util.ArrayList;
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
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.ItdTypeDetails;
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
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
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
	private static final int MAX_LIST_VIEW_FIELDS = 7;
	// -- The maximum number of fields to form a String to show in a drop down field.
	private static final int MAX_DROP_DOWN_FIELDS = 4;

	// Fields
	@Reference private ConfigurableMetadataProvider configurableMetadataProvider;
	@Reference private LayerService layerService;
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

		final MemberHoldingTypeDetails memberHoldingTypeDetails = typeLocationService.getTypeDetails(governor);
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
	protected ItdTypeDetailsProvidingMetadataItem getMetadata(final String metadataIdentificationString, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {
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

		final MethodMetadata identifierAccessor = persistenceMemberLocator.getIdentifierAccessor(entity);
		final MethodMetadata versionAccessor = persistenceMemberLocator.getVersionAccessor(entity);
		final Set<JsfFieldHolder> locatedFields = locateFields(entity, memberDetails, metadataIdentificationString, identifierAccessor, versionAccessor);

		// Remember that this entity JavaType matches up with this metadata identification string
		// Start by clearing any previous association
		final JavaType oldEntity = managedBeanMidToEntityMap.get(metadataIdentificationString);
		if (oldEntity != null) {
			entityToManagedBeanMidMap.remove(oldEntity);
		}
		entityToManagedBeanMidMap.put(entity, metadataIdentificationString);
		managedBeanMidToEntityMap.put(metadataIdentificationString, entity);

		String physicalTypeIdentifier = typeLocationService.getPhysicalTypeIdentifier(entity);
		LogicalPath path = PhysicalTypeIdentifier.getPath(physicalTypeIdentifier);
		final PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(entity, path));
		Assert.notNull(pluralMetadata, "Could not determine plural for '" + entity.getSimpleTypeName() + "'");
		final String plural = pluralMetadata.getPlural();

		final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions = getCrudAdditions(entity, metadataIdentificationString);

		return new JsfManagedBeanMetadata(metadataIdentificationString, aspectName, governorPhysicalTypeMetadata, annotationValues, plural, crudAdditions, locatedFields, identifierAccessor);
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

			final JavaType fieldType = field.getFieldType();

			// Check field is to be displayed in the entity's list view
			boolean listViewField = false;
			if (listViewFields < MAX_LIST_VIEW_FIELDS && isFieldOfInterest(field)) {
				listViewFields++;
				listViewField = true;
			}

			final boolean enumerated = field.getCustomData().keySet().contains(CustomDataKeys.ENUMERATED_FIELD) || isEnum(fieldType);
			JavaType genericType = null;
			String genericTypeBeanName = null;
			String genericTypePlural = null;
			boolean applicationType = false;
			final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions = new LinkedHashMap<MethodMetadataCustomDataKey, MemberTypeAdditions>();
			final List<FieldMetadata> applicationTypeFields = new ArrayList<FieldMetadata>();
			
			if (!enumerated) {
				if (fieldType.isCommonCollectionType()) {
					genericTypeLoop: for (JavaType parameter : fieldType.getParameters()) {
						if (!typeLocationService.isInProject(parameter)) {
							continue;
						}
						for (ClassOrInterfaceTypeDetails managedBean : managedBeans) {
							AnnotationMetadata managedBeanAnnotation = managedBean.getAnnotation(ROO_JSF_MANAGED_BEAN);
							AnnotationAttributeValue<?> entityAttribute = managedBeanAnnotation.getAttribute("entity");
							if (entityAttribute != null) {
								JavaType attrValue = (JavaType) entityAttribute.getValue();
								if (attrValue.equals(parameter)) {
									AnnotationAttributeValue<?> beanNameAttribute = managedBeanAnnotation.getAttribute("beanName");
									genericType = parameter;
									genericTypeBeanName = (String) beanNameAttribute.getValue();
									ClassOrInterfaceTypeDetails genericTypeDetails = typeLocationService.getTypeDetails(genericType);
									Assert.notNull(genericTypeDetails, "The type '" + genericType + "' could not be resolved");
									LogicalPath path = PhysicalTypeIdentifier.getPath(genericTypeDetails.getDeclaredByMetadataId());
									final PluralMetadata pluralMetadata = (PluralMetadata) metadataService.get(PluralMetadata.createIdentifier(genericType, path));
									genericTypePlural = pluralMetadata.getPlural();
									break genericTypeLoop; // Only support one generic type parameter
								}
							}
							// Generic type is not an entity - test for an enum
							if (isEnum(parameter)) {
								genericType = parameter;
							}
						}
					}
				} else {
					if (typeLocationService.isInProject(fieldType) && !field.getCustomData().keySet().contains(CustomDataKeys.EMBEDDED_FIELD)) {
						applicationType = true;
						int dropDownFields = 0;
						MemberDetails applicationTypeMemberDetails = getMemberDetails(fieldType);
						final MethodMetadata applicationTypeIdentifierAccessor = persistenceMemberLocator.getIdentifierAccessor(entity);
						final MethodMetadata applicationTypeVersionAccessor = persistenceMemberLocator.getVersionAccessor(entity);

						for (final MethodMetadata applicationTypeMethod : applicationTypeMemberDetails.getMethods()) {
							if (!BeanInfoUtils.isAccessorMethod(applicationTypeMethod)) {
								continue;
							}
							if (applicationTypeMethod.hasSameName(applicationTypeIdentifierAccessor, applicationTypeVersionAccessor)) {
								continue;
							}
							FieldMetadata applicationTypeField = BeanInfoUtils.getFieldForJavaBeanMethod(applicationTypeMemberDetails, applicationTypeMethod);
							if (applicationTypeField == null) {
								continue;
							}

							if (dropDownFields < MAX_DROP_DOWN_FIELDS && isFieldOfInterest(applicationTypeField)) {
								dropDownFields++;
								applicationTypeFields.add(applicationTypeField);
							}
						}
						
						if (applicationTypeFields.isEmpty()) {
							applicationTypeFields.add(BeanInfoUtils.getFieldForJavaBeanMethod(applicationTypeMemberDetails, applicationTypeIdentifierAccessor));
						}

						crudAdditions.putAll(getCrudAdditions(fieldType, metadataId));
					}
				}
			}

			final JsfFieldHolder jsfFieldHolder = new JsfFieldHolder(field, enumerated, listViewField, genericType, genericTypePlural, genericTypeBeanName, applicationType, applicationTypeFields, crudAdditions);
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
		metadataDependencyRegistry.registerDependency(typeLocationService.getPhysicalTypeIdentifier(entity), metadataId);
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

	private boolean isEnum(final JavaType fieldType) {
		ClassOrInterfaceTypeDetails cid = typeLocationService.getTypeDetails(fieldType);
		return cid != null && cid.getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION;
	}

	private boolean isFieldOfInterest(final FieldMetadata field) {
		final JavaType fieldType = field.getFieldType();
		return !fieldType.isCommonCollectionType() && !fieldType.isArray() // Exclude collections and arrays
			&& !typeLocationService.isInProject(fieldType) // Exclude references to other domain objects as they are too verbose
			&& !fieldType.equals(BOOLEAN_PRIMITIVE) && !fieldType.equals(BOOLEAN_OBJECT) // Boolean values would not be meaningful in this presentation
			&& !fieldType.equals(BYTE_ARRAY_PRIMITIVE)
			&& !field.getCustomData().keySet().contains(EMBEDDED_FIELD); // Not interested in embedded types
	}

	public String getItdUniquenessFilenameSuffix() {
		return "ManagedBean";
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
		final JavaType javaType = JsfManagedBeanMetadata.getJavaType(metadataIdentificationString);
		final LogicalPath path = JsfManagedBeanMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	@Override
	protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
		return JsfManagedBeanMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return JsfManagedBeanMetadata.getMetadataIdentiferType();
	}
}