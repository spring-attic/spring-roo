package org.springframework.roo.addon.jsf;

import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.EMBEDDED_FIELD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.FIND_ALL_METHOD;
import static org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys.PERSISTENT_TYPE;
import static org.springframework.roo.model.JavaType.BOOLEAN_OBJECT;
import static org.springframework.roo.model.JavaType.BOOLEAN_PRIMITIVE;
import static org.springframework.roo.model.JavaType.BYTE_ARRAY_PRIMITIVE;
import static org.springframework.roo.model.RooJavaType.ROO_JSF_CONVERTER;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
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
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;

/**
 * Implementation of {@link JsfConverterMetadataProvider}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component(immediate = true) 
@Service 
public final class JsfConverterMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider implements JsfConverterMetadataProvider {

	// Constants
	private static final int LAYER_POSITION = LayerType.HIGHEST.getPosition();
	// -- The maximum number of entity fields to show in a list view.
	private static final int MAX_LIST_VIEW_FIELDS = 4;

	// Fields
	@Reference private LayerService layerService;
	@Reference private PersistenceMemberLocator persistenceMemberLocator;
	@Reference private TypeLocationService typeLocationService;
	private final Map<JavaType, String> entityToConverterMidMap = new LinkedHashMap<JavaType, String>();
	private final Map<String, JavaType> converterMidToEntityMap = new LinkedHashMap<String, JavaType>();

	protected void activate(final ComponentContext context) {
		metadataDependencyRegistry.addNotificationListener(this);
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		addMetadataTrigger(ROO_JSF_CONVERTER);
	}

	protected void deactivate(final ComponentContext context) {
		metadataDependencyRegistry.removeNotificationListener(this);
		metadataDependencyRegistry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
		removeMetadataTrigger(ROO_JSF_CONVERTER);
	}

	@Override
	protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
		// Determine the governor for this ITD, and whether any metadata is even hoping to hear about changes to that JavaType and its ITDs
		JavaType governor = itdTypeDetails.getName();
		String localMid = entityToConverterMidMap.get(governor);
		if (localMid != null) {
			return localMid;
		}

		MemberHoldingTypeDetails memberHoldingTypeDetails = typeLocationService.findClassOrInterface(itdTypeDetails.getGovernor().getName());
		if (memberHoldingTypeDetails != null && memberHoldingTypeDetails.getCustomData().get(LayerCustomDataKeys.LAYER_TYPE) != null) {
			@SuppressWarnings("unchecked")
			List<JavaType> domainTypes = (List<JavaType>) memberHoldingTypeDetails.getCustomData().get(LayerCustomDataKeys.LAYER_TYPE);
			if (domainTypes != null) {
				for (JavaType type : domainTypes) {
					String localMidType = entityToConverterMidMap.get(type);
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
		final JsfConverterAnnotationValues annotationValues = new JsfConverterAnnotationValues(governorPhysicalTypeMetadata);
		final JavaType entity = annotationValues.getEntity();
		if (!annotationValues.isAnnotationFound() || entity == null) {
			return null;
		}

		// Lookup the entity's metadata
		final MemberDetails memberDetails = getMemberDetails(entity);
		final MemberHoldingTypeDetails persistenceMemberHoldingTypeDetails = MemberFindingUtils.getMostConcreteMemberHoldingTypeDetailsWithTag(memberDetails, PERSISTENT_TYPE);
		if (persistenceMemberHoldingTypeDetails == null) {
			return null;
		}

		// We need to be informed if our dependent metadata changes
		metadataDependencyRegistry.registerDependency(persistenceMemberHoldingTypeDetails.getDeclaredByMetadataId(), metadataId);

		// Remember that this entity JavaType matches up with this metadata identification string
		// Start by clearing any previous association
		final JavaType oldEntity = converterMidToEntityMap.get(metadataId);
		if (oldEntity != null) {
			entityToConverterMidMap.remove(oldEntity);
		}
		entityToConverterMidMap.put(entity, metadataId);
		converterMidToEntityMap.put(metadataId, entity);

		final MemberTypeAdditions findAllMethod = getFindAllMethod(entity, metadataId);
		final List<MethodMetadata> converterMethods = getConverterMethods(entity, memberDetails, metadataId);

		return new JsfConverterMetadata(metadataId, aspectName, governorPhysicalTypeMetadata, annotationValues, findAllMethod, converterMethods);
	}

	private MemberTypeAdditions getFindAllMethod(final JavaType entity, final String metadataId) {
		metadataDependencyRegistry.registerDependency(PhysicalTypeIdentifier.createIdentifier(entity), metadataId);
		final List<FieldMetadata> idFields = persistenceMemberLocator.getIdentifierFields(entity);
		if (idFields.isEmpty()) {
			return null;
		}
		final FieldMetadata identifierField = idFields.get(0);
		final JavaType identifierType = persistenceMemberLocator.getIdentifierType(entity);
		if (identifierType == null) {
			return null;
		}
		metadataDependencyRegistry.registerDependency(identifierField.getDeclaredByMetadataId(), metadataId);
		
		return layerService.getMemberTypeAdditions(metadataId, FIND_ALL_METHOD.name(), entity, identifierType, LAYER_POSITION);
	}
	
	private List<MethodMetadata> getConverterMethods(final JavaType entity, final MemberDetails memberDetails, final String metadataIdentificationString) {
		final List<MethodMetadata> converterMethods = new ArrayList<MethodMetadata>();
		
		final MethodMetadata identifierAccessor = persistenceMemberLocator.getIdentifierAccessor(entity);
		final MethodMetadata versionAccessor = persistenceMemberLocator.getVersionAccessor(entity);

		int converterMethodCount = 0;
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
			if (converterMethodCount <= MAX_LIST_VIEW_FIELDS && isDisplayableInListView(field)) {
				converterMethodCount++;
				converterMethods.add(method);
			}
			
		}
		return converterMethods;
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
		return "Converter";
	}

	@Override
	protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
		final JavaType javaType = JsfConverterMetadata.getJavaType(metadataIdentificationString);
		final Path path = JsfConverterMetadata.getPath(metadataIdentificationString);
		return PhysicalTypeIdentifier.createIdentifier(javaType, path);
	}

	@Override
	protected String createLocalIdentifier(final JavaType javaType, final Path path) {
		return JsfConverterMetadata.createIdentifier(javaType, path);
	}

	public String getProvidesType() {
		return JsfConverterMetadata.getMetadataIdentiferType();
	}
}